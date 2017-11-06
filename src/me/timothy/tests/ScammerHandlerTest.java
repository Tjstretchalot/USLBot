package me.timothy.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLTraditionalScammerHandler;
import me.timothy.bots.memory.TraditionalScammerHandlerResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class ScammerHandlerTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLTraditionalScammerHandler handler;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		handler = new USLTraditionalScammerHandler(database, config);
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	/**
	 * Make sure setup got everything not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(handler);
	}
	
	@Test
	public void testNormalInitialBan() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		long now = System.currentTimeMillis();
		TraditionalScammer paulScammer = new TraditionalScammer(-1, paul.id, "gfther", "#scammer", new Timestamp(now));
		database.getTraditionalScammerMapping().save(paulScammer);
		
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, ericsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		database.getResponseMapping().save(new Response(-1, "traditional_scammer_banned_ban_message", "You are on the universal scammer list.", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "traditional_scammer_banned_title", "banned <banned person>", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "traditional_scammer_banned_body", "banned <banned person> (descr: <description>)", new Timestamp(now), new Timestamp(now)));
		
		TraditionalScammerHandlerResult result = handler.handleTraditionalScammer(paulScammer, ericsSub);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
		assertEquals("banned paul", result.modmailPMs.get(0).title);
		assertEquals("banned paul (descr: #scammer)", result.modmailPMs.get(0).body);
		assertTrue(result.userPMs.isEmpty());
	}
	
	@Test
	public void testDoesntBanAlreadyBanned() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person emma = database.getPersonMapping().fetchOrCreateByUsername("emma");
		
		long now = System.currentTimeMillis();
		TraditionalScammer paulScammer = new TraditionalScammer(-1, paul.id, "gfther", "#scammer", new Timestamp(now));
		database.getTraditionalScammerMapping().save(paulScammer);
		
		MonitoredSubreddit emmasSub = new MonitoredSubreddit(-1, "emmassub", false, false, false);
		database.getMonitoredSubredditMapping().save(emmasSub);
		
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, emmasSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		HandledModAction emmaBansPaulHMA = new HandledModAction(-1, emmasSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(emmaBansPaulHMA);
		
		BanHistory emmaBansPaulBH = new BanHistory(-1, emma.id, paul.id, emmaBansPaulHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(emmaBansPaulBH);
		
		TraditionalScammerHandlerResult result = handler.handleTraditionalScammer(paulScammer, emmasSub);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	@Test
	public void testChecksTagsBeforeBanning() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		long now = System.currentTimeMillis();
		TraditionalScammer paulScammer = new TraditionalScammer(-1, paul.id, "gfther", "#scammer", new Timestamp(now));
		database.getTraditionalScammerMapping().save(paulScammer);
		
		MonitoredSubreddit emmasSub = new MonitoredSubreddit(-1, "emmassub", false, false, false);
		database.getMonitoredSubredditMapping().save(emmasSub);
		
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, emmasSub.id, "#sketchy", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		TraditionalScammerHandlerResult result = handler.handleTraditionalScammer(paulScammer, emmasSub);
		assertEquals(0, result.bans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(0, result.userPMs.size());
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		handler = null;
	}
}
