package me.timothy.tests.database;

import static org.junit.Assert.*;
import static me.timothy.tests.database.mysql.MysqlTestUtils.*;

import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.models.MonitoredSubreddit;

/**
 * Describes a test focused on testing a MonitoredSubredditMapping. The 
 * database must be *completely* empty after each setup, and will be 
 * filled after each test.
 * 
 * @author Timothy
 */
public class MonitoredSubredditMappingTest {
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
	 * Tests that {@link me.timothy.bots.database.ObjectMapping#save(Object) saving}
	 * monitored subreddits will set their {@link MonitoredSubreddit#id id} to a strictly positive
	 * number, and that the monitored subreddit can be fetched again with
	 * {@link me.timothy.bots.database.ObjectMapping#fetchAll() fetchAll()}. 
	 */
	@Test
	public void testSave() {
		final String testSubredditString = "pYGug4KcPv7hLej2h7kK2qgm";
		MonitoredSubreddit mSub = new MonitoredSubreddit(-1, testSubredditString, true, false, true);
		
		database.getMonitoredSubredditMapping().save(mSub);
		
		assertTrue(mSub.id > 0);
		
		List<MonitoredSubreddit> fetched = database.getMonitoredSubredditMapping().fetchAll();
		
		assertEquals(1, fetched.size());
		assertEquals(mSub, fetched.get(0));
		assertEquals(mSub.id, fetched.get(0).id);
		assertEquals(mSub.subreddit, fetched.get(0).subreddit);
	}
	
	/**
	 * Tests the fetchConcatenated works for 0, 1, 2, or 3 fullnames in the database.
	 */
	@Test
	public void testFetchConcatenated() {
		final String testString1 = "Db281";
		final String testString2 = "9nR14";
		final String testString3 = "1v9UF";
		
		MonitoredSubredditMapping map = database.getMonitoredSubredditMapping();
		
		assertEquals("", map.fetchAllAndConcatenate());
		
		map.save(new MonitoredSubreddit(-1, testString1, true, false, false));
		
		assertEquals(testString1, map.fetchAllAndConcatenate());
		
		map.save(new MonitoredSubreddit(-1, testString2, false, true, true));
		
		String result = map.fetchAllAndConcatenate();
		String[] resultSplit = result.split("\\+");
		assertStringArrContents(resultSplit, testString1, testString2);
		
		map.save(new MonitoredSubreddit(-1, testString3, false, true, false));
		
		result = map.fetchAllAndConcatenate();
		resultSplit = result.split("\\+");
		assertStringArrContents(resultSplit, testString1, testString2, testString3);
	}

	@Test
	public void testFetchByID() {
		MonitoredSubreddit mSub = new MonitoredSubreddit(-1, "johnssub", true, false, true);
		database.getMonitoredSubredditMapping().save(mSub);
		
		MonitoredSubreddit fromDB = database.getMonitoredSubredditMapping().fetchByID(mSub.id);
		assertEquals(mSub, fromDB);
		
		fromDB = database.getMonitoredSubredditMapping().fetchByID(mSub.id + 1);
		assertNull(fromDB);
	}
}
