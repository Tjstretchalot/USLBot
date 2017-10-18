package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;

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
		BanHistory banHist = new BanHistory(-1, johnssub.id, john.id, paul.id, BanHistory.BanReasonIdentifier.SubBanNotScammer.id, 
				"i hate u", "i think its cause john doesnt like paul", false, new Timestamp(now), new Timestamp(now), new Timestamp(now));
		database.getBanHistoryMapping().save(banHist);
		assertTrue(banHist.id > 0);
		assertEquals(0, banHist.createdAt.getNanos());
		assertEquals(0, banHist.occurredAt.getNanos());
		assertEquals(0, banHist.updatedAt.getNanos());
		
		List<BanHistory> fetched = database.getBanHistoryMapping().fetchAll();
		assertEquals(1, fetched.size());
		assertEquals(banHist, fetched.get(0));
		
		banHist.banReasonAdditional = "john </3 paul";
		database.getBanHistoryMapping().save(banHist);
		assertEquals(fetched.get(0).id, banHist.id);
		assertNotEquals(fetched.get(0), banHist);
		
		fetched = database.getBanHistoryMapping().fetchAll();
		assertEquals(1, fetched.size());
		assertEquals(banHist, fetched.get(0));
	}
}
