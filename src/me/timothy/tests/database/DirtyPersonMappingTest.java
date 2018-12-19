package me.timothy.tests.database;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.DirtyPerson;
import me.timothy.bots.models.Person;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class DirtyPersonMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		DBShortcuts db = new DBShortcuts(database);
		DirtyPersonMapping map = database.getDirtyPersonMapping();
		
		Person p1 = db.mod();
		assertFalse(map.contains(p1.id));
		map.save(new DirtyPerson(p1.id));
		assertTrue(map.contains(p1.id));
		MysqlTestUtils.assertListContentsPreds(map.fetch(10), (a) -> a.personID == p1.id);
		map.delete(p1.id);
		assertFalse(map.contains(p1.id));
		
		Person p2 = db.mod2();
		assertFalse(map.contains(p1.id));
		assertFalse(map.contains(p2.id));
		map.save(new DirtyPerson(p2.id));
		assertTrue(map.contains(p2.id));
		assertFalse(map.contains(p1.id));
		MysqlTestUtils.assertListContentsPreds(map.fetch(10), (a) -> a.personID == p2.id);
		map.save(new DirtyPerson(p1.id));
		MysqlTestUtils.assertListContentsPreds(map.fetch(10), 
				(a) -> a.personID == p2.id,
				(a) -> a.personID == p1.id);
		MysqlTestUtils.assertListContentsPreds(map.fetch(1), 
				(a) -> a.personID == p1.id || a.personID == p2.id);
		assertTrue(map.contains(p1.id));
		assertTrue(map.contains(p2.id));
		map.delete(p1.id);
		assertTrue(map.contains(p2.id));
		assertFalse(map.contains(p1.id));
		MysqlTestUtils.assertListContentsPreds(map.fetch(10), (a) -> a.personID == p2.id);
		MysqlTestUtils.assertListContentsPreds(map.fetch(1), (a) -> a.personID == p2.id);
		map.delete(p2.id);
		MysqlTestUtils.assertListContentsPreds(map.fetch(10));
		assertFalse(map.contains(p1.id));
		assertFalse(map.contains(p2.id));
	}

}
