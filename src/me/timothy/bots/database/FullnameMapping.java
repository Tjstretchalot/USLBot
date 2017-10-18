package me.timothy.bots.database;

import me.timothy.bots.models.Fullname;

/**
 * Describes a mapping for fullnames.
 * 
 * @author Timothy
 */
public interface FullnameMapping extends ObjectMapping<Fullname> {
	/**
	 * Checks if this mapping has already saved the specified fullname
	 * @param fullname the fullname to look for
	 * @return if it can be found
	 */
	public boolean contains(String fullname);
}
