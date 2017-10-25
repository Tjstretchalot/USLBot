package me.timothy.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLPropagator;
import me.timothy.bots.USLBotDriver;
import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests the USLBanHistoryPropagator
 * 
 * @author Timothy
 */
public class PropagatorTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLPropagator propagator;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		propagator = new USLPropagator(database, config);
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	/**
	 * Make sure setup got everything not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(propagator);
	}
	
	/**
	 * Tests that the propagator wont try to ban user john on 
	 * paulssub due to a ban on paulssub.
	 */
	@Test
	public void testDoesntPropagateSubredditBanToItself() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, paulsSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBanningJohn = new BanHistory(-1, paul.id, john.id, hma.id, "#rekt", "permanent");
		database.getBanHistoryMapping().save(paulBanningJohn);
		
		
		PropagateResult result = propagator.propagateBan(paulsSub, hma, paulBanningJohn);
		assertNotNull(result);
		assertEquals(paulsSub, result.subreddit);
		assertEquals(hma, result.handledModAction);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Test that if paul bans john on paulssub for 30 days, john isn't
	 * banned on other monitored subs + they aren't notified
	 */
	@Test
	public void testDoesntPropagateTemporary() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);

		SubscribedHashtag hashtag = new SubscribedHashtag(-1, paulsSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag);

		SubscribedHashtag hashtag2 = new SubscribedHashtag(-1, ericsSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag2);

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBanningJohn = new BanHistory(-1, paul.id, john.id, hma.id, "#rekt", "30 days");
		database.getBanHistoryMapping().save(paulBanningJohn);
		
		database.getBanHistoryMapping().save(paulBanningJohn);
		
		PropagateResult result = propagator.propagateBan(paulsSub, hma, paulBanningJohn);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		
		result = propagator.propagateBan(ericsSub, hma, paulBanningJohn);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());	
	}
	
	/**
	 * Test that if user.username bans someone, thats not propagated.
	 */
	@Test
	public void testDoesntPropagateOwnBans() {
		final String ourName = config.getProperty("user.username");
		assertNotNull("You must set user.username for this test!", ourName);
		assertFalse("You must set user.username for this test!", ourName.trim().isEmpty());
		
		final String modUser = ourName.equalsIgnoreCase("paul") ? "john" : "paul";
		final String bannedU = ourName.equalsIgnoreCase("john") ? "eric" : "john";
		
		Person botPerson = database.getPersonMapping().fetchOrCreateByUsername(ourName);
		Person modPerson = database.getPersonMapping().fetchOrCreateByUsername(modUser);
		Person banPerson = database.getPersonMapping().fetchOrCreateByUsername(bannedU);
		
		assertNotEquals(botPerson.id, modPerson.id);
		assertNotEquals(botPerson.id, banPerson.id);
		assertNotEquals(modPerson.id, banPerson.id);
		
		MonitoredSubreddit modsSub = new MonitoredSubreddit(-1, "modssub", false, false, false);
		database.getMonitoredSubredditMapping().save(modsSub);
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, modsSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		MonitoredSubreddit secondSub = new MonitoredSubreddit(-1, "anothersub", false, false, false);
		database.getMonitoredSubredditMapping().save(secondSub);

		SubscribedHashtag hashtag2 = new SubscribedHashtag(-1, secondSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag2);

		HandledModAction hma = new HandledModAction(-1, modsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory botsBan = new BanHistory(-1, botPerson.id, banPerson.id, hma.id, "#rekt", "permanent");
		database.getBanHistoryMapping().save(botsBan);
		

		PropagateResult result = propagator.propagateBan(modsSub, hma, botsBan);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		
		result = propagator.propagateBan(secondSub, hma, botsBan);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());	
	}
	
	
	/* javadoc works poorly for this message
	 * 
	 * Tests that when paul bans john on paulssub with hashtag that
	 * ericssub listens on, john is banned on ericssub. 
	 * 
	 * the ban message is propagated_ban_message (repls. <original mod>, <original description>, <original subreddit>, <new subreddit>, <triggering tags>)
	 * the ban note is propagated_ban_note - <original mod>, <original description>, <original subreddit>, <new subreddit>, <triggering tags>
	 * 
	 * ericssub is not silent, so it recieves a message
	 * propagated_ban_modmail_title - <original mod>, <original description>, <original subreddit>, <original timestamp>, <original id>, <banned user>, <triggering tags>
	 * propagated_ban_modmail_body - <original mod>, <original description>, <original subreddit>, <original timestamp>, <original id>, <banned user>, <triggering tags>
	 */
	@Test 
	public void testPropagatesToOther() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);

		SubscribedHashtag hashtag = new SubscribedHashtag(-1, ericsSub.id, "#rekt", 
				new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansJohn = new BanHistory(-1, paul.id, john.id, hma.id, "#rekt", "permanent");
		database.getBanHistoryMapping().save(paulBansJohn);
		
		database.getResponseMapping().save(new Response(-1, "propagated_ban_message", "1 <original mod>, <original description>, <original subreddit>, <new subreddit>, <triggering tags>", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_note", "2 <original mod>, <original description>, <original subreddit>, <new subreddit>, <triggering tags>", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_title", "3 <original mod>, <original description>, <original subreddit>, <original timestamp>, <original id>, <banned user>, <triggering tags>", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_body", "4 <original mod>, <original description>, <original subreddit>, <original timestamp>, <original id>, <banned user>, <triggering tags>", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		
		final String expBanMessage = "1 paul, #rekt, paulssub, ericssub, #rekt";
		final String expBanNote = "2 paul, #rekt, paulssub, ericssub, #rekt";
		final String expModmailTitle = "3 paul, #rekt, paulssub, " + USLBotDriver.timeToPretty(hma.occurredAt.getTime()) + ", ModAction_ID1, john, #rekt";
		final String expModmailBody = "4 paul, #rekt, paulssub, " + USLBotDriver.timeToPretty(hma.occurredAt.getTime()) + ", ModAction_ID1, john, #rekt";
		
		PropagateResult result = propagator.propagateBan(ericsSub, hma, paulBansJohn);
		assertNotNull(result);
		assertEquals(1, result.bans.size());
		assertEquals(expBanMessage, result.bans.get(0).banMessage);
		assertEquals(expBanNote, result.bans.get(0).banNote);
		assertEquals("other", result.bans.get(0).banReason);
		assertEquals(john, result.bans.get(0).person);
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
		assertEquals(expModmailBody, result.modmailPMs.get(0).body);
		assertEquals(expModmailTitle, result.modmailPMs.get(0).title);
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * init required responses with filler data. only 1 thing should test responses
	 * substitutions, the rest should just test they are sent out
	 */
	private void initResponses() {
		database.getResponseMapping().save(new Response(-1, "propagated_ban_message", "ban message", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_note", "ban note", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_title", "ban modmail title", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_body", "ban modmail body", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())));
	}
	
	/**
	 * 3 subreddits: paulsSub, johnsSub, ericsSub.
	 * adam is banned on paulssub with tag #rekt, which is 
	 * followed by ericssub but not johnssub.
	 */
	@Test
	public void testPropagatesDifferentHashtags() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person adam = database.getPersonMapping().fetchOrCreateByUsername("adam");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		SubscribedHashtag johnsHashtag = new SubscribedHashtag(-1, johnsSub.id, "#onionlover", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(johnsHashtag);
		
		SubscribedHashtag ericsHashtag = new SubscribedHashtag(-1, ericsSub.id, "#onionhater", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(ericsHashtag);
		

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansAdam = new BanHistory(-1, paul.id, adam.id, hma.id, "probable #onionhater", "permanent");
		database.getBanHistoryMapping().save(paulBansAdam);
		
		PropagateResult result = propagator.propagateBan(johnsSub, hma, paulBansAdam);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		
		result = propagator.propagateBan(ericsSub, hma, paulBansAdam);
		assertEquals(1, result.bans.size());
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Two subreddits, paulsSub and johnsSub. johnsSub originally
	 * subscribes to 2 hashtags, but deletes one. Paul bans eric
	 * on the hashtag john is still subscribed to, and it propagates.
	 * Paul bans adam on the hashtag john is no longer subscribed to
	 * and it does not propagate.
	 */
	@Test
	public void testDoesntPropagateDeletedHashtags() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person adam = database.getPersonMapping().fetchOrCreateByUsername("adam");

		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		SubscribedHashtag johnTag1 = new SubscribedHashtag(-1, johnsSub.id, "#onionhater", new Timestamp(System.currentTimeMillis() - 5000), new Timestamp(System.currentTimeMillis()));
		database.getSubscribedHashtagMapping().save(johnTag1);
		SubscribedHashtag johnTag2 = new SubscribedHashtag(-1, johnsSub.id, "#onionlover", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(johnTag2);
		

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansEric = new BanHistory(-1, paul.id, eric.id, hma.id, "proven #onionlover", "permanent");
		database.getBanHistoryMapping().save(paulBansEric);
		
		PropagateResult result = propagator.propagateBan(johnsSub, hma, paulBansEric);
		assertEquals(1, result.bans.size());
		assertEquals(johnsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(johnsSub, result.modmailPMs.get(0).subreddit);
		assertTrue(result.userPMs.isEmpty());

		HandledModAction hma2 = new HandledModAction(-1, paulsSub.id, "ModAction_ID2", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansAdam = new BanHistory(-1, paul.id, adam.id, hma2.id, "known #onionhater", "permanent");
		database.getBanHistoryMapping().save(paulBansAdam);
		
		result = propagator.propagateBan(johnsSub, hma2, paulBansAdam);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Two subreddits, paulsSub and johnsSub. johnsSub is silent.
	 * paul bans eric on something john subscribes to. eric is
	 * banned on johnsSub, but johnsSub is not pmd
	 */
	@Test
	public void testDoesntMailSilent() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");

		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", true, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		SubscribedHashtag johnTag = new SubscribedHashtag(-1, johnsSub.id, "#onionlover", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(johnTag);

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansEric = new BanHistory(-1, paul.id, eric.id, hma.id, "likely #onionlover", "permanent");
		database.getBanHistoryMapping().save(paulBansEric);
		
		PropagateResult result = propagator.propagateBan(johnsSub, hma, paulBansEric);
		assertEquals(1, result.bans.size());
		assertEquals(johnsSub, result.bans.get(0).subreddit);
		assertEquals(eric, result.bans.get(0).person);
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Two subreddits, paulsSub and johnsSub. paulsSub is read-only.
	 * paul bans eric on something john is subscribed to. nothing
	 * happens.
	 */
	@Test
	public void testDoesntPropagateFromReadOnly() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");

		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, true, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		SubscribedHashtag johnTag = new SubscribedHashtag(-1, johnsSub.id, "#onionlover", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(johnTag);

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansEric = new BanHistory(-1, paul.id, eric.id, hma.id, "likely #onionlover", "permanent");
		database.getBanHistoryMapping().save(paulBansEric);
		
		PropagateResult result = propagator.propagateBan(johnsSub, hma, paulBansEric);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Two subreddits, paulsSub and johnsSub. johnsSub is write-only
	 * paul bans eric on something john is subscribed to. nothing
	 * happens.
	 */
	@Test
	public void testDoesntPropagateToWriteOnly() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");

		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, true);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		SubscribedHashtag johnTag = new SubscribedHashtag(-1, johnsSub.id, "#onionlover", new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(johnTag);

		HandledModAction hma = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory paulBansEric = new BanHistory(-1, paul.id, eric.id, hma.id, "likely #onionlover", "permanent");
		database.getBanHistoryMapping().save(paulBansEric);
		
		PropagateResult result = propagator.propagateBan(johnsSub, hma, paulBansEric);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}

	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		propagator = null;
	}
}
