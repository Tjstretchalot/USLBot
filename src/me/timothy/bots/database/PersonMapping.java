package me.timothy.bots.database;

import me.timothy.bots.models.Person;

/**
 * Describes a mapping for a particular person.
 * 
 * @author Timothy
 */
public interface PersonMapping extends ObjectMapping<Person> {
	/**
	 * Get the person with the specified username in the database, or 
	 * create a new one and return that if no such person exists.
	 * 
	 * If the new one is created, it is saved to the database and will
	 * be returned with a newly generated id and created at/updated at
	 * timestamps.
	 * 
	 * @param username the username
	 * @return the existing person with that username or a new one
	 */
	public Person fetchOrCreateByUsername(String username);
	
	/**
	 * Get the person with the specified username in the database, or 
	 * return null if no such person exists.
	 * 
	 * @param username the username
	 * @return the person with that username or null
	 */
	public Person fetchByUsername(String username);
	
	/**
	 * Get the person with the specified email in the database, or
	 * return null if no such person exists.
	 * 
	 * @param email the email
	 * @return the person with that email or null
	 */
	public Person fetchByEmail(String email);
}
