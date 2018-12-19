package me.timothy.conversion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLDatabaseBackupManager;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.mysql.MysqlHashtagMapping;
import me.timothy.bots.database.mysql.MysqlPersonMapping;
import me.timothy.bots.database.mysql.MysqlResponseMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPropagateStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubscribedHashtagMapping;
import me.timothy.bots.models.ActionLog;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;

/**
 * This conversion tool takes us from before we had USLAction's to after we had USLAction's
 * 
 * @author Timothy
 */
public class FullHistoryProcess {
	public static void doConversionTags(Connection conn) throws SQLException {
		Statement statement = conn.createStatement();
		
		MysqlHashtagMapping htgs = new MysqlHashtagMapping(null, conn);
		htgs.validateSchema();
		
		List<String> hashtags = new ArrayList<>();
		statement.execute("RENAME TABLE subscribed_hashtags TO subscribed_hashtags_old");
		
		MysqlSubscribedHashtagMapping newSubTagMapping = new MysqlSubscribedHashtagMapping(null, conn);
		newSubTagMapping.validateSchema();
		
		ResultSet oldTags = statement.executeQuery("SELECT DISTINCT hashtag FROM subscribed_hashtags_old");
		while(oldTags.next()) {
			hashtags.add(oldTags.getString(1));
		}
		oldTags.close();
		
		PersonMapping personMapping = new MysqlPersonMapping(null, conn);
		Person bot = personMapping.fetchOrCreateByUsername("USLBot"); 
		
		Map<String, Hashtag> tags = new HashMap<String, Hashtag>();
		for(String tg : hashtags) {
			Hashtag hashtag = new Hashtag(-1, tg, "Not yet added", bot.id, bot.id, 
					new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
			htgs.save(hashtag);
			tags.put(tg, hashtag);
		}
		
		
		List<SubscribedHashtag> tagsToSave = new ArrayList<>();
		oldTags = statement.executeQuery("SELECT monitored_subreddit_id, hashtag, created_at, deleted_at FROM subscribed_hashtags_old");
		while(oldTags.next()) {
			int monSubId = oldTags.getInt(1);
			String tg = oldTags.getString(2);
			Timestamp createdAt = oldTags.getTimestamp(3);
			Timestamp deletedAt = oldTags.getTimestamp(4);
			
			SubscribedHashtag newTag = new SubscribedHashtag(-1, monSubId, tags.get(tg).id, createdAt, deletedAt);
			tagsToSave.add(newTag);
		}
		oldTags.close();
		
		for(SubscribedHashtag newTag : tagsToSave) {
			newSubTagMapping.save(newTag);
		}
		
		statement.execute("DROP TABLE subscribed_hashtags_old");
		statement.close();
	}
	
	public static void doConversionSubredditPropStatus(Connection conn) throws SQLException {
		// we need to do a full rescan so just drop it and revalidate
		
		try(Statement statement = conn.createStatement()) {
			statement.execute("DROP TABLE subreddit_propagate_status");
		}
		
		MysqlSubredditPropagateStatusMapping map = new MysqlSubredditPropagateStatusMapping(null, conn);
		map.validateSchema();
	}
	
	public static void doConversionHandledAtTimestamp(Connection conn) throws SQLException {
		// we are rescanning so just drop it. don't even need to revalidate since we don't
		// do these in sql anymore
		
		try(Statement statement = conn.createStatement()) {
			statement.execute("DROP TABLE handled_at_timestamps");
		}
	}
	
	public static void doConversionResponses(Connection conn, File folder) throws SQLException, IOException {
		MysqlResponseMapping respMap = new MysqlResponseMapping(null, conn);
		
		File[] allFiles = folder.listFiles();
		
		for(File file : allFiles) {
			if(file.isFile()) {
				String fileName = file.getName();
				if(fileName.endsWith(".txt")) {
					String responseName = fileName.substring(0, fileName.length() - 4);
					String responseBody = new String(Files.readAllBytes(file.toPath()));
					
					int i;
					for(i = responseBody.length(); i >= 0 && (responseBody.charAt(i - 1) == '\n' || responseBody.charAt(i - 1) == '\r'); i--);
					
					responseBody = responseBody.substring(0, i);
					
					Response resp = respMap.fetchByName(responseName);
					
					if(resp == null) {
						resp = new Response(-1, responseName, responseBody,
								new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
					}else {
						resp.responseBody = responseBody;
						resp.updatedAt = new Timestamp(System.currentTimeMillis());
					}
					
					respMap.save(resp);
				}
			}
		}
	}
	
	public static class MockActionLogMapping implements ActionLogMapping {
		private static final Logger logger = LogManager.getLogger();
		
		@Override
		public void save(ActionLog a) throws IllegalArgumentException {
			logger.info("Action: " + a.action);
		}

		@Override
		public List<ActionLog> fetchAll() {
			return Collections.emptyList();
		}

		@Override
		public void append(String action) {
			logger.info("Action: " + action);
		}

		@Override
		public List<ActionLog> fetchOrderedByTime() {
			return Collections.emptyList();
		}

		@Override
		public void clear() {
		}
	}
	
	public static class MockUSLDatabase extends USLDatabase {
		public MockUSLDatabase() {}
		@Override
		public void disconnect() {}
		@Override
		public void connect(String user, String pass, String url, File folder) {}
		
		@Override
		public ActionLogMapping getActionLogMapping() {
			return new MockActionLogMapping();
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			realMain2();
		}catch(Exception e) {
			LogManager.getLogger().throwing(e);
			throw e;
		}
	}
	
	public static void realMain2() throws Exception {
		Logger logger = LogManager.getLogger();
		USLFileConfiguration config = new USLFileConfiguration();
		config.load();
		
		logger.debug("File configuration loaded.");
		
		logger.debug("Initiating database backup...");
		USLDatabaseBackupManager backup = new USLDatabaseBackupManager(new MockUSLDatabase(), config);
		backup.considerBackup();
		
		logger.debug("Backup complete...");
		
		logger.debug("Connecting to database..");
		Connection connection = DriverManager.getConnection(config.getProperty("database.url"),
				config.getProperty("database.username"), config.getProperty("database.password"));
		
		
		logger.debug("Converting responses..");
		doConversionResponses(connection, new File("new_responses"));
		
		connection.close();
		
		logger.debug("Done and disconnected");
	}
	
	public static void realMain() throws Exception {
		Logger logger = LogManager.getLogger();
		USLFileConfiguration config = new USLFileConfiguration();
		config.load();
		
		logger.debug("File configuration loaded.");
		
		logger.debug("Initiating database backup...");
		USLDatabaseBackupManager backup = new USLDatabaseBackupManager(new MockUSLDatabase(), config);
		backup.considerBackup();
		
		logger.debug("Backup complete...");
		
		logger.debug("Connecting to database..");
		Connection connection = DriverManager.getConnection(config.getProperty("database.url"),
				config.getProperty("database.username"), config.getProperty("database.password"));
		
		logger.info("Converting tags...");
		doConversionTags(connection);
		
		logger.info("Converting HAT table...");
		doConversionHandledAtTimestamp(connection);
		
		logger.info("Converting propagation status...");
		doConversionSubredditPropStatus(connection);
		
		logger.info("Conversion complete. Disconnecting...");
		connection.close();
		connection = null;
		
		logger.info("Reconnecting with wrappers...");
		USLDatabase database = new USLDatabase();
		try {
			database.connect(config.getProperty("database.username"), config.getProperty("database.password"), config.getProperty("database.url"), 
					new File(config.getProperty("database.flat_folder")));
			
			logger.info("Validating table state..");
			database.validateTableState();
			
			logger.info("Success!");
		}finally {
			logger.info("Disconnecting from database..");
			database.disconnect();
		}
	}
}
