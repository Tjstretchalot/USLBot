package me.timothy.tests.database;

import static org.junit.Assert.*;
import static me.timothy.tests.database.mysql.MysqlTestUtils.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.USLAction;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

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
	
	@Test
	public void testFetchByName() {
		MonitoredSubreddit mSub = new MonitoredSubreddit(-1, "johnssub", true, false, true);
		database.getMonitoredSubredditMapping().save(mSub);
		
		MonitoredSubreddit fromDB = database.getMonitoredSubredditMapping().fetchByName("johnssub");
		assertEquals(mSub, fromDB);

		fromDB = database.getMonitoredSubredditMapping().fetchByName("JohnsSub");
		assertEquals(mSub, fromDB);

		fromDB = database.getMonitoredSubredditMapping().fetchByName("NotJohnsSub");
		assertNull(fromDB);
	}
	
	@Test
	public void testFetchByHashtag() {
		Person mod = database.getPersonMapping().fetchOrCreateByUsername("mod");
		
		Hashtag scammer = new Hashtag(-1, "#scammer", "scammer tag", mod.id, mod.id, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
		database.getHashtagMapping().save(scammer);
		
		Hashtag sketchy = new Hashtag(-1, "#sketchy", "sketchy tag", mod.id, mod.id, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
		database.getHashtagMapping().save(sketchy);
		
		List<Integer> fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		MonitoredSubreddit mSub1 = new MonitoredSubreddit(-1, "johnssub", true, false, false);
		database.getMonitoredSubredditMapping().save(mSub1);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		SubscribedHashtag sh = new SubscribedHashtag(-1, mSub1.id, sketchy.id, new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(sh);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id);
		
		MonitoredSubreddit mSub2 = new MonitoredSubreddit(-1, "paulssub", true, false, false);
		database.getMonitoredSubredditMapping().save(mSub2);

		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id);
		
		SubscribedHashtag sh2 = new SubscribedHashtag(-1, mSub2.id, sketchy.id, new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(sh2);

		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id, mSub2.id);
		
		SubscribedHashtag sh3 = new SubscribedHashtag(-1, mSub2.id, scammer.id, new Timestamp(System.currentTimeMillis()), null);
		database.getSubscribedHashtagMapping().save(sh3);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb, mSub2.id);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id, mSub2.id);
		
		sh3.deletedAt = new Timestamp(System.currentTimeMillis());
		database.getSubscribedHashtagMapping().save(sh3);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id, mSub2.id);
		
		SubscribedHashtag sh4 = new SubscribedHashtag(-1, mSub2.id, scammer.id, new Timestamp(System.currentTimeMillis() + 5000), null);
		database.getSubscribedHashtagMapping().save(sh4);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(scammer.id);
		MysqlTestUtils.assertListContents(fromDb, mSub2.id);
		
		fromDb = database.getMonitoredSubredditMapping().fetchIDsThatFollow(sketchy.id);
		MysqlTestUtils.assertListContents(fromDb, mSub1.id, mSub2.id);
	}
	
	@Test
	public void testFetchByActionTags() {
		MonitoredSubredditMapping map = database.getMonitoredSubredditMapping();
		DBShortcuts db = new DBShortcuts(database);
		
		MonitoredSubreddit sub = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		
		Person banned = db.user1();
		
		Hashtag scammer = db.scammerTag();
		Hashtag sketchy = db.sketchyTag();
		
		USLAction act = db.action(true, banned, new Hashtag[] { scammer }, null, null);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id));
		
		db.attach(sub2, scammer);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id), sub2.id);
		
		db.attach(sub, sketchy);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id), sub2.id);
		
		USLAction act2 = db.action(true, banned, new Hashtag[] { sketchy }, null, null);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id), sub2.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act2.id), sub.id);
		
		USLAction act3 = db.action(true, banned, new Hashtag[] { scammer, sketchy }, null, null);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id), sub2.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act2.id), sub.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act3.id), sub.id, sub2.id);
		
		db.attach(sub2, sketchy);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id), sub2.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act2.id), sub.id, sub2.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act3.id), sub.id, sub2.id);
		
		sub2.writeOnly = true;
		database.getMonitoredSubredditMapping().save(sub2);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act.id));
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act2.id), sub.id);
		MysqlTestUtils.assertListContents(map.fetchReadableIDsThatFollowActionsTags(act3.id), sub.id);
	}
}
