package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.ResetPasswordRequest;

/**
 * Maps reset password requests to/from the database
 * 
 * @author Timothy
 */
public interface ResetPasswordRequestMapping extends ObjectMapping<ResetPasswordRequest> {
	/**
	 * Fetch up to limit unsent reset password requests, oldest created
	 * at to newest
	 * 
	 * @param limit maximum number of results
	 * @return up to limit unsent reset password requests
	 */
	public List<ResetPasswordRequest> fetchUnsent(int limit);
}
