package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class UnbanRequestMappingTest {
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
	
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		List<UnbanRequest> fromDB = database.getUnbanRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB);
		
		long now = System.currentTimeMillis();
		UnbanRequest unbanReq = new UnbanRequest(-1, john.id, paul.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(unbanReq);
		
		assertTrue(unbanReq.id > 0);
		assertEquals(0, unbanReq.createdAt.getNanos());
		
		fromDB = database.getUnbanRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, unbanReq);
		
		int oldID = unbanReq.id;
		unbanReq.handledAt = new Timestamp(now + 5000);
		database.getUnbanRequestMapping().save(unbanReq);
		
		assertEquals(oldID, unbanReq.id);
		assertEquals(0, unbanReq.handledAt.getNanos());
		
		fromDB = database.getUnbanRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, unbanReq);
		
		unbanReq.bannedPersonID = -1;
		try {
			database.getUnbanRequestMapping().save(unbanReq);
			assertTrue("this shouldn't have worked", false);
		}catch(IllegalArgumentException e) {
			assertTrue(true);
		}catch(RuntimeException e) {
			e.printStackTrace();
			assertTrue("wrong exception type", false);
		}
	}
	
	@Test
	public void testFetchUnhandled() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		List<UnbanRequest> fromDB = database.getUnbanRequestMapping().fetchUnhandled(10);
		MysqlTestUtils.assertListContents(fromDB);
		
		long now = System.currentTimeMillis();
		UnbanRequest unbanReq = new UnbanRequest(-1, paul.id, eric.id, new Timestamp(now), null, false);
		database.getUnbanRequestMapping().save(unbanReq);
		
		fromDB = database.getUnbanRequestMapping().fetchUnhandled(10);
		MysqlTestUtils.assertListContents(fromDB, unbanReq);
		
		UnbanRequest unbanReq2 = new UnbanRequest(-1, paul.id, john.id, new Timestamp(now), null, true);
		database.getUnbanRequestMapping().save(unbanReq2);
		
		fromDB = database.getUnbanRequestMapping().fetchUnhandled(10);
		MysqlTestUtils.assertListContents(fromDB, unbanReq, unbanReq2);
		
		fromDB = database.getUnbanRequestMapping().fetchUnhandled(1);
		assertEquals(1, fromDB.size());
		if(!fromDB.contains(unbanReq)) 
			assertTrue(fromDB.contains(unbanReq2));
		
		unbanReq.handledAt = new Timestamp(now + 1000);
		database.getUnbanRequestMapping().save(unbanReq);
		
		fromDB = database.getUnbanRequestMapping().fetchUnhandled(10);
		MysqlTestUtils.assertListContents(fromDB, unbanReq2);
		
		unbanReq2.handledAt = new Timestamp(now + 1000);
		database.getUnbanRequestMapping().save(unbanReq2);
		
		fromDB = database.getUnbanRequestMapping().fetchUnhandled(10);
		MysqlTestUtils.assertListContents(fromDB);
	}
	
	@Test
	public void testFetchLatestValidByBannedPerson() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		UnbanRequest fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertNull(fromDB);
		
		long now = System.currentTimeMillis();
		long oneDayInMS = 1000 * 60 * 60 * 24;
		long fiveDaysAgo = now - (oneDayInMS * 5);
		long fourDaysAgo = now - (oneDayInMS * 4);
		long threeDaysAgo = now - (oneDayInMS * 3);
		long twoDaysAgo = now - (oneDayInMS * 2);
		
		UnbanRequest req1 = new UnbanRequest(-1, john.id, paul.id, new Timestamp(fiveDaysAgo), null, false);
		database.getUnbanRequestMapping().save(req1);
		
		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req1, fromDB);
		
		UnbanRequest req2 = new UnbanRequest(-1, paul.id, john.id, new Timestamp(fourDaysAgo), null, false);
		database.getUnbanRequestMapping().save(req2);
		
		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req1, fromDB);
		
		UnbanRequest req3 = new UnbanRequest(-1, john.id, paul.id, new Timestamp(threeDaysAgo), null, false);
		database.getUnbanRequestMapping().save(req3);

		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req3, fromDB);
		
		req3.handledAt = new Timestamp(twoDaysAgo);
		database.getUnbanRequestMapping().save(req3);

		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req3, fromDB);
		
		req3.invalid = true;
		database.getUnbanRequestMapping().save(req3);

		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req1, fromDB);
		
		req3.handledAt = null;
		database.getUnbanRequestMapping().save(req3);

		fromDB = database.getUnbanRequestMapping().fetchLatestValidByBannedPerson(paul.id);
		assertEquals(req3, fromDB);
	}
	
	@Test
	public void testFetchLatest() {
		DBShortcuts db = new DBShortcuts(database);
		UnbanRequestMapping map = database.getUnbanRequestMapping();
		
		List<UnbanRequest> fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		UnbanRequest req1 = db.unbanRequest(db.user1(), db.now(-60000));
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		db.handle(req1, db.now(-55000), true);
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		UnbanRequest req2 = db.unbanRequest(db.user1(), db.now(-50000));
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		
		db.handle(req2, db.now(-45000), false);
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		assertEquals(1, fromDB.size());
		assertEquals(req2, fromDB.get(0));
		
		fromDB = map.fetchLatestValid(db.now(-50000), db.now(), 10);
		assertEquals(1, fromDB.size());
		assertEquals(req2, fromDB.get(0));
		
		fromDB = map.fetchLatestValid(db.now(-5000), db.now(), 10);
		assertTrue(fromDB.isEmpty());
		
		UnbanRequest req3 = db.unbanRequest(db.user1(), db.now(-50000));
		db.handle(req3, db.now(-48000), false);
		
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 10);
		assertEquals(2, fromDB.size());
		assertEquals(req3, fromDB.get(0));
		assertEquals(req2, fromDB.get(1));
		
		fromDB = map.fetchLatestValid(db.epoch, db.now(), 1);
		assertEquals(1, fromDB.size());
		assertEquals(req3, fromDB.get(0));

		
		fromDB = map.fetchLatestValid(db.now(-46000), db.now(), 10);
		assertEquals(1, fromDB.size());
		assertEquals(req2, fromDB.get(0));
	}
	
}
