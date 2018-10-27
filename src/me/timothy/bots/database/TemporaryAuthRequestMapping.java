package me.timothy.bots.database;

import me.timothy.bots.models.TemporaryAuthRequest;

/**
 * Describes a mapping of TemporaryAuthRequests to/from the database.
 * 
 * @author Timothy
 */
public interface TemporaryAuthRequestMapping extends ObjectMapping<TemporaryAuthRequest> {
	/**
	 * Get the oldest request or null if there are no requests.
	 * 
	 * @return the oldest request still in the database
	 */
	public TemporaryAuthRequest fetchOldestRequest();
	
	/**
	 * Delete the request by the given id
	 * @param id the id of the request to delete
	 */
	public void deleteById(int id);
}
