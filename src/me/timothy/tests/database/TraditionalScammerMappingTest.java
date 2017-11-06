package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.TraditionalScammerMapping;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class TraditionalScammerMappingTest {
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
		Person person = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		TraditionalScammer paulScammer = new TraditionalScammer(-1, person.id, "test", "#scammer", new Timestamp(System.currentTimeMillis()));
		database.getTraditionalScammerMapping().save(paulScammer);
		
		assertTrue(paulScammer.id > 0);
		assertEquals(0, paulScammer.createdAt.getNanos());
		
		List<TraditionalScammer> fromDB = database.getTraditionalScammerMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, paulScammer);
		
		int oldID = paulScammer.id;
		paulScammer.reason = "testing";
		database.getTraditionalScammerMapping().save(paulScammer);
		assertEquals(oldID, paulScammer.id);
		
		fromDB = database.getTraditionalScammerMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, paulScammer);
		
		paulScammer.reason = null;
		try {
			database.getTraditionalScammerMapping().save(paulScammer);
			assertTrue("this shouldnt have worked", false);
		}catch(IllegalArgumentException e)
		{
			assertTrue(true); // so the assert counter is correct
		}catch(RuntimeException e) {
			e.printStackTrace();
			assertTrue("wrong type of exception", false);
		}
	}
	
	@Test
	public void testFetchAndDeleteByPersonID() {
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		TraditionalScammer fromDB = database.getTraditionalScammerMapping().fetchByPersonID(eric.id);
		assertNull(fromDB);
		
		TraditionalScammer ericScammer = new TraditionalScammer(-1, eric.id, "test", "#scammer", new Timestamp(System.currentTimeMillis()));
		database.getTraditionalScammerMapping().save(ericScammer);
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(eric.id);
		assertEquals(ericScammer, fromDB);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(john.id);
		assertNull(fromDB);
		
		TraditionalScammer johnScammer = new TraditionalScammer(-1, john.id, "grandfathered", "#scammer", new Timestamp(System.currentTimeMillis()));
		database.getTraditionalScammerMapping().save(johnScammer);
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(eric.id);
		assertEquals(ericScammer, fromDB);
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(john.id);
		assertEquals(johnScammer, fromDB);
		
		database.getTraditionalScammerMapping().deleteByPersonID(eric.id);
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(eric.id);
		assertNull(fromDB);
		
		fromDB = database.getTraditionalScammerMapping().fetchByPersonID(john.id);
		assertEquals(johnScammer, fromDB);
	}
	
	@Test
	public void testFetchAfterID() {
		Person ella = database.getPersonMapping().fetchOrCreateByUsername("ella");
		Person emma = database.getPersonMapping().fetchOrCreateByUsername("emma");
		Person leia = database.getPersonMapping().fetchOrCreateByUsername("leia");
		
		TraditionalScammerMapping map = database.getTraditionalScammerMapping();
		List<TraditionalScammer> fromDB = map.fetchEntriesAfterID(-1, 10);
		MysqlTestUtils.assertListContents(fromDB);
		
		TraditionalScammer ellaScammer = new TraditionalScammer(-1, ella.id, "asdf", "onions", new Timestamp(System.currentTimeMillis()));
		map.save(ellaScammer);
		
		fromDB = map.fetchEntriesAfterID(-1, 10);
		MysqlTestUtils.assertListContents(fromDB, ellaScammer);
		
		fromDB = map.fetchEntriesAfterID(ellaScammer.id, 10);
		MysqlTestUtils.assertListContents(fromDB);
		
		TraditionalScammer emmaScammer = new TraditionalScammer(-1, emma.id, "asdf", "not onions", new Timestamp(System.currentTimeMillis()));
		map.save(emmaScammer);
		
		fromDB = map.fetchEntriesAfterID(-1, 10);
		assertEquals(fromDB.get(0), ellaScammer); // order matters
		assertEquals(fromDB.get(1), emmaScammer);
		assertEquals(2, fromDB.size());
		
		fromDB = map.fetchEntriesAfterID(ellaScammer.id, 10);
		MysqlTestUtils.assertListContents(fromDB, emmaScammer);
		
		fromDB = map.fetchEntriesAfterID(emmaScammer.id, 10);
		MysqlTestUtils.assertListContents(fromDB);
		
		fromDB = map.fetchEntriesAfterID(-1, 1);
		MysqlTestUtils.assertListContents(fromDB, ellaScammer);
		
		TraditionalScammer leiaScammer = new TraditionalScammer(-1, leia.id, "asf", "oranges", new Timestamp(System.currentTimeMillis()));
		map.save(leiaScammer);

		fromDB = map.fetchEntriesAfterID(ellaScammer.id, 10);
		assertEquals(fromDB.get(0), emmaScammer); 
		assertEquals(fromDB.get(1), leiaScammer);
		assertEquals(2, fromDB.size());
		
		fromDB = map.fetchEntriesAfterID(emmaScammer.id, 10);
		MysqlTestUtils.assertListContents(fromDB, leiaScammer);
		
		fromDB = map.fetchEntriesAfterID(ellaScammer.id, 1);
		MysqlTestUtils.assertListContents(fromDB, emmaScammer);
	}
}
