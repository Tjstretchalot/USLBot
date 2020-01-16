package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.HardwareSwapAction;

public interface HardwareSwapActionMapping extends ObjectMapping<HardwareSwapAction> {
	/**
	 * Fetch the action with the given database row identifier, if there is one
	 * @param id the row identifier you are interested in
	 * @return the corresponding row
	 */
	public HardwareSwapAction fetchByID(int id);
	
	/**
	 * Fetch the action for the given person, if there is one
	 * @param personID the person you are interested in
	 * @return the corresponding row, if there is one
	 */
	public HardwareSwapAction fetchByPersonID(int personID);
	
	/**
	 * Fetches all actions which have a person who does not have an associated
	 * HardwareSwapBan
	 * 
	 * @return the bans we have on users which are no longer banned
	 */
	public List<HardwareSwapAction> fetchActionsOnUnbanned();
	
	/**
	 * Deletes the row with the given id
	 * @param id the id you want to delete
	 */
	public void deleteByID(int id);
}
