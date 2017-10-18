package me.timothy.bots;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMSummon;
import me.timothy.jreddit.requests.Utils;

/**
 * Entry point to the program. Initializes the bot and then 
 * passes off to the USLBotDriver.
 * 
 * @author Timothy Moore
 */
public class USLBotMain {
	public static void main(String[] args) {
		Logger logger = LogManager.getLogger();
		
		logger.debug("Loading file configuration...");

		USLFileConfiguration config = new USLFileConfiguration();
		try {
			config.load();
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			return;
		}
		
		Utils.USER_AGENT = config.getProperty("user.appClientID") + ":v10.11.2017 (by /u/Tjstretchalot)";
		
		logger.debug("Connecting to database..");
		USLDatabase database = new USLDatabase();

		try {
			database.connect(config.getProperty("database.username"), config.getProperty("database.password"), config.getProperty("database.url"));
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
				new PMSummon[] { },
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
