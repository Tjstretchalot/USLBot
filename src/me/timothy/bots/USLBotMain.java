package me.timothy.bots;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMSummon;
import me.timothy.bots.summon.UnbanRequestPMSummon;
import me.timothy.jreddit.requests.Utils;

/**
 * Entry point to the program. Initializes the bot and then 
 * passes off to the USLBotDriver.
 * 
 * @author Timothy Moore
 */
public class USLBotMain {
	/**
	 * This is solely here so that the custom apache log4j hooks can access
	 * my file configuration rather than having to duplicate that information
	 * via the log4j2.xml file
	 */
	public static USLFileConfiguration mainConfig;
	
	public static void main(String[] args) {
		Logger logger = LogManager.getLogger();
		USLFileConfiguration config = new USLFileConfiguration();
		try {
			config.load();
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			return;
		}
		
		mainConfig = config;
		
		logger.debug("File configuration loaded.");
		
		Utils.USER_AGENT = config.getProperty("user.appClientID") + ":v12.18.2018 (by /u/Tjstretchalot)";
		
		logger.debug("Connecting to database..");
		USLDatabase database = new USLDatabase();

		try {
			database.connect(config.getProperty("database.username"), config.getProperty("database.password"), config.getProperty("database.url"), 
					new File(config.getProperty("database.flat_folder")));
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		
		logger.debug("Verifying database schema..");
		database.validateTableState();
		
		logger.debug("Initializing bot information..");
		Bot uslBot = new Bot(database.getMonitoredSubredditMapping().fetchAllAndConcatenate());
		
		logger.debug("Running USLBotDriver..");
		BotDriver driver = new USLBotDriver(database, config, uslBot,
				new CommentSummon[] { }, 
				new PMSummon[] { 
						new UnbanRequestPMSummon()
				},
				new LinkSummon[] { });
		
		while(true) {
			try {
				driver.run();
			}catch(Exception e) {
				e.printStackTrace();
				logger.log(Level.FATAL, e.getMessage(), e);
				
				logger.catching(e);
				driver.sleepFor(2000);
			}
		}
	}
	
	
}
