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
	
	/**
	 * Fetch the monitored subreddit in the database with the specified
	 * id, or null if no such monitored subreddit exists in the database.
	 * 
	 * @param id the id to search for
	 * @return monitored subreddit with specified id or null
	 */
	public MonitoredSubreddit fetchByID(int id);

	/**
	 * Fetch the subreddit with the specified name, case insensitive
	 * 
	 * @param name name of the subreddit
	 * @return subreddit with that name, case insensitive
	 */
	public MonitoredSubreddit fetchByName(String name);
}
