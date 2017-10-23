package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;

/**
 * Describes tests focused on a UserMapping. The database must be cleared on each setup
 * and will be filled during testing.
 * 
 * @author Timothy
 */
public class PersonMappingTest {

	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.MonitoredSubredditMapping MonitoredSubredditMapping} to test.
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
	 * A person is created with a -1 id and null username. Attempting
	 * to save throws an exception.
	 */
	@Test
	public void testSaveMalformed() {
		Person person = new Person(-1, null, null, null, 0, null, null);
		
		try {
			database.getPersonMapping().save(person);
			assertTrue("expected an exception but succeeded when saving invalid person", false);
		}catch(Exception e) {
		}
		
		assertEquals(-1, person.id);
		assertEquals(0, database.getPersonMapping().fetchAll().size());
		
		person.createdAt = new Timestamp(System.currentTimeMillis());
		person.updatedAt = new Timestamp(System.currentTimeMillis());
		
		try {
			database.getPersonMapping().save(person);
			assertTrue("expected an exception but succeeded when saving invalid person", false);
		}catch(Exception e) {
		}
		
		assertEquals(-1, person.id);
		assertEquals(0, database.getPersonMapping().fetchAll().size());
		
		person.createdAt = null;
		person.username = "john";

		try {
			database.getPersonMapping().save(person);
			assertTrue("expected an exception but succeeded when saving invalid person", false);
		}catch(Exception e) {
		}
		
		assertEquals(-1, person.id);
		assertEquals(0, database.getPersonMapping().fetchAll().size());
	}
	
	/**
	 * A person who is sufficiently valid is saved to the database. The persons
	 * id is updated to a strictly positive value, and the person can be fetched
	 * using fetchAll.
	 */
	@Test
	public void testSaveFirst() {
		long now = System.currentTimeMillis();
		Person person = new Person(-1, "john", null, null, 0, new Timestamp(now), new Timestamp(now));

		assertEquals(0, database.getPersonMapping().fetchAll().size());
		
		database.getPersonMapping().save(person);
		assertTrue("expected positive id after saving!", person.id > 0);
		
		List<Person> fetched = database.getPersonMapping().fetchAll();
		assertEquals(1, fetched.size());
		assertEquals("john", fetched.get(0).username);
		assertEquals(person.id, fetched.get(0).id);
	}
	
	/**
	 * A person who is sufficiently valid is saved to the database. The persons
	 * email and updated at are modified, and the person is resaved. The person
	 * does not get assigned a second id and the modifications stay when fetched
	 * using fetchAll. 
	 */
	@Test
	public void testSaveModify() {
		long now = System.currentTimeMillis();
		Person person = new Person(-1, "john", null, null, 0, new Timestamp(now), new Timestamp(now));

		assertEquals(0, database.getPersonMapping().fetchAll().size());
		
		database.getPersonMapping().save(person);

		List<Person> fetched = database.getPersonMapping().fetchAll();
		assertEquals(1, fetched.size());
		Person old = fetched.get(0);
		assertNull(old.email);
		
		person.email = "myemail@fake-url.com";
		person.updatedAt = new Timestamp(System.currentTimeMillis());
		
		database.getPersonMapping().save(person);
		
		fetched = database.getPersonMapping().fetchAll();
		Person latest = fetched.get(0);
		
		assertEquals("myemail@fake-url.com", latest.email);
		assertNull(old.email);
		assertEquals(old.id, latest.id);
	}
	
	/**
	 * A username is fetched from the database, and null is returned. A person is 
	 * created with that username and saved. That username is fetched from the database,
	 * and it corresponds to the newly created person. 
	 */
	@Test
	public void testFetchByUsername() {
		assertNull(database.getPersonMapping().fetchByUsername("john"));

		long now = System.currentTimeMillis();
		Person person = new Person(-1, "john", null, null, 0, new Timestamp(now), new Timestamp(now));
		database.getPersonMapping().save(person);
		
		Person fetched = database.getPersonMapping().fetchByUsername("john");
		assertNotNull(fetched);
		assertEquals(person.id, fetched.id);
	}
	
	
	/**
	 * An email is fetched from the database, and null is returned. A person is 
	 * created with that email and saved. That email is fetched from the database,
	 * and it corresponds to the newly created person. The email is modified and the
	 * person is saved. The old email is fetched from the database, and the result is
	 * null. The new email is fetched from the database, and the result corresponds
	 * with the modified person 
	 */
	@Test
	public void testFetchByEmail() {
		assertNull(database.getPersonMapping().fetchByEmail("fake-email@emailstore.com"));
		
		long now = System.currentTimeMillis();
		Person original = new Person(-1, "john", null, "fake-email@emailstore.com", 0, new Timestamp(now), new Timestamp(now));
		database.getPersonMapping().save(original);
		
		Person fetched = database.getPersonMapping().fetchByEmail("fake-email@emailstore.com");
		assertEquals(original, fetched);
		
		original.email = "fake-email2@emailstore.com";
		database.getPersonMapping().save(original);

		assertNull(database.getPersonMapping().fetchByEmail("fake-email@emailstore.com"));
		fetched = database.getPersonMapping().fetchByEmail("fake-email2@emailstore.com");
		assertEquals(original, fetched);
	}
	
	/**
	 * The database is empty. A user john is fetched or created -> resulting in
	 * its creation. Another user paul is fetched or created -> resulting in its
	 * creation. User john is fetched or created -> its fetched. Ids are all reasonable
	 * (noncolliding and strictly positive)
	 */
	@Test
	public void testFetchOrCreateByUsername() {
		assertEquals(0, database.getPersonMapping().fetchAll().size());
		
		Person john1 = database.getPersonMapping().fetchOrCreateByUsername("john");
		assertNotNull(john1);
		assertTrue(john1.id > 0);
		
		Person paul1 = database.getPersonMapping().fetchOrCreateByUsername("paul");
		assertNotNull(paul1);
		assertTrue(paul1.id > 0);
		assertNotEquals(john1.id, paul1.id);
		
		Person john2 = database.getPersonMapping().fetchOrCreateByUsername("john");
		assertNotNull(john2);
		assertEquals(john1, john2);
	}
	
	/**
	 * fetch or create paul, then fetch by his id, then fetch by a
	 * different id
	 */
	@Test
	public void testFetchByID() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		Person fromDB = database.getPersonMapping().fetchByID(paul.id);
		assertEquals(paul, fromDB);
		
		fromDB = database.getPersonMapping().fetchByID(paul.id + 1);
		assertNull(fromDB);
	}
}
