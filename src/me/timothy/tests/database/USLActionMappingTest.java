package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.USLActionMapping;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class USLActionMappingTest {

	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testSave() {
		final long now = System.currentTimeMillis();
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		USLAction action = new USLAction(-1, true, true, paul.id, new Timestamp(now));
		database.getUSLActionMapping().save(action);
		
		assertTrue(action.id > 0);
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action);
		
		USLAction action2 = new USLAction(-1, true, true, john.id, new Timestamp(now));
		database.getUSLActionMapping().save(action2);
		
		assertTrue(action2.id > 0);
		assertNotEquals(action.id, action2.id);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action, action2);
		
		action.isLatest = false;
		database.getUSLActionMapping().save(action);
		assertFalse(action.isLatest);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action, action2);
		
		USLAction action3 = new USLAction(-1, false, true, paul.id, new Timestamp(now));
		database.getUSLActionMapping().save(action3);
		
		assertTrue(action3.id > 0);
		assertNotEquals(action.id, action3.id);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action, action2, action3);
		
		USLAction action4 = new USLAction(-1, true, true, eric.id, new Timestamp(now));
		database.getUSLActionMapping().save(action4);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action, action2, action3, action4);
	}
	
	@Test
	public void testCreate() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		USLAction action = database.getUSLActionMapping().create(true, john.id, new Timestamp(System.currentTimeMillis()));
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action);
		
		assertTrue(action.id > 0);
		assertTrue(action.isBan);
		assertTrue(action.isLatest);
		assertEquals(john.id, action.personID);
		assertNotNull(action.createdAt);
		
		USLAction action2 = database.getUSLActionMapping().create(false, john.id, new Timestamp(System.currentTimeMillis()));
		
		List<USLAction> fromDb = database.getUSLActionMapping().fetchAll();
		assertFalse(fromDb.contains(action));
		assertTrue(fromDb.stream().anyMatch((a) -> a.id == action.id));
		
		action.isLatest = false;
		MysqlTestUtils.assertListContents(fromDb, action, action2);
		
		USLAction action3 = database.getUSLActionMapping().create(true, eric.id, new Timestamp(System.currentTimeMillis()));
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll(), action, action2, action3);
	}
	
	@Test
	public void fetchAfter() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		USLActionMapping map = database.getUSLActionMapping();
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true));
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false));
		
		USLAction johnBanned = map.create(true, john.id, new Timestamp(System.currentTimeMillis()));
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, false), johnBanned);
		
		USLAction paulBanned = map.create(true, paul.id, new Timestamp(System.currentTimeMillis()));
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true), johnBanned, paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false), johnBanned, paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, false), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, true), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, false), paulBanned);
		
		USLAction johnUnbanned = map.create(false, john.id, new Timestamp(System.currentTimeMillis()));
		johnBanned.isLatest = false;
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true), johnBanned, paulBanned, johnUnbanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false), paulBanned, johnUnbanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, false), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, true), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, false), paulBanned);
		
		USLAction ericBanned = map.create(true, eric.id, new Timestamp(System.currentTimeMillis()));
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true), johnBanned, paulBanned, johnUnbanned, ericBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false), paulBanned, johnUnbanned, ericBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, false), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, true), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, false), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 5, true), paulBanned, johnUnbanned, ericBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 5, false), paulBanned, johnUnbanned, ericBanned);
		
		USLAction johnRebanned = map.create(true, john.id, new Timestamp(System.currentTimeMillis()));
		johnUnbanned.isLatest = false;
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, true), johnBanned, paulBanned, johnUnbanned, ericBanned, johnRebanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 5, false), paulBanned, ericBanned, johnRebanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, true), johnBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(0, 1, false), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, true), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 1, false), paulBanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 5, true), paulBanned, johnUnbanned, ericBanned, johnRebanned);
		MysqlTestUtils.assertListContents(map.getActionsAfter(johnBanned.id + 1, 5, false), paulBanned, ericBanned, johnRebanned);
	}
	
	@Test
	public void fetchLatest() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		USLActionMapping map = database.getUSLActionMapping();
		assertNull(map.fetchLatest(john.id));
		
		USLAction johnBanned = map.create(true, john.id, new Timestamp(System.currentTimeMillis()));
		assertEquals(johnBanned, map.fetchLatest(john.id));
		assertNull(map.fetchLatest(paul.id));
		
		USLAction johnMoreBanned = map.create(true, john.id, new Timestamp(System.currentTimeMillis()));
		assertEquals(johnMoreBanned, map.fetchLatest(john.id));
		assertNull(map.fetchLatest(paul.id));
		
		USLAction paulBanned = map.create(true, paul.id, new Timestamp(System.currentTimeMillis()));
		assertEquals(johnMoreBanned, map.fetchLatest(john.id));
		assertEquals(paulBanned, map.fetchLatest(paul.id));

		USLAction paulUnbanned = map.create(false, paul.id, new Timestamp(System.currentTimeMillis()));
		assertEquals(johnMoreBanned, map.fetchLatest(john.id));
		assertEquals(paulUnbanned, map.fetchLatest(paul.id));
	}
}
