package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.UnbanHistory;
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
public class USLPropagator {
	
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
	public USLPropagator(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	/**
	 * Determine what actions need to take place, if any, as a result of the given ban based
	 * on the given subreddits configuration.
	 * 
	 * @param subreddit the subreddit
	 * @param hma the handled mod action for history
	 * @param history the history
	 * @return the actions that need to take place
	 */
	public PropagateResult propagateBan(MonitoredSubreddit subreddit, HandledModAction hma, BanHistory history)
	{
		Person mod = database.getPersonMapping().fetchByID(history.modPersonID);
		if(mod.username.equalsIgnoreCase(config.getProperty("user.username"))) 
			return new PropagateResult(subreddit, hma);
		
		if(!history.banDetails.equalsIgnoreCase("permanent"))
			return new PropagateResult(subreddit, hma);
		
		if(subreddit.writeOnly)
			return new PropagateResult(subreddit, hma);
		
		if(hma.monitoredSubredditID == subreddit.id)
			return new PropagateResult(subreddit, hma);
		
		MonitoredSubreddit from = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
		if(from.readOnly)
			return new PropagateResult(subreddit, hma);
		
		List<SubscribedHashtag> relevant = new ArrayList<>();
		String descriptionLower = history.banDescription.toLowerCase();
		List<SubscribedHashtag> hashtags = database.getSubscribedHashtagMapping().fetchForSubreddit(subreddit.id, false);
		for(SubscribedHashtag tag : hashtags) {
			if(descriptionLower.contains(tag.hashtag.toLowerCase())) {
				relevant.add(tag);
			}
		}
		
		if(relevant.isEmpty()) 
			return new PropagateResult(subreddit, hma);
		String tagsStringified = String.join(", ", relevant.stream().map(tag -> tag.hashtag).collect(Collectors.toList()));
		
		Person banned = database.getPersonMapping().fetchByID(history.bannedPersonID);

		List<UserBanInformation> bans = new ArrayList<>();
		List<ModmailPMInformation> modmailPms = new ArrayList<>();
		List<UserPMInformation> userPms = new ArrayList<>();

		List<BanHistory> personBannedOnSubreddit = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(history.bannedPersonID, subreddit.id);
		
		boolean banSuppressed = false;
		if(!personBannedOnSubreddit.isEmpty()) {
			banSuppressed = handleHaveOldHistoryOnPerson(subreddit, hma, history, mod, banned, from, relevant, tagsStringified, personBannedOnSubreddit, bans, modmailPms, userPms);
		}
		
		if(!banSuppressed) {
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
			
			
			if(!subreddit.silent) {
				ResponseInfo messageRespInfo = new ResponseInfo(ResponseInfoFactory.base);
				messageRespInfo.addTemporaryString("original mod", mod.username);
				messageRespInfo.addTemporaryString("original description", history.banDescription);
				messageRespInfo.addTemporaryString("original subreddit", from.subreddit);
				messageRespInfo.addTemporaryString("original timestamp", USLBotDriver.timeToPretty(hma.occurredAt.getTime()));
				messageRespInfo.addTemporaryString("original id", hma.modActionID);
				messageRespInfo.addTemporaryString("banned user", banned.username);
				messageRespInfo.addTemporaryString("triggering tags", tagsStringified);
				ResponseFormatter modMailTitleFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_modmail_title").responseBody, messageRespInfo);
				String modMailTitle = modMailTitleFormatter.getFormattedResponse(config, database);
				ResponseFormatter modMailBodyFormatter = new ResponseFormatter(database.getResponseMapping().fetchByName("propagated_ban_modmail_body").responseBody, messageRespInfo);
				String modMailBody = modMailBodyFormatter.getFormattedResponse(config, database);
				modmailPms.add(new ModmailPMInformation(subreddit, modMailTitle, modMailBody));
			}
		}
		
		
		return new PropagateResult(subreddit, hma, bans, modmailPms, userPms);
	}

	private class TimestampStringTuple implements Comparable<TimestampStringTuple> {
		public Timestamp timestamp;
		public String string;
		
		public TimestampStringTuple(Timestamp timestamp, String string) {
			this.timestamp = timestamp;
			this.string = string;
		}

		@Override
		public int compareTo(TimestampStringTuple tst) {
			return timestamp.compareTo(tst.timestamp);
		}
	}
	
	private boolean handleHaveOldHistoryOnPerson(MonitoredSubreddit subreddit, HandledModAction hma, BanHistory history,
			Person mod, Person banned, MonitoredSubreddit from, List<SubscribedHashtag> relevant, String tagsStringified,
			List<BanHistory> personBannedOnSubreddit,
			List<UserBanInformation> bans, List<ModmailPMInformation> modmailPms, List<UserPMInformation> userPms) {
		// First we need to determine if the last thing to happen is us banning the guy on the subreddit,
		// in which case we should do nothing
				
		BanHistory latestBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(banned.id, subreddit.id);
		UnbanHistory latestUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(banned.id, subreddit.id);
		
		if(latestBan != null) {
			HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(latestBan.id);
			boolean beforeUnbanOrNoUnban = true;
			if(latestUnban != null) {
				HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(latestUnban.id);
				
				if(mostRecentUnbanHMA.occurredAt.after(mostRecentBanHMA.occurredAt)) {
					beforeUnbanOrNoUnban = false;
				}
			}
			
			if(beforeUnbanOrNoUnban) {
				Person latestBanMod = database.getPersonMapping().fetchByID(latestBan.modPersonID);
				
				if(latestBanMod.username.equalsIgnoreCase(config.getProperty("user.username"))) {
					return true;
				}
			}
		}
						
		List<UnbanHistory> personUnbannedOnSubreddit = database.getUnbanHistoryMapping().fetchUnbanHistoriesByPersonAndSubreddit(history.bannedPersonID, subreddit.id);
		
		
		Map<Integer, Person> modsFromDB = new HashMap<>();
		Map<Person, List<BanHistory>> oldBanHistoriesByPerson = new HashMap<>();
		Map<Person, List<UnbanHistory>> oldUnbanHistoriesByPerson = new HashMap<>();
		List<TimestampStringTuple> historyOfPersonOnSubreddit = new ArrayList<>();
		
		for(BanHistory bh : personBannedOnSubreddit) {
			Person oldMod;
			if(!modsFromDB.containsKey(bh.modPersonID)) {
				oldMod = database.getPersonMapping().fetchByID(bh.modPersonID);
				modsFromDB.put(oldMod.id, oldMod);
			}else {
				oldMod = modsFromDB.get(bh.modPersonID);
			}
			
			List<BanHistory> otherBans;
			if(!oldBanHistoriesByPerson.containsKey(oldMod)) {
				otherBans = new ArrayList<>();
				oldBanHistoriesByPerson.put(oldMod, otherBans);
			}else {
				otherBans = oldBanHistoriesByPerson.get(oldMod);
			}
			
			otherBans.add(bh);
			
			HandledModAction bhHMA = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
			historyOfPersonOnSubreddit.add(new TimestampStringTuple(bhHMA.occurredAt, 
					String.format("%s banned %s for %s - %s", oldMod.username, banned.username, bh.banDetails, bh.banDescription)
					));
		}

		for(UnbanHistory ubh : personUnbannedOnSubreddit) {
			Person oldMod;
			if(!modsFromDB.containsKey(ubh.modPersonID)) {
				oldMod = database.getPersonMapping().fetchByID(ubh.modPersonID);
				modsFromDB.put(oldMod.id, oldMod);
			}else {
				oldMod = modsFromDB.get(ubh.modPersonID);
			}
			
			List<UnbanHistory> otherBans;
			if(!oldUnbanHistoriesByPerson.containsKey(oldMod)) {
				otherBans = new ArrayList<>();
				oldUnbanHistoriesByPerson.put(oldMod, otherBans);
			}else {
				otherBans = oldUnbanHistoriesByPerson.get(oldMod);
			}
			
			otherBans.add(ubh);
			
			HandledModAction ubhHMA = database.getHandledModActionMapping().fetchByID(ubh.handledModActionID);
			historyOfPersonOnSubreddit.add(new TimestampStringTuple(ubhHMA.occurredAt, 
					String.format("%s unbanned %s", oldMod.username, banned.username)
					));
		}
		
		if(latestBan == null) {
			throw new IllegalArgumentException("shouldn't get here without latestBan");
		}
		
		HandledModAction latestBanHMA = database.getHandledModActionMapping().fetchByID(latestBan.handledModActionID);
		boolean currentlyBanned = true, currentlyPermabanned = true;
		
		if(latestUnban != null) {
			HandledModAction latestUnbanHMA = database.getHandledModActionMapping().fetchByID(latestUnban.handledModActionID);
			
			if(latestUnbanHMA.occurredAt.after(latestBanHMA.occurredAt)) {
				currentlyBanned = false;
				currentlyPermabanned = false;
			}
		}
		
		if(currentlyPermabanned && !latestBan.banDetails.equalsIgnoreCase("permanent")) {
			currentlyPermabanned = false;
		}
		
		boolean suppressed = (currentlyBanned && currentlyPermabanned);

		Collections.sort(historyOfPersonOnSubreddit);
		String historyOfPersonString = String.join("\n", 
				historyOfPersonOnSubreddit.stream().map(
						(tst) -> "- " + USLBotDriver.timeToPretty(tst.timestamp.getTime()) + " - " + tst.string
				).collect(Collectors.toList()));
		
		Response userPMResponseTitle = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_with_history_userpm_title");
		Response userPMResponseBody = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_with_history_userpm_body");
		ResponseInfo userPMResponseInfo = new ResponseInfo(ResponseInfoFactory.base);
		userPMResponseInfo.addLongtermString("new usl subreddit", from.subreddit);
		userPMResponseInfo.addLongtermString("new usl mod", mod.username);
		userPMResponseInfo.addLongtermString("new ban description", history.banDescription);
		userPMResponseInfo.addLongtermString("banned user", banned.username);
		userPMResponseInfo.addLongtermString("old subreddit", subreddit.subreddit);
		userPMResponseInfo.addLongtermString("currently banned", Boolean.toString(currentlyBanned));
		userPMResponseInfo.addLongtermString("currently permabanned", Boolean.toString(currentlyPermabanned));
		userPMResponseInfo.addLongtermString("suppressed", Boolean.toString(suppressed));
		userPMResponseInfo.addLongtermString("triggering tags", tagsStringified);
		userPMResponseInfo.addLongtermString("full history", historyOfPersonString);
		for(int modPersonWithHistoryID : modsFromDB.keySet()) {
			Person modPersonWithHistory = modsFromDB.get(modPersonWithHistoryID);
			
			if(modPersonWithHistory.username.equalsIgnoreCase(config.getProperty("user.username"))) {
				continue;
			}
			
			userPMResponseInfo.addTemporaryString("old mod", modPersonWithHistory.username);
			
			List<BanHistory> thisModPersonsBans = oldBanHistoriesByPerson.get(modPersonWithHistory);
			if(thisModPersonsBans == null) {
				thisModPersonsBans = new ArrayList<>();
			}
			List<UnbanHistory> thisModPersonsUnbans = oldUnbanHistoriesByPerson.get(modPersonWithHistory);
			if(thisModPersonsUnbans == null) {
				thisModPersonsUnbans = new ArrayList<>();
			}
			
			userPMResponseInfo.addTemporaryString("old mod num bans", Integer.toString(thisModPersonsBans.size()));
			userPMResponseInfo.addTemporaryString("old mod num unbans", Integer.toString(thisModPersonsUnbans.size()));
			
			String title = new ResponseFormatter(userPMResponseTitle.responseBody, userPMResponseInfo).getFormattedResponse(config, database);
			String body = new ResponseFormatter(userPMResponseBody.responseBody, userPMResponseInfo).getFormattedResponse(config, database);
			
			userPms.add(new UserPMInformation(modPersonWithHistory, title, body));
			
			userPMResponseInfo.clearTemporary();
		}
		
		return suppressed;
	}

	/**
	 * Determine what actions need to take place, if any, as a result of the given unban based on the
	 * given subreddits configuration
	 * 
	 * @param subreddit the subreddit
	 * @param hma the handled mod action for history
	 * @param history the history
	 * @return the actions that need to take place
	 */
	public PropagateResult propagateUnban(MonitoredSubreddit subreddit, HandledModAction hma, UnbanHistory history) {
		return new PropagateResult(subreddit, hma);
	}
}