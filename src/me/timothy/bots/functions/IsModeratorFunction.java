package me.timothy.bots.functions;

/**
 * Function definition for "isModerator"
 * 
 * @author Timothy
 */
@FunctionalInterface
public interface IsModeratorFunction {
	/**
	 * Determine if the specified user is a moderator of the specified subreddit
	 *  
	 * @param subreddit the subreddit to check
	 * @param user the user to check
	 * @return if the user is a moderator of subreddit
	 */
	public Boolean isModerator(String subreddit, String user);
}
