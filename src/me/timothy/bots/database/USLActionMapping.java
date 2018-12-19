package me.timothy.bots.database;

import java.sql.Timestamp;
import java.util.List;

import me.timothy.bots.models.USLAction;

/**
 * Maps USLAction's to/from the database. 
 * 
 * @author Timothy
 */
public interface USLActionMapping extends ObjectMapping<USLAction> {
	/**
	 * Get all the actions which are after the given start id, up to the specified maximum number
	 * of responses. Typically you want to just ignore non-latest.
	 * @param startId the first id to look at. Inclusive.
	 * @param limit the maximum number of rows to fetch.
	 * @param returnAllRowsWithoutRestriction true to return rows where is_latest is false. false to not get rows where is_latest is false
	 * @return Up to limit USLActions who have an id as great or greater than startId
	 */
	public List<USLAction> getActionsAfter(int startId, int limit, boolean returnAllRowsWithoutRestriction);
	
	/**
	 * Get the latest action for the given person
	 * @param personId the person
	 * @return the latest action or null if no actions for that person
	 */
	public USLAction fetchLatest(int personId);
	
	/**
	 * Fetch the action with the given id.
	 * @param id the id of the row you want
	 * @return the corresponding action
	 */
	public USLAction fetchByID(int id);
	
	/**
	 * Fetch all actions for the given person, regardless of if they are the latest or not.
	 * This is only used for reports.
	 * 
	 * @param personId the person you are interested in
	 * @return all of the actions for that person
	 */
	public List<USLAction> fetchByPerson(int personId);
	
	/**
	 * Creates a new uslaction row for the given person id. You must populate all the USLActionXXX mappings to make
	 * this row actually meaningful.
	 * 
	 * This will ensure that if there is an existing row, is_latest is set to false.
	 * 
	 * @param isBan false if this is only removing tags that the person is banend with, true otherwise
	 * @param personId the id of the person affected}
	 * @param ocurredAt when the thing ocurred that caused this action to be generated.
	 * @return the row that was just created. Includes the id.
	 */
	public USLAction create(boolean isBan, int personId, Timestamp ocurredAt);
}
