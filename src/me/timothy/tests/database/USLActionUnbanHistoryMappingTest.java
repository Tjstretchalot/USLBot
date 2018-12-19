package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.USLActionUnbanHistoryMapping;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class USLActionUnbanHistoryMappingTest {

	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testAll() {
		final long now = System.currentTimeMillis();
		Person mod = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		MonitoredSubreddit modsSub = new MonitoredSubreddit(-1, "ericssub", true, false, false);
		database.getMonitoredSubredditMapping().save(modsSub);
		
		HandledModAction hmaModUnbansJohn = new HandledModAction(-1, modsSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(hmaModUnbansJohn);
		
		UnbanHistory hisModUnbansJohn = new UnbanHistory(-1, mod.id, john.id, hmaModUnbansJohn.id);
		database.getUnbanHistoryMapping().save(hisModUnbansJohn);
		
		USLAction action = database.getUSLActionMapping().create(true, john.id, new Timestamp(System.currentTimeMillis()));
		
		USLActionUnbanHistoryMapping map = database.getUSLActionUnbanHistoryMapping();
		
		MysqlTestUtils.assertListContents(map.fetchAll());
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(action.id));
		MysqlTestUtils.assertListContents(map.fetchByUnbanHistoryID(hisModUnbansJohn.id));
		
		USLActionUnbanHistory actionUnban = new USLActionUnbanHistory(action.id, hisModUnbansJohn.id);
		map.save(actionUnban);

		MysqlTestUtils.assertListContents(map.fetchAll(), actionUnban);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(action.id), actionUnban);
		MysqlTestUtils.assertListContents(map.fetchByUnbanHistoryID(hisModUnbansJohn.id), actionUnban);
	}
}
