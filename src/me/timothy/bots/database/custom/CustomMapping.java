package me.timothy.bots.database.custom;

import me.timothy.bots.database.ObjectMapping;

/**
 * Describes a custom mapping. These require additional callbacks since they
 * don't have the MySQL connection to handle opening/closing.
 * 
 * @author Timothy
 */
public interface CustomMapping<A> extends ObjectMapping<A> {
	/**
	 * Recovers the database. This deletes anything in memory.
	 */
	public void recover();
	public void close();
}
