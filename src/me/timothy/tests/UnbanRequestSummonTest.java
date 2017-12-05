package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.summon.SummonResponse;
import me.timothy.bots.summon.UnbanRequestPMSummon;
import me.timothy.jreddit.info.Message;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class UnbanRequestSummonTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private UnbanRequestPMSummon summon;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		System.out.println("--------------------");
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		summon = new UnbanRequestPMSummon();
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	@After 
	public void cleanUp() {
		((USLDatabase) database).disconnect();
		database = null;
		config = null;
		summon = null;
	}
	
	/**
	 * Ensure that database and config are not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(summon);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testResponds() {
		long now = System.currentTimeMillis();
		database.getResponseMapping().save(new Response(-1, "unban_request_from_pm_response_body", "body", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_from_pm_response_title", "title", new Timestamp(now), new Timestamp(now)));
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person ebz0324 = database.getPersonMapping().fetchOrCreateByUsername("Ebz0324");
		
		JSONObject msgObj = new JSONObject();
		msgObj.put("body", "$unban /u/Ebz0324");
		msgObj.put("author", "john");
		Message message = new Message(msgObj);
		
		SummonResponse response = summon.handlePM(message, database, config);
		assertNotNull(response);
		assertEquals(1, response.getPMResponses().size());
		assertEquals("john", response.getPMResponses().get(0).getTo());
		assertEquals("title", response.getPMResponses().get(0).getTitle());
		assertEquals("body", response.getPMResponses().get(0).getText());
		
		List<UnbanRequest> fromDb = database.getUnbanRequestMapping().fetchAll();
		assertEquals(1, fromDb.size());
		assertEquals(ebz0324.id, fromDb.get(0).bannedPersonID);
		assertEquals(john.id, fromDb.get(0).modPersonID);
	}
}
