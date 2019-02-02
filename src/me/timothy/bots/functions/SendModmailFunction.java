package me.timothy.bots.functions;

import me.timothy.bots.memory.ModmailPMInformation;

/**
 * Sends some modmail to the specified subreddit, redirecting as appropriate.
 * 
 * @author Timothy
 */
public interface SendModmailFunction {
	/**
	 * Send a notification to the given subreddit, via modmail unless the subreddit preferences
	 * specify an alternative.
	 * 
	 * @param info the information about the pm
	 */
	public void sendModmail(ModmailPMInformation info);
}
