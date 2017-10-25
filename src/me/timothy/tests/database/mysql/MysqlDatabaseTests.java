package me.timothy.tests.database.mysql;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(
		{
			MysqlResponseMappingTest.class,
			MysqlFullnameMappingTest.class,
			MysqlMonitoredSubredditMappingTest.class,
			MysqlPersonMappingTest.class,
			MysqlBanHistoryMappingTest.class,
			MysqlSubscribedHashtagMappingTest.class,
			MysqlSubredditModqueueProgressMappingTest.class,
			MysqlSubredditPropagateStatusMappingTest.class,
			MysqlUnbanHistoryMappingTest.class,
			MysqlHandledModActionMappingTest.class, 
			MysqlHandledAtTimestampMappingTest.class
		})
public class MysqlDatabaseTests {

}
