package me.timothy.tests.database;

import static org.junit.Assert.*;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing a SubredditModqueueProgressMapping in a MappingDatabase
 * 
 * @author Timothy
 */
public class SubredditModqueueProgressMappingTest {
	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.SubredditModqueueProgressMapping SubredditModqueueProgressMapping} to test.
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
	 * Create a monitored subreddit suba
	 * Create a modqueue progress subaprogress and save it
	 * Ensure that the saved thing was set to a positive id + updated at is pretty close to now
	 * Modify subaprogress and save it
	 * Ensure updated at was updated and the positive id didnt change
	 * Retrieve from fetchAll and ensure it exactly matches subaprogress
	 * @throws InterruptedException 
	 */
	@Test
	public void testSave() throws InterruptedException {
		MonitoredSubreddit subA = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subA);
		
		SubredditModqueueProgress subrModqueueProgr = new SubredditModqueueProgress(-1, subA.id, true, null, null, null);
		long now = System.currentTimeMillis();
		database.getSubredditModqueueProgressMapping().save(subrModqueueProgr);
		
		assertTrue(subrModqueueProgr.id > 0);
		assertNotNull(subrModqueueProgr.updatedAt);
		assertTrue(Math.abs(now - subrModqueueProgr.updatedAt.getTime()) < 10000);

		MysqlTestUtils.assertListContents(database.getSubredditModqueueProgressMapping().fetchAll(), subrModqueueProgr);
		
		int oldID = subrModqueueProgr.id;
		long oldTimestamp = subrModqueueProgr.updatedAt.getTime();
		Thread.sleep(2000);
		MonitoredSubreddit subB = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subB);
		
		subrModqueueProgr.monitoredSubredditID = subB.id;
		database.getSubredditModqueueProgressMapping().save(subrModqueueProgr);
		
		assertEquals(oldID, subrModqueueProgr.id);
		assertNotEquals(oldTimestamp, subrModqueueProgr.updatedAt.getTime());
		assertTrue(oldTimestamp < subrModqueueProgr.updatedAt.getTime());
		MysqlTestUtils.assertListContents(database.getSubredditModqueueProgressMapping().fetchAll(), subrModqueueProgr);
	}
	
	/**
	 * <ol>
	 * 	<li>Save subreddit subA</li>
	 * 	<li>Save subreddit subB</li>
	 * 	<li>getSubredditProgr subA -> null</li>
	 * 	<li>getSubredditProgr subB -> null</li>
	 * 	<li>Save subredditprogress subAProgr -> subA</li>
	 * 	<li>getSubredditProgr subA -> subAProgr</li>
	 * 	<li>getSubredditProgr subB -> null</li>
	 * 	<li>Save subredditprogress subBProgr -> sub</li>
	 * 	<li>getSubredditProgr subA -> subAProgr</li>
	 * 	<li>getSubredditProgr subB -> subBProgr</li>
	 * </ol>
	 */
	@Test
	public void fetchBySubreddit() {
		MonitoredSubreddit subA = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(subA);

		MonitoredSubreddit subB = new MonitoredSubreddit(-1, "paulsub", false, false, false);
		database.getMonitoredSubredditMapping().save(subB);
		
		SubredditModqueueProgress fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subA.id);
		assertNull(fromDB);
		
		fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subB.id);
		assertNull(fromDB);
		
		SubredditModqueueProgress subAProgr = new SubredditModqueueProgress(-1, subA.id, true, null, null, null);
		database.getSubredditModqueueProgressMapping().save(subAProgr);
		
		fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subA.id);
		assertNotNull(fromDB);
		assertEquals(subAProgr, fromDB);

		fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subB.id);
		assertNull(fromDB);

		SubredditModqueueProgress subBProgr = new SubredditModqueueProgress(-1, subB.id, true, null, null, null);
		database.getSubredditModqueueProgressMapping().save(subBProgr);
		
		assertNotEquals(subAProgr.id, subBProgr.id);
		
		fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subA.id);
		assertNotNull(fromDB);
		assertEquals(subAProgr, fromDB);

		fromDB = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subB.id);
		assertNotNull(fromDB);
		assertEquals(subBProgr, fromDB);
	}
}
