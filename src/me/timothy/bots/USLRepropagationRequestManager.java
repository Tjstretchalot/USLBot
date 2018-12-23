package me.timothy.bots;

import static me.timothy.bots.ResponseUtils.verifyFormat;

import java.sql.Timestamp;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.custom.CustomDirtyPersonMapping;
import me.timothy.bots.database.custom.CustomHandledAtTimestampMapping;
import me.timothy.bots.database.custom.CustomRedditToMeaningProgressMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionBanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionHashtagMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionUnbanHistoryMapping;
import me.timothy.bots.functions.SendPMFunction;
import me.timothy.bots.functions.SubmitSelfFunction;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.RepropagationRequest;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.PropagatorSetting.PropagatorSettingKey;

/**
 * Manages repropagation requests - that is to say, this handles clearing the database & setting up the 
 * configuration as appropraite for repropagating. This doesn't cause any additional reddit requests, just
 * some cpu time.
 * 
 * @author Timothy
 */
public class USLRepropagationRequestManager {
	private static final Logger logger = LogManager.getLogger();
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected USLDatabaseBackupManager backupManager;
	protected SubmitSelfFunction submitSelf;
	protected SendPMFunction sendPM;
	
	public USLRepropagationRequestManager(USLDatabase database, USLFileConfiguration config, USLDatabaseBackupManager backupManager,
			SubmitSelfFunction submitSelf, SendPMFunction sendPM) {
		this.database = database;
		this.config = config;
		this.backupManager = backupManager;
		this.submitSelf = submitSelf;
		this.sendPM = sendPM;
	}
	
	/**
	 * Verify that the database contains all the responses necessary for this request manager.
	 */
	public void verifyHaveResponses() {
		verifyFormat(database, "reprop_approve_pm_title", "The title for the message we send to the user to tell them we approved their request",
				"user", "The username for the moderator which made the request");
		verifyFormat(database, "reprop_approve_pm_body", "The body for the message we send to the user to tell them we approved their request",
				"user", "The username for the moderator which made the request",
				"reason", "The reason listed for the request");
		verifyFormat(database, "reprop_approve_post_title", "The title for the post to the notification subreddit when we begin to repropagate",
				"user", "The username for the moderator which made the request");
		verifyFormat(database, "reprop_approve_post_body", "The body for the post to the notification subreddit when we begin to repropagate",
				"user", "The username for the moderator which made the request",
				"reason", "The reason listed for the request");
		verifyFormat(database, "reprop_reject_already_repropagating_pm_title", "The title for the message we send to the user to tell them "
				+ "we rejected their request since we are already repropagating",
				"user", "The username for the moderator which made the request");
		verifyFormat(database, "reprop_reject_already_repropagating_pm_body", "The body for the message we send to the user to tell them "
				+ "we rejected their request since we are already repropagating",
				"user", "The username for the moderator which made the request",
				"reason", "The reason listed for the request");
	}
	
	/**
	 * Processes the given request. This may require a large number of database
	 * actions (ie. database truncation) which may take some time.
	 * 
	 * @param request the request to process
	 */
	public void processRequest(RepropagationRequest request) {
		String suppressNoOp = database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES);
		if(suppressNoOp != null && suppressNoOp.equals("true")) {
			rejectAlreadyRepropagating(request);
			return;
		}
		
		approve(request);
	}
	
	/**
	 * Rejects the given request because we are already repropagating. Sends a message to the user but otherwise
	 * does nothing.
	 * 
	 * @param request the request to reject
	 */
	protected void rejectAlreadyRepropagating(RepropagationRequest request) {
		Person requester = database.getPersonMapping().fetchByID(request.requestingPersonID);
		
		String titleFormat = database.getResponseMapping().fetchByName("reprop_reject_already_repropagating_pm_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("reprop_reject_already_repropagating_pm_body").responseBody;
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("user", requester.username);
		respInfo.addTemporaryString("reason", request.reason);
		
		String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
		respInfo.clearTemporary();
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		sendPM.send(new UserPMInformation(requester, title, body));
		
		request.approved = false;
		request.handledAt = new Timestamp(System.currentTimeMillis());
		database.getRepropagationRequestMapping().save(request);
	}
	
	/**
	 * Approves the given request. Sends them a message, posts on the subreddit, backs up the database, clears the
	 * appropriate tables and sets the correct configuration for repropagating, then backs up the database again.
	 * 
	 * @param request the request to approve
	 */
	protected void approve(RepropagationRequest request) {
		Person requester = database.getPersonMapping().fetchByID(request.requestingPersonID);
		
		logger.printf(Level.INFO, "Processing approved repropagation request by %s. Reason: %s", requester.username, request.reason);
		database.getActionLogMapping().append("Setting up to repropagate (reason given: " + request.reason + ")");
		
		String pmTitleFormat = database.getResponseMapping().fetchByName("reprop_approve_pm_title").responseBody;
		String pmBodyFormat = database.getResponseMapping().fetchByName("reprop_approve_pm_body").responseBody;
		String postTitleFormat = database.getResponseMapping().fetchByName("reprop_approve_post_title").responseBody;
		String postBodyFormat = database.getResponseMapping().fetchByName("reprop_approve_post_body").responseBody;
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("user", requester.username);
		respInfo.addTemporaryString("reason", request.reason);
		
		String pmBody = new ResponseFormatter(pmBodyFormat, respInfo).getFormattedResponse(config, database);
		String postBody = new ResponseFormatter(postBodyFormat, respInfo).getFormattedResponse(config, database);
		respInfo.clearTemporary();
		String pmTitle = new ResponseFormatter(pmTitleFormat, respInfo).getFormattedResponse(config, database);
		String postTitle = new ResponseFormatter(postTitleFormat, respInfo).getFormattedResponse(config, database);
		String postSub = config.getProperty("general.notifications_sub");
		
		sendPM.send(new UserPMInformation(requester, pmTitle, pmBody));
		submitSelf.submitSelf(postSub, postTitle, postBody);
		
		backupManager.forceBackup();
		
		MysqlUSLActionMapping actMap = (MysqlUSLActionMapping)database.getUSLActionMapping();
		actMap.truncate();
		
		MysqlUSLActionBanHistoryMapping actBanMap = (MysqlUSLActionBanHistoryMapping)database.getUSLActionBanHistoryMapping();
		actBanMap.truncate();
		
		MysqlUSLActionUnbanHistoryMapping actUnbanMap = (MysqlUSLActionUnbanHistoryMapping)database.getUSLActionUnbanHistoryMapping();
		actUnbanMap.truncate();
		
		MysqlUSLActionHashtagMapping actTagMap = (MysqlUSLActionHashtagMapping)database.getUSLActionHashtagMapping();
		actTagMap.truncate();
		
		CustomDirtyPersonMapping custDirtPersMap = (CustomDirtyPersonMapping)database.getDirtyPersonMapping();
		custDirtPersMap.purgeSchema();
		
		CustomHandledAtTimestampMapping custHatMap = (CustomHandledAtTimestampMapping)database.getHandledAtTimestampMapping();
		custHatMap.purgeSchema();
		
		CustomRedditToMeaningProgressMapping custRtmpMap = (CustomRedditToMeaningProgressMapping)database.getRedditToMeaningProgressMapping();
		custRtmpMap.purgeSchema();
		
		database.getPropagatorSettingMapping().put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "true");
		
		backupManager.forceBackup();
		
		request.approved = true;
		request.handledAt = new Timestamp(System.currentTimeMillis());
		database.getRepropagationRequestMapping().save(request);
	}
}
