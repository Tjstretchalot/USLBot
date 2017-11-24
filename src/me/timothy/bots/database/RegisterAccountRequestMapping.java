package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.RegisterAccountRequest;

/**
 * Maps RegisterAccountRequest s to/from the database.
 * 
 * @author Timothy Moore
 */
public interface RegisterAccountRequestMapping extends ObjectMapping<RegisterAccountRequest> {
	/**
	 * Fetch up to limit RegisterAccountRequests which have unsent_at = null
	 * 
	 * @param limit maximum number to fetch.
	 * @return up to limit unsent register account requests
	 */
	public List<RegisterAccountRequest> fetchUnsent(int limit);
}
