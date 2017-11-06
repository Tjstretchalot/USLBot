package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.TraditionalScammer;

public interface TraditionalScammerMapping extends ObjectMapping<TraditionalScammer> {
	/**
	 * Fetch the entry with the specified banned person id, if one exists.
	 * 
	 * @param personID the id of the person to search for
	 * @return the entry with that bannedPersonID, if one exists
	 */
	public TraditionalScammer fetchByPersonID(int personID);

	/**
	 * Remove the entry with the specified banned person id from the database,
	 * if one exists
	 * 
	 * @param personID the person to remove
	 */
	public void deleteByPersonID(int personID);
	
	/**
	 * Fetch up to limit entries with an id greater than the specified id, in 
	 * ascending order
	 * 
	 * @param id the largest id NOT included
	 * @param limit maximum number of results
	 * @return entries with id greater than specified
	 */
	public List<TraditionalScammer> fetchEntriesAfterID(int id, int limit);
	
}
