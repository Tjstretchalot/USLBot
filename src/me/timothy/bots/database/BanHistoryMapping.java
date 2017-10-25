package me.timothy.bots.database;

import java.util.Collection;
import java.util.List;

import me.timothy.bots.models.BanHistory;

public interface BanHistoryMapping extends ObjectMapping<BanHistory> {
	/**
	 * Fetch the ban history with the specified id
	 * @param id the id
	 * @return the ban history with that id or null
	 */
	public BanHistory fetchByID(int id);
	
	/**
	 * Fetch the ban history with the specified handled mod action id
	 * @param handledModActionID the id of the handled mod action
	 * @return the banhistory with that handledmodactionid or null
	 */
	public BanHistory fetchByHandledModActionID(int handledModActionID);
	
	/**
	 * Fetch all of the ban histories with a handled mod action id that is in the
	 * collection of handled mod action ids.
	 * 
	 * @param handledModActionIDs the list of handled mod action ids
	 * @return the corresponding ban histories
	 */
	public List<BanHistory> fetchByHandledModActionIDs(Collection<Integer> handledModActionIDs);
	
	/**
	 * Fetches the ban history of a user on a specific subreddit. Useful for determining 
	 * if we already know that a user is banned on a subreddit. This operation requires
	 * getting the monitored subreddit id from the handled mod action.
	 * 
	 * If there are multiple results, returns the latest one.
	 * 
	 * @param bannedPersonId the banned person
	 * @param monitoredSubredditId the monitored subreddit id
	 * @return ban history with banned user personId and subreddit subredditID or null
	 */
	public BanHistory fetchBanHistoryByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId);
	
	/**
	 * Fetches all ban histories with the specified banned person on the specified subreddit.
	 * 
	 * @param bannedPersonId banned person id
	 * @param monitoredSubredditId subreddit id
	 * @return
	 */
	public List<BanHistory> fetchBanHistoriesByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId);
}
