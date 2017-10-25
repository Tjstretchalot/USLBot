package me.timothy.bots.database;

import java.util.Collection;
import java.util.List;

import me.timothy.bots.models.UnbanHistory;

/**
 * Maps subreddit unbans between the database and memory. Similiar
 * to BanHistory, these should only be saved as a direct result of
 * a response from reddit.
 * 
 * @author Timothy
 */
public interface UnbanHistoryMapping extends ObjectMapping<UnbanHistory> {
	/**
	 * Fetch the unban history with the specified id
	 * @param id the id
	 * @return the unban history with that id or null
	 */
	public UnbanHistory fetchByID(int id);
	
	/**
	 * Fetch the unban history with the specified handled mod action id
	 * 
	 * @param handledModActionID the handled mod action id
	 * @return the unban history with that id
	 */
	public UnbanHistory fetchByHandledModActionID(int handledModActionID);
	
	/**
	 * Fetch each unban history that has a handledModActionID that is in the given collection
	 * of handledModActionsIDs.
	 * 
	 * @param handledModActionIDs the ids to set
	 * @return the list of unban historys with handledModActionID that is in the collection
	 */
	public List<UnbanHistory> fetchByHandledModActionIDS(Collection<Integer> handledModActionIDs);
	
	/**
	 * Fetches the unban history of a user on a specific subreddit. Useful for determining 
	 * if we already know that a user is banned/unbanned on a subreddit by comparing 
	 * timestamps
	 * 
	 * If there are multiple results, returns the latest one.
	 * 
	 * @param unbannedPersonId the unbanned person
	 * @param monitoredSubredditId the monitored subreddit id
	 * @return unban history with banned user personId and subreddit subredditID or null
	 */
	public UnbanHistory fetchUnbanHistoryByPersonAndSubreddit(int unbannedPersonId, int monitoredSubredditId);
	

	/**
	 * Fetches the unban histories of a user on a specific subreddit. Useful for determining 
	 * if we already know that a user is banned/unbanned on a subreddit by comparing 
	 * timestamps
	 * 
	 * @param unbannedPersonId the unbanned person
	 * @param monitoredSubredditId the monitored subreddit id
	 * @return unban histories with banned user personId and subreddit subredditID
	 */
	public List<UnbanHistory> fetchUnbanHistoriesByPersonAndSubreddit(int unbannedPersonId, int monitoredSubredditId);
}
