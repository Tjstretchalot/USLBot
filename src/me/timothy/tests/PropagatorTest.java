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
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
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
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ericsSub, hma, paulBanningJohn);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());	
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(secondSub, hma, botsBan);
		assertNotNull(result);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());	
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ericsSub, hma, paulBansAdam);
		assertEquals(1, result.bans.size());
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);

		HandledModAction hma2 = new HandledModAction(-1, paulsSub.id, "ModAction_ID2", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory paulBansAdam = new BanHistory(-1, paul.id, adam.id, hma2.id, "known #onionhater", "permanent");
		database.getBanHistoryMapping().save(paulBansAdam);
		
		result = propagator.propagateBan(johnsSub, hma2, paulBansAdam);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
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
		assertFalse(result.postpone);
	}
	
	@Test
	public void testSendsPMToSubredditModeratorsWithHistory() {
		initResponses();
		
		final long now = System.currentTimeMillis();
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_with_history_userpm_title", "<old mod> - USL Update on <banned user>", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_with_history_userpm_body", "<new usl subreddit>, <new usl mod>, <new ban description>, <banned user>, <old subreddit>, <currently banned>, <currently permabanned>, <suppressed>, <triggering tags>, <full history>, <old mod>, <old mod num bans>, <old mod num unbans>", new Timestamp(now), new Timestamp(now)));
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person adam = database.getPersonMapping().fetchOrCreateByUsername("adam");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		MonitoredSubreddit adamsSub = new MonitoredSubreddit(-1, "adamssub", false, false, false);
		database.getMonitoredSubredditMapping().save(adamsSub);
		
		SubscribedHashtag paulsHashtag = new SubscribedHashtag(-1, paulsSub.id, "#potato", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(paulsHashtag);
		
		HandledModAction paulBansEricHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(paulBansEricHMA);
		
		BanHistory paulBansEric = new BanHistory(-1, paul.id, eric.id, paulBansEricHMA.id, "no tags here", "permanent");
		database.getBanHistoryMapping().save(paulBansEric);
		
		HandledModAction adamBansEricHMA = new HandledModAction(-1, adamsSub.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(adamBansEricHMA);
		
		BanHistory adamBansEric = new BanHistory(-1, adam.id, eric.id, adamBansEricHMA.id, "#potato", "permanent");
		database.getBanHistoryMapping().save(adamBansEric);
		
		String nowPretty = USLBotDriver.timeToPretty(now);
		PropagateResult result = propagator.propagateBan(paulsSub, adamBansEricHMA, adamBansEric);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertEquals(1, result.userPMs.size());
		UserPMInformation userPM = result.userPMs.get(0);
		assertEquals(paul, userPM.person);
		assertEquals("paul - USL Update on eric", userPM.title);
		assertEquals(
				"adamssub, adam, #potato, eric, paulssub, true, true, true, #potato, - " + nowPretty + " - paul banned eric for permanent - no tags here, paul, 1, 0",
				userPM.body
				);
		assertFalse(result.postpone);
		
		// doesn't send it twice!
		result = propagator.propagateBan(paulsSub, adamBansEricHMA, adamBansEric);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
	}
	
	@Test
	public void testDoesntPMHimself() {
		initResponses();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person bot = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		//Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		long now = System.currentTimeMillis();
		SubscribedHashtag paulsTag = new SubscribedHashtag(-1, paulsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(paulsTag);
		
		SubscribedHashtag ericsTag = new SubscribedHashtag(-1, ericsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ericsTag);
		
		HandledModAction paulBansJohnHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(paulBansJohnHMA);
		
		BanHistory paulBansJohnBH = new BanHistory(-1, paul.id, john.id, paulBansJohnHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansJohnBH);
		
		PropagateResult result = propagator.propagateBan(paulsSub, paulBansJohnHMA, paulBansJohnBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ericsSub, paulBansJohnHMA, paulBansJohnBH);
		assertEquals(1, result.bans.size());
		assertEquals(1, result.modmailPMs.size());
		assertTrue(result.userPMs.isEmpty());
		
		assertEquals(john, result.bans.get(0).person);
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
		assertFalse(result.postpone);
		
		HandledModAction botBansJohnEricsSubHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(botBansJohnEricsSubHMA);
		
		BanHistory botBansJohnEricsSubBH = new BanHistory(-1, bot.id, john.id, botBansJohnEricsSubHMA.id, "USLBot; #scammer", "permanent");
		database.getBanHistoryMapping().save(botBansJohnEricsSubBH);
		
		result = propagator.propagateBan(paulsSub, botBansJohnEricsSubHMA, botBansJohnEricsSubBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ericsSub, botBansJohnEricsSubHMA, botBansJohnEricsSubBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(paulsSub, paulBansJohnHMA, paulBansJohnBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ericsSub, paulBansJohnHMA, paulBansJohnBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
	}
	
	@Test
	public void testDoesntPMOrPropagateOldBots() {
		initResponses();
		
		Person guzbot3000 = database.getPersonMapping().fetchOrCreateByUsername("Guzbot3000");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person ella = database.getPersonMapping().fetchOrCreateByUsername("ella");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		MonitoredSubreddit ellasSub = new MonitoredSubreddit(-1, "ellasub", false, false, false);
		database.getMonitoredSubredditMapping().save(ellasSub);
		
		MonitoredSubreddit usl = new MonitoredSubreddit(-1, "usl", false, false, false);
		database.getMonitoredSubredditMapping().save(usl);
		
		long now = System.currentTimeMillis();
		SubscribedHashtag johnHashtag = new SubscribedHashtag(-1, johnsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(johnHashtag);
		
		SubscribedHashtag ellasHashtag = new SubscribedHashtag(-1, ellasSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ellasHashtag);
		
		SubscribedHashtag uslHashtag = new SubscribedHashtag(-1, usl.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(uslHashtag);

		long oneDayInMilliseconds = 1000 * 60 * 60 * 24;
		long fiftyDaysInMilliseconds = 50 * oneDayInMilliseconds;
		long fiftyDaysAgo = now - fiftyDaysInMilliseconds;
		HandledModAction ellasOldBanHMA = new HandledModAction(-1, ellasSub.id, "ModAction_ID4", new Timestamp(fiftyDaysAgo));
		database.getHandledModActionMapping().save(ellasOldBanHMA);
		
		BanHistory ellasOldBanBH = new BanHistory(-1, ella.id, eric.id, ellasOldBanHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(ellasOldBanBH);
		
		long thirtyDaysInMilliseconds = 30 * oneDayInMilliseconds;
		long thirtyDaysAgo = now - thirtyDaysInMilliseconds;
		HandledModAction ellasOldUnbanHMA = new HandledModAction(-1, ellasSub.id, "ModAction_ID5", new Timestamp(thirtyDaysAgo));
		database.getHandledModActionMapping().save(ellasOldUnbanHMA);
		
		UnbanHistory ellasOldUnbanUBH = new UnbanHistory(-1, ella.id, eric.id, ellasOldUnbanHMA.id);
		database.getUnbanHistoryMapping().save(ellasOldUnbanUBH);
		
		HandledModAction johnsOriginalBanHMA = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(johnsOriginalBanHMA);		
		
		BanHistory johnsOriginalBanBH = new BanHistory(-1, john.id, eric.id, johnsOriginalBanHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(johnsOriginalBanBH);
		
		HandledModAction guzbotsPropagateToUSLHMA = new HandledModAction(-1, usl.id, "ModAction_ID2", new Timestamp(now + 5000));
		database.getHandledModActionMapping().save(guzbotsPropagateToUSLHMA);
		
		BanHistory guzbotsBanOnUSLBH = new BanHistory(-1, guzbot3000.id, eric.id, guzbotsPropagateToUSLHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(guzbotsBanOnUSLBH);
		
		HandledModAction guzbotsPropagateToEllaHMA = new HandledModAction(-1, ellasSub.id, "ModAction_ID3", new Timestamp(now + 10000));
		database.getHandledModActionMapping().save(guzbotsPropagateToEllaHMA);
		
		BanHistory guzbotsPropagateToEllaBH = new BanHistory(-1, guzbot3000.id, eric.id, guzbotsPropagateToEllaHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(guzbotsPropagateToEllaBH);
		
		PropagateResult result;


		result = propagator.propagateBan(usl, ellasOldBanHMA, ellasOldBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(johnsSub, ellasOldBanHMA, ellasOldBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, ellasOldBanHMA, ellasOldBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);

		
		result = propagator.propagateUnban(usl, ellasOldUnbanHMA, ellasOldUnbanUBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateUnban(johnsSub, ellasOldUnbanHMA, ellasOldUnbanUBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateUnban(ellasSub, ellasOldUnbanHMA, ellasOldUnbanUBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		
		result = propagator.propagateBan(usl, johnsOriginalBanHMA, johnsOriginalBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(johnsSub, johnsOriginalBanHMA, johnsOriginalBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, johnsOriginalBanHMA, johnsOriginalBanBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);

		
		result = propagator.propagateBan(usl, guzbotsPropagateToUSLHMA, guzbotsBanOnUSLBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(johnsSub, guzbotsPropagateToUSLHMA, guzbotsBanOnUSLBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, guzbotsPropagateToUSLHMA, guzbotsBanOnUSLBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		

		result = propagator.propagateBan(usl, guzbotsPropagateToEllaHMA, guzbotsPropagateToEllaBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(johnsSub, guzbotsPropagateToEllaHMA, guzbotsPropagateToEllaBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, guzbotsPropagateToEllaHMA, guzbotsPropagateToEllaBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
	}
	
	@Test
	public void testHandlesBanDurationChanged() {
		initResponses();
		
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		//Person ella = database.getPersonMapping().fetchOrCreateByUsername("ella");
		//Person me = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		MonitoredSubreddit ellasSub = new MonitoredSubreddit(-1, "ellassub", false, false, false);
		database.getMonitoredSubredditMapping().save(ellasSub);
		
		MonitoredSubreddit mySub = new MonitoredSubreddit(-1, "mycoalitionsub", false, false, false);
		database.getMonitoredSubredditMapping().save(mySub);
		
		long oneDay = 1000 * 60 * 60 * 60 * 24;
		long now = System.currentTimeMillis();
		SubscribedHashtag ericsTag1 = new SubscribedHashtag(-1, ericsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ericsTag1);
		
		SubscribedHashtag ellasTag1 = new SubscribedHashtag(-1, ellasSub.id, "#sketchy", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ellasTag1);
		
		SubscribedHashtag myTag1 = new SubscribedHashtag(-1, mySub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(myTag1);
		
		SubscribedHashtag myTag2 = new SubscribedHashtag(-1, mySub.id, "#sketchy", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(myTag2);
		
		long thirtyDaysAgo = now - (oneDay * 30);
		long twentyDaysAgo = now - (oneDay * 20);
		long fifteenDaysAgo = now - (oneDay * 15);
		long tenDaysAgo = now - (oneDay * 10);
		
		HandledModAction ericBansPaulTemporarilyHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID1", new Timestamp(thirtyDaysAgo));
		database.getHandledModActionMapping().save(ericBansPaulTemporarilyHMA);
		
		BanHistory ericBansPaulTemporarilyBH = new BanHistory(-1, eric.id, paul.id, ericBansPaulTemporarilyHMA.id, "#sketchy", "30 days");
		database.getBanHistoryMapping().save(ericBansPaulTemporarilyBH);
		
		PropagateResult result;
		
		result = propagator.propagateBan(ericsSub, ericBansPaulTemporarilyHMA, ericBansPaulTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, ericBansPaulTemporarilyHMA, ericBansPaulTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(mySub, ericBansPaulTemporarilyHMA, ericBansPaulTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		
		HandledModAction ericBansJohnTemporarilyHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID2", new Timestamp(twentyDaysAgo));
		database.getHandledModActionMapping().save(ericBansJohnTemporarilyHMA);
		
		BanHistory ericBansJohnTemporarilyBH = new BanHistory(-1, eric.id, john.id, ericBansJohnTemporarilyHMA.id, "#scammer", "90 days");
		database.getBanHistoryMapping().save(ericBansJohnTemporarilyBH);
		
		result = propagator.propagateBan(ericsSub, ericBansJohnTemporarilyHMA, ericBansJohnTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, ericBansJohnTemporarilyHMA, ericBansJohnTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(mySub, ericBansJohnTemporarilyHMA, ericBansJohnTemporarilyBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		
		HandledModAction ericChangesPaulsBanToPermanentHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID3", new Timestamp(fifteenDaysAgo));
		database.getHandledModActionMapping().save(ericChangesPaulsBanToPermanentHMA);
		
		BanHistory ericChangesPaulsBanToPermanentBH = new BanHistory(-1, eric.id, paul.id, ericChangesPaulsBanToPermanentHMA.id, null, "changed to permanent");
		database.getBanHistoryMapping().save(ericChangesPaulsBanToPermanentBH);
		
		result = propagator.propagateBan(ericsSub, ericChangesPaulsBanToPermanentHMA, ericChangesPaulsBanToPermanentBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, ericChangesPaulsBanToPermanentHMA, ericChangesPaulsBanToPermanentBH);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(ellasSub, result.modmailPMs.get(0).subreddit);
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(mySub, ericChangesPaulsBanToPermanentHMA, ericChangesPaulsBanToPermanentBH);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(1, result.modmailPMs.size());
		assertEquals(mySub, result.modmailPMs.get(0).subreddit);
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		
		HandledModAction ericChangesJohnBanTo180DaysHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID4", new Timestamp(tenDaysAgo));
		database.getHandledModActionMapping().save(ericChangesJohnBanTo180DaysHMA);
		
		BanHistory ericChangesJohnBanTo180DaysBH = new BanHistory(-1, eric.id, john.id, ericChangesJohnBanTo180DaysHMA.id, null, "changed to 180 days");
		database.getBanHistoryMapping().save(ericChangesJohnBanTo180DaysBH);
		
		result = propagator.propagateBan(ericsSub, ericChangesJohnBanTo180DaysHMA, ericChangesJohnBanTo180DaysBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(ellasSub, ericChangesJohnBanTo180DaysHMA, ericChangesJohnBanTo180DaysBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(mySub, ericChangesJohnBanTo180DaysHMA, ericChangesJohnBanTo180DaysBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
	}

	@Test
	public void testDoesntBanUnbannedUsers() {
		initResponses();
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		long now = System.currentTimeMillis();
		long oneDayInMS = 1000 * 60 * 60 * 24;
		
		SubscribedHashtag paulTag = new SubscribedHashtag(-1, paulsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(paulTag);
		
		long tenDaysAgo = now - (10 * oneDayInMS);
		HandledModAction johnBansEricHMA = new HandledModAction(-1, johnsSub.id, "ModAction_1", new Timestamp(tenDaysAgo));
		database.getHandledModActionMapping().save(johnBansEricHMA);
		
		BanHistory johnBansEricBH = new BanHistory(-1, john.id, eric.id, johnBansEricHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(johnBansEricBH);
		
		long fiveDaysAgo = now - (5 * oneDayInMS);
		HandledModAction johnUnbansEricHMA = new HandledModAction(-1, johnsSub.id, "ModAction_2", new Timestamp(fiveDaysAgo));
		database.getHandledModActionMapping().save(johnUnbansEricHMA);
		
		UnbanHistory johnUnbansEricUBH = new UnbanHistory(-1, john.id, eric.id, johnUnbansEricHMA.id);
		database.getUnbanHistoryMapping().save(johnUnbansEricUBH);
		
		PropagateResult result = propagator.propagateBan(johnsSub, johnBansEricHMA, johnBansEricBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		result = propagator.propagateBan(paulsSub, johnBansEricHMA, johnBansEricBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
	}
	
	@Test
	public void testDoesntBanIfLaterUnbanRequest() {
		initResponses();
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		long now = System.currentTimeMillis();
		long oneDayInMS = 1000 * 60 * 60 * 24;
		
		long fifteenDaysAgo = now - (oneDayInMS * 15);
		long tenDaysAgo = now - (oneDayInMS * 10);
		long fiveDaysAgo = now - (oneDayInMS * 5);
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		SubscribedHashtag ericTag = new SubscribedHashtag(-1, ericsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ericTag);
		
		
		HandledModAction johnBansPaulHMA = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(tenDaysAgo));
		database.getHandledModActionMapping().save(johnBansPaulHMA);
		
		BanHistory johnBansPaulBH = new BanHistory(-1, john.id, paul.id, johnBansPaulHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(johnBansPaulBH);
		
		PropagateResult result = propagator.propagateBan(ericsSub, johnBansPaulHMA, johnBansPaulBH);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		UnbanRequest unbanRequest = new UnbanRequest(-1, john.id, paul.id, new Timestamp(fiveDaysAgo), new Timestamp(now), false);
		database.getUnbanRequestMapping().save(unbanRequest);
		
		result = propagator.propagateBan(ericsSub, johnBansPaulHMA, johnBansPaulBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		unbanRequest.createdAt = new Timestamp(fifteenDaysAgo);
		database.getUnbanRequestMapping().save(unbanRequest);
		
		result = propagator.propagateBan(ericsSub, johnBansPaulHMA, johnBansPaulBH);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		unbanRequest.invalid = true;
		unbanRequest.createdAt = new Timestamp(fiveDaysAgo);
		database.getUnbanRequestMapping().save(unbanRequest);

		result = propagator.propagateBan(ericsSub, johnBansPaulHMA, johnBansPaulBH);
		assertEquals(1, result.bans.size());
		assertEquals(paul, result.bans.get(0).person);
		assertEquals(ericsSub, result.bans.get(0).subreddit);
		assertEquals(1, result.modmailPMs.size());
		assertTrue(result.userPMs.isEmpty());
		assertFalse(result.postpone);
		
		unbanRequest.invalid = false;
		unbanRequest.handledAt = null;
		database.getUnbanRequestMapping().save(unbanRequest);

		result = propagator.propagateBan(ericsSub, johnBansPaulHMA, johnBansPaulBH);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
		assertTrue(result.postpone);
	}
	
	@Test
	public void testWriteOnlyReadOnlyPreviousBan() {
		initResponses();
		long now = System.currentTimeMillis();
		long fiveMinutesFromNow = now + 1000 * 60 * 5;
		
		Person babyMonkeyOnPig = database.getPersonMapping().fetchOrCreateByUsername("BabyMonkeyOnPig");
		Person c4cBanBot = database.getPersonMapping().fetchOrCreateByUsername("C4Cbanbot");
		Person timCookz = database.getPersonMapping().fetchOrCreateByUsername("TimCookz");
		
		MonitoredSubreddit appleswap = new MonitoredSubreddit(-1, "appleswap", true, false, false);
		database.getMonitoredSubredditMapping().save(appleswap);
		
		SubscribedHashtag appleswapScammer = new SubscribedHashtag(-1, appleswap.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(appleswapScammer);
		
		MonitoredSubreddit cash4Cash = new MonitoredSubreddit(-1, "Cash4Cash", true, true, true);
		database.getMonitoredSubredditMapping().save(cash4Cash);

		SubscribedHashtag cash4CashScammer = new SubscribedHashtag(-1, cash4Cash.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(cash4CashScammer);
		
		MonitoredSubreddit borrow = new MonitoredSubreddit(-1, "borrow", true, false, false);
		database.getMonitoredSubredditMapping().save(borrow);

		SubscribedHashtag borrowScammer = new SubscribedHashtag(-1, borrow.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(borrowScammer);
		
		
		HandledModAction babyMonkeyBansCookzHMA = new HandledModAction(-1, appleswap.id, "babyMonkeyBansCookz", new Timestamp(now));
		database.getHandledModActionMapping().save(babyMonkeyBansCookzHMA);
		
		HandledModAction c4cBanBotBansCookzHMA = new HandledModAction(-1, cash4Cash.id, "c4cBanBotBansCookz", new Timestamp(fiveMinutesFromNow));
		database.getHandledModActionMapping().save(c4cBanBotBansCookzHMA);
		
		BanHistory babyMonkeyBansCookz = new BanHistory(-1, babyMonkeyOnPig.id, timCookz.id, babyMonkeyBansCookzHMA.id, "#scammer never sent items", "permanent");
		database.getBanHistoryMapping().save(babyMonkeyBansCookz);
		
		BanHistory c4cBanBotBansCookz = new BanHistory(-1, c4cBanBot.id, timCookz.id, c4cBanBotBansCookzHMA.id, "scammer never sent items", "permanent");
		database.getBanHistoryMapping().save(c4cBanBotBansCookz);
		
		PropagateResult result = propagator.propagateBan(borrow, babyMonkeyBansCookzHMA, babyMonkeyBansCookz);
		assertNotNull(result.bans);
		assertEquals(1, result.bans.size());
		
		UserBanInformation info = result.bans.get(0);
		assertEquals(timCookz.id, info.person.id);
		assertEquals(borrow.id, info.subreddit.id);
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		propagator = null;
	}
}
