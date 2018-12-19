package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
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
import me.timothy.bots.functions.IsModeratorFunction;
import me.timothy.bots.memory.UnbanRequestResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class UnbanRequestHandlerTest {
	private static class MockIsModeratorFunction implements IsModeratorFunction {
		public Map<String, Set<String>> usersToSubreddits;
		
		public MockIsModeratorFunction() {
			usersToSubreddits = new HashMap<>();
		}
		
		@Override
		public Boolean isModerator(String subreddit, String user) {
			Set<String> subs = usersToSubreddits.getOrDefault(user, null);
			if(subs == null)
				return false;
			return subs.contains(subreddit);
		}
		
		public void addMod(String user, String sub) {
			Set<String> cur = usersToSubreddits.getOrDefault(user, null);
			if(cur == null) {
				cur = new HashSet<>();
				usersToSubreddits.put(user, cur);
			}
			cur.add(sub);
		}
	}
	
	private USLDatabase database;
	private USLFileConfiguration config;
	
	private MockIsModeratorFunction isModerator;
	private USLUnbanRequestHandler handler;
	
	private DBShortcuts db;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		
		MysqlTestUtils.clearDatabase(database);

		db = new DBShortcuts(database, config);
		isModerator = new MockIsModeratorFunction();
		handler = new USLUnbanRequestHandler(database, config, isModerator);
		
		setupResponses();
	}
	
	private void setupResponses() {
		db.response("unban_request_to_mod_denied_generic_title");
		db.response("unban_request_to_mod_denied_generic_body");
		db.response("unban_request_to_mod_denied_prevented_title");
		db.response("unban_request_to_mod_denied_prevented_body");
		db.response("unban_request_to_mod_denied_unknown_title");
		db.response("unban_request_to_mod_denied_unknown_body");
		db.response("unban_request_to_mod_denied_no_tags_title");
		db.response("unban_request_to_mod_denied_no_tags_body");
		db.response("unban_request_to_mod_approved_title");
		db.response("unban_request_to_mod_approved_body_no_footer");
		db.response("unban_request_to_mod_approved_tradscammer_append");
		db.response("unban_request_to_mod_approved_override_append");
		db.response("unban_request_to_mod_approved_footer");
	}
	
	@Test
	public void deniesRandomUser() {
		Person mod = db.mod();
		Person banned = db.user1();
		Person rando = db.person("rando");
		db.bot();
		
		MonitoredSubreddit sub = db.sub();
		db.primarySub();
		
		db.sketchyTag();
		Hashtag scammer = db.scammerTag();
		
		HandledModAction hma = db.hma(sub, db.now(-20000));
		BanHistory bh = db.bh(mod, banned, hma, "#scammer", true);
		
		USLAction act = db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh }, null);
		act.createdAt = db.now(-10000);
		database.getUSLActionMapping().save(act);
		
		UnbanRequest req = db.unbanRequest(rando, banned, db.now(-5000), null, false);
		
		// verify it didn't actually do anything
		UnbanRequestResult result = handler.handleUnbanRequest(req);
		assertFalse(req.invalid);
		
		UnbanRequest fromDb = database.getUnbanRequestMapping().fetchByID(req.id);
		assertFalse(fromDb.invalid);
		
		// verify request was denied & we responded
		assertTrue(result.invalid);
		assertFalse(result.userPMs.isEmpty());
	}
	
	@Test
	public void deniesReadOnly() {
		Person mod2 = db.mod2();
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		db.primarySub();
		
		sub1.readOnly = true;
		database.getMonitoredSubredditMapping().save(sub1);
		
		isModerator.addMod(mod.username, sub1.subreddit);
		isModerator.addMod(mod2.username, sub2.subreddit);
		
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub1, scammer);
		db.attach(sub2, scammer);
		
		for(int i = 0; i < 3; i++) { db.hma(sub1, db.now(-100000 * i)); db.hma(sub2, db.now(-90000 * (i+1))); }
		
		HandledModAction hma1 = db.hma(sub1, db.now(-50000));
		BanHistory ban1 = db.bh(mod, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub2, db.now(-45000));
		BanHistory ban2 = db.bh(bot, banned, hma2, "#scammer", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban1, ban2 }, null);
		
		UnbanRequest req1 = db.unbanRequest(mod, banned, db.now(-30000), null, false);
		
		UnbanRequestResult res = handler.handleUnbanRequest(req1);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
	}
	
	@Test
	public void deniesIfNotBannedWithAction() {
		Person mod = db.mod();
		Person banned = db.user1();
		Person unrelated = db.person("user2");
		
		MonitoredSubreddit sub = db.sub();
		db.primarySub();
		
		db.hma(sub, db.now(-59000));
		
		HandledModAction hma1 = db.hma(sub, db.now(-40000));
		db.bh(mod, banned, hma1, "msg", true);
		
		isModerator.addMod(mod.username, sub.subreddit);
		
		UnbanRequest req = db.unbanRequest(mod, banned, db.now(-10000), null, false);
		
		UnbanRequestResult result = handler.handleUnbanRequest(req);
		assertTrue(result.invalid);
		assertFalse(result.userPMs.isEmpty());
		
		UnbanRequest req2 = db.unbanRequest(mod, unrelated, db.now(-5000), null, false);
		
		result = handler.handleUnbanRequest(req2);
		assertTrue(result.invalid);
		assertFalse(result.userPMs.isEmpty());
				
	}
	
	@Test
	public void approvesForModOfOriginatingSubreddit() {
		Person mod2 = db.mod2();
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		db.primarySub();
		
		isModerator.addMod(mod.username, sub1.subreddit);
		isModerator.addMod(mod2.username, sub2.subreddit);
		
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub1, scammer);
		db.attach(sub2, scammer);
		
		for(int i = 0; i < 3; i++) { db.hma(sub1, db.now(-100000 * i)); db.hma(sub2, db.now(-90000 * (i+1))); }
		
		HandledModAction hma1 = db.hma(sub1, db.now(-50000));
		BanHistory ban1 = db.bh(mod, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub2, db.now(-45000));
		BanHistory ban2 = db.bh(bot, banned, hma2, "#scammer", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban1, ban2 }, null);
		
		UnbanRequest req1 = db.unbanRequest(mod, banned, db.now(-30000), null, false);
		
		UnbanRequestResult res = handler.handleUnbanRequest(req1);
		assertFalse(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		
		UnbanRequest req2 = db.unbanRequest(mod2, banned, db.now(-28000), null, false);
		
		res = handler.handleUnbanRequest(req2);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
	}
	
	@Test
	public void approvesModeratorForMainSubreddit() {
		Person mod = db.mod();
		Person mod2 = db.mod2();
		Person banned = db.user1();
		db.bot();
		
		MonitoredSubreddit prim = db.primarySub();
		MonitoredSubreddit sub = db.sub();
		
		Hashtag scammer = db.scammerTag();
		
		isModerator.addMod(mod.username, sub.subreddit);
		isModerator.addMod(mod2.username, prim.subreddit);
		
		db.hma(sub, db.now(-30000));
		db.hma(sub, db.now(-33000));
		db.hma(prim, db.now(-15000));
		
		HandledModAction hma = db.hma(sub, db.now(-10000));
		BanHistory bh = db.bh(mod, banned, hma, "#scammer", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh }, null);
		
		UnbanRequest req = db.unbanRequest(mod2, banned, db.now(-8000), null, false);
		
		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertFalse(res.invalid);
		assertFalse(res.userPMs.isEmpty());
	}
	
	@Test
	public void deniesIfConflict() { 
		Person mod2 = db.mod2();
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		MonitoredSubreddit prim = db.primarySub();
		MonitoredSubreddit sub = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		
		Hashtag scammer = db.scammerTag();
		Hashtag sketchy = db.sketchyTag();
		
		db.attach(prim, scammer);
		db.attach(sub, scammer);
		db.attach(sub2, scammer);
		db.attach(sub2, sketchy);
		
		isModerator.addMod(mod.username, sub.subreddit);
		isModerator.addMod(mod2.username, sub2.subreddit);
		
		HandledModAction hma1 = db.hma(prim, db.now(-45000));
		BanHistory ban1 = db.bh(bot, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub, db.now(-60000));
		BanHistory ban2 = db.bh(mod, banned, hma2, "#scammer", true);
		
		HandledModAction hma3 = db.hma(sub2, db.now(-50000));
		BanHistory ban3 = db.bh(mod2, banned, hma3, "#sketchy", true);
		
		db.action(true, banned, new Hashtag[] { scammer, sketchy }, new BanHistory[] { ban1, ban2, ban3 }, null);
		
		UnbanRequest req = db.unbanRequest(mod2, banned, db.now(-8000), null, false);
		
		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		
		req = db.unbanRequest(mod, banned, db.now(-5000), null, false);
		
		res = handler.handleUnbanRequest(req);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
	}
	
	@Test
	public void removesFromTradListIfUSLMod() {
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		MonitoredSubreddit prim = db.primarySub();
		MonitoredSubreddit sub = db.sub();
		
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub, scammer);
		
		isModerator.addMod(mod.username, prim.subreddit);
		
		HandledModAction hma = db.hma(sub, db.now(-10000));
		BanHistory bh = db.bh(bot, banned, hma, "#scammer", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh }, null);
		
		database.getTraditionalScammerMapping().save(new TraditionalScammer(-1, banned.id, "on the trad list", "#scammer", db.now(-30000)));
		
		UnbanRequest req = db.unbanRequest(mod, banned, db.now(-5000), null, false);
		
		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertFalse(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		assertNotNull(res.scammerToRemove);
		assertEquals(banned.id, res.scammerToRemove.personID);
	}
	
	@Test
	public void removesFromTradListIfNotUSLModButOnlyStake() {
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		/*MonitoredSubreddit prim = */db.primarySub();
		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		
		db.sketchyTag();
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub1, scammer);
		db.attach(sub2, scammer);
		
		isModerator.addMod(mod.username, sub1.subreddit);
		
		HandledModAction hma1 = db.hma(sub1, db.now(-30000));
		BanHistory bh1 = db.bh(mod, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub2, db.now(-25000));
		BanHistory bh2 = db.bh(bot, banned, hma2, "#scammer", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh1, bh2 }, null);
		database.getTraditionalScammerMapping().save(new TraditionalScammer(-1, banned.id, "on the trad list", "#scammer", db.now(-30000)));
		
		UnbanRequest req = db.unbanRequest(mod, banned, db.now(-10000), null, false);

		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertFalse(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		assertNotNull(res.scammerToRemove);
		assertEquals(banned.id, res.scammerToRemove.personID);
	}
	
	@Test
	public void deniesFromTradListIfNoStake() {
		Person banned = db.user1();
		Person mod = db.mod();
		Person bot = db.bot();

		MonitoredSubreddit sub1 = db.sub();
		db.primarySub();
		MonitoredSubreddit sub2 = db.sub2();
		
		db.sketchyTag();
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub1, scammer);
		db.attach(sub2, scammer);
		
		isModerator.addMod(mod.username, sub2.subreddit);
		
		HandledModAction hma1 = db.hma(sub1, db.now(-30000));
		BanHistory bh1 = db.bh(bot, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub2, db.now(-25000));
		BanHistory bh2 = db.bh(mod, banned, hma2, "notag", true);
		
		db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh1, bh2 }, null);
		database.getTraditionalScammerMapping().save(new TraditionalScammer(-1, banned.id, "on the trad list", "#scammer", db.now(-30000)));

		UnbanRequest req = db.unbanRequest(mod, banned, db.now(-10000), null, false);

		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		assertNull(res.scammerToRemove);
	}
	
	@Test
	public void deniesFromTradListIfConflict() {
		db.bot();
		Person mod1 = db.mod();
		Person mod2 = db.mod2();
		Person banned = db.user1();
		
		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		db.primarySub();
		
		db.scammerTag();
		Hashtag sketchy = db.sketchyTag();
		
		db.attach(sub1, sketchy);
		db.attach(sub2, sketchy);
		
		isModerator.addMod(mod1.username, sub1.subreddit);
		isModerator.addMod(mod2.username, sub2.subreddit);
		
		HandledModAction hma1 = db.hma(sub2, db.now(-30000));
		BanHistory bh1 = db.bh(mod2, banned, hma1, "#sketchy", true);
		
		HandledModAction hma2 = db.hma(sub1, db.now(-25000));
		BanHistory bh2 = db.bh(mod1, banned, hma2, "#sketchy", true);
		
		db.action(true, banned, new Hashtag[] { sketchy }, new BanHistory[] { bh1,  bh2 }, null);
		database.getTraditionalScammerMapping().save(new TraditionalScammer(-1, banned.id, "on the trad list", "#scammer", db.now(-30000)));

		UnbanRequest req = db.unbanRequest(mod1, banned, db.now(-10000), null, false);

		UnbanRequestResult res = handler.handleUnbanRequest(req);
		assertTrue(res.invalid);
		assertFalse(res.userPMs.isEmpty());
		assertNull(res.scammerToRemove);
	}

	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		handler = null;
	}
}
