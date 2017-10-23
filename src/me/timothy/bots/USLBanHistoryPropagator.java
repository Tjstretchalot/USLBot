package me.timothy.bots;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.timothy.bots.memory.BanHistoryPropagateResult;
import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * The purpose of this class is to propagate our local ban histories
 * to subreddits, according to their configuration.
 * 
 * This class does not manage deciding which banhistory / subreddit 
 * combinations we haven't looked at yet. All it does is, when given
 * a subreddit and a banhistory, ensures the necessary action has 
 * been taken on the subreddit for that BanHistory.
 * 
 * This class does NOT interact with reddit or modify the database!
 * 
 * @author Timothy
 */
public class USLBanHistoryPropagator {
	
	/**
	 * The file configuration
	 */
	protected USLFileConfiguration config;
	
	/**
	 * The database
	 */
	protected USLDatabase database;
	
	/**
	 * Create a new instance of the USLBanHistoryPropagator using configuration
	 * information from config and uses the database for storage.
	 * 
	 * @param config config
	 * @param database database
	 */
	public USLBanHistoryPropagator(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	/**
	 * Determine what actions need to take place, if any, as a result of the given ban based
	 * on the given subreddits configuration.
	 * 
	 * @param subreddit the subreddit
	 * @param history the history
	 * @return the actions that need to take place
	 */
	public BanHistoryPropagateResult propagateBan(MonitoredSubreddit subreddit, BanHistory history)
	{
		Person mod = database.getPersonMapping().fetchByID(history.modPersonID);
		if(mod.username.equalsIgnoreCase(config.getProperty("user.username"))) 
			return new BanHistoryPropagateResult(subreddit, history);
		
		if(!history.banDetails.equalsIgnoreCase("permanent"))
			return new BanHistoryPropagateResult(subreddit, history);
		
		if(subreddit.writeOnly)
			return new BanHistoryPropagateResult(subreddit, history);
		
		if(history.monitoredSubredditID == subreddit.id)
			return new BanHistoryPropagateResult(subreddit, history);
		
		MonitoredSubreddit from = database.getMonitoredSubredditMapping().fetchByID(history.monitoredSubredditID);
		if(from.readOnly)
			return new BanHistoryPropagateResult(subreddit, history);
		
		
		
		List<SubscribedHashtag> relevant = new ArrayList<>();
		String descriptionLower = history.banDescription.toLowerCase();
		List<SubscribedHashtag> hashtags = database.getSubscribedHashtagMapping().fetchForSubreddit(subreddit.id, false);
		for(SubscribedHashtag tag : hashtags) {
			if(descriptionLower.contains(tag.hashtag.toLowerCase())) {
				relevant.add(tag);
			}
		}
		
		if(relevant.isEmpty()) 
			return new BanHistoryPropagateResult(subreddit, history);
		String tagsStringified = String.join(", ", relevant.stream().map(tag -> tag.hashtag).collect(Collectors.toList()));
		
		Person banned = database.getPersonMapping().fetchByID(history.bannedPersonID);
		
		List<UserBanInformation> bans = new ArrayList<>();
		ResponseInfo banResponseInfo = new ResponseInfo(ResponseInfoFactory.base);
		banResponseInfo.addTemporaryString("original mod", mod.username);
		banResponseInfo.addTemporaryString("original description", history.banDescription);
		banResponseInfo.addTemporaryString("original subreddit", from.subreddit);
		banResponseInfo.addTemporaryString("new subreddit", subreddit.subreddit);
		banResponseInfo.addTemporaryString("triggering tags", tagsStringified);
		ResponseFormatter banMessageFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_message").responseBody, banResponseInfo);
		String banMessage = banMessageFormatter.getFormattedResponse(config, database);
		ResponseFormatter banNoteFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_note").responseBody, banResponseInfo);
		String banNote = banNoteFormatter.getFormattedResponse(config, database);
		bans.add(new UserBanInformation(banned, subreddit, banMessage, "other", banNote));
		
		
		List<ModmailPMInformation> modmailPms = new ArrayList<>();
		if(!subreddit.silent) {
			ResponseInfo messageRespInfo = new ResponseInfo(ResponseInfoFactory.base);
			messageRespInfo.addTemporaryString("original mod", mod.username);
			messageRespInfo.addTemporaryString("original description", history.banDescription);
			messageRespInfo.addTemporaryString("original subreddit", from.subreddit);
			messageRespInfo.addTemporaryString("original timestamp", USLBotDriver.timeToPretty(history.occurredAt.getTime()));
			messageRespInfo.addTemporaryString("original id", history.modActionID);
			messageRespInfo.addTemporaryString("banned user", banned.username);
			messageRespInfo.addTemporaryString("triggering tags", tagsStringified);
			ResponseFormatter modMailTitleFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_modmail_title").responseBody, messageRespInfo);
			String modMailTitle = modMailTitleFormatter.getFormattedResponse(config, database);
			ResponseFormatter modMailBodyFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_modmail_body").responseBody, messageRespInfo);
			String modMailBody = modMailBodyFormatter.getFormattedResponse(config, database);
			modmailPms.add(new ModmailPMInformation(subreddit, modMailTitle, modMailBody));
		}
		
		List<UserPMInformation> userPms = new ArrayList<>();
		
		return new BanHistoryPropagateResult(subreddit, history, bans, modmailPms, userPms);
	}
}
