package me.timothy.bots.database;

import java.sql.Timestamp;
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
	
	/**
	 * Fetch all unban requests for the specified person, including results that are invalid, 
	 * but not requests that have not been handled yet
	 * 
	 * @param personID the person you are interested in
	 * @return all unban requests for that person
	 */
	public List<UnbanRequest> fetchHandledByBannedPerson(int personID);
	
	/**
	 * Fetch UnbanRequests that have been handled and are valid. Returns requests
	 * where they were *handled* at or later than after, and strictly before before.
	 * Returns no more than num results. The returned results are in ascending order
	 * on the handled at timestamp.
	 * 
	 * @param after The minimum time for returned results
	 * @param before
	 * @param num
	 * @return
	 */
	public List<UnbanRequest> fetchLatestValid(Timestamp after, Timestamp before, int num);
	
	/**
	 * Fetch the unban request with the given id.
	 * 
	 * @param id the id of the request you are interested in
	 * @return the request with that id
	 */
	public UnbanRequest fetchByID(int id);
}
