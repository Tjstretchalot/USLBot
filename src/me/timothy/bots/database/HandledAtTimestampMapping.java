package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.HandledAtTimestamp;

/**
 * Maps HandledAtTimestamps to/from the database
 * 
 * @author Timothy
 */
public interface HandledAtTimestampMapping extends ObjectMapping<HandledAtTimestamp> {
	/**
	 * Fetch all HandledAtTimestamp for the specified major/minor subreddit pair
	 * 
	 * @param majorSubredditID subreddit doing the propagating
	 * @param minorSubredditID subreddit being propagated
	 * @return the list of handled mod action ids that have been handled for that subreddit at the latest timestamp
	 */
	public List<HandledAtTimestamp> fetchBySubIDs(int majorSubredditID, int minorSubredditID);
	
	/**
	 * Delete all HandledAtTimestamps for the specified major/minor subreddit pair
	 * 
	 * @param majorSubredditID the subreddit doing the propagating
	 * @param minorSubredditID the subreddit being propagated
	 */
	public void deleteBySubIDs(int majorSubredditID, int minorSubredditID);
}
