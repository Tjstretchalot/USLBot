package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		Person banned = database.getPersonMapping().fetchByID(history.bannedPersonID);
		Person mod = database.getPersonMapping().fetchByID(history.modPersonID);
		if(mod.username.equalsIgnoreCase(config.getProperty("user.username"))) 
			return new PropagateResult(subreddit, hma);
		
		if(banned.username.equalsIgnoreCase("[deleted]"))
			return new PropagateResult(subreddit, hma);
		
		if(mod.username.equalsIgnoreCase("Guzbot3000"))
			return new PropagateResult(subreddit, hma);
		
		if(subreddit.writeOnly)
			return new PropagateResult(subreddit, hma);
		
		if(hma.monitoredSubredditID == subreddit.id)
			return new PropagateResult(subreddit, hma);

		UnbanHistory mostRecentUnbanOnBannedSubreddit = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(history.bannedPersonID, hma.monitoredSubredditID);
		if(mostRecentUnbanOnBannedSubreddit != null) {
			HandledModAction mostRecentUnbanOnBannedSubredditHMA = database.getHandledModActionMapping().fetchByID(mostRecentUnbanOnBannedSubreddit.handledModActionID);
			if(mostRecentUnbanOnBannedSubredditHMA.occurredAt.after(hma.occurredAt)) {
				return new PropagateResult(subreddit, hma);
			}
		}
		
		MonitoredSubreddit from = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
		if(from.readOnly)
			return new PropagateResult(subreddit, hma);
		
		if(history.banDetails.startsWith("changed to"))
			return propagateBanChangedDuration(subreddit, hma, history, mod, from);

		if(!history.banDetails.equalsIgnoreCase("permanent"))
			return new PropagateResult(subreddit, hma);
		
		if(history.banDescription == null)
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

	/**
	 * This is called when history.banDetails starts with "changed to", which implies that there
	 * exists an earlier BanHistory with the user.
	 * 
	 * @param subreddit the subreddit that the propagate should effect
	 * @param hma the hma of history
	 * @param history the history being propagated
	 * @param mod the person with id history.modPersonID
	 * @param from the subreddit with id hma.monitoredSubredditID
	 * @return what to do
	 */
	private PropagateResult propagateBanChangedDuration(MonitoredSubreddit subreddit, HandledModAction hma,
			BanHistory history, Person mod, MonitoredSubreddit from) {
		if(!history.banDetails.equals("changed to permanent"))
			return new PropagateResult(subreddit, hma); // effectively this is unbanning the dude as far as we're concerned
		
		Person banned = database.getPersonMapping().fetchByID(history.bannedPersonID);
		
		List<BanHistory> allKnownBans = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(banned.id, from.id);
		BanHistory mostRecentNonDurationChangingBanOlderThanHistory = null;
		HandledModAction mostRecentNonDurationChangingBanOlderThanHistoryHMA = null;
		
		for(BanHistory bh : allKnownBans) {
			if(bh.banDetails.startsWith("changed to"))
				continue;
			
			HandledModAction bhHMA = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
			
			if(bhHMA.occurredAt.after(hma.occurredAt))
				continue;
			
			if(mostRecentNonDurationChangingBanOlderThanHistory == null 
					|| bhHMA.occurredAt.after(mostRecentNonDurationChangingBanOlderThanHistoryHMA.occurredAt)) {
				mostRecentNonDurationChangingBanOlderThanHistory = bh;
				mostRecentNonDurationChangingBanOlderThanHistoryHMA = bhHMA;
			}
		}
		
		if(mostRecentNonDurationChangingBanOlderThanHistory == null) {
			logger.warn("Got a duration-changing ban history without a corresponding ban to change duration!");
			return new PropagateResult(subreddit, hma);
		}
		
		BanHistory fakeBanHistory = new BanHistory(-1, history.modPersonID, history.bannedPersonID, 
				hma.id, mostRecentNonDurationChangingBanOlderThanHistory.banDescription, "permanent");
		return propagateBan(subreddit, hma, fakeBanHistory);
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
		// First we need to determine if the last thing to happen is us or Guzbot3000 banning the guy on the subreddit,
		// in which case we should do nothing
		
		// Also we need to ensure we're not pming people that have "old history" when their "old history" is actually
		// newer than this ban, which is possible
		
		Person mePerson = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		Person guzbotPerson = database.getPersonMapping().fetchOrCreateByUsername("Guzbot3000");
		
		BanHistory latestBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(banned.id, subreddit.id);
		UnbanHistory latestUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(banned.id, subreddit.id);
		
		if(latestBan != null) {
			HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(latestBan.handledModActionID);
			boolean beforeUnbanOrNoUnban = true;
			if(latestUnban != null) {
				HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(latestUnban.handledModActionID);
				
				if(mostRecentUnbanHMA.occurredAt.after(mostRecentBanHMA.occurredAt)) {
					beforeUnbanOrNoUnban = false;
				}
			}
			
			if(beforeUnbanOrNoUnban) {
				if(latestBan.modPersonID == mePerson.id || latestBan.modPersonID == guzbotPerson.id)
					return true;
			}
		}
						
		List<UnbanHistory> personUnbannedOnSubreddit = database.getUnbanHistoryMapping().fetchUnbanHistoriesByPersonAndSubreddit(history.bannedPersonID, subreddit.id);
		
		
		Map<Integer, Person> modsFromDB = new HashMap<>();
		Map<Integer, Timestamp> oldestHistoryByPerson = new HashMap<>();
		Map<Person, List<BanHistory>> oldBanHistoriesByPerson = new HashMap<>();
		Map<Person, List<UnbanHistory>> oldUnbanHistoriesByPerson = new HashMap<>();
		List<TimestampStringTuple> historyOfPersonOnSubreddit = new ArrayList<>();

		
		boolean botHistory = false;
		Timestamp newestBotHistory = null;
		
		for(BanHistory bh : personBannedOnSubreddit) {
			if(bh.handledModActionID == hma.id)
				continue;
			
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
			
			if(!oldestHistoryByPerson.containsKey(oldMod.id)) {
				oldestHistoryByPerson.put(oldMod.id, bhHMA.occurredAt);
			}else {
				Timestamp oldTime = oldestHistoryByPerson.get(oldMod.id);
				if(oldTime.after(bhHMA.occurredAt)) {
					oldestHistoryByPerson.put(oldMod.id, bhHMA.occurredAt);
				}
			}
			
			if(bh.modPersonID == mePerson.id || bh.modPersonID == guzbotPerson.id) {
				botHistory = true;
				if(newestBotHistory == null || bhHMA.occurredAt.after(newestBotHistory)) {
					newestBotHistory = bhHMA.occurredAt;
				}
			}
			
			if(bh.banDetails.startsWith("changed to")) {
				historyOfPersonOnSubreddit.add(new TimestampStringTuple(bhHMA.occurredAt, 
						String.format("%s modified the ban on %s - %s", oldMod.username, banned.username, bh.banDetails)
						));
			}else {
				historyOfPersonOnSubreddit.add(new TimestampStringTuple(bhHMA.occurredAt, 
						String.format("%s banned %s for %s - %s", oldMod.username, banned.username, bh.banDetails, bh.banDescription)
						));
			}
		}

		for(UnbanHistory ubh : personUnbannedOnSubreddit) {
			if(ubh.handledModActionID == hma.id)
				continue;
			
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

			if(!oldestHistoryByPerson.containsKey(oldMod.id)) {
				oldestHistoryByPerson.put(oldMod.id, ubhHMA.occurredAt);
			}else {
				Timestamp oldTime = oldestHistoryByPerson.get(oldMod.id);
				if(oldTime.after(ubhHMA.occurredAt)) {
					oldestHistoryByPerson.put(oldMod.id, ubhHMA.occurredAt);
				}
			}
			
			if(ubh.modPersonID == mePerson.id || ubh.modPersonID == guzbotPerson.id) {
				botHistory = true;
				if(newestBotHistory == null || ubhHMA.occurredAt.after(newestBotHistory)) {
					newestBotHistory = ubhHMA.occurredAt;
				}
			}
			
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
			
			if(modPersonWithHistory.id == mePerson.id) {
				continue;
			}else if(modPersonWithHistory.id == guzbotPerson.id) {
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
			
			if(botHistory) {
				Timestamp latestActionByThisMod = null;
				for(BanHistory bh : thisModPersonsBans) {
					HandledModAction bhHMA = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
					if(latestActionByThisMod == null || bhHMA.occurredAt.after(latestActionByThisMod)) {
						latestActionByThisMod = bhHMA.occurredAt;
					}
				}
				for(UnbanHistory ubh : thisModPersonsUnbans) {
					HandledModAction ubhHMA = database.getHandledModActionMapping().fetchByID(ubh.handledModActionID);
					if(latestActionByThisMod == null || ubhHMA.occurredAt.after(latestActionByThisMod)) {
						latestActionByThisMod = ubhHMA.occurredAt;
					}
				}
				
				if(latestActionByThisMod.before(newestBotHistory)) {
					logger.trace("Not sending a pm to " + modPersonWithHistory.username + " because a bot has taken actions since his last action and "
							+ "it would be redundant to pm him about it now");
					continue;
				}
			}
			
			Timestamp oldestAction = oldestHistoryByPerson.get(modPersonWithHistory.id);
			if(oldestAction.after(hma.occurredAt)) {
				// We shouldn't send a pm to this guy, because he's not the "old mod" with "old history" considering
				// his oldest action is AFTER this action
				continue;
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
