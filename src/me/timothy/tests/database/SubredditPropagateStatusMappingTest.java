package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.SubredditPropagateStatusMapping;
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
	
	@Test
	public void testAll() {
		MonitoredSubreddit borrow = new MonitoredSubreddit(-1, "borrow", true, false, false);
		database.getMonitoredSubredditMapping().save(borrow);
		
		MonitoredSubreddit usl = new MonitoredSubreddit(-1, "universalscammerlist", true, false, false);
		database.getMonitoredSubredditMapping().save(usl);
		
		SubredditPropagateStatus uslStatus = new SubredditPropagateStatus(-1, usl.id, 0, null);
		
		SubredditPropagateStatusMapping map = database.getSubredditPropagateStatusMapping();
		map.save(uslStatus);
		assertTrue(uslStatus.id > 0);
		assertEquals(0, uslStatus.actionID);
		assertNotNull(uslStatus.updatedAt);
		
		assertEquals(uslStatus, map.fetchForSubreddit(usl.id));
		assertEquals(uslStatus, map.fetchOrCreateForSubreddit(usl.id));
		MysqlTestUtils.assertListContents(map.fetchAll(), uslStatus);
		assertEquals(uslStatus, map.fetchOrCreateForSubreddit(usl.id));
		assertNull(map.fetchForSubreddit(borrow.id));
		
		SubredditPropagateStatus borrowStatus = map.fetchOrCreateForSubreddit(borrow.id);
		assertTrue(borrowStatus.id > 0);
		assertNotEquals(uslStatus.id, borrowStatus.id);
		assertEquals(uslStatus.monitoredSubredditID, usl.id);
		assertEquals(borrowStatus.monitoredSubredditID, borrow.id);

		assertEquals(uslStatus, map.fetchForSubreddit(usl.id));
		assertEquals(uslStatus, map.fetchOrCreateForSubreddit(usl.id));
		MysqlTestUtils.assertListContents(map.fetchAll(), uslStatus, borrowStatus);
		assertEquals(uslStatus, map.fetchOrCreateForSubreddit(usl.id));
		assertEquals(borrowStatus, map.fetchForSubreddit(borrow.id));
		assertEquals(borrowStatus, map.fetchOrCreateForSubreddit(borrow.id));
	}
}
