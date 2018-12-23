package me.timothy.bots.functions;

/**
 * This function submits a self-post to a subreddit.
 * 
 * @author Timothy
 */
public interface SubmitSelfFunction {
	/**
	 * Submit a self-post to the given subreddit with the given submission title and body.
	 * 
	 * @param subreddit the subreddit to post on, i.e. "test" or "universalscammerlist"
	 * @param title the title of the submission
	 * @param body the body of the submission / the self-text for the post
	 */
	public void submitSelf(String subreddit, String title, String body);
}
