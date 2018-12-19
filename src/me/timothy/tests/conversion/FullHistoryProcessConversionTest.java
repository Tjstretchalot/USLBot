package me.timothy.tests.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.conversion.FullHistoryProcess;
import me.timothy.tests.database.mysql.MysqlTestUtils;

import me.timothy.bots.database.mysql.MysqlSubscribedHashtagMapping;
import me.timothy.bots.database.mysql.MysqlHashtagMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPropagateStatusMapping;

public class FullHistoryProcessConversionTest {
	@Test
	public void testConvertTags() throws Exception {
		Properties testDBProperties = MysqlTestUtils.fetchTestDatabaseProperties();
		USLDatabase database = MysqlTestUtils.getDatabase(testDBProperties);
		
		try {
			database.purgeAll();
			database.validateTableState();
			
			MonitoredSubreddit usl = new MonitoredSubreddit(-1, "universalscammerlist", true, false, false);
			database.getMonitoredSubredditMapping().save(usl);
			
			MonitoredSubreddit borrow = new MonitoredSubreddit(-1, "borrow", true, false, false);
			database.getMonitoredSubredditMapping().save(borrow);
			
			Connection conn = database.getConnection();
			Statement statement = conn.createStatement();
			statement.execute("DROP TABLE subscribed_hashtags"); // Drop the new table
			statement.execute("DROP TABLE hashtags"); // this table didn't exist before
			statement.execute("CREATE TABLE subscribed_hashtags (" // recreate the old schema
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "monitored_subreddit_id INT NOT NULL, "
					+ "hashtag VARCHAR(50) NOT NULL, "
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "deleted_at TIMESTAMP NULL DEFAULT NULL, "
					+ "PRIMARY KEY(id), "
					+ "INDEX ind_subschashtag_monsub_id (monitored_subreddit_id), "
					+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
					+ ")");
			statement.close();
			
			// Create 4 tags, usl tracks #scammer #sketchy #troll, borrow tracks #scammer, but usl stopped tracking #sketchy (deleted)
			PreparedStatement prep = conn.prepareStatement("INSERT INTO subscribed_hashtags (monitored_subreddit_id, hashtag, created_at, deleted_at) "
					+ "VALUES (?, ?, NOW(), ?)");
			prep.setInt(1, usl.id);
			prep.setString(2, "#scammer");
			prep.setTimestamp(3, null);
			prep.execute();
			prep.setInt(1, usl.id);
			prep.setString(2, "#sketchy");
			prep.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			prep.execute();
			prep.setInt(1, usl.id);
			prep.setString(2, "#troll");
			prep.setTimestamp(3, null);
			prep.execute();
			prep.setInt(1, borrow.id);
			prep.setString(2, "#scammer");
			prep.setTimestamp(3, null);
			prep.execute();
			prep.close();
			
			// Run conversion
			FullHistoryProcess.doConversionTags(conn);
			
			((MysqlHashtagMapping) database.getHashtagMapping()).validateSchema();
			((MysqlSubscribedHashtagMapping) database.getSubscribedHashtagMapping()).validateSchema();
			
			List<Hashtag> hashtags = database.getHashtagMapping().fetchAll();
			Hashtag scammerTag = hashtags.stream().filter((ht) -> ht.tag.equals("#scammer")).findFirst().get();
			Hashtag sketchyTag = hashtags.stream().filter((ht) -> ht.tag.equals("#sketchy")).findFirst().get();
			Hashtag trollTag = hashtags.stream().filter((ht) -> ht.tag.equals("#troll")).findFirst().get();
			assertEquals(3, hashtags.size());
			
			List<SubscribedHashtag> subTags = database.getSubscribedHashtagMapping().fetchAll();
			subTags.stream().filter((sh) -> (sh.hashtagID == scammerTag.id && sh.deletedAt == null && sh.monitoredSubredditID == usl.id)).findFirst().get();
			subTags.stream().filter((sh) -> (sh.hashtagID == sketchyTag.id && sh.deletedAt != null && sh.monitoredSubredditID == usl.id)).findFirst().get();
			subTags.stream().filter((sh) -> (sh.hashtagID == trollTag.id && sh.deletedAt == null && sh.monitoredSubredditID == usl.id)).findFirst().get();
			subTags.stream().filter((sh) -> (sh.hashtagID == scammerTag.id && sh.deletedAt == null && sh.monitoredSubredditID == borrow.id)).findFirst().get();
			assertEquals(4, subTags.size());
		}finally {
			database.disconnect();
		}
	}
	
	@Test
	public void testConvertSubredditPropagateStatus()  throws Exception {
		Properties testDBProperties = MysqlTestUtils.fetchTestDatabaseProperties();
		USLDatabase database = MysqlTestUtils.getDatabase(testDBProperties);
		
		try {
			database.purgeAll();
			database.validateTableState();
	
			Connection conn = database.getConnection();
			Statement statement = conn.createStatement(); 
			statement.execute("DROP TABLE subreddit_propagate_status");
			statement.execute("CREATE TABLE subreddit_propagate_status ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "major_subreddit_id INT NOT NULL, "
					+ "minor_subreddit_id INT NOT NULL, "
					+ "latest_propagated_action_time TIMESTAMP NULL DEFAULT NULL, "
					+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
					+ "PRIMARY KEY(id), "
					+ "INDEX ind_subpropstat_majmonsub_id (major_subreddit_id), "
					+ "INDEX ind_subpropstat_minmonsub_id (minor_subreddit_id), "
					+ "UNIQUE KEY(major_subreddit_id, minor_subreddit_id), "
					+ "FOREIGN KEY (major_subreddit_id) REFERENCES monitored_subreddits(id), "
					+ "FOREIGN KEY (minor_subreddit_id) REFERENCES monitored_subreddits(id)"
					+ ")");
			statement.close();
			
	
			FullHistoryProcess.doConversionSubredditPropStatus(conn);
			((MysqlSubredditPropagateStatusMapping) database.getSubredditPropagateStatusMapping()).validateSchema();
		}finally {
			database.disconnect();
		}
	}
	
	@Test
	public void testNewResponses() throws Exception {
		Properties testDBProperties = MysqlTestUtils.fetchTestDatabaseProperties();
		USLDatabase database = MysqlTestUtils.getDatabase(testDBProperties);
		
		try { 
			MysqlTestUtils.clearDatabase(database);
		
			Connection conn = database.getConnection();
			
			assertNull(database.getResponseMapping().fetchByName("test"));
			FullHistoryProcess.doConversionResponses(conn, new File("tests/new_responses"));
			
			Response fromDb = database.getResponseMapping().fetchByName("test");
			Assert.assertNotNull(fromDb);
			assertEquals("this is a test\r\nwith two lines", fromDb.responseBody);
		}finally {
			database.disconnect();
		}
	}
}
