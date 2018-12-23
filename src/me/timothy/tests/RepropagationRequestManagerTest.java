package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLDatabaseBackupManager;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLRepropagationRequestManager;
import me.timothy.bots.functions.SendPMFunction;
import me.timothy.bots.functions.SubmitSelfFunction;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.DirtyPerson;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.PropagatorSetting.PropagatorSettingKey;
import me.timothy.bots.models.RepropagationRequest;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class RepropagationRequestManagerTest {
	public class MockDatabaseBackupManager extends USLDatabaseBackupManager {
		public Queue<Runnable> handlers;
		
		public MockDatabaseBackupManager() {
			super(RepropagationRequestManagerTest.this.database, RepropagationRequestManagerTest.this.config);
			
			handlers = new ArrayDeque<>();
		}
		
		public void postExpect() {
			assertTrue(handlers.isEmpty());
		}
		
		@Override
		public void forceBackup() {
			assertFalse(handlers.isEmpty());
			handlers.poll().run();
		}
	}
	
	public class MockSubmitSelfFunction implements SubmitSelfFunction {
		public Queue<SubmitSelfFunction> expectedCalls;
		
		public MockSubmitSelfFunction() {
			expectedCalls = new ArrayDeque<>();
		}
		
		public void postExpect() {
			assertTrue(expectedCalls.isEmpty());
		}
		
		public void expect(final String subreddit) {
			expectedCalls.add((sub, title, body) -> { assertEquals(subreddit, sub); });
		}
		
		@Override
		public void submitSelf(String subreddit, String title, String body) {
			assertFalse(expectedCalls.isEmpty());
			expectedCalls.poll().submitSelf(subreddit, title, body);
		}	
	}
	
	public class MockSendPMFunction implements SendPMFunction {
		public Queue<SendPMFunction> expectedCalls;
		
		public MockSendPMFunction() {
			expectedCalls = new ArrayDeque<>();
		}
		
		public void postExpect() {
			assertTrue(expectedCalls.isEmpty());
		}
		
		public void expect(Person person) {
			expectedCalls.add((pmInfo) -> { assertEquals(person, pmInfo.person); });
		}

		@Override
		public void send(UserPMInformation pmInfo) {
			assertFalse(expectedCalls.isEmpty());
			expectedCalls.poll().send(pmInfo);
		}
		
	}
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected DBShortcuts db;
	
	protected MockDatabaseBackupManager backupManager;
	protected MockSubmitSelfFunction submitSelf;
	protected MockSendPMFunction sendPM;
	protected USLRepropagationRequestManager repropManager;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		db = new DBShortcuts(database, config);
		
		MysqlTestUtils.clearDatabase(database);
		
		backupManager = new MockDatabaseBackupManager();
		submitSelf = new MockSubmitSelfFunction();
		sendPM = new MockSendPMFunction();
		repropManager = new USLRepropagationRequestManager(database, config, backupManager, submitSelf, sendPM);
	}
	
	protected void postExpect() {
		backupManager.postExpect();
		submitSelf.postExpect();
		sendPM.postExpect();
	}
	
	protected void setupResponses() {
		db.response("reprop_approve_pm_title");
		db.response("reprop_approve_pm_body");
		db.response("reprop_approve_post_title");
		db.response("reprop_approve_post_body");
		db.response("reprop_reject_already_repropagating_pm_title");
		db.response("reprop_reject_already_repropagating_pm_body");
	}
	
	@Test
	public void testApproveWhenNotRepropagating() {
		setupResponses();
		repropManager.verifyHaveResponses();
		
		MonitoredSubreddit sub = db.sub();
		MonitoredSubreddit usl = db.primarySub();
		MonitoredSubreddit notifs = db.sub(config.getProperty("general.notifications_sub"));

		Hashtag sketchy = db.sketchyTag();
		Hashtag scammer = db.scammerTag();
		
		db.attach(sub, scammer);
		db.attach(usl, scammer);
		db.attach(sub, sketchy);
		
		Person mod = db.mod();
		Person bot = db.bot();
		Person banned = db.user1();
		
		HandledModAction hma1 = db.hma(usl, db.now(-80000));
		BanHistory ban1 = db.bh(mod, banned, hma1, "#scammer", true);
		
		HandledModAction hma2 = db.hma(sub, db.now(-75000));
		BanHistory ban2 = db.bh(bot, banned, hma2, "#scammer", true);
		
		HandledModAction hma3 = db.hma(sub, db.now(-72000));
		UnbanHistory unban1 = db.ubh(mod, banned, hma3);
		
		final USLAction act = db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban1 }, new UnbanHistory[] { unban1 });
		
		RepropagationRequest req = new RepropagationRequest(-1, mod.id, "why not", false, db.now(), null);
		database.getRepropagationRequestMapping().save(req);
		
		database.getDirtyPersonMapping().save(new DirtyPerson(banned.id));
		
		backupManager.handlers.add(() -> {
			// Before clearing
			MysqlTestUtils.assertListContents(database.getPersonMapping().fetchAll(), mod, bot, banned);
			MysqlTestUtils.assertListContents(database.getBanHistoryMapping().fetchAll(), ban1, ban2);
			MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), act);
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionBanHistoryMapping().fetchAll(), 
					(a) -> a.actionID == act.id && a.banHistoryID == ban1.id);
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionUnbanHistoryMapping().fetchAll(),
					(a) -> a.actionID == act.id && a.unbanHistoryID == unban1.id);
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionHashtagMapping().fetchAll(), 
					(a) -> a.actionID == act.id && a.hashtagID == scammer.id);
			assertTrue(database.getDirtyPersonMapping().contains(banned.id));
		});
		
		backupManager.handlers.add(() -> {
			// After clearing
			MysqlTestUtils.assertListContents(database.getPersonMapping().fetchAll(), mod, bot, banned);
			MysqlTestUtils.assertListContents(database.getBanHistoryMapping().fetchAll(), ban1, ban2);
			MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionBanHistoryMapping().fetchAll());
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionUnbanHistoryMapping().fetchAll());
			MysqlTestUtils.assertListContentsPreds(database.getUSLActionHashtagMapping().fetchAll());
			assertFalse(database.getDirtyPersonMapping().contains(banned.id));
			assertEquals("true", database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		});
		
		submitSelf.expect(notifs.subreddit);
		sendPM.expect(mod);
		assertNull(database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		repropManager.processRequest(req);
		
		req = database.getRepropagationRequestMapping().fetchByID(req.id);
		assertNotNull(req);
		assertTrue(req.approved);
		assertNotNull(req.handledAt);
		
		String setting = database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES);
		assertNotNull(setting);
		assertEquals("true", setting);
		
		postExpect();
	}
	
	@Test
	public void testRejectWhenRepropagating() {
		setupResponses();
		repropManager.verifyHaveResponses();
		
		Person mod = db.mod();
		
		RepropagationRequest req = new RepropagationRequest(-1, mod.id, "for testing!", false, db.now(), null);
		database.getRepropagationRequestMapping().save(req);
		
		sendPM.expect(mod);
		database.getPropagatorSettingMapping().put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "true");
		assertEquals("true", database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		repropManager.processRequest(req);
		
		req = database.getRepropagationRequestMapping().fetchByID(req.id);
		assertNotNull(req);
		assertFalse(req.approved);
		assertNotNull(req.handledAt);
		assertEquals("true", database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		postExpect();
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		backupManager = null;
		submitSelf = null;
		sendPM = null;
		repropManager = null;
	}
}
