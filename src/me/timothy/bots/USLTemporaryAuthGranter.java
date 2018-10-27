package me.timothy.bots;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.functions.IsModeratorFunction;
import me.timothy.bots.functions.TemporaryAuthGrantResultHandlerFunction;
import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.TemporaryAuthGranterResult;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.TemporaryAuthLevel;
import me.timothy.bots.models.TemporaryAuthRequest;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * Grants people temporary authorization on the website if they have requested
 * it and we have verified they are a moderator on the universal scammer list. This
 * is a pretty long term grant (~1 month) that can be renewed as long as they are 
 * still a moderator on the subreddit.
 * 
 * @author Timothy
 */
public class USLTemporaryAuthGranter {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * The file configuration
	 */
	protected USLFileConfiguration config;
	
	/**
	 * The database
	 */
	protected USLDatabase database;
	
	/**
	 * Determines if someone is a moderator of a subreddit
	 */
	protected IsModeratorFunction isModeratorFn;
	
	/**
	 * Actually sends out the pms
	 */
	protected TemporaryAuthGrantResultHandlerFunction handlerFn;

	/**
	 * Create a new instance of the USLTemporaryAuthGranter using configuration
	 * information from config and uses the database for storage.
	 * 
	 * @param config config
	 * @param database database
	 */
	public USLTemporaryAuthGranter(USLDatabase database, USLFileConfiguration config, IsModeratorFunction isModeratorFn, 
			TemporaryAuthGrantResultHandlerFunction handlerFn) {
		this.database = database;
		this.config = config;
		this.isModeratorFn = isModeratorFn;
		this.handlerFn = handlerFn;
	}
	
	/**
	 * Handle an appropriate number of requests
	 */
	public void handleRequests() {
		int numRequests = Integer.valueOf(config.getProperty("temp_auth_granter.max_requests_per_loop"));
		
		for(int i = 0; i < numRequests; i++) {
			TemporaryAuthRequest request = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
			if(request == null)
				return;
			
			TemporaryAuthGranterResult result = handleRequest(request);
			if(result != null) {
				handlerFn.handleResult(result);
			}
			
			database.getTemporaryAuthRequestMapping().deleteById(request.id);
		}
	}
	
