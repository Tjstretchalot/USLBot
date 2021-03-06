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
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing an implementation of BanHistory. Must be subclassed
 * to make the actual mapping database. The database must be cleared on setup
 * and will be filled by the tests.
 * 
 * @author Timothy
 */
public class BanHistoryMappingTest {
	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.FullnameMapping FullnameMapping} to test.
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
	 * Create persons john and paul. Create monitored subreddit johnssub. Add ban history
	 * for john banning paul on johnssub. The banhistorys id is set to a strictly positive.
	 * value. Also verify it can be retrieved using fetchAll. Modifying and saving doesn't 
	 * effect id.
	 */
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		
		long now = System.currentTimeMillis();
		
		HandledModAction hma = new HandledModAction(-1, johnssub.id, "ModAction_ID", new Timestamp(now));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory banHist = new BanHistory(-1, john.id, paul.id, hma.id, "i hate you", "permanent");
		database.getBanHistoryMapping().save(banHist);
		
		assertTrue(banHist.id > 0);
		
		List<BanHistory> fetched = database.getBanHistoryMapping().fetchAll();
		assertEquals(1, fetched.size());
		assertEquals(banHist, fetched.get(0));
		
		banHist.banDescription = "sorry";
		database.getBanHistoryMapping().save(banHist);
		assertEquals(fetched.get(0).id, banHist.id);
		assertNotEquals(fetched.get(0), banHist);
		
		fetched = database.getBanHistoryMapping().fetchAll();
		assertEquals(1, fetched.size());
		assertEquals(banHist, fetched.get(0));
	}
	
	/**
	 * Fetch by id 5 should give null. Save a new ban history, fetch by its
	 * id, get not null.
	 */
	@Test
	public void testFetchByID() {
		BanHistory fromDB = database.getBanHistoryMapping().fetchByID(5);
		assertNull(fromDB);

		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		long now = System.currentTimeMillis();

		HandledModAction hma = new HandledModAction(-1, johnssub.id, "ModAction_ID", new Timestamp(now));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, john.id, paul.id, hma.id, "i hate you", "permanent");
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchByID(ban.id);
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
	}
	
	@Test
	public void testFetchByHandledModActionID() {
		BanHistory fromDB = database.getBanHistoryMapping().fetchByHandledModActionID(5);
		assertNull(fromDB);

		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		long now = System.currentTimeMillis();

		HandledModAction hma = new HandledModAction(-1, johnssub.id, "ModAction_ID", new Timestamp(now));
		database.getHandledModActionMapping().save(hma);
		
		fromDB = database.getBanHistoryMapping().fetchByHandledModActionID(hma.id);
		assertNull(fromDB);
		
		BanHistory ban = new BanHistory(-1, john.id, paul.id, hma.id, "i hate you", "permanent");
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchByHandledModActionID(hma.id);
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
	}
	
	@Test
	public void testFetchByBannedAndSubreddit() {
		BanHistory fromDB = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(1, 1);
		assertNull(fromDB);
		
		List<BanHistory> lFromDB = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(1, 1);
		MysqlTestUtils.assertListContents(lFromDB);

		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		long now = System.currentTimeMillis();
		
		MonitoredSubreddit ericssub = new MonitoredSubreddit(-1, "ericssub", false, false, false);
		database.getMonitoredSubredditMapping().save(ericssub);
		
		HandledModAction hma = new HandledModAction(-1, johnssub.id, "ModAction_ID", new Timestamp(now - 5000));
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, john.id, paul.id, hma.id, "i hate you", "5 seconds");
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(paul.id, johnssub.id);
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
		
		lFromDB = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(paul.id, johnssub.id);
		MysqlTestUtils.assertListContents(lFromDB, ban);
		
		HandledModAction hma2 = new HandledModAction(-1, johnssub.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, john.id, paul.id, hma2.id, "die friendo", "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(paul.id, johnssub.id);
		assertEquals(ban2, fromDB);

		lFromDB = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(paul.id, johnssub.id);
		MysqlTestUtils.assertListContents(lFromDB, ban, ban2);
		
		HandledModAction hma3 = new HandledModAction(-1, ericssub.id, "ModAction_ID3", new Timestamp(now + 5000));
		database.getHandledModActionMapping().save(hma3);
		
		BanHistory ban3 = new BanHistory(-1, eric.id, paul.id, hma3.id, "annoys john", "permanent");
		database.getBanHistoryMapping().save(ban3);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(paul.id, johnssub.id);
		assertEquals(ban2, fromDB);
		
		lFromDB = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(paul.id, johnssub.id);
		MysqlTestUtils.assertListContents(lFromDB, ban, ban2);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(paul.id, ericssub.id);
		assertEquals(ban3, fromDB);
		
		lFromDB = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(paul.id, ericssub.id);
		MysqlTestUtils.assertListContents(lFromDB, ban3);
	}
	
	@Test
	public void testFetchByPerson() {
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		List<BanHistory> fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(paul.id);
		assertTrue(fromDB.isEmpty());
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(john.id);
		assertTrue(fromDB.isEmpty());
		
		long now = System.currentTimeMillis();
		HandledModAction paulBansJohnHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(paulBansJohnHMA);
		
		BanHistory paulBansJohnBH = new BanHistory(-1, paul.id, john.id, paulBansJohnHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansJohnBH);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(paul.id);
		assertTrue(fromDB.isEmpty());
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(john.id);
		MysqlTestUtils.assertListContents(fromDB, paulBansJohnBH);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(eric.id);
		assertTrue(fromDB.isEmpty());
		
		HandledModAction paulBansEricHMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(paulBansEricHMA);
		
		BanHistory paulBansEricBH = new BanHistory(-1, paul.id, eric.id, paulBansEricHMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansEricBH);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(paul.id);
		assertTrue(fromDB.isEmpty());
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(john.id);
		MysqlTestUtils.assertListContents(fromDB, paulBansJohnBH);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(eric.id);
		MysqlTestUtils.assertListContents(fromDB, paulBansEricBH);
		
		HandledModAction paulBansJohn2HMA = new HandledModAction(-1, paulsSub.id, "ModAction_ID3", new Timestamp(now));
		database.getHandledModActionMapping().save(paulBansJohn2HMA);
		
		BanHistory paulBansJohn2BH = new BanHistory(-1, paul.id, john.id, paulBansJohn2HMA.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(paulBansJohn2BH);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(paul.id);
		assertTrue(fromDB.isEmpty());
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(john.id);
		MysqlTestUtils.assertListContents(fromDB, paulBansJohnBH, paulBansJohn2BH);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesByPerson(eric.id);
		MysqlTestUtils.assertListContents(fromDB, paulBansEricBH);
	}
	
	@Test
	public void testFetchByActionAndSubreddit() {
		final long now = System.currentTimeMillis();
		Person mod = database.getPersonMapping().fetchOrCreateByUsername("mod");
		Person mod2 = database.getPersonMapping().fetchOrCreateByUsername("mod2");
		Person banned = database.getPersonMapping().fetchOrCreateByUsername("banned");
		
		MonitoredSubreddit sub1 = new MonitoredSubreddit(-1, "sub1", true, false, false);
		database.getMonitoredSubredditMapping().save(sub1);
		
		MonitoredSubreddit sub2 = new MonitoredSubreddit(-1, "sub2", true, false, false);
		database.getMonitoredSubredditMapping().save(sub2);
		
		
		HandledModAction hma1 = new HandledModAction(-1, sub1.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned.id, hma1.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, sub2.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, mod2.id, banned.id, hma2.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		HandledModAction hma3 = new HandledModAction(-1, sub2.id, "ModAction_ID3", new Timestamp(now));
		database.getHandledModActionMapping().save(hma3);
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod2.id, banned.id, hma2.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		HandledModAction hma4 = new HandledModAction(-1, sub2.id, "ModAction_ID4", new Timestamp(now));
		database.getHandledModActionMapping().save(hma4);
		
		BanHistory ban3 = new BanHistory(-1, mod2.id, banned.id, hma4.id, "#scammer", "permanent");
		database.getBanHistoryMapping().save(ban3);
		
		USLAction action = database.getUSLActionMapping().create(true, banned.id, new Timestamp(now));
		
		database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban1.id));
		database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, ban3.id));
		
		BanHistory fromDb = database.getBanHistoryMapping().fetchByActionAndSubreddit(action.id, sub1.id);
		assertEquals(ban1, fromDb);
		
		fromDb = database.getBanHistoryMapping().fetchByActionAndSubreddit(action.id, sub2.id);
		assertEquals(ban3, fromDb);
	}
}
