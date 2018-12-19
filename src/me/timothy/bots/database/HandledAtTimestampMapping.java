package me.timothy.bots.database;

import java.sql.Timestamp;

import me.timothy.bots.models.HandledAtTimestamp;

/**
 * Maps HandledAtTimestamps to/from the database
 * 
 * @author Timothy
 */
public interface HandledAtTimestampMapping extends ObjectMapping<HandledAtTimestamp> {
	/**
	 * Determines if the given ban history is in this map.
	 * 
	 * @param banHistoryID the ban history you are interested in
	 * @return if it is in this database
	 */
	public boolean containsBanHistory(int banHistoryID);
	
	/**
	 * Determines if the given unban history id is in this map
	 * 
	 * @param unbanHistoryID the id of the unban history you are interested in
	 * @return if it is in the set
	 */
	public boolean containsUnbanHistory(int unbanHistoryID);
	
	/**
	 * Determines if the given unban request is in this map
	 * 
	 * @param unbanRequestID the id of the unban request you are interested in
	 * @return if it is in the set
	 */
	public boolean containsUnbanRequest(int unbanRequestID);
	
	/**
	 * Adds the given ban history to this collection
	 * 
	 * @param banHistoryID the history you handled
	 */
	public void addBanHistory(int banHistoryID);
	
	/**
	 * Adds the given unban history to this collection
	 * 
	 * @param unbanHistoryID the unban history you handled
	 */
	public void addUnbanHistory(int unbanHistoryID);
	
	/**
	 * Adds the given unban request to this collection
	 * 
	 * @param unbanRequestID the id of the unban request you are interested in
	 */
	public void addUnbanRequest(int unbanRequestID);
	
	/**
	 * Clears the list and sets it to the new timestamp.
	 * @param newTimestamp the new timestamp for the things that will be in this list
	 */
	public void clear(Timestamp newTimestamp);
}
