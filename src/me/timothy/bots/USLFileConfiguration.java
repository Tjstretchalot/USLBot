package me.timothy.bots;

import java.io.IOException;

/**
 * Loads the necessary file-level configuration for the universal
 * scammer list. This doesn't contain the actual loading code, but 
 * simply improves the error messages if any files are missing.
 * 
 * @author Timothy Moore
 */
public class USLFileConfiguration extends FileConfiguration {
	/**
	 * Load the required file configuration
	 * 
	 *  @throws IOException if an ioexception occurs (or a file is not found)
	 *  @throws NullPointerException if a required key is missing
	 */
	@Override
	public void load() throws IOException, NullPointerException {
		super.load();

		addProperties("database", true, "url", "username", "password");
	}
	
}
