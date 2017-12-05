package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubredditPropagateStatus;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on a SubredditPropagateStatusMapping in a MappingDatabase
 * 
 * @author Timothy
 */
public class SubredditPropagateStatusMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	/**
	 * <ol>
	 * 	<li>create a monitored subreddit subA</li>
	 *  <li>create propagate status subAStatus -> subA</li>
	 *  <li>ensure result has positive id and non-null updated at and its updated at is pretty close to now</li>
	 *  <li>wait a bit</li>
	 *  <li>modify subAStatus and save</li>
	 *  <li>ensure id didnt change but updatedAt did</li>
	 *  <li>fetchall fetches subAStatus</li>
 	 * </ol>
	 * @throws InterruptedException 
	 */
	@Test
	public void testSave() throws InterruptedException {
		MonitoredSubreddit subA = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subA);
		
		SubredditPropagateStatus subAsubAStatus = new SubredditPropagateStatus(-1, subA.id, subA.id, null, null);
		database.getSubredditPropagateStatusMapping().save(subAsubAStatus);
		
		assertTrue(subAsubAStatus.id > 0);
		assertNotNull(subAsubAStatus.updatedAt);
		assertTrue(Math.abs(subAsubAStatus.updatedAt.getTime() - System.currentTimeMillis()) < 10000);
		
		int oldID = subAsubAStatus.id;
		long oldUpdatedAt = subAsubAStatus.updatedAt.getTime();
		
		Thread.sleep(2000);
		
		MonitoredSubreddit subB = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subB);
		
		assertNotEquals(subA.id, subB.id);
		
		subAsubAStatus.minorSubredditID = subB.id;
		database.getSubredditPropagateStatusMapping().save(subAsubAStatus);

		assertEquals(oldID, subAsubAStatus.id);
		assertNotEquals(oldUpdatedAt, subAsubAStatus.updatedAt.getTime());
		assertTrue(oldUpdatedAt < subAsubAStatus.updatedAt.getTime());
		MysqlTestUtils.assertListContents(database.getSubredditPropagateStatusMapping().fetchAll(), subAsubAStatus);
	}
	
	/**
	 * <ol>
	 * 	<li>create subreddit subA</li>
	 * 	<li>create subreddit subB</li>
	 * 	<li>get status subA -> null</li>
	 * 	<li>get status subB -> null</li>
	 *  <li>create status subAStatus</li>
	 *  <li>get status subA -> subAStatus</li>
	 *  <li>get status subB -> null</li>
	 *  <li>create status subBStatus</li>
	 *  <li>get status subA -> subAStatus</li>
	 *  <li>get status subB -> subBStatus</li>
	 * </ol>
	 */
	@Test
	public void testFetchForSubreddit() {
		MonitoredSubreddit subA = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subA);

		MonitoredSubreddit subB = new MonitoredSubreddit(-1, "paulsub", false, false, false);
		database.getMonitoredSubredditMapping().save(subB);
		
		SubredditPropagateStatus fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subA.id, subA.id);
		assertNull(fromDB);
		
		fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subB.id, subB.id);
		assertNull(fromDB);
		
		SubredditPropagateStatus subAStatus = new SubredditPropagateStatus(-1, subA.id, subA.id, null, null);
		database.getSubredditPropagateStatusMapping().save(subAStatus);
		
		fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subA.id, subA.id);
		assertNotNull(fromDB);
		assertEquals(subAStatus, fromDB);

		fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subA.id, subB.id);
		assertNull(fromDB);

		SubredditPropagateStatus subBStatus = new SubredditPropagateStatus(-1, subA.id, subB.id, null, null);
		database.getSubredditPropagateStatusMapping().save(subBStatus);
		
		assertNotEquals(subAStatus.id, subBStatus.id);
		
		fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subA.id, subA.id);
		assertNotNull(fromDB);
		assertEquals(subAStatus, fromDB);

		fromDB = database.getSubredditPropagateStatusMapping().fetchForSubredditPair(subA.id, subB.id);
		assertNotNull(fromDB);
		assertEquals(subBStatus, fromDB);
	}
}
