package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;

/**
 * Takes the information about what happened on reddit and translates it to updating USLAction
 * 
 * @author Timothy
 */
public class USLRedditToMeaningProcessor {
	private static final Logger logger = LogManager.getLogger();
	
	private USLDatabase database;
	private USLFileConfiguration config;
	private final boolean extremeTrace;
	
	public USLRedditToMeaningProcessor(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
		this.extremeTrace = this.config.getProperty("reddit_to_meaning.extreme_trace").equals("true");
	}
	
	private static List<Hashtag> getRelevant(List<Hashtag> tags, BanHistory ban) {
		String descLower = ban.banDescription.toLowerCase();
		List<Hashtag> result = null;
		for(Hashtag tag : tags) {
			if(descLower.contains(tag.tag.toLowerCase())) {
				if(result == null) { result = new ArrayList<>(); }
				result.add(tag);
			}
		}
		return (result == null ? Collections.emptyList() : result);
	}
	
	private static boolean allOverlap(List<Hashtag> relevant, List<USLActionHashtag> existing) {
		for(Hashtag tag : relevant) {
			boolean found = false;
			for(USLActionHashtag actTag : existing) {
				if(actTag.hashtagID == tag.id) {
					found = true;
					break;
				}
			}
			if(!found)
				return false;
		}
		return true;
	}
	
	private Set<Integer> processBanWhenNotFirst(List<Hashtag> tags, HandledModAction modAction, BanHistory ban, List<Hashtag> relevant, USLAction latest) {
		if(extremeTrace) { logger.printf(Level.TRACE, "We have existing history: %s", latest.toPrettyString(database)); }
		
		List<USLActionHashtag> latestTags = database.getUSLActionHashtagMapping().fetchByUSLActionID(latest.id);
		if(extremeTrace) { logger.printf(Level.TRACE, "Existing tags: %s", latestTags.stream().map((a) -> a.toPrettyString(database)).collect(Collectors.joining(", "))); }
		
		if(allOverlap(relevant, latestTags)) {
			if(extremeTrace) { logger.printf(Level.TRACE, "All tags overlap."); }
			
			UnbanHistory unban = database.getUnbanHistoryMapping().fetchByActionAndSubreddit(latest.id, modAction.monitoredSubredditID);
			if(unban != null) {
				if(extremeTrace) { logger.printf(Level.TRACE, "Detaching existing unban"); }
				database.getUSLActionUnbanHistoryMapping().delete(latest.id, unban.id);
			}
			
			BanHistory existingBan = database.getBanHistoryMapping().fetchByActionAndSubreddit(latest.id, modAction.monitoredSubredditID);
			if(existingBan != null) {
				if(extremeTrace) { logger.printf(Level.TRACE, "Detaching existing ban which has no paired unban"); }
				
				HandledModAction existingHMA = database.getHandledModActionMapping().fetchByID(existingBan.handledModActionID);
				if(existingHMA.occurredAt.after(modAction.occurredAt))  {
					throw new RuntimeException("Not being propagated in order! Found attached ban that is newer than me; have " + existingBan.toPrettyString(database) 
					+ " attached when processing " + ban.toPrettyString(database));
				}
				
				database.getUSLActionBanHistoryMapping().delete(latest.id, existingBan.id);
			}
			
			if(extremeTrace) { logger.printf(Level.TRACE, "Attaching new ban"); }
			
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(latest.id, ban.id));
			
			if(ban.modPersonID == database.getPersonMapping().fetchByUsername(config.getProperty("user.username")).id) {
				return Collections.emptySet();
			}
			
			return Collections.singleton(latest.personID);
		}
		
		if(extremeTrace) { logger.printf(Level.TRACE, "Not all tags overlap. Creating new action"); }
		USLAction action = database.getUSLActionMapping().create(true, ban.bannedPersonID, modAction.occurredAt);
		
