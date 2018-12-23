package me.timothy.tests;

import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.Bot;
import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.jreddit.requests.Utils;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * This class is just a placeholder for testing api responses manually
 * 
 * @author Timothy
 */
public class ManualTests {
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected Bot bot;

	@Before
	public void setUp() throws Exception {
		Utils.USER_AGENT = "manual testing:v12.21.2018 (by /u/Tjstretchalot)";
		
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		
		MysqlTestUtils.clearDatabase(database);
		
		bot = new Bot("test");
		bot.loginReddit(config.getProperty("user.username"),
				config.getProperty("user.password"),
				config.getProperty("user.appClientID"),
				config.getProperty("user.appClientSecret"));
	}
	
	@Test
	public void test() throws Exception {
		Boolean res = Boolean.TRUE;
		while(res == Boolean.TRUE) {
			res = bot.submitSelf("uslbotnotifications", "[Test] Testing throttle speed", "Testing to verify response when throttled");
			System.out.println("Got result=" + res);
		}
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
	}
}
