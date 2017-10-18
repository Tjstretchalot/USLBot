package me.timothy.bots.database;

import java.util.List;

/**
 * <p>An object-relational mapping (ORM) converts objects into relational
 * databases - saving to and loading from.</p>
 * 
 * <p>This interface is a more generic version of that, and is simply capable
 * of storing and retrieving objects from memory to <i>something else</i>.</p>
 * 
 * @author Timothy
 */
public interface ObjectMapping<A> {
	/**
	 * <p>Saves/updates the object to/in the mapping, such as storing/updating in a relational
	 * database or sending over a network. If the object isn't quite in a mapplable state, but
	 * the solution is quite simple, the object is updated to reflect those changes. If the objects
	 * state in the database would be different than the one sent, the object should be updated
	 * to reflect those changes.</p>
	 * 
	 * <p>Examples of situations where the object should be updated:</p>
	 * <ul>
	 *   <li>The object is not currently in the database - the id should be updated to reflect the newly
	 *       generated one</li>
	 *   <li>The mapping does not support nanosecond precision for timestamps, but the one sent <i>does</i>
	 *       have nanosecond level precision. The object should be updated to remove that precision</li>
	 * </ul>
	 * 
	 * <p>Examples of situations where an IllegalArgumentException should be generated:</p>
	 * <ul>
	 *   <li>The object should have created_at and updated_at timestamps, but they are null</li>
	 *   <li>The object has complected* variables that are not set correctly.</li>
	 * </ul>
	 * 
	 * <p>Examples of situations where a RuntimeException should be generated:</p>
	 * <ul>
	 *   <li>The database is relational and the primary key should autogenerate, but no generated key could be found</li>
	 *   <li>The database is offline or the schema is incorrect</li>
	 *   <li>A foreign key fails to resolve</li>
	 *   <li>There is an "impossible" programming exception</li>
	 * </ul>
	 * 
	 * <small>* - tied together, dependent on each other. e.g. hasColor and color</small>
	 * @param a the object to save
	 * @throws IllegalArgumentException if the object is not ready to be mapped
	 * @throws RuntimeException if a database exception or other environmental exception occurs. Will wrap the true error
	 *                          if it exists.
	 */
	public void save(A a) throws IllegalArgumentException;
	
	/**
	 * Fetches all of the {@code A}'s in the mapping, or an empty list. 
	 * 
	 * @return all objects stored in this mapping
	 */
	public List<A> fetchAll(); 
}