	private TemporaryAuthGranterResult handleRequest(TemporaryAuthRequest request) {
		DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
		
		Person requester = database.getPersonMapping().fetchByID(request.personId);
		boolean verified = false;
		List<UserPMInformation> userPms = new ArrayList<>();
		List<ModmailPMInformation> subPms = new ArrayList<>();
		
		long minRetryElapsedMs = Long.valueOf(config.getProperty("temp_auth_granter.min_retry_elapsed_ms"));
		
		TemporaryAuthLevel currentLevel = database.getTemporaryAuthLevelMapping().fetchByPersonID(request.personId);
		if(currentLevel != null) {
			long elapsedMs = System.currentTimeMillis() - currentLevel.createdAt.getTime();
			
			if(minRetryElapsedMs > elapsedMs) {
				ResponseInfo info = new ResponseInfo(ResponseInfoFactory.base);
				info.addTemporaryString("username", requester.username);
				info.addTemporaryString("auth level time", dateFormat.format(currentLevel.createdAt));
				info.addTemporaryString("request time", dateFormat.format(request.createdAt));
				info.addTemporaryString("next acceptable time", dateFormat.format(new Timestamp(currentLevel.createdAt.getTime() + minRetryElapsedMs)));
				
				String titleFormat = database.getResponseMapping().fetchByName("tauth_too_soon_title").responseBody;
				String bodyFormat = database.getResponseMapping().fetchByName("tauth_too_soon_body").responseBody;
				
				String title = new ResponseFormatter(titleFormat, info).getFormattedResponse(config, database);
				String body = new ResponseFormatter(bodyFormat, info).getFormattedResponse(config, database);
				
				userPms.add(new UserPMInformation(requester, title, body));

				logger.printf(Level.DEBUG, "Authorized Request for %s DELAYED: Tried too recently (last got auth at %s, next acceptable is %s)", 
						requester.username, dateFormat.format(currentLevel.createdAt),
						dateFormat.format(new Timestamp(currentLevel.createdAt.getTime() + minRetryElapsedMs)));
				
				return new TemporaryAuthGranterResult(requester, verified, userPms, subPms);
			}
			database.getTemporaryAuthLevelMapping().deleteById(currentLevel.id);
			currentLevel = null;
		}
		
		String subreddit = config.getProperty("temp_auth_granter.subreddit");
		if(isModeratorFn.isModerator(subreddit, requester.username)) {
			verified = true;

			long authDurationMs = Long.parseLong(config.getProperty("temp_auth_granter.duration_verified_ms"));
			Timestamp granted = new Timestamp(System.currentTimeMillis());
			Timestamp expires = new Timestamp(System.currentTimeMillis() + authDurationMs);
			
			TemporaryAuthLevel level = new TemporaryAuthLevel(-1, requester.id, 5, granted, expires);
			database.getTemporaryAuthLevelMapping().save(level);
			
			
			ResponseInfo info = new ResponseInfo(ResponseInfoFactory.base);
			info.addTemporaryString("username", requester.username);
			info.addTemporaryString("request time", dateFormat.format(request.createdAt));
			info.addTemporaryString("expires time", dateFormat.format(expires));
			
			String titleFormat = database.getResponseMapping().fetchByName("tauth_authorized_title").responseBody;
			String bodyFormat = database.getResponseMapping().fetchByName("tauth_authorized_body").responseBody;
			
			String title = new ResponseFormatter(titleFormat, info).getFormattedResponse(config, database);
			String body = new ResponseFormatter(bodyFormat, info).getFormattedResponse(config, database);
			
			userPms.add(new UserPMInformation(requester, title, body));
			
			logger.printf(Level.INFO, "Authorization Request for %s APPROVED: Granted level %d authorization until %s",
					requester.username, level.authLevel, dateFormat.format(expires));
			
			return new TemporaryAuthGranterResult(requester, verified, userPms, subPms);
		}
		
		{ // this indent is potentially not useful but helps reusing variable names / show that this can be refactored later
			Timestamp nextRetryTime = new Timestamp(System.currentTimeMillis() + minRetryElapsedMs);
			
			TemporaryAuthLevel level = new TemporaryAuthLevel(-1, requester.id, 0, new Timestamp(System.currentTimeMillis()), nextRetryTime);
			database.getTemporaryAuthLevelMapping().save(level);
			
			ResponseInfo info = new ResponseInfo(ResponseInfoFactory.base);
			info.addTemporaryString("username", requester.username);
			info.addTemporaryString("request time", dateFormat.format(request.createdAt));
			info.addTemporaryString("next retry time", dateFormat.format(nextRetryTime));
			
			String titleFormat = database.getResponseMapping().fetchByName("tauth_unauthorized_to_requester_title").responseBody;
			String bodyFormat = database.getResponseMapping().fetchByName("tauth_unauthorized_to_requester_body").responseBody;
			
			String title = new ResponseFormatter(titleFormat, info).getFormattedResponse(config, database);
			String body = new ResponseFormatter(bodyFormat, info).getFormattedResponse(config, database);
			
			userPms.add(new UserPMInformation(requester, title, body));
			
			titleFormat = database.getResponseMapping().fetchByName("tauth_unauthorized_to_subreddit_title").responseBody;
			bodyFormat = database.getResponseMapping().fetchByName("tauth_unauthorized_to_subreddit_body").responseBody;
			
			title = new ResponseFormatter(titleFormat, info).getFormattedResponse(config, database);
			body = new ResponseFormatter(bodyFormat, info).getFormattedResponse(config, database);
			
			subPms.add(new ModmailPMInformation(database.getMonitoredSubredditMapping().fetchByName(subreddit), title, body));
			

			logger.printf(Level.INFO, "Authorization Request for %s DENIED: Set level %d authorization until %s",
					requester.username, level.authLevel, dateFormat.format(nextRetryTime));
			return new TemporaryAuthGranterResult(requester, verified, userPms, subPms);
		}
	}
}
