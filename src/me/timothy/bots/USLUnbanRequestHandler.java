package me.timothy.bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.timothy.bots.functions.IsModeratorFunction;
import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.UnbanRequestResult;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.memory.UserUnbanInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * Figures out what to do with unban requests
 * 
 * @author Timothy
 */
public class USLUnbanRequestHandler {
	/**
	 * Contains all the mappings to/from the relational database
	 */
	protected USLDatabase database;
	
	/**
	 * Contains all the simple static configuration options saved in
	 * a flat file format
	 */
	protected USLFileConfiguration config;
	
	/**
	 * How to determine if a user is a moderator of a subreddit
	 */
	protected IsModeratorFunction isModeratorFunction;
	
	/**
	 * Create a new unban request handler attached to the specified database and 
	 * configuration
	 * 
	 * @param database the database
	 * @param config the file configuration
	 */
	public USLUnbanRequestHandler(USLDatabase database, USLFileConfiguration config,
			IsModeratorFunction isModeratorFunction) {
		this.database = database;
		this.config = config;
		this.isModeratorFunction = isModeratorFunction;
	}
	
	/**
	 * Determines what to do as a result of an unban request. Does not actually 
	 * change anything in the database, but does fetch information from it. The
	 * driver must handle deciding when to handle unban requests and determining
	 * which ones have already been handled!
	 * 
	 * @param unbanRequest the unban request
	 * @return what to do about it
	 */
	public UnbanRequestResult handleUnbanRequest(UnbanRequest unbanRequest) {
		Person moderator = database.getPersonMapping().fetchByID(unbanRequest.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(unbanRequest.bannedPersonID);
		
		TraditionalScammer scammer = database.getTraditionalScammerMapping().fetchByPersonID(unbanRequest.bannedPersonID);
		if(scammer != null) {
			return handleUnbanOnListRequest(unbanRequest, moderator, banned, scammer);
		}
		
		Map<Integer, Boolean> knownModeratingSubreddits = new HashMap<>();
		
		BanHistory[] sourceHistory = new BanHistory[1];
		HandledModAction[] sourceHMA = new HandledModAction[1];
		if (!moderatorHasPermission(unbanRequest, moderator, banned, knownModeratingSubreddits,sourceHistory, sourceHMA)) {
			return getNoPermissionResult(unbanRequest, moderator);
		}
		
		List<MonitoredSubreddit> allSubs = database.getMonitoredSubredditMapping().fetchAll();
		List<UserUnbanInformation> unbans = new ArrayList<>();
		List<ModmailPMInformation> modmailPMs = new ArrayList<>();
		List<UserPMInformation> userPMs = new ArrayList<>();

		
		MonitoredSubreddit sourceSub = database.getMonitoredSubredditMapping().fetchByID(sourceHMA[0].monitoredSubredditID);
		Person sourceModerator = database.getPersonMapping().fetchByID(sourceHistory[0].modPersonID);
		for(MonitoredSubreddit sub : allSubs) {
			determineActionOnSubreddit(unbanRequest, sub, moderator, banned, unbans, modmailPMs, userPMs, knownModeratingSubreddits,
					sourceHistory[0], sourceHMA[0], sourceSub, sourceModerator);
		}
		
		return new UnbanRequestResult(unbanRequest, unbans, modmailPMs, userPMs, null, false);
	}

	/**
	 * Gets the result that should occur if moderator has no permission to do unbanRequest
	 * 
	 * @param unbanRequest the request
	 * @param moderator the moderator with no permission
	 * @return the appropriate result
	 */
	protected UnbanRequestResult getNoPermissionResult(UnbanRequest unbanRequest, Person moderator) {
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		Response pmTitleResponse = database.getResponseMapping().fetchByName("unban_request_moderator_unauthorized_title");
		Response pmBodyResponse = database.getResponseMapping().fetchByName("unban_request_moderator_unauthorized_body");
		
		String pmTitle = new ResponseFormatter(pmTitleResponse.responseBody, respInfo).getFormattedResponse(config, database);
		String pmBody = new ResponseFormatter(pmBodyResponse.responseBody, respInfo).getFormattedResponse(config, database);
		UserPMInformation pm = new UserPMInformation(moderator, pmTitle, pmBody);
		
		return new UnbanRequestResult(unbanRequest, new ArrayList<UserUnbanInformation>(), new ArrayList<ModmailPMInformation>(), Arrays.asList(pm), null, true);
	}

	/**
	 * Handles an unban request that has a banned person that is on the traditional scammer list.
	 * Instead of normal requirements, the moderator must be a moderator on config user.main_sub 
	 * and the person will be unbanned on all subreddits he is banned on
	 * 
	 * @param unbanReq unban request
	 * @param moderator moderator of unbanReq.moderatorPersonID
	 * @param banned banned person of unbanReq.bannedPersonID
	 * @param scammer scammer with id unbanReq.bannedPersonID
	 * @return the actions to take
	 */
	protected UnbanRequestResult handleUnbanOnListRequest(UnbanRequest unbanReq, Person moderator, Person banned, TraditionalScammer scammer) {
		List<UserUnbanInformation> unbans = new ArrayList<>();
		List<ModmailPMInformation> modmailPMs = new ArrayList<>();
		List<UserPMInformation> userPMs = new ArrayList<>();
		
		MonitoredSubreddit mainSub = database.getMonitoredSubredditMapping().fetchByName(config.getProperty("user.main_sub"));
		if(!isModeratorFunction.isModerator(mainSub.subreddit, moderator.username)) {
			return getNoPermissionResult(unbanReq, moderator);
		}
		ResponseInfo responseInfo = new ResponseInfo(ResponseInfoFactory.base);
		responseInfo.addLongtermString("unban mod", moderator.username);
		responseInfo.addLongtermString("unbanned user", banned.username);
		responseInfo.addLongtermString("scammer reason", scammer.reason);
		responseInfo.addLongtermString("scammer description", scammer.description);
		
		{
			// nest so we can use these variable names later
			
			Response titleResponse = database.getResponseMapping().fetchByName("unban_request_removed_from_list_title");
			Response bodyResponse = database.getResponseMapping().fetchByName("unban_request_removed_from_list_body");
			
			String titleString = new ResponseFormatter(titleResponse.responseBody, responseInfo).getFormattedResponse(config, database);
			String bodyString = new ResponseFormatter(bodyResponse.responseBody, responseInfo).getFormattedResponse(config, database);
			
			modmailPMs.add(new ModmailPMInformation(mainSub, titleString, bodyString));
			
		}

		List<MonitoredSubreddit> allSubs = database.getMonitoredSubredditMapping().fetchAll();
		for(MonitoredSubreddit sub : allSubs) {
			if(!USLUtils.potentiallyBannedOnSubreddit(database, banned.id, sub))
				continue; // it is impossible for him to be banned right now, don't unban
			
			this.doUnbanActionForSubredditUsingResponseInfo(sub, responseInfo, unbans, modmailPMs, banned, "unban_request_valid_modmail_list");
		}
		
		return new UnbanRequestResult(unbanReq, unbans, modmailPMs, userPMs, scammer, false);
	}

	/**
	 * Determine if a moderator has permission to make the specified unban request. The moderator
	 * must either be a moderator of the subreddit that performed the original ban, or must be
	 * a moderator of the universalscammerlist.
	 * 
	 * @param unbanRequest the request made
	 * @param moderator the moderator
	 * @param banned the banned person
	 * @param knownModeratingSubreddits subreddits to if moderator moderates it
	 * @param outSourceHistory array of length 1 that is set with the source history if mod has perm
	 * @param outSourceHMA array of length 1 that is set with the source hma if mod has perm
	 * @return if the moderator can request banned be unbanned
	 */
	protected boolean moderatorHasPermission(UnbanRequest unbanRequest, Person moderator, Person banned, Map<Integer, Boolean> knownModeratingSubreddits,
			BanHistory[] outSourceHistory, HandledModAction[] outSourceHMA) {
		List<BanHistory> historyOfBanned = database.getBanHistoryMapping().fetchBanHistoriesByPerson(banned.id);
		List<Person> ignorePersons = Arrays.asList(
				database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username")),
				database.getPersonMapping().fetchOrCreateByUsername("Guzbot3000")
				);
		
		
		BanHistory valid = null;
		HandledModAction validHMA = null;
		for(BanHistory history : historyOfBanned) {
			if(ignorePersons.stream().anyMatch((p) -> p.id == history.modPersonID))
				continue;
			
			HandledModAction hma = database.getHandledModActionMapping().fetchByID(history.handledModActionID);
			
			if(!knownModeratingSubreddits.containsKey(hma.monitoredSubredditID)) {
				MonitoredSubreddit monSub = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);

				knownModeratingSubreddits.put(hma.monitoredSubredditID, isModeratorFunction.isModerator(monSub.subreddit, moderator.username));
			}
			
			boolean isMod = knownModeratingSubreddits.get(hma.monitoredSubredditID);
			
			if(!isMod)
				continue;
			
			if(valid != null && hma.occurredAt.before(validHMA.occurredAt))
				continue;
			
			valid = history;
			validHMA = hma;
		}
		
		
		if(valid == null)
			return false;
		
		// Check for the following situation:
		// paul bans eric on paulssub
		// me bans eric on emmassub
		// emma unbans eric on emmassub
		// emma bans eric on emmassub
		// emma requests to unban eric
		
		// This might block some peculiar legitamate requests, but lets assume a system
		// administrator can deal with those
		
		List<BanHistory> historyOnSubreddit = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(banned.id, validHMA.monitoredSubredditID);
		if(historyOnSubreddit.stream().anyMatch((h) -> ignorePersons.stream().anyMatch((p) -> p.id == h.modPersonID))) {
			return false;
		}
		
		outSourceHistory[0] = valid;
		outSourceHMA[0] = validHMA;
		return true;
	}

	
	/**
	 * Determine what actions need to be taken as a result of the specified unban request on the
	 * specified subreddit. Also given some of the results from the database to avoid repeating the
	 * same work over and over
	 * 
	 * @param unbanRequest the unban request
	 * @param sub the subreddit to consider
	 * @param moderator the moderator requesting the unban
	 * @param banned the person requested to be unbanned
	 * @param unbans the list of unbans to do, should be modified
	 * @param modmailPMs the list of modmail pms to do, should be modified
	 * @param userPMs the list of user pms to do, should be modified
	 * @param knownModeratingSubreddits  sub id to if moderator moderates it
	 * @param sourceHistory the history that moderator is using to be allowed to unban
	 * @param sourceHMA the handled mod action for sourceHistory
	 * @param sourceSub the monitored subreddit of sourceHMA
	 * @param sourceModerator the moderator in sourceHistory
	 */
	protected void determineActionOnSubreddit(UnbanRequest unbanRequest, MonitoredSubreddit sub, Person moderator,
			Person banned, List<UserUnbanInformation> unbans, List<ModmailPMInformation> modmailPMs,
			List<UserPMInformation> userPMs, Map<Integer, Boolean> knownModeratingSubreddits,
			BanHistory sourceHistory, HandledModAction sourceHMA, MonitoredSubreddit sourceSub,
			Person sourceModerator) {
		
		BanHistory mostRecentBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(banned.id, sub.id);
		if(mostRecentBan == null)
			return;
		
		UnbanHistory mostRecentUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(banned.id, sub.id);
		
		if(mostRecentUnban != null) {
			HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(mostRecentBan.handledModActionID);
			HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(mostRecentUnban.handledModActionID);
			
			if(mostRecentUnbanHMA.occurredAt.after(mostRecentBanHMA.occurredAt))
				return;
		}
		
		ResponseInfo modmailInfo = new ResponseInfo(ResponseInfoFactory.base);
		modmailInfo.addLongtermString("unban mod", moderator.username);
		modmailInfo.addLongtermString("unbanned user", banned.username);
		modmailInfo.addLongtermString("original ban subreddit", sourceSub.subreddit);
		modmailInfo.addLongtermString("original ban mod", sourceModerator.username);
		modmailInfo.addLongtermString("original description", sourceHistory.banDescription);
		
		doUnbanActionForSubredditUsingResponseInfo(sub, modmailInfo, unbans, modmailPMs, banned, "unban_request_valid_modmail");
	}
	
