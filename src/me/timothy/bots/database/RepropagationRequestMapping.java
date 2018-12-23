package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.RepropagationRequest;

/**
 * Maps requests to repropagate to/from the database
 * 
 * @author Timothy
 */
public interface RepropagationRequestMapping extends ObjectMapping<RepropagationRequest> {
	/**
	 * Returns all the repropagation requests that have not been handled yet.
	 * 
	 * @return unhandled repropagation requests.
	 */
	public List<RepropagationRequest> fetchUnhandled();
	
	/**
	 * Fetch the repropagation request with the given row identifier
	 * 
	 * @param id the id of the request you are interested in
	 * @return the complete request
	 */
	public RepropagationRequest fetchByID(int id);
}
