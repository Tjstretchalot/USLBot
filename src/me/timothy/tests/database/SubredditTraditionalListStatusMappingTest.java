package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubredditTraditionalListStatus;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class SubredditTraditionalListStatusMappingTest {
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
	
	@Test
	public void testSave() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(sub);
		
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		long now = System.currentTimeMillis();
		TraditionalScammer johnScammer = new TraditionalScammer(-1, john.id, "grndfther", "#scammer", new Timestamp(now));
		database.getTraditionalScammerMapping().save(johnScammer);
		
		SubredditTraditionalListStatus status = new SubredditTraditionalListStatus(-1, sub.id, null, null);
		database.getSubredditTraditionalListStatusMapping().save(status);
		
		List<SubredditTraditionalListStatus> fromDB = database.getSubredditTraditionalListStatusMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, status);
		assertTrue(status.id > 0);
		
		int oldID = status.id;
		status.lastHandledAt = new Timestamp(System.currentTimeMillis());
		status.lastHandledID = johnScammer.id;
		database.getSubredditTraditionalListStatusMapping().save(status);
		assertEquals(oldID, status.id);
		assertEquals(0, status.lastHandledAt.getNanos());
	}
	
	
	@Test
	public void testFetchBySubreddit() {
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		SubredditTraditionalListStatus fromDB = database.getSubredditTraditionalListStatusMapping().fetchBySubredditID(johnsSub.id);
		assertNull(fromDB);
		
		SubredditTraditionalListStatus johnStatus = new SubredditTraditionalListStatus(-1, johnsSub.id, null, null);
		database.getSubredditTraditionalListStatusMapping().save(johnStatus);
		
		fromDB = database.getSubredditTraditionalListStatusMapping().fetchBySubredditID(johnsSub.id);
		assertEquals(johnStatus, fromDB);
		
		fromDB = database.getSubredditTraditionalListStatusMapping().fetchBySubredditID(paulsSub.id);
		assertNull(fromDB);
		
	}
}
