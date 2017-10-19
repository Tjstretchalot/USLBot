package me.timothy.tests.database.mysql;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses(
		{
			MysqlFullnameMappingTest.class,
			MysqlMonitoredSubredditMappingTest.class,
			MysqlPersonMappingTest.class,
			MysqlBanHistoryMappingTest.class,
			MysqlSubscribedHashtagMappingTest.class
		})
public class MysqlDatabaseTests {

}
