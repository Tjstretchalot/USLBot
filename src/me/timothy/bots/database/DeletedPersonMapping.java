package me.timothy.bots.database;

import me.timothy.bots.models.DeletedPerson;

/**
 * Acts as a set of persons which are deleted. This will have a very low number of writes
 * and a moderate number of reads, all of which will be by personID.
 * 
 * @author Timothy
 */
public interface DeletedPersonMapping extends ObjectMapping<DeletedPerson> {
	/**
	 * Add the given person to the deleted person mapping if he is not already there.
	 * 
	 * @param personID the person to add
	 */
	public void addIfNotExists(int personID);
	
	/**
	 * Determines if the given person is in the database
	 * 
	 * @param personID the id of the person of interest
	 * @return true if the person is in this mapping and has thus deleted their account, false otherwise
	 */
	public boolean contains(int personID);
}
