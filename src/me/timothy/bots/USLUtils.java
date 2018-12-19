package me.timothy.bots;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.UnbanHistory;

/**
 * Collection of utility functions that are shared
 * 
 * @author Timothy
 */
public class USLUtils {

	/**
	 * Figure out what tags are relevant for the specified subreddit for a ban with
	 * the specified description
	 * 
	 * @param database database
	 * @param config config
	 * @param subreddit subreddit
	 * @param descriptionLower description on ban, lowercase
	 * @return the relevant hashtags
	 */
	public static List<SubscribedHashtag> getRelevantTags(USLDatabase database, USLFileConfiguration config,
			MonitoredSubreddit subreddit, String descriptionLower) {
		List<SubscribedHashtag> relevant = new ArrayList<>();
		List<SubscribedHashtag> hashtags = database.getSubscribedHashtagMapping().fetchForSubreddit(subreddit.id, false);
		for(SubscribedHashtag tag : hashtags) {
			Hashtag realTag = database.getHashtagMapping().fetchByID(tag.hashtagID);
			if(descriptionLower.contains(realTag.tag.toLowerCase())) {
				relevant.add(tag);
			}
		}
		return relevant;
	}
	
	/**
	 * Get the tags as a string with each hashtag seperated by a comma
	 * 
	 * @param tags the tags
	 * @return the tags as a string
	 */
	public static String combineTagsWithCommas(List<Hashtag> tags) {
		return String.join(", ", tags.stream().map(tag -> tag.tag).collect(Collectors.toList()));
	}

	
	/**
	 * Determine if the person with personID is already banned on subreddit according to our
	 * local database
	 * 
	 * @param database database
	 * @param personID the id of the person to check
	 * @param subreddit the subreddit to check on
	 * @return if the person is already banned on subreddit
	 */
	public static boolean alreadyBanned(USLDatabase database, int personID, MonitoredSubreddit subreddit) {
		BanHistory mostRecentBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(personID, subreddit.id);
		if(mostRecentBan == null)
			return false; // if we dont see a ban he's maybe probably not already banned
		
		if(!mostRecentBan.banDetails.equals("changed to permanent") && !mostRecentBan.banDetails.equals("permanent")) 
			return false; // if the most recent ban is not permanent he's probably not already banned
		
		UnbanHistory mostRecentUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(personID, subreddit.id);
		if(mostRecentUnban == null)
			return true; // if the most recent ban exists and is permanent, he's probably already banned
		
		HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(mostRecentBan.handledModActionID);
		HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(mostRecentUnban.handledModActionID);
		
		// if the most recent ban is permanent and after the most recent unban, he's banned
		return mostRecentBanHMA.occurredAt.after(mostRecentUnbanHMA.occurredAt);
	}
	
	/**
	 * Determine if it's possible that personID is not permanently banned on the specified subreddit
	 * 
	 * @param database database
	 * @param personID person to check
	 * @param subreddit subreddit to check
	 * @return if maybe personID is not banned on subreddit
	 */
	public static boolean potentiallyNotBannedOnSubreddit(USLDatabase database, int personID, MonitoredSubreddit subreddit) {
		UnbanHistory mostRecentUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(personID, subreddit.id);
		BanHistory mostRecentBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(personID, subreddit.id);
		if(mostRecentBan == null && mostRecentUnban == null)
			return true; // no information, so anything is possible
		
		if(mostRecentBan == null && mostRecentUnban != null)
			return true; // he's definitely not banned
		
		if(!mostRecentBan.banDetails.equals("changed to permanent") && !mostRecentBan.banDetails.equals("permanent")) 
			return true; // if the most recent ban is not permanent he's not permanently banned
		
		if(mostRecentUnban == null)
			return false; // if the most recent ban is permanent and there are no recent unbans, he's permanently banned
		
		HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(mostRecentBan.handledModActionID);
		HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(mostRecentUnban.handledModActionID);
		
		// if we banned before unbanning he's not banned
		return mostRecentBanHMA.occurredAt.before(mostRecentUnbanHMA.occurredAt);
	}
	
	/**
	 * Determine if there is any way that the specified person is currently banned on the specified subreddit
	 * 
	 * @param database the database
	 * @param personID the person
	 * @param subreddit the subreddit
	 * @return if it is possible that person is banned on subreddit
	 */
	public static boolean potentiallyBannedOnSubreddit(USLDatabase database, int personID, MonitoredSubreddit subreddit) {
		UnbanHistory mostRecentUnban = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(personID, subreddit.id);
		BanHistory mostRecentBan = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(personID, subreddit.id);
		if(mostRecentBan == null && mostRecentUnban == null)
			return true; // no information, so anything is possible
		
		if(mostRecentBan == null && mostRecentUnban != null)
			return false; // he's definitely not banned
		
		if(!mostRecentBan.banDetails.equals("changed to permanent") && !mostRecentBan.banDetails.equals("permanent")) 
			return false; // if the most recent ban is not permanent he's not permanently banned
		
		if(mostRecentUnban == null)
			return true; // if the most recent ban is permanent and there are no recent unbans, he's permanently banned
		
		HandledModAction mostRecentBanHMA = database.getHandledModActionMapping().fetchByID(mostRecentBan.handledModActionID);
		HandledModAction mostRecentUnbanHMA = database.getHandledModActionMapping().fetchByID(mostRecentUnban.handledModActionID);
		
		// if we banned after unbanning he's banned
		return mostRecentBanHMA.occurredAt.after(mostRecentUnbanHMA.occurredAt);
	}
}
