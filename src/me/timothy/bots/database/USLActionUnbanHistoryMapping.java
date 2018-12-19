package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.USLActionUnbanHistory;

/**
 * This handles producing the mapping between usl actions and unban histories.
 * Note that the unban histories that are mapped this way are all of the unban
 * histories for tracked subreddits which do not have a later ban. This means
 * that if the person is mapped in this way, then they are NOT banned on the
 * subreddit.
 * 
 * If they are not mapped via this nor the ban history mapping, then it's ambiguous if the 
 * person is or is not banned.
 * 
 * @author Timothy
 */
public interface USLActionUnbanHistoryMapping extends ObjectMapping<USLActionUnbanHistory> {
	/**
	 * Fetch all of the maps where it has the given usl action id.
	 * 
	 * @param uslActionId the id of the USLAction
	 * @return the mapping to the unbans for that action
	 */
	public List<USLActionUnbanHistory> fetchByUSLActionID(int uslActionId);
	
	/**
	 * Fetch all of the maps where we have the given unban history.
	 * 
	 * @param unbanHistoryId the id of the unban history
	 * @return the mapping to the actions for that unban history
	 */
	public List<USLActionUnbanHistory> fetchByUnbanHistoryID(int unbanHistoryId);
	
	/**
	 * Delete the row with the specified columns
	 * @param uslActionId the id of the action
	 * @param unbanHistoryId the id of the unban history
	 */
	public void delete(int uslActionId, int unbanHistoryId);
}
