package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLPropagator;
import me.timothy.bots.USLPropagatorManager;
import me.timothy.bots.USLRedditToMeaningProcessor;
import me.timothy.bots.USLValidUnbanRequestToMeaningProcessor;
import me.timothy.bots.functions.PropagateResultHandlerFunction;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class PropagatorManagerTest {
	public class MockPropagator extends USLPropagator {
		public Queue<Predicate<USLAction>> expectedArgs;
		public Queue<Function<USLAction, PropagateResult>> results;
		
		public MockPropagator() {
			super(PropagatorManagerTest.this.database, PropagatorManagerTest.this.config);
			
			expectedArgs = new ArrayDeque<>();
			results = new ArrayDeque<>();
		}
		
		public void expect(Predicate<USLAction> action, Function<USLAction, PropagateResult> result) {
			expectedArgs.add(action);
			results.add(result);
		}
		
		public void postExpect() {
			assertTrue(expectedArgs.isEmpty());
			assertTrue(results.isEmpty());
		}
		
		@Override
		public PropagateResult propagateAction(HashMap<Integer, MonitoredSubreddit> readingSubreddits, USLAction action) {
			assertTrue(results.size() > 0);
			assertTrue(expectedArgs.size() > 0);
			assertTrue(expectedArgs.poll().test(action));
			
			return results.poll().apply(action);
		}
	}
	
	public class MockPropagatorResultHandler implements PropagateResultHandlerFunction {
		public Queue<Predicate<PropagateResult>> expectedArgs;
		public Queue<Boolean> results;
		
		public MockPropagatorResultHandler() {
			expectedArgs = new ArrayDeque<>();
			results = new ArrayDeque<>();
		}
		
		public void expect(Predicate<PropagateResult> prop, boolean result) {
			expectedArgs.add(prop);
			results.add(result);
		}
		
		public void postExpect() {
			assertTrue(expectedArgs.isEmpty());
			assertTrue(results.isEmpty());
		}
		
		@Override
		public boolean handleResult(PropagateResult prop) {
			assertFalse(results.isEmpty());
			assertFalse(expectedArgs.isEmpty());
			assertTrue(expectedArgs.poll().test(prop));
			return results.poll();
		}
	}
	
	public class MockRedditToMeaningProcessor extends USLRedditToMeaningProcessor {
		public Queue<Boolean> expectedOrderOfBanUnban;
		
		public Queue<BanHistory> expectedBanArgs;
		public Queue<Supplier<Set<Integer>>> banRunnables;
		
		public Queue<UnbanHistory> expectedUnbanArgs;
		public Queue<Supplier<Set<Integer>>> unbanRunnables;
		
		public MockRedditToMeaningProcessor() {
			super(PropagatorManagerTest.this.database, PropagatorManagerTest.this.config);
			
			expectedOrderOfBanUnban = new ArrayDeque<>();
			
			expectedBanArgs = new ArrayDeque<>();
			banRunnables = new ArrayDeque<>();
			
			expectedUnbanArgs = new ArrayDeque<>();
			unbanRunnables = new ArrayDeque<>();
		}
		
		public void expectBan(BanHistory ban, Supplier<Set<Integer>> handler) {
			expectedOrderOfBanUnban.add(true);
			expectedBanArgs.add(ban);
			banRunnables.add(handler);
		}
		
		public void expectUnban(UnbanHistory unban, Supplier<Set<Integer>> handler) {
			expectedOrderOfBanUnban.add(false);
			expectedUnbanArgs.add(unban);
			unbanRunnables.add(handler);
		}
		
		public void postExpect() {
			assertTrue(expectedOrderOfBanUnban.isEmpty());
			assertTrue(expectedBanArgs.isEmpty());
			assertTrue(banRunnables.isEmpty());
			assertTrue(expectedUnbanArgs.isEmpty());
			assertTrue(unbanRunnables.isEmpty());
		}
		
		@Override
		public Set<Integer> processBan(List<Hashtag> tags, HandledModAction hma, BanHistory ban) {
			assertFalse(expectedOrderOfBanUnban.isEmpty());
			assertFalse(expectedBanArgs.isEmpty());
			assertFalse(banRunnables.isEmpty());
			assertTrue(expectedOrderOfBanUnban.poll());
			assertEquals(expectedBanArgs.poll(), ban);
			return banRunnables.poll().get();
		}
		
		@Override
		public Set<Integer> processUnban(List<Hashtag> tags, HandledModAction hma, UnbanHistory unban) {
			assertFalse(expectedOrderOfBanUnban.isEmpty());
			assertFalse(expectedUnbanArgs.isEmpty());
			assertFalse(unbanRunnables.isEmpty());
			assertFalse(expectedOrderOfBanUnban.poll());
			assertEquals(expectedUnbanArgs.poll(), unban);
			return unbanRunnables.poll().get();
		}
	}
	
	public class MockValidUnbanRequestToMeaningProcessor extends USLValidUnbanRequestToMeaningProcessor {
		public Queue<UnbanRequest> expectedRequests;
		public Queue<Runnable> handlers;
		
		public MockValidUnbanRequestToMeaningProcessor() {
			super(PropagatorManagerTest.this.database, PropagatorManagerTest.this.config);
			
			expectedRequests = new ArrayDeque<>();
			handlers = new ArrayDeque<>();
		}
		
		public void expect(UnbanRequest req, Runnable runnable) {
			expectedRequests.add(req);
			handlers.add(runnable);
		}
		
		public void postExpect() {
			assertTrue(expectedRequests.isEmpty());
			assertTrue(handlers.isEmpty());
		}
		
		@Override
		public void processUnbanRequest(UnbanRequest request) {
			assertFalse(expectedRequests.isEmpty());
			assertFalse(handlers.isEmpty());
			
			assertEquals(expectedRequests.poll(), request);
			handlers.poll().run();
		}
	}
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected DBShortcuts db;
	
	protected MockPropagator propagator;
	protected MockPropagatorResultHandler resultHandler;
	protected MockRedditToMeaningProcessor meaning;
	protected MockValidUnbanRequestToMeaningProcessor unbanRequestMeaning;
	protected USLPropagatorManager propManager;

	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		
		MysqlTestUtils.clearDatabase(database);

		db = new DBShortcuts(database, config);
		propagator = new MockPropagator();
		resultHandler = new MockPropagatorResultHandler();
		meaning = new MockRedditToMeaningProcessor();
		unbanRequestMeaning = new MockValidUnbanRequestToMeaningProcessor();
		propManager = new USLPropagatorManager(database, config, propagator, meaning, unbanRequestMeaning, resultHandler);
	}
	
	protected void postExpect() {
		propagator.postExpect();
		resultHandler.postExpect();
		meaning.postExpect();
		unbanRequestMeaning.postExpect();
	}
	
	@Test
	public void testPropagateNothing() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@Test
	public void testPropagateMissingModQueueStatus() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		subs.add(db.sub());
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@Test
	public void testPropagateEmptyWithModQueueStatus() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		subs.add(db.sub());
		
		db.emptyProgress(db.sub());
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@Test
	public void testPropagateSingleHMA() {
		System.out.println("testPropSingleHMA");
		List<MonitoredSubreddit> subs = new ArrayList<>();
		subs.add(db.sub());
		
		db.hma(db.sub());
		db.fullProgress(db.sub(), db.now(10000));
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@Test
	public void testPropagateSinglePermBanNoAction() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		subs.add(db.sub());
		
		db.hma(db.sub()); // we want to avoid everything having the same id, this way hma id != ban id
		HandledModAction hma = db.hma(db.sub());
		
		BanHistory bh = db.bh(db.mod(), db.user1(), hma, "msg", true);
		meaning.expectBan(bh, () -> Collections.emptySet());
		
		db.fullProgress(db.sub(), db.now(10000));
		
		propManager.managePropagating(subs);
		postExpect();
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@Test
	public void testPropagateSinglePermBanWithAction() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		subs.add(db.sub());
		
		db.person("realid1");
		db.person("realid2");
		
		Hashtag scammer = db.scammerTag();
		
		db.hma(db.sub());
		HandledModAction hma = db.hma(db.sub());
		
		Person banned = db.user1();
		BanHistory bh = db.bh(db.mod(), banned, hma, "msg", true);
		meaning.expectBan(bh, () -> {
			db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh }, new UnbanHistory[] {});
			return Collections.singleton(bh.bannedPersonID);
		});
		
		propagator.expect((action) -> action.personID == db.user1().id, (action) -> new PropagateResult(action));
		resultHandler.expect((prop) -> prop.action.personID == db.user1().id, false);
		
		db.fullProgress(db.sub(), db.now(10000));
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	/**
	 * One guy is banned without a tag. Another guy is banned with a tag that all 3 subreddits follow, and that
	 * ban is propagated to the other two subreddits.
	 */
	@Test
	public void testConcurrentScenario1() {
		Random rand = new Random();
		
		List<MonitoredSubreddit> subs = new ArrayList<>();
		MonitoredSubreddit sub1, sub2, sub3;
		subs.add(sub1 = db.sub());
		subs.add(sub2 = db.sub2());
		subs.add(sub3 = db.sub3());
		
		db.person("realid1");
		
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub1, scammer);
		db.attach(sub2, scammer);
		db.attach(sub3, scammer);
		
		Person bot = db.bot();
		Person mod = db.mod();
		Person mod2 = db.mod2();
		Person banned1 = db.person("banned1");
		Person banned2 = db.person("banned2");
		
		propManager.managePropagating(subs);
		postExpect();
		
		// Push some irrelevant actions
		for(MonitoredSubreddit sub : subs) {
			int num = rand.nextInt(10) + 2;
			for(int i = 0; i < num; i++) {
				db.hma(sub, db.now(-(rand.nextInt(100) + 65) * 1000));             // 65 - 165 seconds ago
			}
		}
		
		propManager.managePropagating(subs);
		postExpect();
		
		subs.forEach((sub) -> db.fullProgress(sub, db.now(1000))); // we set full progress to the future so we'll have it for all future calls
		propManager.managePropagating(subs);
		postExpect();
		
		HandledModAction hma1 = db.hma(sub2, db.now(-60000));				  // 60 seconds ago
		BanHistory ban = db.bh(mod, banned1, hma1, "dont like him", true);
		
		meaning.expectBan(ban, () -> Collections.emptySet());
		propManager.managePropagating(subs);
		postExpect();
		
		propManager.managePropagating(subs);
		postExpect();
		
		for(MonitoredSubreddit sub : subs) {
			int num = rand.nextInt(3);
			for(int i = 0; i < num; i++) {
				db.hma(sub, db.now((45 + rand.nextInt(10)) * -1000));		  // 45-55 seconds ago
			}
		}

		propManager.managePropagating(subs);
		postExpect();
		
		HandledModAction hma2 = db.hma(sub3, db.now(-40000));				 // 40 seconds ago			
		BanHistory ban2 = db.bh(mod2, banned2, hma2, "#scammer", true);
		
		meaning.expectBan(ban2, () -> {
			db.action(true, banned2, new Hashtag[] { scammer }, new BanHistory[] { ban2 }, new UnbanHistory[0]);
			return Collections.singleton(banned2.id);
		});
		
		propagator.expect((action) -> action.isBan && action.personID == banned2.id, (action) -> {
			return new PropagateResult(action, 
					Arrays.asList(
							new UserBanInformation(banned2, sub1, "msg", "other", "#scammer"), 
							new UserBanInformation(banned2, sub2, "msg", "other", "#scammer")),
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.emptyList()
					);
		});
		
		resultHandler.expect((result) -> result.bans.size() == 2, true);
		
		propManager.managePropagating(subs);
		postExpect();
		
		USLAction action = database.getUSLActionMapping().fetchLatest(banned2.id);
		assertNotNull(action);
		
		HandledModAction hma3 = db.hma(sub1, db.now(-35000));              // 35 seconds ago
		BanHistory ban3 = db.bh(bot, banned2, hma3, "#scammer", true);
		
		meaning.expectBan(ban3, () -> {
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban3.id));
			return Collections.singleton(action.personID);
		});
		
		HandledModAction hma4 = db.hma(sub2, db.now(-30000));			// 30 seconds ago
		BanHistory ban4 = db.bh(bot, banned2, hma4, "#scammer", true);
		
		meaning.expectBan(ban4, () -> {
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban4.id));
			return Collections.singleton(action.personID);
		});
		
		// We should only see this action propagated once, despite it being dirtied twice. This requires
		// the propagator manager to look into the future, which is where the complexity comes from
		propagator.expect((innerAction) -> innerAction.equals(action), (innerAction) -> {
			// By the time we get here we must have already seen ban3 and ban4. The manager does not
			// have the flexibility of sending things to the propagator in order
			List<USLActionBanHistory> fromDB = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(innerAction.id);
			MysqlTestUtils.assertListContentsPreds(fromDB,
					(a) -> a.banHistoryID == ban2.id,
					(a) -> a.banHistoryID == ban3.id,
					(a) -> a.banHistoryID == ban4.id);
			return new PropagateResult(innerAction);
		});
		
		resultHandler.expect((result) -> result.bans.isEmpty(), false);
		
		propManager.managePropagating(subs);
		postExpect();

		propManager.managePropagating(subs);
		postExpect();
	}
	
	/**
	 * A guy is banned with the #scammer tag in the past. It is propagated to one other
	 * subreddit. The guy is unbanned, which is propagated to both subreddits. The guy is
	 * rebanned with the #sketchy tag. It is propagated to one other subreddit. The bot
	 * doesn't need to do anything.
	 */
	@Test
	public void testPastScenario1() {
		List<MonitoredSubreddit> subs = new ArrayList<>();
		MonitoredSubreddit sub1, sub2, sub3, sub4;
		subs.add(sub1 = db.sub());
		subs.add(sub2 = db.sub2());
		subs.add(sub3 = db.sub3());
		subs.add(sub4 = db.sub("sub4"));
		
		db.person("realid1");
		db.person("realid2");
		
		Person bot = db.bot();
		Person mod = db.mod();
		Person mod2 = db.mod2();
		Person banned = db.user1();

		Hashtag scammer = db.scammerTag();
		Hashtag sketchy = db.sketchyTag();
		
		db.attach(sub1, sketchy);
		db.attach(sub2, scammer);
		db.attach(sub3, scammer);
		db.attach(sub4, sketchy);
		
		// The propagator won't get a chance to manage propagating until all of this is done
		// To make it even harder, we're not going to make the natural
		// ordering coincide with the temporal ordering.
		
		// The manager will be forced to:
		//  - Find the correct order to send to the appropriate meaning processors
		//      This will require a 4-way merge (ban histories, unban histories, handled mod actions, unban requests)
		//		where the tables are NOT evenly spread
		//  - Not send to the propagator until it is current
		//  - Send to the propagator exactly once, even though this user has been dirtied several times
		
		HandledModAction hma1 = db.hma(sub2, db.now(-120000));				    // SECOND THING: bot banned on sub2
		BanHistory ban1 = db.bh(bot, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub3, db.now(-60000));					// FOURTH THING: bot unbanned on sub3
		UnbanHistory unban1 = db.ubh(bot, banned, hma2);
		
		HandledModAction hma3 = db.hma(sub3, db.now(-140000));					// FIRST THING: mod2 banned on sub3
		BanHistory ban2 = db.bh(mod2, banned, hma3, "#scammer", true);			
		
		HandledModAction hma4 = db.hma(sub4, db.now(-30000));					// SIXTH THING: mod banned on sub4
		BanHistory ban3 = db.bh(mod, banned, hma4, "#sketchy", true);
		
		UnbanRequest unbanReq = db.unbanRequest(mod, banned, 					// THIRD THING: mod2 unbanned
				db.now(-110000), db.now(-100000), false);
		
		HandledModAction hma5 = db.hma(sub2, db.now(-50000)); 					// FIFTH THING: bot unbanned on sub2
		UnbanHistory unban2 = db.ubh(bot, banned, hma5);
		
		HandledModAction hma6 = db.hma(sub1, db.now(-20000));					// SEVENTH THING: bot banned on sub1
		BanHistory ban4 = db.bh(bot, banned, hma6, "#sketchy", true);
		
		meaning.expectBan(ban2, () -> {
			db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban2 }, new UnbanHistory[0]);
			return Collections.singleton(banned.id);
		});
		
		meaning.expectBan(ban1, () -> {
			USLAction action = database.getUSLActionMapping().fetchLatest(banned.id);
			assertNotNull(action);
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban1.id));
			return Collections.emptySet(); // bot bans don't dirty actions, though the manager doesn't know that
		});
		
		unbanRequestMeaning.expect(unbanReq, () -> {
			USLAction action = database.getUSLActionMapping().fetchLatest(banned.id);
			assertNotNull(action);
			
			// We need to check to make sure we weren't called before the bans
			List<USLActionBanHistory> fromDB = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(action.id);
			MysqlTestUtils.assertListContentsPreds(fromDB,
					(a) -> a.banHistoryID == ban2.id,
					(a) -> a.banHistoryID == ban1.id);
			
			db.action(false, banned, null, new BanHistory[] { ban2, ban1 }, null);
		});
		
		meaning.expectUnban(unban1, () -> {
			USLAction action = database.getUSLActionMapping().fetchLatest(banned.id);
			
			// We need to check we were called after the unban request
			assertFalse(action.isBan);
			
			database.getUSLActionBanHistoryMapping().delete(action.id, ban2.id);
			database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, unban1.id));
			return Collections.emptySet(); // bot unbans don't dirty actions
		});
		
		meaning.expectUnban(unban2, () -> {
			USLAction action = database.getUSLActionMapping().fetchLatest(banned.id);
			
			database.getUSLActionBanHistoryMapping().delete(action.id, ban1.id);
			database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, unban2.id));
			return Collections.emptySet();
		});
		
		meaning.expectBan(ban3, () -> {
			db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban3 }, new UnbanHistory[] { unban1, unban2 });
			return Collections.singleton(banned.id);
		});
		
		meaning.expectBan(ban4, () -> {
			USLAction action = database.getUSLActionMapping().fetchLatest(banned.id);

			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban4.id));
			return Collections.emptySet(); // bot actions don't dirty actions
		});
		
		propagator.expect((action) -> {
			// We need to verify we were called after all the meaning stuff
			assertTrue(action.isLatest);
			assertTrue(action.isBan);
			assertEquals(banned.id, action.personID);
			List<USLActionBanHistory> bans = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(action.id);
			MysqlTestUtils.assertListContentsPreds(bans, 
					(a) -> a.banHistoryID == ban3.id,
					(a) -> a.banHistoryID == ban4.id);
			
			List<USLActionUnbanHistory> unbans = database.getUSLActionUnbanHistoryMapping().fetchByUSLActionID(action.id);
			MysqlTestUtils.assertListContentsPreds(unbans,
					(a) -> a.unbanHistoryID == unban1.id, 
					(a) -> a.unbanHistoryID == unban2.id);
			return true;
		}, (action) -> new PropagateResult(action));
		
		resultHandler.expect((result) -> result.bans.isEmpty(), false);

		subs.forEach((sub) -> db.fullProgress(sub, db.now(1000)));
		
		propManager.managePropagating(subs);
		postExpect();
		
		propManager.managePropagating(subs);
		postExpect();
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		propagator = null;
	}
}
