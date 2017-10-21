package me.timothy.bots.database;

import me.timothy.bots.models.SubredditPropagateStatus;

/**
 * Maps the SubredditPropagateStatus model to/from the database/memory
 * 
 * @author Timothy
 */
public interface SubredditPropagateStatusMapping extends ObjectMapping<SubredditPropagateStatus> {
	/**
	 * Fetches the subreddit propagate status for the specified monitored subreddit.
	 * 
	 * @param monitoredSubredditID the id of the monitored subreddit
	 * @return the monitored subreddits propagate status
	 */
	public SubredditPropagateStatus fetchForSubreddit(int monitoredSubredditID);
}
