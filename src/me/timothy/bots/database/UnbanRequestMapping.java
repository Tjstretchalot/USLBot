package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.UnbanRequest;

/**
 * Mapping for unban requests to/from the database
 * 
 * @author Timothy
 */
public interface UnbanRequestMapping extends ObjectMapping<UnbanRequest> {
	/**
	 * Fetch up to limit unhandled unban requests
	 * 
	 * @param limit maximum number of results 
	 * @return up to limit unban requests that are unhandled
	 */
	public List<UnbanRequest> fetchUnhandled(int limit);
	
	/**
	 * Fetch the latest unban request for the specified banned person,
	 * not returning results that have been handled and are invalid
	 * 
	 * @param personID the person
	 * @return the latest unban request for that person or null
	 */
	public UnbanRequest fetchLatestValidByBannedPerson(int personID);
}
