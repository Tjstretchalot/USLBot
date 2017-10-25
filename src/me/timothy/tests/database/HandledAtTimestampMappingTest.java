package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.HandledAtTimestamp;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focused on testing a HandledAtTimestampMapping inside
 * a mapping database
 * 
 * @author Timothy
 */
public class HandledAtTimestampMappingTest {
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
	
	@Test
	public void testSave() {
		MonitoredSubreddit sub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(sub);
		
		HandledModAction hma = new HandledModAction(-1, sub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hma);
		
		List<HandledAtTimestamp> fromDB = database.getHandledAtTimestampMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB);
		
		HandledAtTimestamp hat = new HandledAtTimestamp(sub.id, hma.id);
		database.getHandledAtTimestampMapping().save(hat);
		
		fromDB = database.getHandledAtTimestampMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, hat);
	}
	
	private interface TestSituationOneTester {
		public void runTestInSit1(MonitoredSubreddit johnsSub, MonitoredSubreddit paulsSub, HandledModAction hmaJohn1, 
				HandledModAction hmaJohn2, HandledModAction hmaPaul1, HandledModAction hmaPaul2);
	}
	
	private void runSpecificTestSituationOne(TestSituationOneTester tester) {
		MonitoredSubreddit johnsSub = new MonitoredSubreddit(-1, "johnssub", false, false, false);
		database.getMonitoredSubredditMapping().save(johnsSub);
		
		MonitoredSubreddit paulsSub = new MonitoredSubreddit(-1, "paulssub", false, false, false);
		database.getMonitoredSubredditMapping().save(paulsSub);
		
		HandledModAction hmaJohn1 = new HandledModAction(-1, johnsSub.id, "ModAction_ID1", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hmaJohn1);
		
		HandledModAction hmaJohn2 = new HandledModAction(-1, johnsSub.id, "ModAction_ID2", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hmaJohn2);
		
		HandledModAction hmaPaul1 = new HandledModAction(-1, paulsSub.id, "ModAction_ID5", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hmaPaul1);
		
		HandledModAction hmaPaul2 = new HandledModAction(-1, paulsSub.id, "ModAction_ID78", new Timestamp(System.currentTimeMillis()));
		database.getHandledModActionMapping().save(hmaPaul2);
		
		tester.runTestInSit1(johnsSub, paulsSub, hmaJohn1, hmaJohn2, hmaPaul1, hmaPaul2);
	}
	
	@Test
	public void testFetchBySubreddit() {
		runSpecificTestSituationOne(new TestSituationOneTester() {

			@Override
			public void runTestInSit1(MonitoredSubreddit johnsSub, MonitoredSubreddit paulsSub,
					HandledModAction hmaJohn1, HandledModAction hmaJohn2, HandledModAction hmaPaul1,
					HandledModAction hmaPaul2) {
				List<HandledAtTimestamp> fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(johnsSub.id);
				MysqlTestUtils.assertListContents(fromDB);
				
				HandledAtTimestamp hat1 = new HandledAtTimestamp(johnsSub.id, hmaJohn1.id);
				database.getHandledAtTimestampMapping().save(hat1);
				
				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(johnsSub.id);
				MysqlTestUtils.assertListContents(fromDB, hat1);
				
				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(paulsSub.id);
				MysqlTestUtils.assertListContents(fromDB);
				
				HandledAtTimestamp hat2 = new HandledAtTimestamp(johnsSub.id, hmaJohn2.id);
				database.getHandledAtTimestampMapping().save(hat2);

				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(johnsSub.id);
				MysqlTestUtils.assertListContents(fromDB, hat1, hat2);
				
				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(paulsSub.id);
				MysqlTestUtils.assertListContents(fromDB);
				

				HandledAtTimestamp hat3 = new HandledAtTimestamp(paulsSub.id, hmaPaul2.id);
				database.getHandledAtTimestampMapping().save(hat3);

				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(johnsSub.id);
				MysqlTestUtils.assertListContents(fromDB, hat1, hat2);
				
				fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(paulsSub.id);
				MysqlTestUtils.assertListContents(fromDB, hat3);
			}
			
		});
	}
	
	@Test
	public void testDeleteByMonitoredSubredditID() {
		runSpecificTestSituationOne(new TestSituationOneTester() {

			@Override
			public void runTestInSit1(MonitoredSubreddit johnsSub, MonitoredSubreddit paulsSub,
					HandledModAction hmaJohn1, HandledModAction hmaJohn2, HandledModAction hmaPaul1,
					HandledModAction hmaPaul2) {
				List<HandledAtTimestamp> fromDB = database.getHandledAtTimestampMapping().fetchByMonitoredSubredditID(johnsSub.id);
				MysqlTestUtils.assertListContents(fromDB);
				
				HandledAtTimestamp hat1 = new HandledAtTimestamp(johnsSub.id, hmaJohn1.id);
				database.getHandledAtTimestampMapping().save(hat1);
				
				fromDB = database.getHandledAtTimestampMapping().fetchAll();
				MysqlTestUtils.assertListContents(fromDB, hat1);
				
				HandledAtTimestamp hat2 = new HandledAtTimestamp(johnsSub.id, hmaJohn2.id);
				database.getHandledAtTimestampMapping().save(hat2);

				fromDB = database.getHandledAtTimestampMapping().fetchAll();
				MysqlTestUtils.assertListContents(fromDB, hat1, hat2);
				
				database.getHandledAtTimestampMapping().deleteByMonitoredSubredditID(paulsSub.id);
				MysqlTestUtils.assertListContents(fromDB, hat1, hat2);
				
				HandledAtTimestamp hat3 = new HandledAtTimestamp(paulsSub.id, hmaPaul2.id);
				database.getHandledAtTimestampMapping().save(hat3);

				fromDB = database.getHandledAtTimestampMapping().fetchAll();
				MysqlTestUtils.assertListContents(fromDB, hat1, hat2, hat3);
				
				database.getHandledAtTimestampMapping().deleteByMonitoredSubredditID(johnsSub.id);

				fromDB = database.getHandledAtTimestampMapping().fetchAll();
				MysqlTestUtils.assertListContents(fromDB, hat3);
				
				database.getHandledAtTimestampMapping().deleteByMonitoredSubredditID(paulsSub.id);

				fromDB = database.getHandledAtTimestampMapping().fetchAll();
				MysqlTestUtils.assertListContents(fromDB);
			}
		});
	}
}
