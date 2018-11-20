package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.AcceptModeratorInviteRequest;

/**
 * Mapps AcceptModeratorInviteRequests to/from the database
 * 
 * @author Timothy
 */
public interface AcceptModeratorInviteRequestMapping extends ObjectMapping<AcceptModeratorInviteRequest> {
	/**
	 * Get the unfulfilled invite requests, oldest first, and only up to the
	 * specified number of rows.
	 * 
	 * @param limit the maximum number of requests to ftech
	 * @return A list of invite requests, which might be empty if there are none
	 */
	public List<AcceptModeratorInviteRequest> fetchUnfulfilled(int limit);
}
