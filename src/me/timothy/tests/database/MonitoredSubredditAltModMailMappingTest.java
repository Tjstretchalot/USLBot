package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditAltModMailMapping;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.MonitoredSubredditAltModMail;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class MonitoredSubredditAltModMailMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		DBShortcuts db = new DBShortcuts(database);
		MonitoredSubredditAltModMailMapping map = database.getMonitoredSubredditAltModMailMapping();
		
		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		MonitoredSubreddit sub3 = db.sub("sub3");
		
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id));
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id));
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id));
		
		map.save(new MonitoredSubredditAltModMail(-1, sub2.id, "uslnotifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id));
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id));
		
		map.save(new MonitoredSubredditAltModMail(-1, sub2.id, "sub2notifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id));
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs", "sub2notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id));

		map.save(new MonitoredSubredditAltModMail(-1, sub1.id, "sub1notifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id), "sub1notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs", "sub2notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id));

		map.save(new MonitoredSubredditAltModMail(-1, sub3.id, "sub3notifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id), "sub1notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs", "sub2notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id), "sub3notifs");

		map.save(new MonitoredSubredditAltModMail(-1, sub3.id, "uslnotifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id), "sub1notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs", "sub2notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id), "sub3notifs", "uslnotifs");

		map.save(new MonitoredSubredditAltModMail(-1, sub1.id, "uslnotifs"));

		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub1.id), "sub1notifs", "uslnotifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub2.id), "uslnotifs", "sub2notifs");
		MysqlTestUtils.assertListContents(map.fetchForSubreddit(sub3.id), "sub3notifs", "uslnotifs");
	}
}
