package me.timothy.bots.database;

import java.util.List;
import java.util.Set;

import me.timothy.bots.models.HardwareSwapBan;

public interface HardwareSwapBanMapping extends ObjectMapping<HardwareSwapBan> {
	/**
	 * Fetch the ban with the given row identifier
	 * @param id the row identifier you are interested in
	 * @return the ban with that row id, or null if there isn't one
	 */
	public HardwareSwapBan fetchByID(int id);
	
	/**
	 * Fetch the ban which corresponds to the given person, if there is one
	 * 
	 * @param personID the person you are interested in
	 * @return the ban with that person or null if there isn't one
	 */
	public HardwareSwapBan fetchByPersonID(int personID);
	
	/**
	 * Fetches any hardware swap actions that are not associated with one of the persons in 
	 * the specified set. This works by creating a temporary table, filling it with person 
	 * ids, performing the requested query, deleting the temporary table, and returning.
	 * 
	 * @param personIds the set of ids that you expect actions on
	 * @return any rows that have a person id not in the specified set
	 */
	public List<HardwareSwapBan> fetchWherePersonNotIn(Set<Integer> personIds);
	
	/**
	 * Fetches all the bans on person without an associated HardwareSwapAction
	 * 
	 * @param limit the maximum number of results
	 * @return the users which we need to ban
	 */
	public List<HardwareSwapBan> fetchWithoutAction(int limit);
	
	/**
	 * Deletes the row with the given id
	 * @param id the id of the row you want to delete
	 */
	public void deleteByID(int id);
}
