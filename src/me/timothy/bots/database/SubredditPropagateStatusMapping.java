package me.timothy.bots.database;

import me.timothy.bots.models.SubredditPropagateStatus;

/**
 * Maps the SubredditPropagateStatus model to/from the database/memory
 * 
 * @author Timothy
 */
public interface SubredditPropagateStatusMapping extends ObjectMapping<SubredditPropagateStatus> {
	/**
	 * Fetch the propagate status for the given subreddit
	 * 
	 * @param monitoredSubredditID the subreddit whose status you want
	 * @return the propagate status for that subreddit
	 */
	public SubredditPropagateStatus fetchForSubreddit(int monitoredSubredditID);
	
	/**
	 * Fetch or create the propagate status for the given subreddit. This creates it with
	 * the actionID 
	 * @param monitoredSubredditID
	 * @return
	 */
	public SubredditPropagateStatus fetchOrCreateForSubreddit(int monitoredSubredditID);
}
