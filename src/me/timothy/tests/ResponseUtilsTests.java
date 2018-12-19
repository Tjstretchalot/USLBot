package me.timothy.tests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.ResponseUtils;
import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.models.Response;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class ResponseUtilsTests {
	private DBShortcuts db;
	private USLDatabase database;
	private USLFileConfiguration config;

	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		db = new DBShortcuts(database, config);
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	@Test
	public void testFailsIfMissing() {
		try {
			ResponseUtils.verifyFormat(database, "test_response", "a response, for testing", "user1", "The first user");
			fail();
		}catch(AssertionError e) {}
	}
	
	@Test
	public void testFailsIfExtraKey() {
		try {
			database.getResponseMapping().save(new Response(-1, "test_response", "<user1> is not a great guy", db.now(), db.now()));
			
			ResponseUtils.verifyFormat(database, "test_response", "a response, for testing", "username1", "The username of the first user");
			fail();
		}catch(AssertionError e) {}
	}
	
	@Test
	public void testSucceedsIfNoKeys() {
		database.getResponseMapping().save(new Response(-1, "test_response", "he is not a great guy", db.now(), db.now()));
		
		ResponseUtils.verifyFormat(database, "test_response", "a response, for testing", "user1", "The first user");
	}
	
	@Test
	public void testSucceedsIfAllKeysUsed() {
		database.getResponseMapping().save(new Response(-1, "test_response", "<user1> met <user2> at the <loc1>", db.now(), db.now()));
		
		ResponseUtils.verifyFormat(database, "test_response", "a response, for testing", 
				"user1", "The first user",
				"user2", "The second user",
				"loc1", "The location of interest");
	}
	
	@Test
	public void testSucceedsIfMissingKeys() {
		database.getResponseMapping().save(new Response(-1, "test_response", "the location was <loc1>", db.now(), db.now()));
		
		ResponseUtils.verifyFormat(database, "test_response", "a response, for testing", 
				"user1", "The first user",
				"user2", "The second user",
				"loc1", "The location of interest");
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
	}
}
