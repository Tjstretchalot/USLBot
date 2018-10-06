package me.timothy.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLUnbanRequestHandler;
import me.timothy.bots.memory.UnbanRequestResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class UnbanRequestHandlerTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLUnbanRequestHandler handler;
	/** This is how IsModeratorFunction works */
	private Map<String, Set<String>> usersToModeratedSubreddits;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		usersToModeratedSubreddits = new HashMap<>();
		handler = new USLUnbanRequestHandler(database, config, (subreddit, user) -> {
			if(!usersToModeratedSubreddits.containsKey(user)) {
				return false;
			}
			
			Set<String> moderated = usersToModeratedSubreddits.get(user);
			return moderated.contains(subreddit.toLowerCase());
		});
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	private void addModerator(String user, String subreddit) {
		Set<String> moderated = null;
		if(!usersToModeratedSubreddits.containsKey(user)) {
			moderated = new HashSet<>();
			usersToModeratedSubreddits.put(user, moderated);
		}else {
			moderated = usersToModeratedSubreddits.get(user);
		}
		moderated.add(subreddit.toLowerCase());
	}
	
	private void removeModerator(String user, String subreddit) {
		usersToModeratedSubreddits.get(user).remove(subreddit);
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
	
	/**
	 * Sets up the situation that would occur if paul banned john on paulssub, which 
	 * is propagated to ericssub, and then paul requests an unban on john
	 */
	@Test
	public void testTypicalUnban() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person me = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		MonitoredSubreddit ericsSub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericsSub);
		
		addModerator(paul.username, paulsSub.subreddit);
		
		long now = System.currentTimeMillis();
		long oneDayInMS = 1000 * 60 * 60 * 24;
		long fiveDaysAgo = now - (oneDayInMS * 5);
		long fourDaysAgo = now - (oneDayInMS * 4);
		SubscribedHashtag ericsTag = new SubscribedHashtag(-1, ericsSub.id, "#scammer", new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(ericsTag);
		
		HandledModAction paulBansJohnHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(fiveDaysAgo));
		database.getHandledModActionMapping().save(paulBansJohnHMA);
		
		BanHistory paulBansJohnBH = new BanHistory(-1, paul.id, john.id, paulBansJohnHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansJohnBH);
		
		HandledModAction meBanJohnHMA = new HandledModAction(-1, ericsSub.id, "ModAction_ID2", new Timestamp(fiveDaysAgo));
		database.getHandledModActionMapping().save(meBanJohnHMA);
		
		BanHistory meBanJohnBH = new BanHistory(-1, me.id, john.id, meBanJohnHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(meBanJohnBH);
		
		UnbanRequest paulUnbanJohnRequest = new UnbanRequest(-1, paul.id, john.id, new Timestamp(fourDaysAgo), null, false);
		database.getUnbanRequestMapping().save(paulUnbanJohnRequest);
		
		database.getResponseMapping().save(new Response(-1, "unban_request_valid_modmail_title", "<unban mod> removed <unbanned user> from USL", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_valid_modmail_body", "I have unbanned /u/<unbanned user> on the request of /u/<unban mod>. /u/<unbanned user> was originally banned on "
				+ "/r/<original ban subreddit> by /u/<original ban mod>, with the description <original description>", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_authorized_title", "auth title", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_authorized_body", "auth body", new Timestamp(now), new Timestamp(now)));
		
		UnbanRequestResult result = handler.handleUnbanRequest(paulUnbanJohnRequest);
		assertEquals(paulUnbanJohnRequest, result.unbanRequest);
		assertEquals(2, result.unbans.size());
		assertEquals(john, result.unbans.get(0).person);
		assertEquals(john, result.unbans.get(1).person);
		if(result.unbans.get(0).subreddit.equals(paulsSub)) {
			assertEquals(ericsSub, result.unbans.get(1).subreddit);
		}else {
			assertEquals(ericsSub, result.unbans.get(0).subreddit);
			assertEquals(paulsSub, result.unbans.get(1).subreddit);
		}
		assertEquals(2, result.modmailPMs.size());
		assertEquals("paul removed john from USL", result.modmailPMs.get(0).title);
		assertEquals("I have unbanned /u/john on the request of /u/paul. /u/john was originally banned on /r/paulssub by /u/paul, with the description #scammer",
				result.modmailPMs.get(0).body);
		assertEquals("paul removed john from USL", result.modmailPMs.get(1).title);
		assertEquals("I have unbanned /u/john on the request of /u/paul. /u/john was originally banned on /r/paulssub by /u/paul, with the description #scammer",
				result.modmailPMs.get(1).body);
		if(result.modmailPMs.get(0).subreddit.equals(paulsSub)) {
			assertEquals(ericsSub, result.modmailPMs.get(1).subreddit);
		}else {
			assertEquals(ericsSub, result.modmailPMs.get(0).subreddit);
			assertEquals(paulsSub, result.modmailPMs.get(1).subreddit);
		}
		assertEquals(1, result.userPMs.size());
		assertEquals(paul, result.userPMs.get(0).person);
		assertEquals("auth title", result.userPMs.get(0).title);
		assertEquals("auth body", result.userPMs.get(0).body);
		assertFalse(result.invalid);
		assertNull(result.scammerToRemove);
	}
	
	@Test
	public void testFlagsNonModeratorInvalid() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);

		long now = System.currentTimeMillis();
		HandledModAction paulBansEricHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(now - 10000));
		database.getHandledModActionMapping().save(paulBansEricHMA);
		
		BanHistory paulBansEricBH = new BanHistory(-1, paul.id, eric.id, paulBansEricHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansEricBH);
		
		UnbanRequest paulUnbanEricRequest = new UnbanRequest(-1, paul.id, eric.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(paulUnbanEricRequest);
		
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_body", "you dont have perms", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_title", "no perms", new Timestamp(now), new Timestamp(now)));
		UnbanRequestResult result = handler.handleUnbanRequest(paulUnbanEricRequest);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(paul, result.userPMs.get(0).person);
		assertEquals("no perms", result.userPMs.get(0).title);
		assertEquals("you dont have perms", result.userPMs.get(0).body);
		assertTrue(result.invalid);
		assertNull(result.scammerToRemove);
	}
	
	@Test
	public void testFlagsWrongModeratorInvalid() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person me = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);

		MonitoredSubreddit secondSub = new MonitoredSubreddit(-1, "secondsub", false, false, false);
		database.getMonitoredSubredditMapping().save(secondSub);
		
	    addModerator("paul", "secondsub");
	    
		long now = System.currentTimeMillis();
		HandledModAction paulBansEricHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(now - 10000));
		database.getHandledModActionMapping().save(paulBansEricHMA);
		
		BanHistory paulBansEricBH = new BanHistory(-1, paul.id, eric.id, paulBansEricHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansEricBH);
		
		HandledModAction meBanEricHMA = new HandledModAction(-1, secondSub.id, "ModAction_ID2", new Timestamp(now - 10000));
		database.getHandledModActionMapping().save(meBanEricHMA);
		
		BanHistory meBanEricBH = new BanHistory(-1, me.id, eric.id, meBanEricHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(meBanEricBH);
		
		UnbanRequest paulUnbanEricRequest = new UnbanRequest(-1, paul.id, eric.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(paulUnbanEricRequest);
		
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_body", "you dont have perms", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_title", "no perms", new Timestamp(now), new Timestamp(now)));
		UnbanRequestResult result = handler.handleUnbanRequest(paulUnbanEricRequest);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(paul, result.userPMs.get(0).person);
		assertEquals("no perms", result.userPMs.get(0).title);
		assertEquals("you dont have perms", result.userPMs.get(0).body);
		assertNull(result.scammerToRemove);
		assertTrue(result.invalid);
	}
	
	@Test
	public void testSendsUnauthorizedWhenNothingToUnban() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);

		long now = System.currentTimeMillis();
		
		UnbanRequest paulUnbanEricRequest = new UnbanRequest(-1, paul.id, eric.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(paulUnbanEricRequest);
		
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_body", "you dont have perms", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_title", "no perms", new Timestamp(now), new Timestamp(now)));
		UnbanRequestResult result = handler.handleUnbanRequest(paulUnbanEricRequest);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(paul, result.userPMs.get(0).person);
		assertEquals("no perms", result.userPMs.get(0).title);
		assertEquals("you dont have perms", result.userPMs.get(0).body);
		assertNull(result.scammerToRemove);
		assertTrue(result.invalid);
	}
	
	@Test
	public void testSendsAuthorizedWhenNothingToUnban() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);

		long now = System.currentTimeMillis();
		
		addModerator("paul", "paulssub");
		
		UnbanRequest paulUnbanEricRequest = new UnbanRequest(-1, paul.id, eric.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(paulUnbanEricRequest);

		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_body", "you dont have perms", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_title", "no perms", new Timestamp(now), new Timestamp(now)));
		UnbanRequestResult result = handler.handleUnbanRequest(paulUnbanEricRequest);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(paul, result.userPMs.get(0).person);
		assertEquals("no perms", result.userPMs.get(0).title);
		assertEquals("you dont have perms", result.userPMs.get(0).body);
		assertNull(result.scammerToRemove);
		assertTrue(result.invalid);
	}
	
	@Test 
	public void testDoesntUnbanTwice() {
		Person emma = database.getPersonMapping().fetchOrCreateByUsername("emma");
		Person ella = database.getPersonMapping().fetchOrCreateByUsername("ella");
		
		MonitoredSubreddit emmasSub = new MonitoredSubreddit(-1, "emmassub", false, false, false);
		database.getMonitoredSubredditMapping().save(emmasSub);

		long now = System.currentTimeMillis();
		HandledModAction emmaBansEllaHMA = new HandledModAction(-1, emmasSub.id, "ModAction_ID1", new Timestamp(now - 20000));
		database.getHandledModActionMapping().save(emmaBansEllaHMA);
		
		BanHistory emmaBansEllaBH = new BanHistory(-1, emma.id, ella.id, emmaBansEllaHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(emmaBansEllaBH);
		
		HandledModAction emmaUnbansEllaHMA = new HandledModAction(-1, emmasSub.id, "ModAction_ID2", new Timestamp(now - 10000));
		database.getHandledModActionMapping().save(emmaUnbansEllaHMA);
		
		UnbanHistory emmaUnbansEllaUBH = new UnbanHistory(-1, emma.id, ella.id, emmaUnbansEllaHMA.id);
		database.getUnbanHistoryMapping().save(emmaUnbansEllaUBH);
		
		addModerator("emma", "emmassub");
		
		UnbanRequest emmaUnbanEllaReq = new UnbanRequest(-1, emma.id, ella.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(emmaUnbanEllaReq);

		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_authorized_title", "auth title", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_authorized_body", "auth body", new Timestamp(now), new Timestamp(now)));
		
		UnbanRequestResult result = handler.handleUnbanRequest(emmaUnbanEllaReq);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(emma, result.userPMs.get(0).person);
		assertEquals("auth title", result.userPMs.get(0).title);
		assertEquals("auth body", result.userPMs.get(0).body);
		assertNull(result.scammerToRemove);
		assertEquals(false, result.invalid);
	}
	
	@Test
	public void testRemovesFromScammerList() {
		Person me = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		Person emma = database.getPersonMapping().fetchOrCreateByUsername("emma");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit mainSub = new MonitoredSubreddit(-1, config.getProperty("user.main_sub"), false, false, false);
		database.getMonitoredSubredditMapping().save(mainSub);
		
		MonitoredSubreddit emmasSub = new MonitoredSubreddit(-1, "emmassub", false, false, false);
		database.getMonitoredSubredditMapping().save(emmasSub);
		
		long now = System.currentTimeMillis();
		TraditionalScammer paulScammer = new TraditionalScammer(-1, paul.id, "grdftr", "#scammer", new Timestamp(now));
		database.getTraditionalScammerMapping().save(paulScammer);
		
		HandledModAction meBansPaulOnEmmaHMA = new HandledModAction(-1, emmasSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(meBansPaulOnEmmaHMA);
		
		BanHistory meBansPaulOnEmmaBH = new BanHistory(-1, me.id, paul.id, meBansPaulOnEmmaHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(meBansPaulOnEmmaBH);
		
		HandledModAction meBansPaulOnMainHMA = new HandledModAction(-1, mainSub.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(meBansPaulOnMainHMA);
		
		BanHistory meBansPaulOnMainBH = new BanHistory(-1, me.id, paul.id, meBansPaulOnMainHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(meBansPaulOnMainBH);

		UnbanRequest emmaUnbanPaulReq = new UnbanRequest(-1, emma.id, paul.id, new Timestamp(now + 10000), null, false);
		database.getUnbanRequestMapping().save(emmaUnbanPaulReq);
		
		// First request - rejected (emma not moderator on main_sub)
		
		addModerator("emma", "emmassub");
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_body", "you dont have perms", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_moderator_unauthorized_title", "no perms", new Timestamp(now), new Timestamp(now)));

		UnbanRequestResult result = handler.handleUnbanRequest(emmaUnbanPaulReq);
		assertEquals(0, result.unbans.size());
		assertEquals(0, result.modmailPMs.size());
		assertEquals(1, result.userPMs.size());
		assertEquals(emma, result.userPMs.get(0).person);
		assertEquals("no perms", result.userPMs.get(0).title);
		assertEquals("you dont have perms", result.userPMs.get(0).body);
		assertNull(result.scammerToRemove);
		assertEquals(true, result.invalid);
		
		// Second request - works

		removeModerator("emma", "emmassub");
		addModerator("emma", mainSub.subreddit);
		database.getResponseMapping().save(new Response(-1, "unban_request_removed_from_list_title", "removed from list", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_removed_from_list_body", "<unbanned user> is no longer on trad scammer list", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_valid_modmail_list_title", "unbanning", new Timestamp(now), new Timestamp(now)));
		database.getResponseMapping().save(new Response(-1, "unban_request_valid_modmail_list_body", "unbanningbody", new Timestamp(now), new Timestamp(now)));
		
		result = handler.handleUnbanRequest(emmaUnbanPaulReq);
		assertEquals(2, result.unbans.size());
		assertEquals(paul, result.unbans.get(0).person);
		assertEquals(paul, result.unbans.get(0).person);
		assertTrue(result.unbans.stream().anyMatch((un) -> un.subreddit.equals(emmasSub)));
		assertTrue(result.unbans.stream().anyMatch((un) -> un.subreddit.equals(mainSub)));
		assertEquals(3, result.modmailPMs.size());
		assertTrue(result.modmailPMs.stream().anyMatch((pm) -> pm.subreddit.equals(mainSub) && pm.title.equals("removed from list") && pm.body.equals("paul is no longer on trad scammer list")));
		assertTrue(result.modmailPMs.stream().anyMatch((pm) -> pm.subreddit.equals(mainSub) && pm.title.equals("unbanning") && pm.body.equals("unbanningbody")));
		assertTrue(result.modmailPMs.stream().anyMatch((pm) -> pm.subreddit.equals(emmasSub) && pm.title.equals("unbanning") && pm.body.equals("unbanningbody")));
		assertEquals(0, result.userPMs.size());
		assertEquals(false, result.invalid);
		assertEquals(paulScammer, result.scammerToRemove);
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		usersToModeratedSubreddits = null;
		handler = null;
	}
}
