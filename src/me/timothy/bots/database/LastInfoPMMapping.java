package me.timothy.bots.database;

import me.timothy.bots.models.LastInfoPM;

/**
 * Maps LastInfoPMs to/from the database
 * 
 * @author Timothy Moore
 */
public interface LastInfoPMMapping extends ObjectMapping<LastInfoPM> {
	/**
	 * Fetch the latest LastInfoPM between modPersonID and userPersonID
	 * 
	 * @param modPersonID the mod person
	 * @param userPersonID the user person
	 * @return the latest one containing both
	 */
	public LastInfoPM fetchByModAndUser(int modPersonID, int userPersonID);
}
