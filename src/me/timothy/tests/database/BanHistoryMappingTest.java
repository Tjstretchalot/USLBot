package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
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
	 * for john banning paul on johnssub. The banhistorys id is set to a strictly positive 
	 * value and the nanoseconds were set to 0 on all timestamps. Also verify it can be retrieved
	 * using fetchAll. Modifying and saving doesn't effect id.
	 */
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		
		long now = System.currentTimeMillis();
		BanHistory banHist = new BanHistory(-1, johnssub.id, john.id, paul.id, "ModAction_ID", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(banHist);
		assertTrue(banHist.id > 0);
		assertEquals(0, banHist.occurredAt.getNanos());
		
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
		
		BanHistory ban = new BanHistory(-1, johnssub.id, john.id, paul.id, "ModAction_ID", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchByID(ban.id);
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
	}
	
	@Test
	public void testFetchByModActionID() {
		BanHistory fromDB = database.getBanHistoryMapping().fetchByModActionID("ModAction_ID");
		assertNull(fromDB);

		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		long now = System.currentTimeMillis();
		
		BanHistory ban = new BanHistory(-1, johnssub.id, john.id, paul.id, "ModAction_ID", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchByModActionID("ModAction_ID");
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
		

		fromDB = database.getBanHistoryMapping().fetchByModActionID("ModAction_id");
		assertNotNull(fromDB);
		assertEquals(ban, fromDB);
	}
	
	@Test
	public void testFetchBanHistoriesAboveIDSortedByIDAsc() {
		List<BanHistory> fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(0, 2);
		assertNotNull(fromDB);
		assertEquals(0, fromDB.size());

		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person eric = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person adam = database.getPersonMapping().fetchOrCreateByUsername("adam");
	
		MonitoredSubreddit johnssub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnssub);
		long now = System.currentTimeMillis();
		
		BanHistory ban = new BanHistory(-1, johnssub.id, john.id, paul.id, "ModAction_ID", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(ban);
		
		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(0, 2);
		MysqlTestUtils.assertListContents(fromDB, ban);

		BanHistory ban2 = new BanHistory(-1, johnssub.id, john.id, eric.id, "ModAction_ID2", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(ban2);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(0, 2);
		MysqlTestUtils.assertListContents(fromDB, ban, ban2);
		

		BanHistory ban3 = new BanHistory(-1, johnssub.id, john.id, adam.id, "ModAction_ID3", "i hate you", "permanent",
				new Timestamp(now));
		database.getBanHistoryMapping().save(ban3);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(0, 2);
		MysqlTestUtils.assertListContents(fromDB, ban, ban2);
		assertEquals(fromDB.get(0), ban);
		assertEquals(fromDB.get(1), ban2);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(1, 2);
		MysqlTestUtils.assertListContents(fromDB, ban2, ban3);

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(2, 2);
		MysqlTestUtils.assertListContents(fromDB, ban3);
		

		fromDB = database.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(3, 2);
		MysqlTestUtils.assertListContents(fromDB);
	}
}
