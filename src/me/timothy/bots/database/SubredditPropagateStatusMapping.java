package me.timothy.bots.database;

import me.timothy.bots.models.SubredditPropagateStatus;

/**
 * Maps the SubredditPropagateStatus model to/from the database/memory
 * 
 * @author Timothy
 */
public interface SubredditPropagateStatusMapping extends ObjectMapping<SubredditPropagateStatus> {
	/**
	 * Fetches the subreddit propagate status for the major sub when it comes
	 * to propagating actions from the specified minor sub
	 * 
	 * @param majorSubID the major sub
	 * @param minorSubID the minor sub
	 */
	public SubredditPropagateStatus fetchForSubredditPair(int majorSubID, int minorSubID);
}
