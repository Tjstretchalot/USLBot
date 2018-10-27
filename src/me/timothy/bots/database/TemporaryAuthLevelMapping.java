package me.timothy.bots.database;

import me.timothy.bots.models.TemporaryAuthLevel;

/**
 * Maps TemporaryAuthLevels to/from the database.
 * 
 * @author Timothy
 */
public interface TemporaryAuthLevelMapping extends ObjectMapping<TemporaryAuthLevel> {
	/**
	 * Fetch the TemporaryAuthLevel corresponding with the given person.
	 * 
	 * @param personID the person
	 * @return the corresponding temporary auth level
	 */
	public TemporaryAuthLevel fetchByPersonID(int personID);
	
	/**
	 * Delete the row with the given id.
	 * 
	 * @param id the id of the row to delete
	 */
	public void deleteById(int id);
}