	protected void doUnbanActionForSubredditUsingResponseInfo(MonitoredSubreddit sub, ResponseInfo modmailInfo, 
			List<UserUnbanInformation> unbans, List<ModmailPMInformation> modmailPMs, Person banned,
			String responsePrefix) {
		if(!sub.writeOnly) {
			Response titleResponse = database.getResponseMapping().fetchByName(responsePrefix + "_title");
			Response bodyResponse = database.getResponseMapping().fetchByName(responsePrefix + "_body");
			String titleString = new ResponseFormatter(titleResponse.responseBody, modmailInfo).getFormattedResponse(config, database);
			String bodyString = new ResponseFormatter(bodyResponse.responseBody, modmailInfo).getFormattedResponse(config, database);
			
			unbans.add(new UserUnbanInformation(banned, sub));
			modmailPMs.add(new ModmailPMInformation(sub, titleString, bodyString));
			
		}else {
			Response titleResponse = database.getResponseMapping().fetchByName(responsePrefix + "_writeonly_title");
			Response bodyResponse = database.getResponseMapping().fetchByName(responsePrefix + "_writeonly_body");
			String titleString = new ResponseFormatter(titleResponse.responseBody, modmailInfo).getFormattedResponse(config, database);
			String bodyString = new ResponseFormatter(bodyResponse.responseBody, modmailInfo).getFormattedResponse(config, database);

			modmailPMs.add(new ModmailPMInformation(sub, titleString, bodyString));
		}
	}
}
