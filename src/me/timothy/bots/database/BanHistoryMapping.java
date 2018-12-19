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
	 * Fetch all the time we know of that the specified person was banned
	 * on one of our monitored subreddits
	 * 
	 * @param bannedPersonId the person
	 * @return when he was banned
	 */
	public List<BanHistory> fetchBanHistoriesByPerson(int bannedPersonId);
	
	/**
	 * Fetches all ban histories with the specified banned person on the specified subreddit.
	 * 
	 * @param bannedPersonId banned person id
	 * @param monitoredSubredditId subreddit id
	 * @return
	 */
	public List<BanHistory> fetchBanHistoriesByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId);

	/**
	 * This is a merged action. It must lookup each of the ban histories that this is mapped to
	 * the given action, then figure out the monitored subreddit for the ban history. It then returns
	 * the ban history that is mapped to the given action that is for the given subreddit. Since
	 * this is a lookup for a ban history, it is placed in the ban history mapping.
	 * 
	 * @param uslActionId the action
	 * @param subredditId the subreddit you are interested in
	 * @return the ban history mapped to the given action with the given subreddit id.
	 */
	public BanHistory fetchByActionAndSubreddit(int uslActionId, int subredditId);
}
