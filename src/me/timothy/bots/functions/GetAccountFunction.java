package me.timothy.bots.functions;

import me.timothy.jreddit.info.Account;

/**
 * A function that retrieves the account for a username
 * 
 * @author Timothy
 */
public interface GetAccountFunction {
	/**
	 * Get the account for the given person
	 * 
	 * @param person the person
	 * @return the persons account info
	 */
	public Account getAccount(String person);
}
