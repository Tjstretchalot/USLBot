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

	/**
	 * This is a merged action. It must lookup each of the unban histories that this is mapped to
	 * the given action, then figure out the monitored subreddit for the unban history. It then returns
	 * the unban history that is mapped to the given action that is for the given subreddit. Since
	 * this is a lookup for a unban history, it is placed in the unban history mapping.
	 * 
	 * @param uslActionId the action
	 * @param subredditId the subreddit you are interested in
	 * @return the unban history mapped to the given action with the given subreddit id.
	 */
	public UnbanHistory fetchByActionAndSubreddit(int uslActionId, int subredditId);
	
	/**
	 * Fetch all unbans for the given person. This is hard to use directly.
	 * 
	 * @param personId the person you are interested
	 * @return all unbans with the specified unbanned person id
	 */
	public List<UnbanHistory> fetchByPerson(int personId);
}
