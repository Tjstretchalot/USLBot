package me.timothy.bots.database;

import java.sql.Timestamp;
import java.util.List;

import me.timothy.bots.models.HandledModAction;

/**
 * Maps HandledModActions to/from the database
 * 
 * @author Timothy
 */
public interface HandledModActionMapping extends ObjectMapping<HandledModAction> {
	/**
	 * Fetch the HandledModAction by its id
	 * @param id the id
	 * @return the handled mod action with that id or null
	 */
	public HandledModAction fetchByID(int id);
	
	/**
	 * Fetch the HandledModAction by the mod action id
	 * @param modActionID the mod action id
	 * @return the handled mod action with the specified modAction id or null
	 */
	public HandledModAction fetchByModActionID(String modActionID);

	/**
	 * Fetch handled mod actions by timestamp
	 * 
	 * @param timestamp the time stamp
	 * @return handled mod actions with that timestamp
	 */
	public List<HandledModAction> fetchByTimestamp(Timestamp timestamp);
	
	/**
	 * Fetch handled mod actions after the specified timestamp
	 * 
	 * @param after after time
	 * @param num number
	 * @return up to num handled mod actions strictly later than after
	 */
	public List<HandledModAction> fetchLatest(Timestamp after, int num);
	
	/**
	 * Reddit only uses second-precision on its result, so theres a very
	 * good chance of timestamp collisions. It's important to handle these.
	 * 
	 * @param monitoredSubredditID the subreddit
	 * @param timestamp the timestamp
	 * @return the list of handled mod actions at the given timestamp for the specified subreddit
	 */
	@Deprecated
	public List<HandledModAction> fetchByTimestampForSubreddit(int monitoredSubredditID, Timestamp timestamp);
	
	/**
	 * Fetches at most num HandledModActions, such that occurredAt is strictly
	 * greater than after.
	 * 
	 * @param monitoredSubredditID the subreddit
	 * @param after the id to fetch results after
	 * @param num the maximum number of results returned
	 * @return at most num HandledModAction with id>after in ascending order
	 */
	@Deprecated
	public List<HandledModAction> fetchLatestForSubreddit(int monitoredSubredditID, Timestamp after, int num);
	
}
