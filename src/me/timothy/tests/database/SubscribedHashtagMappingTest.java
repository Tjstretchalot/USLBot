package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing an implementation of SubscribedHastagMapping. Must be subclassed
 * to make the actual mapping database. The database must be cleared on setup and will be filled 
 * by the tests.
 * 
 * @author Timothy
 */
public class SubscribedHashtagMappingTest {

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
	 * subscribed hashtags will set their {@link SubscribedHastag#id id} to a strictly positive
	 * number, and that the subscribed hashtag can be fetched again with
	 * {@link me.timothy.bots.database.ObjectMapping#fetchAll() fetchAll()}.
	 * 
	 * Also verifies that timestamps are normalized (nanoseconds cannot be saved)
	 */
	@Test
	public void testSave() {
		final String testHashtag = "IPHku8PDI2oaeQh";
		final String testSubredd = "7Q73fVc8uaFkaa8";
		final long now = System.currentTimeMillis();
		
		Person tj = database.getPersonMapping().fetchOrCreateByUsername("tjstretchalot");
		
		MonitoredSubreddit monSub = new MonitoredSubreddit(-1, testSubredd, true, false, false);
		database.getMonitoredSubredditMapping().save(monSub);
		
		Hashtag testHashtagInst = new Hashtag(-1, testHashtag, "none", tj.id, tj.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(testHashtagInst);
		
		SubscribedHashtag hashtag = new SubscribedHashtag(-1, monSub.id, testHashtagInst.id, new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		assertTrue(hashtag.id > 0);
		assertEquals(0, hashtag.createdAt.getNanos());
		
		List<SubscribedHashtag> fromDb = database.getSubscribedHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDb, hashtag);
	}
	
	/**
	 * Test saving subreddit A, then hashtag 1 for subreddit A, then hashtag 2
	 * for subreddit A, then deleting hashtag 1, then saving subreddit B, then 
	 * hashtag 3 for subreddit B.
	 * 
	 * At each step, verify {@link me.timothy.bots.database.SubscribedHashtagMapping#fetchForSubreddit(int, boolean)}
	 * returns the correct results.
	 */
	@Test
	public void testFetchBySubreddit() {
		final String testHashtag = "05fBOeSSWDuQitA";
		final String testHashtag2 = "63P1qQsCDz7R21U";
		final String testHashtag3 = "ctY2c5ZEwx0ODgO";
		final String testSubredd = "o002aQHGNhKXog5";
		final String testSubredd2 = "i5YD2Hw39SrA6g4";
		final long now = System.currentTimeMillis();
		
		Person fox = database.getPersonMapping().fetchOrCreateByUsername("FoxK56");
		
		
		Hashtag testHashtagInst = new Hashtag(-1, testHashtag, "none1", fox.id, fox.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(testHashtagInst);
		Hashtag testHashtag2Inst = new Hashtag(-1, testHashtag2, "none2", fox.id, fox.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(testHashtag2Inst);
		Hashtag testHashtag3Inst = new Hashtag(-1, testHashtag3, "none3", fox.id, fox.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(testHashtag3Inst);
		
		MonitoredSubreddit monSub = new MonitoredSubreddit(-1, testSubredd, true, false, false);
		database.getMonitoredSubredditMapping().save(monSub);

		SubscribedHashtag hashtag = new SubscribedHashtag(-1, monSub.id, testHashtagInst.id, new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		List<SubscribedHashtag> fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, false);
		MysqlTestUtils.assertListContents(fromDb, hashtag);
		
		SubscribedHashtag hashtag2 = new SubscribedHashtag(-1, monSub.id, testHashtag2Inst.id, new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag2);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, false);
		MysqlTestUtils.assertListContents(fromDb, hashtag, hashtag2);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, true);
		MysqlTestUtils.assertListContents(fromDb, hashtag, hashtag2);
		
		hashtag.deletedAt = new Timestamp(now);
		database.getSubscribedHashtagMapping().save(hashtag);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, false);
		MysqlTestUtils.assertListContents(fromDb, hashtag2);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, true);
		MysqlTestUtils.assertListContents(fromDb, hashtag, hashtag2);
		
		MonitoredSubreddit monSub2 = new MonitoredSubreddit(-1, testSubredd2, false, true, false);
		database.getMonitoredSubredditMapping().save(monSub2);
		
		SubscribedHashtag hashtag3 = new SubscribedHashtag(-1, monSub2.id, testHashtag3Inst.id, new Timestamp(now), null);
		database.getSubscribedHashtagMapping().save(hashtag3);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub.id, false);
		MysqlTestUtils.assertListContents(fromDb, hashtag2);
		
		fromDb = database.getSubscribedHashtagMapping().fetchForSubreddit(monSub2.id, false);
		MysqlTestUtils.assertListContents(fromDb, hashtag3);
	}
}
