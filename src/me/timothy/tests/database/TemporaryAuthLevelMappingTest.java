package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.TemporaryAuthLevel;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing an implementation of TemporaryAuthLevelMapping. Must be subclassed
 * to make the actual mapping database. The database must be cleared on setup and will be filled 
 * by the tests.
 * 
 * @author Timothy
 */
public class TemporaryAuthLevelMappingTest {

	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.TemporaryAuthLevelMapping TemporaryAuthLevelMapping} to test.
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
	 * Verifies saving works reasonably & doesn't screw up the timestamps
	 */
	@Test
	public void testSave() {
		final long now = System.currentTimeMillis();
		Timestamp nowTime = new Timestamp(now);
		Timestamp soonTime = new Timestamp(now + 5000);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		TemporaryAuthLevel level = new TemporaryAuthLevel(-1, john.id, 5, nowTime, soonTime);
		assertTrue(level.isValid());
		
		database.getTemporaryAuthLevelMapping().save(level);
		assertNotEquals(-1, level.id);
		assertTrue(level.id > 0);
		
		List<TemporaryAuthLevel> listFromDb = database.getTemporaryAuthLevelMapping().fetchAll();
		assertEquals(1, listFromDb.size());
		
		TemporaryAuthLevel fromDb = listFromDb.get(0);
		assertEquals(level, fromDb);
		assertEquals(level.id, fromDb.id);
		assertEquals(level.personID, fromDb.personID);
		assertEquals(level.authLevel, fromDb.authLevel);
		assertEquals(level.createdAt, fromDb.createdAt);
		assertEquals(level.expiresAt, fromDb.expiresAt);
		
		Person greg = database.getPersonMapping().fetchOrCreateByUsername("greg");
		
		TemporaryAuthLevel level2 = new TemporaryAuthLevel(-1, greg.id, 5, nowTime, soonTime);
		assertTrue(level2.isValid());
		
		database.getTemporaryAuthLevelMapping().save(level2);
		assertNotEquals(-1, level2.id);
		assertNotEquals(level.id, level2.id);
		assertTrue(level2.id > 0);
		
		listFromDb = database.getTemporaryAuthLevelMapping().fetchAll();
		MysqlTestUtils.assertListContents(listFromDb, level, level2);
		
		int oldId = level2.id;
		level2.authLevel = 10;
		database.getTemporaryAuthLevelMapping().save(level2);
		assertEquals(level2.id, oldId);
		assertEquals(10, level2.authLevel);

		listFromDb = database.getTemporaryAuthLevelMapping().fetchAll();
		MysqlTestUtils.assertListContents(listFromDb, level, level2);
		
		level2.expiresAt = nowTime;
		database.getTemporaryAuthLevelMapping().save(level2);
		
		listFromDb = database.getTemporaryAuthLevelMapping().fetchAll();
		MysqlTestUtils.assertListContents(listFromDb, level, level2);
	}
	
	@Test
	public void testFetchByPersonID() {
		final long now = System.currentTimeMillis();
		Timestamp nowTime = new Timestamp(now);
		Timestamp soonTime = new Timestamp(now + 5000);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person greg = database.getPersonMapping().fetchOrCreateByUsername("greg");
		
		TemporaryAuthLevel fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(john.id);
		assertNull(fromDb);
		
		
		TemporaryAuthLevel level = new TemporaryAuthLevel(-1, john.id, 5, nowTime, soonTime);
		database.getTemporaryAuthLevelMapping().save(level);
		
		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(john.id);
		assertEquals(level, fromDb);
		
		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(greg.id);
		assertNull(fromDb);
		
		TemporaryAuthLevel level2 = new TemporaryAuthLevel(-1, greg.id, 7, nowTime, soonTime);
		database.getTemporaryAuthLevelMapping().save(level2);
		
		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(john.id);
		assertEquals(level, fromDb);
		
		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(greg.id);
		assertEquals(level2, fromDb);
	}
	
	@Test
	public void testDelete() {
		final long now = System.currentTimeMillis();
		Timestamp nowTime = new Timestamp(now);
		Timestamp soonTime = new Timestamp(now + 5000);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person greg = database.getPersonMapping().fetchOrCreateByUsername("greg");

		TemporaryAuthLevel level = new TemporaryAuthLevel(-1, john.id, 5, nowTime, soonTime);
		database.getTemporaryAuthLevelMapping().save(level);

		TemporaryAuthLevel level2 = new TemporaryAuthLevel(-1, greg.id, 7, nowTime, soonTime);
		database.getTemporaryAuthLevelMapping().save(level2);
		
		database.getTemporaryAuthLevelMapping().deleteById(level.id);
		
		TemporaryAuthLevel fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(john.id);
		assertNull(fromDb);

		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(greg.id);
		assertEquals(level2, fromDb);
		
		database.getTemporaryAuthLevelMapping().deleteById(level2.id);
		
		fromDb = database.getTemporaryAuthLevelMapping().fetchByPersonID(greg.id);
		assertNull(fromDb);
	}
}
