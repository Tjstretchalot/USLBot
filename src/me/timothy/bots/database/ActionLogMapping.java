package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.ActionLog;

/**
 * Maps action logs to/from the database. Expected to be cleared as the cycle restarts.
 * 
 * @author Timothy
 */
public interface ActionLogMapping extends ObjectMapping<ActionLog> {
	/**
	 * Append the specified action string, assuming it occurs
	 * at the current timestamp.
	 * 
	 * @param action the action string
	 */
	public void append(String action);
	
	/**
	 * Fetch actions ordered by time
	 * 
	 * @return the action log, ordered by time
	 */
	public List<ActionLog> fetchOrderedByTime();
	
	/**
	 * Empties out the table
	 */
	public void clear();
}
