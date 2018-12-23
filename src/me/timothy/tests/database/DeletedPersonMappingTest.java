package me.timothy.tests.database;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.DeletedPersonMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;
import me.timothy.tests.DBShortcuts;

public class DeletedPersonMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		DBShortcuts db = new DBShortcuts(database);
		DeletedPersonMapping map = database.getDeletedPersonMapping();
		
		db.person("person1");
		db.person("person2");
		Person pers3 = db.person("person3");
		db.person("person4");
		db.person("person5");
		db.person("person6");
		Person pers7 = db.person("person7");
		Person pers8 = db.person("person8");
		
		assertFalse(map.contains(pers3.id));
		assertFalse(map.contains(pers7.id));
		assertFalse(map.contains(pers8.id));
		
		map.addIfNotExists(pers7.id);
		assertFalse(map.contains(pers3.id));
		assertTrue(map.contains(pers7.id));
		assertFalse(map.contains(pers8.id));
		
		map.addIfNotExists(pers7.id);
		assertFalse(map.contains(pers3.id));
		assertTrue(map.contains(pers7.id));
		assertFalse(map.contains(pers8.id));
		
		map.addIfNotExists(pers3.id);
		assertTrue(map.contains(pers3.id));
		assertTrue(map.contains(pers7.id));
		assertFalse(map.contains(pers8.id));
		
		map.addIfNotExists(pers7.id);
		assertTrue(map.contains(pers3.id));
		assertTrue(map.contains(pers7.id));
		assertFalse(map.contains(pers8.id));
		
		map.addIfNotExists(pers8.id);
		assertTrue(map.contains(pers3.id));
		assertTrue(map.contains(pers7.id));
		assertTrue(map.contains(pers8.id));
	}
}
