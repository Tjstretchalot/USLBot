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
	 * @deprecated use with subreddit
	 */
	public List<HandledModAction> fetchByTimestamp(Timestamp timestamp);
	
	/**
	 * Fetch the handled mod actions that occurred at the specified timestamp
	 * for the specified subreddit id
	 * 
	 * @param timestamp the timestamp
	 * @param subredditID the subreddit id
	 * @return handled mod actions meeting the specified criteria
	 */
	public List<HandledModAction> fetchByTimestampAndSubreddit(Timestamp timestamp, int subredditID);
	
	/**
	 * Fetch handled mod actions between the specified timestamps
	 * 
	 * @param after after time
	 * @param before before time
	 * @param num number
	 * @return up to num handled mod actions strictly later than after
	 * @deprecated use with subreddit
	 */
	public List<HandledModAction> fetchLatest(Timestamp after, Timestamp before, int num);
	
	/**
	 * Fetch handled mod actions for the specified subreddit that is between
	 * the specified timestamps
	 * 
	 * @param subredditID the id for the subreddit
	 * @param after after time
	 * @param before before time or null for any
	 * @param num maximum number of results
	 * @return up to num hma's with mon_sub_id subreddit at or later than after and strictly before before
	 */
	public List<HandledModAction> fetchLatestForSubreddit(int subredditID, Timestamp after, Timestamp before, int num);
	
}
