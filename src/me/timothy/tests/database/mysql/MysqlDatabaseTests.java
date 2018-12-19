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
			MysqlHandledAtTimestampMappingTest.class,
			MysqlTraditionalScammerMappingTest.class,
			MysqlUnbanRequestMappingTest.class,
			MysqlSubredditTraditionalListStatusMappingTest.class,
			MysqlLastInfoPMMappingTest.class,
			MysqlRegisterAccountMappingTest.class,
			MysqlResetPasswordRequestMappingTest.class,
			MysqlActionLogMappingTest.class,
			MysqlTemporaryAuthLevelMappingTest.class,
			MysqlTemporaryAuthRequestMappingTest.class,
			MysqlAcceptModeratorInviteRequestMappingTest.class,
			MysqlHashtagMappingTest.class,
			MysqlUSLActionMappingTest.class,
			MysqlUSLActionBanHistoryMappingTest.class,
			MysqlUSLActionHashtagMappingTest.class,
			MysqlUSLActionUnbanHistoryMappingTest.class,
			MysqlDirtyPersonMappingTest.class,
			MysqlRedditToMeaningProgressMappingTest.class
		})
public class MysqlDatabaseTests {

}
