package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.memory.HandledModActionJoinHistory;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class HandledModActionMappingTest {
	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.FullnameMapping FullnameMapping} to test.
	 */
	protected MappingDatabase database;
	
	/**
	 * Verifies the test is setup correctly by ensuring the {@link #database} is not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	/**
	 * create subreddit johnssub, save modaction, ensure id > 0 and
	 * timestamp nanos are stripped. ensure fetchable with fetchAll.
	 * modify, save again, ensure fetchable
	 */
	@Test
	public void testSave() {
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		HandledModAction mAction = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(mAction);
		
		assertTrue(mAction.id > 0);
		assertEquals(0, mAction.occurredAt.getNanos());
		
		List<HandledModAction> fromDB = database.getHandledModActionMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, mAction);
		
		int oldID = mAction.id;
		mAction.modActionID = "ModAction_ID2";
		database.getHandledModActionMapping().save(mAction);
		assertEquals(oldID, mAction.id);
		
		fromDB = database.getHandledModActionMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, mAction);
	}
	
	@Test
	public void testFetchByID() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, true);
		database.getMonitoredSubredditMapping().save(sub);

		HandledModAction mAction = new HandledModAction(-1, sub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(mAction);
		
		HandledModAction fromDB = database.getHandledModActionMapping().fetchByID(mAction.id);
		assertEquals(mAction, fromDB);
		
		fromDB = database.getHandledModActionMapping().fetchByID(mAction.id + 1);
		assertNull(fromDB);
	}
	
	@Test
	public void testFetchByModActionID() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, true);
		database.getMonitoredSubredditMapping().save(sub);

		HandledModAction mAction = new HandledModAction(-1, sub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(mAction);
		
		HandledModAction fromDB = database.getHandledModActionMapping().fetchByModActionID("ModAction_ID1");
		assertEquals(mAction, fromDB);
		
		fromDB = database.getHandledModActionMapping().fetchByModActionID("ModAction_ID2");
		assertNull(fromDB);
	}
	
	@Test
	public void testFetchByTimestampAndSubreddit() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, true);
		database.getMonitoredSubredditMapping().save(sub);

		HandledModAction mAction = new HandledModAction(-1, sub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(mAction);
		
		List<HandledModAction> fromDB = database.getHandledModActionMapping().fetchByTimestampAndSubreddit(mAction.occurredAt, sub.id);
		MysqlTestUtils.assertListContents(fromDB, mAction);

		fromDB = database.getHandledModActionMapping().fetchByTimestampAndSubreddit(mAction.occurredAt, sub.id + 1);
		MysqlTestUtils.assertListContents(fromDB);

		fromDB = database.getHandledModActionMapping().fetchByTimestampAndSubreddit(new Timestamp(mAction.occurredAt.getTime() + 1000), sub.id);
		MysqlTestUtils.assertListContents(fromDB);
		
		HandledModAction mAction2 = new HandledModAction(-1, sub.id, "ModAction_ID2", new Timestamp(mAction.occurredAt.getTime() + 5000));
		database.getHandledModActionMapping().save(mAction2);
		
		fromDB = database.getHandledModActionMapping().fetchByTimestampAndSubreddit(mAction2.occurredAt, sub.id);
		MysqlTestUtils.assertListContents(fromDB, mAction2);
		
		HandledModAction mAction3 = new HandledModAction(-1, sub.id, "ModAction_ID3", new Timestamp(mAction2.occurredAt.getTime()));
		database.getHandledModActionMapping().save(mAction3);
		
		fromDB = database.getHandledModActionMapping().fetchByTimestampAndSubreddit(mAction2.occurredAt, sub.id);
		MysqlTestUtils.assertListContents(fromDB, mAction2, mAction3);
	}
	
	@Test
	public void testFetchLatestBySubreddit() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, true);
		database.getMonitoredSubredditMapping().save(sub);
		
		final long now = System.currentTimeMillis();
		List<HandledModAction> fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(now), null, 10);
		MysqlTestUtils.assertListContents(fromDB);
		
		HandledModAction mAction = new HandledModAction(-1, sub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(mAction);
		
		final long nowRounded = mAction.occurredAt.getTime();
		
		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(nowRounded), null, 10);
		MysqlTestUtils.assertListContents("now=" + now + ", nowRounded=" + nowRounded, fromDB, mAction);
		
		
		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(nowRounded - 1000), null, 10);
		MysqlTestUtils.assertListContents(fromDB, mAction);
		
		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id + 1, new Timestamp(nowRounded - 1000), null, 10);
		MysqlTestUtils.assertListContents(fromDB);
		
		HandledModAction mAction2 = new HandledModAction(-1, sub.id, "ModAction_ID2", new Timestamp(nowRounded + 1000));
		database.getHandledModActionMapping().save(mAction2);
		
		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(nowRounded), null, 10);
		MysqlTestUtils.assertListContents(fromDB, mAction, mAction2);
		
		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(mAction2.occurredAt.getTime()), null, 10);
		MysqlTestUtils.assertListContents(fromDB, mAction2);

		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(nowRounded - 1000), null, 10);
		MysqlTestUtils.assertListContents(fromDB, mAction, mAction2);	

		fromDB = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, new Timestamp(nowRounded - 1000), null, 1);
		MysqlTestUtils.assertListContents(fromDB, mAction);
	}
	
	@Test
	public void testFetchLatestJoined() {
		DBShortcuts db = new DBShortcuts(database);
		HandledModActionMapping map = database.getHandledModActionMapping();
		
		MonitoredSubreddit sub = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		
		List<HandledModActionJoinHistory> fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10); 
		assertTrue(fromDB.isEmpty());
		
		HandledModAction hma1 = db.hma(sub, db.now(-60000));
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		db.hma(sub, db.now(-55000));
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		BanHistory ban1 = db.bh(db.mod(), db.user1(), hma1, "msg", true);
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertEquals(1, fromDB.size());
		HandledModActionJoinHistory hmaJH = fromDB.get(0);
		assertEquals(hma1, hmaJH.handledModAction);
		assertEquals(ban1, hmaJH.banHistory);
		assertNull(hmaJH.unbanHistory);
		assertTrue(hmaJH.isBan());
		assertFalse(hmaJH.isUnban());
		
		db.hma(sub, db.now(-50000));
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertEquals(1, fromDB.size());
		assertEquals(ban1, hmaJH.banHistory);
		
		HandledModAction hma4 = db.hma(sub2, db.now(-65000));
		BanHistory ban2 = db.bh(db.mod(), db.user1(), hma4, "msg", true);
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertEquals(2, fromDB.size());
		hmaJH = fromDB.get(0);
		assertEquals(hma4, hmaJH.handledModAction);
		assertEquals(ban2, hmaJH.banHistory);
		assertNull(hmaJH.unbanHistory);
		assertTrue(hmaJH.isBan());
		hmaJH = fromDB.get(1);
		assertEquals(hma1, hmaJH.handledModAction);
		assertEquals(ban1, hmaJH.banHistory);
		assertNull(hmaJH.unbanHistory);
		
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 1);
		assertEquals(1, fromDB.size());
		hmaJH = fromDB.get(0);
		assertEquals(hma4, hmaJH.handledModAction);
		assertEquals(ban2, hmaJH.banHistory);
		assertNull(hmaJH.unbanHistory);
		
		fromDB = map.fetchLatestJoined(db.epoch, db.now(-65000), 1);
		assertTrue(fromDB.isEmpty());

		fromDB = map.fetchLatestJoined(db.epoch, db.now(-63000), 1);
		assertEquals(1, fromDB.size());
		assertEquals(hma4, fromDB.get(0).handledModAction);
		assertEquals(ban2, fromDB.get(0).banHistory);
		assertNull(fromDB.get(0).unbanHistory);
		
		fromDB = map.fetchLatestJoined(db.now(-65000), db.now(), 1);
		assertEquals(1, fromDB.size());
		assertEquals(hma4, fromDB.get(0).handledModAction);
		assertEquals(ban2, fromDB.get(0).banHistory);
		assertNull(fromDB.get(0).unbanHistory);
		
		fromDB = map.fetchLatestJoined(db.now(-65000), db.now(), 10);
		assertEquals(2, fromDB.size());
		assertEquals(hma4, fromDB.get(0).handledModAction);
		assertEquals(ban2, fromDB.get(0).banHistory);
		assertNull(fromDB.get(0).unbanHistory);
		assertEquals(hma1, fromDB.get(1).handledModAction);
		assertEquals(ban1, fromDB.get(1).banHistory);
		assertNull(fromDB.get(1).unbanHistory);
		
		fromDB = map.fetchLatestJoined(db.now(-60000), db.now(), 10);
		assertEquals(1, fromDB.size());
		assertEquals(hma1, fromDB.get(0).handledModAction);
		assertEquals(ban1, fromDB.get(0).banHistory);
		assertNull(fromDB.get(0).unbanHistory);
		
		HandledModAction hma5 = db.hma(sub2, db.now(-45000));
		UnbanHistory unban1 = db.ubh(db.mod2(), db.user1(), hma5);
		fromDB = map.fetchLatestJoined(db.epoch, db.now(), 10);
		assertEquals(3, fromDB.size());
		assertEquals(hma4, fromDB.get(0).handledModAction);
		assertEquals(ban2, fromDB.get(0).banHistory);
		assertNull(fromDB.get(0).unbanHistory);
		assertEquals(hma1, fromDB.get(1).handledModAction);
		assertEquals(ban1, fromDB.get(1).banHistory);
		assertNull(fromDB.get(1).unbanHistory);
		assertEquals(hma5, fromDB.get(2).handledModAction);
		assertNull(fromDB.get(2).banHistory);
		assertEquals(unban1, fromDB.get(2).unbanHistory);
	}
}