		Set<Integer> addedTagIds = new HashSet<Integer>();
		for(Hashtag tag : relevant) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Adding tag %s", tag.tag); }
			addedTagIds.add(tag.id);
			database.getUSLActionHashtagMapping().save(new USLActionHashtag(action.id, tag.id));
		}
		
		for(USLActionHashtag old : latestTags) {
			if(!addedTagIds.contains(old.hashtagID)) {
				if(extremeTrace) { logger.printf(Level.TRACE, "Adding tag %s", database.getHashtagMapping().fetchByID(old.hashtagID).tag); }
				database.getUSLActionHashtagMapping().save(new USLActionHashtag(action.id, old.hashtagID));
			}
		}
		
		UnbanHistory unbanToNotMoveForward = database.getUnbanHistoryMapping().fetchByActionAndSubreddit(latest.id, modAction.monitoredSubredditID);
		int idNotToMoveForward = unbanToNotMoveForward != null ? unbanToNotMoveForward.id : -1;
		
		List<USLActionUnbanHistory> unbanMaps = database.getUSLActionUnbanHistoryMapping().fetchByUSLActionID(latest.id);
		for(USLActionUnbanHistory unban : unbanMaps) {
			if(unban.unbanHistoryID != idNotToMoveForward) {
				if(extremeTrace) { logger.printf(Level.TRACE, "Moving unban forward; was %s", unban.toPrettyString(database)); }
				database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, unban.unbanHistoryID));
			}
		}
		
		List<USLActionBanHistory> banMaps = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(latest.id);
		for(USLActionBanHistory banToMove : banMaps) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Moving ban forward; was %s", banToMove.toPrettyString(database)); }
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, banToMove.banHistoryID));
		}
		
		if(extremeTrace) { logger.printf(Level.TRACE, "Adding new ban"); }
		database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban.id));
		return Collections.singleton(action.personID);
	}
	
	/**
	 * Processes the given ban and modifies the USLAction and mapping tables to take it into account. This
	 * assumes that this is the *most recent* action that is being processed. Order is very important for
	 * using this function.
	 *
	 * Example:
	 * 
	 * <p>John bans Paul for the first time, but it matches no tags. Nothing happens. Then Alex bans Paul
	 * with the #scammer tag. Now an action is created, and it has Johns and Alex's ban attached. The bot
	 * bans on a different subreddit. It is attached to the current action. Alex unbans Paul. The Alex ban
	 * is detached from the action and then the unban is attached. Alex bans Paul with a #scammer tag. The
	 * unban gets detached and the new ban gets attached. Alex unbans Paul. The ban gets detached and the
	 * new unban gets attached. Alex bans Paul with the #sketchy tag. A new action gets built, with the 
	 * #scammer and #sketchy tags tacked on and the most current bans/unbans attached.</p>
	 * 
	 * @param tags the tags that we are processing on 
	 * @param modAction the reference to the mod action on reddit.
	 * @param ban information about the ban that occurred
	 * @return persons who are now dirty due to this ban
	 */
	public Set<Integer> processBan(List<Hashtag> tags, HandledModAction modAction, BanHistory ban) {
		if(extremeTrace) { logger.printf(Level.TRACE, "processBan ban=%s", ban.toPrettyString(database)); }
		
		if(!ban.banDetails.equals("permanent")) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Skipping; not permanent"); }
			return Collections.emptySet();
		}

		Person bannedPerson = database.getPersonMapping().fetchByID(ban.bannedPersonID);
		if(bannedPerson.username.equals("[deleted]")) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Skipping since this is [deleted]"); }
			return Collections.emptySet();
		}
		
		if(ban.banDescription == null) 
			throw new NullPointerException("ban description is null!");
		
		MonitoredSubreddit banSub = database.getMonitoredSubredditMapping().fetchByID(modAction.monitoredSubredditID);
		
		List<Hashtag> relevant;
		if(banSub.readOnly) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Ignoring tags; subreddit /r/%s can't write", banSub.subreddit); }
			relevant = Collections.emptyList();
		}else {
			relevant = getRelevant(tags, ban);
		}
		
		if(extremeTrace) { logger.printf(Level.TRACE, "Relevant tags: %s", relevant.stream().map((a) -> a.toPrettyString(database)).collect(Collectors.joining(", "))); }
		
		USLAction latest = database.getUSLActionMapping().fetchLatest(ban.bannedPersonID);
		
		if(latest != null) {
			return processBanWhenNotFirst(tags, modAction, ban, relevant, latest);
		}
		
		if(relevant.isEmpty()) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Skipping; no relevant tags and no existing action"); }
			return Collections.emptySet();
		}
		if(extremeTrace) { logger.printf(Level.TRACE, "Creating first action for this user"); }
		
		USLAction action = database.getUSLActionMapping().create(true, ban.bannedPersonID, modAction.occurredAt);
		
		Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		if(ban.modPersonID != bot.id) { 
			for(Hashtag tag : relevant) {
				if(extremeTrace) { logger.printf(Level.TRACE, "Attaching tag %s", tag.tag); }
				database.getUSLActionHashtagMapping().save(new USLActionHashtag(action.id, tag.id));
			}
		}else {
			if(extremeTrace) { logger.printf(Level.TRACE, "Not attaching any tags since this was done by the bot"); }
		}
		
		List<MonitoredSubreddit> subreddits = database.getMonitoredSubredditMapping().fetchAll();
		for(MonitoredSubreddit sub : subreddits) {
			List<BanHistory> bansOnSub = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(ban.bannedPersonID, sub.id);
			BanHistory bestBan = null;
			Timestamp bestTime = null;
			for(BanHistory subBan : bansOnSub) {
				HandledModAction hma = database.getHandledModActionMapping().fetchByID(subBan.handledModActionID);
				if(!hma.occurredAt.after(modAction.occurredAt)) {
					if(bestBan == null || hma.occurredAt.after(bestTime)) {
						bestBan = subBan;
						bestTime = hma.occurredAt;
					}
				}
			}
			bansOnSub = null;
			
			List<UnbanHistory> unbansOnSub = database.getUnbanHistoryMapping().fetchUnbanHistoriesByPersonAndSubreddit(ban.bannedPersonID, sub.id);
			UnbanHistory bestUnban = null;
			Timestamp bestUnbanTime = null;
			for(UnbanHistory subUnban : unbansOnSub) {
				HandledModAction hma = database.getHandledModActionMapping().fetchByID(subUnban.handledModActionID);
				if(!hma.occurredAt.after(modAction.occurredAt)) { // It's important that we match == 
					if(bestUnban == null || hma.occurredAt.after(bestUnbanTime)) {
						bestUnban = subUnban;
						bestUnbanTime = hma.occurredAt;
					}
				}
			}
			
			if(bestBan == null && bestUnban == null)
				continue;
			
			if(bestUnban == null || (bestBan != null && bestTime.after(bestUnbanTime))) { // here we give the benefit of the doubt to unbans
				if(extremeTrace) { logger.printf(Level.TRACE, "Attaching ban %s", bestBan.toPrettyString(database)); }
				database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, bestBan.id));
			}else {
				if(extremeTrace) { logger.printf(Level.TRACE, "Attaching unban %s", bestUnban.toPrettyString(database)); }
				database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, bestUnban.id));
			}
		}
		
		return Collections.singleton(action.personID);
	}
	
	/**
	 * Processes the given unban and modifies / creates a USLAction and mapping tables to take it into account.
	 * This assumes that this is the *most recent* action that is being processed. Order is very important for
	 * using this function.
	 * 
	 * 
	 * @param tags the tags that we are processing on
	 * @param modAction the reference to the mod action on reddit
	 * @param unban the unban that occurred
	 * @return the people that were affected by this processing
	 */
	public Set<Integer> processUnban(List<Hashtag> tags, HandledModAction modAction, UnbanHistory unban) {
		if(extremeTrace) { logger.printf(Level.TRACE, "processUnban unban=%s", unban.toPrettyString(database)); }
		
		USLAction latest = database.getUSLActionMapping().fetchLatest(unban.unbannedPersonID);
		
		if(latest == null) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Skipping; no active action for this person"); }
			return Collections.emptySet();
		}

		Person unbannedPerson = database.getPersonMapping().fetchByID(unban.unbannedPersonID);
		if(unbannedPerson.username.equals("[deleted]")) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Skipping since this is [deleted]"); }
			return Collections.emptySet();
		}
		
		BanHistory curBan = database.getBanHistoryMapping().fetchByActionAndSubreddit(latest.id, modAction.monitoredSubredditID);
		if(curBan != null) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Detaching ban %s", curBan.toPrettyString(database)); }
			database.getUSLActionBanHistoryMapping().delete(latest.id, curBan.id);
		}
		
		// Since we give == times to the unbans, we need to do this check for unbans but not for bans when things are correct
		UnbanHistory curUnban = database.getUnbanHistoryMapping().fetchByActionAndSubreddit(latest.id, modAction.monitoredSubredditID);
		if(curUnban != null) {
			if(extremeTrace) { logger.printf(Level.TRACE, "Detaching unban %s", curUnban.toPrettyString(database)); }
			database.getUSLActionUnbanHistoryMapping().delete(latest.id, curUnban.id);
		}
		
		if(extremeTrace) { logger.printf(Level.TRACE, "Attaching the unban"); }
		database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(latest.id, unban.id));
		return Collections.singleton(unban.unbannedPersonID);
	}
}
