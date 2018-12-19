package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionUnbanHistory;
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
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod.id, banned.id, hma1.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		HandledModAction hma2 = new HandledModAction(-1, sub2.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(hma2);
		
		UnbanHistory unban2 = new UnbanHistory(-1, mod2.id, banned.id, hma2.id);
		database.getUnbanHistoryMapping().save(unban2);
		
		USLAction action = database.getUSLActionMapping().create(false, banned.id, new Timestamp(now));
		
		database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, unban1.id));
		database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, unban2.id));
		
		assertEquals(unban1, database.getUnbanHistoryMapping().fetchByActionAndSubreddit(action.id, sub1.id));
		assertEquals(unban2, database.getUnbanHistoryMapping().fetchByActionAndSubreddit(action.id, sub2.id));
	}
}
