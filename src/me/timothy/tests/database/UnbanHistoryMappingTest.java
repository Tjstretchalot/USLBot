package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focusing on testing an UnbanHistoryMapping in a MappingDatabase
 * 
 * @author Timothy
 */
public class UnbanHistoryMappingTest {
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
	 * John unbans paul on johnssub. When saved, id is positive
	 * and can be fetched with fetchAll. Can be modified and id wont change
	 */
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		HandledModAction hma = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		UnbanHistory johnUnbansPaul = new UnbanHistory(-1, john.id, paul.id, hma.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul);
		
		assertTrue(johnUnbansPaul.id > 0);
		
		List<UnbanHistory> fromDB = database.getUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, johnUnbansPaul);
		
		HandledModAction hma2 = new HandledModAction(-1, johnsSub.id, "ModAction_ID2", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma2);
		
		int oldID = johnUnbansPaul.id;
		johnUnbansPaul.handledModActionID = hma2.id;
		database.getUnbanHistoryMapping().save(johnUnbansPaul);
		
		assertEquals(oldID, johnUnbansPaul.id);
		
		fromDB = database.getUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, johnUnbansPaul);
	}
	
	@Test
	public void testFetchByID() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);

		HandledModAction hma = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		UnbanHistory johnUnbansPaul = new UnbanHistory(-1, john.id, paul.id, hma.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul);
		
		UnbanHistory fromDB = database.getUnbanHistoryMapping().fetchByID(johnUnbansPaul.id);
		assertEquals(johnUnbansPaul, fromDB);
		
		fromDB = database.getUnbanHistoryMapping().fetchByID(johnUnbansPaul.id + 1);
		assertNull(fromDB);
	}
	
	@Test
	public void testFetchByHandledModActionID() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);

		HandledModAction hma = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		UnbanHistory johnUnbansPaul = new UnbanHistory(-1, john.id, paul.id, hma.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul);
		
		UnbanHistory fromDB = database.getUnbanHistoryMapping().fetchByHandledModActionID(hma.id);
		assertEquals(johnUnbansPaul, fromDB);
		
		fromDB = database.getUnbanHistoryMapping().fetchByHandledModActionID(hma.id + 1);
		assertNull(fromDB);
	}
	
	@Test
	public void testFetchByPersonAndSubreddit() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);

		HandledModAction hma = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis() - 15000));
		database.getHandledModActionMapping().save(hma);
		
		UnbanHistory johnUnbansPaul = new UnbanHistory(-1, john.id, paul.id, hma.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul);
		
		UnbanHistory fromDB = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(paul.id, johnsSub.id);
		assertEquals(johnUnbansPaul, fromDB);
		
		List<UnbanHistory> lFromDB = database.getUnbanHistoryMapping().fetchUnbanHistoriesByPersonAndSubreddit(paul.id, johnsSub.id);
		MysqlTestUtils.assertListContents(lFromDB, johnUnbansPaul);
		
		HandledModAction hma2 = new HandledModAction(-1, johnsSub.id, "ModAction_ID2", new Timestamp(System.currentTimeMillis() - 5000));
		database.getHandledModActionMapping().save(hma2);
		
		UnbanHistory johnUnbansPaul2 = new UnbanHistory(-1, john.id, paul.id, hma2.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul2);

		fromDB = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(paul.id, johnsSub.id);
		assertEquals(johnUnbansPaul2, fromDB);


		HandledModAction hma3 = new HandledModAction(-1, johnsSub.id, "ModAction_ID5", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma3);
		
		UnbanHistory johnUnbansPaul3 = new UnbanHistory(-1, john.id, paul.id, hma3.id);
		database.getUnbanHistoryMapping().save(johnUnbansPaul3);

		fromDB = database.getUnbanHistoryMapping().fetchUnbanHistoryByPersonAndSubreddit(paul.id, johnsSub.id);
		assertEquals(johnUnbansPaul3, fromDB);
	}
	
}
