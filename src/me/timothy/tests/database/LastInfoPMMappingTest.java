package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.LastInfoPM;
import me.timothy.bots.models.Person;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests related to a LastInfoPMMapping inside a mapping database. Must be subclassed
 * 
 * @author Timothy
 */
public class LastInfoPMMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		LastInfoPM pm = new LastInfoPM(-1, john.id, paul.id, new Timestamp(System.currentTimeMillis()));
		database.getLastInfoPMMapping().save(pm);
		
		assertTrue(pm.id > 0);
		assertEquals(0, pm.createdAt.getNanos());
		
		List<LastInfoPM> fromDB = database.getLastInfoPMMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, pm);
		
		int oldID = pm.id;
		pm.bannedPersonID = eric.id;
		database.getLastInfoPMMapping().save(pm);
		assertEquals(oldID, pm.id);
		
		fromDB = database.getLastInfoPMMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, pm);
	}
	
	@Test
	public void testFetchByPersons() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		LastInfoPM pm = new LastInfoPM(-1, john.id, paul.id, new Timestamp(System.currentTimeMillis()));
		database.getLastInfoPMMapping().save(pm);
		
		assertTrue(pm.id > 0);
		assertEquals(0, pm.createdAt.getNanos());
		
		LastInfoPM fromDB = database.getLastInfoPMMapping().fetchByModAndUser(john.id, paul.id);
		assertEquals(fromDB, pm);
		
		int oldID = pm.id;
		pm.bannedPersonID = eric.id;
		database.getLastInfoPMMapping().save(pm);
		assertEquals(oldID, pm.id);

		fromDB = database.getLastInfoPMMapping().fetchByModAndUser(john.id, paul.id);
		assertNull(fromDB);
		
		fromDB = database.getLastInfoPMMapping().fetchByModAndUser(john.id, eric.id);
		assertEquals(pm, fromDB);

		LastInfoPM pm2 = new LastInfoPM(-1, john.id, eric.id, new Timestamp(System.currentTimeMillis() + 10000));
		database.getLastInfoPMMapping().save(pm2);

		fromDB = database.getLastInfoPMMapping().fetchByModAndUser(john.id, eric.id);
		assertEquals(pm2, fromDB);
	}
}
