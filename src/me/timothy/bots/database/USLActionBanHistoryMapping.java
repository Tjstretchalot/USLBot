package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.USLActionBanHistory;

/**
 * This maps USLActions and BanHistories together. This particular mapping returns the relationship
 * between those two.
 * 
 * @author Timothy
 */
public interface USLActionBanHistoryMapping extends ObjectMapping<USLActionBanHistory> {
	/**
	 * Fetch all of the maps where it has the given usl action id.
	 * 
	 * @param uslActionId the id of the USLAction
	 * @return the mapping to the bans for that action
	 */
	public List<USLActionBanHistory> fetchByUSLActionID(int uslActionId);
	
	/**
	 * Fetch all of the maps where we have the given ban history.
	 * 
	 * @param banHistoryId the id of the ban history
	 * @return the mapping to the actions for that ban history
	 */
	public List<USLActionBanHistory> fetchByBanHistoryID(int banHistoryId);
	
	/**
	 * Determines if this mapping contains the given relationship
	 * 
	 * @param uslActionId the action
	 * @param banHistoryId the ban history
	 * @return if that relationship is in the database
	 */
	public boolean contains(int uslActionId, int banHistoryId);
	
	/**
	 * Delete the row with the specified columns
	 * @param uslActionId the id of the action
	 * @param banHistoryId the id of the ban history
	 */
	public void delete(int uslActionId, int banHistoryId);
	
}
