package me.timothy.bots.database;

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
	 * Fetch the ban history with the specified mod action id
	 * @param modActionID the id of the mod action
	 * @return the ban history with that mod action
	 */
	public BanHistory fetchByModActionID(String modActionID);
	
	/**
	 * Returns a List (of length num or lower) of BanHistorys whose
	 * id are greater than id, sorted by id in ascending order, such 
	 * that there are no gaps unless there is a corresponding gap in
	 * the database that will not be filled.
	 * 
	 * 
	 * @param id the maximum id to be excluded
	 * @param num maximum number of results
	 * @return
	 */
	public List<BanHistory> fetchBanHistoriesAboveIDSortedByIDAsc(int id, int num);
}
