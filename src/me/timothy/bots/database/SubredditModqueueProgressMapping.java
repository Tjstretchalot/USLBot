package me.timothy.bots.database;

import me.timothy.bots.models.SubredditModqueueProgress;

/**
 * Maps the subreddit modqueue progress between the database and memory.
 * 
 * This mapping does NOT explicity set the updated_at timestamp. Since there is exactly one
 * timestamp and it's updated_at, the database is assumed to be able to handle it.
 * 
 * When using save or fetch you will retrieve the current updated_at. (In the case of save, you 
 * will recieve the new updated_at after the save operation ended)
 * 
 * @author Timothy
 */
public interface SubredditModqueueProgressMapping extends ObjectMapping<SubredditModqueueProgress> {
	/**
	 * Fetch the modqueue progress for the specified subreddit.
	 * 
	 * @param monitoredSubredditID the monitored subreddit id
	 * @return the progress for that id.
	 */
	public SubredditModqueueProgress fetchForSubreddit(int monitoredSubredditID);
}
