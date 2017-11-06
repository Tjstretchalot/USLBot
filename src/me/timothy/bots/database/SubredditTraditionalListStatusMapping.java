package me.timothy.bots.database;

import me.timothy.bots.models.SubredditTraditionalListStatus;

/**
 * Maps to/from the database and memory for the status of a subreddit of 
 * handling the traditional scammer list
 * 
 * @author Timothy
 */
public interface SubredditTraditionalListStatusMapping extends ObjectMapping<SubredditTraditionalListStatus> {
	/**
	 * Get the status for the specified subreddit
	 * 
	 * @param monitoredSubredditID the id of the subreddit
	 * @return that subreddits traditional list status
	 */
	public SubredditTraditionalListStatus fetchBySubredditID(int monitoredSubredditID);
}
