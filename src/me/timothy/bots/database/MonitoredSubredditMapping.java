package me.timothy.bots.database;

import me.timothy.bots.models.MonitoredSubreddit;

/**
 * Describes a mapping for monitored subreddits
 * 
 * @author Timothy
 * @see me.timothy.bots.models.MonitoredSubreddit
 */
public interface MonitoredSubredditMapping extends ObjectMapping<MonitoredSubreddit> {
	/**
	 * Fetch all the monitored subreddits and join them on their
	 * subreddit with a + seperator.
	 * 
	 * @return the subreddits concatenated
	 */
	public String fetchAllAndConcatenate();
}
