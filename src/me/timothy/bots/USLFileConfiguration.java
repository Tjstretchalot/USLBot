package me.timothy.bots;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads the necessary file-level configuration for the universal
 * scammer list. This doesn't contain the actual loading code, but 
 * simply improves the error messages if any files are missing.
 * 
 * @author Timothy Moore
 */
public class USLFileConfiguration extends FileConfiguration {
	
	/**
	 * Initialize the file configuration at the current folder
	 */
	public USLFileConfiguration() {
		super();
	}
	
	/**
	 * Initialize the file configuration with the root at the specified
	 * folder.
	 * @param folder root for files
	 */
	public USLFileConfiguration(Path folder) {
		super.folder = folder;
	}
	
	/**
	 * Load the required file configuration
	 * 
	 *  @throws IOException if an ioexception occurs (or a file is not found)
	 *  @throws NullPointerException if a required key is missing
	 */
	@Override
	public void load() throws IOException, NullPointerException {
		super.load();

		addProperties("database", true, "url", "username", "password", "database", "flat_folder");
		addProperties("ftpbackups", true, "host", "username", "password", "knownhostsfile", "port", "dbfolder", "logsfolder", "secure", "failintervalms");
		addProperties("general", true, "ma_processor_extreme_trace", "notifications_sub");
		addProperties("temp_auth_granter", true, "max_requests_per_loop", "subreddit", "auth_level_verified", 
				"duration_verified_ms", "min_retry_elapsed_ms");
		addProperties("reddit_to_meaning", true, "extreme_trace");
		addProperties("hwswapuser", true, "username", "password", "appClientID", "appClientSecret");
		addProperties("hwswap", true, "savepath", "max_bans_per_loop", "bansub");
		addProperties("register_account_requests", false, "limit_per_loop");
	}
	
}
