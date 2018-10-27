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
import me.timothy.bots.models.TemporaryAuthRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing an implementation of TemporaryAuthRequestMapping. Must be subclassed
 * to make the actual mapping database. The database must be cleared on setup and will be filled 
 * by the tests.
 * 
 * @author Timothy
 */
public class TemporaryAuthRequestMappingTest {
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
	 * Verifies that saving / core features of any mapping act correctly
	 */
	@Test
	public void testSave() {
		final long now = System.currentTimeMillis();
		Timestamp nowTime = new Timestamp(now);
		
		Person alex = database.getPersonMapping().fetchOrCreateByUsername("alex");
		
		TemporaryAuthRequest alexReq = new TemporaryAuthRequest(-1, alex.id, nowTime);
		database.getTemporaryAuthRequestMapping().save(alexReq);
		assertNotEquals(-1, alexReq.id);
		assertTrue(alexReq.id > 0);
		
		List<TemporaryAuthRequest> fromDb = database.getTemporaryAuthRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb, alexReq);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		TemporaryAuthRequest johnReq = new TemporaryAuthRequest(-1, john.id, nowTime);
		database.getTemporaryAuthRequestMapping().save(johnReq);
		assertNotEquals(-1, johnReq.id);
		assertNotEquals(alexReq.id, johnReq.id);
		assertTrue(johnReq.id > 0);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb, alexReq, johnReq);
	}
	
	@Test
	public void testDelete() {
		final long now = System.currentTimeMillis();
		Timestamp nowTime = new Timestamp(now);
		
		Person alex = database.getPersonMapping().fetchOrCreateByUsername("alex");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		TemporaryAuthRequest alexReq = new TemporaryAuthRequest(-1, alex.id, nowTime);
		database.getTemporaryAuthRequestMapping().save(alexReq);
		
		TemporaryAuthRequest johnReq = new TemporaryAuthRequest(-1, john.id, nowTime);
		database.getTemporaryAuthRequestMapping().save(johnReq);

		List<TemporaryAuthRequest> fromDb = database.getTemporaryAuthRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb, alexReq, johnReq);
		
		database.getTemporaryAuthRequestMapping().deleteById(alexReq.id);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb, johnReq);
		
		database.getTemporaryAuthRequestMapping().deleteById(johnReq.id);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb);
	}
	
	@Test
	public void testFetchOldest() {
		final long oldest = System.currentTimeMillis() - 15000;
		final long older = oldest + 5000;
		final long old = older + 5000;
		
		Person alex = database.getPersonMapping().fetchOrCreateByUsername("alex");
		Person greg = database.getPersonMapping().fetchOrCreateByUsername("greg");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		TemporaryAuthRequest fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertNull(fromDb);
		
		TemporaryAuthRequest alexReq = new TemporaryAuthRequest(-1, alex.id, new Timestamp(older));
		database.getTemporaryAuthRequestMapping().save(alexReq);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertEquals(alexReq, fromDb);
		
		TemporaryAuthRequest gregReq = new TemporaryAuthRequest(-1, greg.id, new Timestamp(oldest));
		database.getTemporaryAuthRequestMapping().save(gregReq);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertEquals(gregReq, fromDb);
		
		database.getTemporaryAuthRequestMapping().deleteById(greg.id);
		
		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertEquals(alexReq, fromDb);
		
		TemporaryAuthRequest johnReq = new TemporaryAuthRequest(-1, john.id, new Timestamp(old));
		database.getTemporaryAuthRequestMapping().save(johnReq);

		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertEquals(alexReq, fromDb);
		
		database.getTemporaryAuthRequestMapping().deleteById(alexReq.id);

		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertEquals(johnReq, fromDb);
		
		database.getTemporaryAuthRequestMapping().deleteById(johnReq.id);

		fromDb = database.getTemporaryAuthRequestMapping().fetchOldestRequest();
		assertNull(fromDb);
	}
}
