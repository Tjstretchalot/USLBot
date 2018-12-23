package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.DirtyPerson;

/**
 * This acts as a giant pile of dirty people with no enforced ordering whatsoever. Every entry
 * in this mapping needs to get sent to the propagator, and the order does not matter.
 * 
 * @author Timothy
 */
public interface DirtyPersonMapping extends ObjectMapping<DirtyPerson> {
	/**
	 * Fetch up to the specified limit number of entries in the mapping.
	 * 
	 * @param limit the maximum number of entries to return
	 * @return up to limit dirty persons, with no respect to insertion order
	 */
	public List<DirtyPerson> fetch(int limit);
	
	/**
	 * Determines if the given person is in this collection.
	 * 
	 * @param personId the id of the person you are interested in
	 * @return if there is a row with the given person id
	 */
	public boolean contains(int personId);
	
	/**
	 * Deletes the row with the given person id, if there is one.
	 * 
	 * @param personId the id of the person in the row you want to delete
	 */
	public void delete(int personId);
	
	/**
	 * Counts the number of rows that are in this database.
	 * 
	 * @return the number of rows in the database
	 */
	public int count();
}
