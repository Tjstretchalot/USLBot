package me.timothy.bots.database;

import java.util.List;

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
	
	/**
	 * Returns a list of just the ids of the monitored subreddits which subscribe
	 * to the given hashtag.
	 * 
	 * @param hashtagID the id of the hashtag
	 * @return the ids for all subreddits that subscribe to the given hashtag.
	 */
	public List<Integer> fetchIDsThatFollow(int hashtagID);
	
	/**
	 * Returns a list of just the ids of the monitored subreddits which subscribe to
	 * a hashtag which is mapped to a given USLAction.
	 * 
	 * @param actionID the id of the action which you are interested in
	 * @return all the subreddits which follow tags that are mapped to that action
	 */
	public List<Integer> fetchReadableIDsThatFollowActionsTags(int actionID);
}
