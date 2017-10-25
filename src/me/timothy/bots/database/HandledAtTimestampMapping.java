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
	 * Fetch all HandledAtTimestamp for the specified monitoredSubredditID.
	 * 
	 * @param monitoredSubredditID the monitoerd subreddit
	 * @return the list of handled mod action ids that have been handled for that subreddit at the latest timestamp
	 */
	public List<HandledAtTimestamp> fetchByMonitoredSubredditID(int monitoredSubredditID);
	
	/**
	 * Delete all HandledAtTimestamps for the specified monitored subreddit id
	 * 
	 * @param monitoredSubredditID the subreddit to remove all from
	 */
	public void deleteByMonitoredSubredditID(int monitoredSubredditID);
}
