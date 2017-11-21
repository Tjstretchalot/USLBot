package me.timothy.bots;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.BanHistoryMapping;
import me.timothy.bots.database.FullnameMapping;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.database.LastInfoPMMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.ResponseMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.database.SiteSessionMapping;
import me.timothy.bots.database.SubredditModqueueProgressMapping;
import me.timothy.bots.database.SubredditPropagateStatusMapping;
import me.timothy.bots.database.SubredditTraditionalListStatusMapping;
import me.timothy.bots.database.SubscribedHashtagMapping;
import me.timothy.bots.database.TraditionalScammerMapping;
import me.timothy.bots.database.UnbanHistoryMapping;
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.database.mysql.MysqlBanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlFullnameMapping;
import me.timothy.bots.database.mysql.MysqlHandledAtTimestampMapping;
import me.timothy.bots.database.mysql.MysqlHandledModActionMapping;
import me.timothy.bots.database.mysql.MysqlLastInfoPMMapping;
import me.timothy.bots.database.mysql.MysqlMonitoredSubredditMapping;
import me.timothy.bots.database.mysql.MysqlPersonMapping;
import me.timothy.bots.database.mysql.MysqlResponseMapping;
import me.timothy.bots.database.mysql.MysqlSiteSessionMapping;
import me.timothy.bots.database.mysql.MysqlSubredditModqueueProgressMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPropagateStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubredditTraditionalListStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubscribedHashtagMapping;
import me.timothy.bots.database.mysql.MysqlTraditionalScammerMapping;
import me.timothy.bots.database.mysql.MysqlUnbanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUnbanRequestMapping;
import me.timothy.bots.models.Fullname;

/**
 * The database for the universal scammer list.
 * Uses a MYSql connection and acts as a MappingDatabase.
 * 
 * @author Timothy Moore
 */
public class USLDatabase extends Database implements MappingDatabase {
	private Logger logger;
	private Connection connection;
	
	private FullnameMapping fullnameMapping;
	private MonitoredSubredditMapping monitoredSubredditMapping;
	private PersonMapping personMapping;
	private HandledModActionMapping handledModActionMapping;
	private BanHistoryMapping banHistoryMapping;
	private ResponseMapping responseMapping;
	private SubscribedHashtagMapping subscribedHashtagMapping;
	private SubredditModqueueProgressMapping subredditModqueueProgressMapping;
	private SubredditPropagateStatusMapping subredditPropagateStatusMapping;
	private UnbanHistoryMapping unbanHistoryMapping;
	private HandledAtTimestampMapping handledAtTimestampMapping;
	private TraditionalScammerMapping traditionalScammerMapping;
	private UnbanRequestMapping unbanRequestMapping;
	private SubredditTraditionalListStatusMapping subredditTraditionalListStatusMapping; 
	private LastInfoPMMapping lastInfoPMMapping;
	private SiteSessionMapping siteSessionMapping;
	
	private SchemaValidator fullnameValidator;
	private SchemaValidator monitoredSubredditValidator;
	private SchemaValidator personValidator;
	private SchemaValidator handledModActionValidator;
	private SchemaValidator banHistoryValidator;
	private SchemaValidator responseValidator;
	private SchemaValidator subscribedHashtagValidator;
	private SchemaValidator subredditModqueueProgressValidator;
	private SchemaValidator subredditPropagateStatusValidator;
	private SchemaValidator unbanHistoryValidator;
	private SchemaValidator handledAtTimestampValidator;
	private SchemaValidator traditionalScammerValidator;
	private SchemaValidator unbanRequestValidator;
	private SchemaValidator subredditTraditionalListStatusValidator;
	private SchemaValidator lastInfoPMValidator;
	private SchemaValidator siteSessionValidator;
	
	/**
	 * Connects to the specified database. If there is an active connection
	 * already, the active connection is explicitly closed.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param url
	 *            the url
	 * @throws SQLException
	 *             if a sql-related exception occurs
	 */
	public void connect(String username, String password, String url) 
			throws SQLException {
		if (connection != null) {
			disconnect();
		}
		
		connection = DriverManager.getConnection(url, username, password);
		
		fullnameMapping = new MysqlFullnameMapping(this, connection);
		monitoredSubredditMapping = new MysqlMonitoredSubredditMapping(this, connection);
		personMapping = new MysqlPersonMapping(this, connection);
		handledModActionMapping = new MysqlHandledModActionMapping(this, connection);
		banHistoryMapping = new MysqlBanHistoryMapping(this, connection);
		responseMapping = new MysqlResponseMapping(this, connection);
		subscribedHashtagMapping = new MysqlSubscribedHashtagMapping(this, connection);
		subredditModqueueProgressMapping = new MysqlSubredditModqueueProgressMapping(this, connection);
		subredditPropagateStatusMapping = new MysqlSubredditPropagateStatusMapping(this, connection);
		unbanHistoryMapping = new MysqlUnbanHistoryMapping(this, connection);
		handledAtTimestampMapping = new MysqlHandledAtTimestampMapping(this, connection);
		traditionalScammerMapping = new MysqlTraditionalScammerMapping(this, connection);
		unbanRequestMapping = new MysqlUnbanRequestMapping(this, connection);
		subredditTraditionalListStatusMapping = new MysqlSubredditTraditionalListStatusMapping(this, connection);
		lastInfoPMMapping = new MysqlLastInfoPMMapping(this, connection);
		siteSessionMapping = new MysqlSiteSessionMapping(this, connection);
		
		fullnameValidator = (SchemaValidator) fullnameMapping;
		monitoredSubredditValidator = (SchemaValidator) monitoredSubredditMapping;
		personValidator = (SchemaValidator) personMapping;
		handledModActionValidator = (SchemaValidator) handledModActionMapping;
		banHistoryValidator = (SchemaValidator) banHistoryMapping;
		responseValidator = (SchemaValidator) responseMapping;
		subscribedHashtagValidator = (SchemaValidator) subscribedHashtagMapping;
		subredditModqueueProgressValidator = (SchemaValidator) subredditModqueueProgressMapping;
		subredditPropagateStatusValidator = (SchemaValidator) subredditPropagateStatusMapping;
		unbanHistoryValidator = (SchemaValidator) unbanHistoryMapping;
		handledAtTimestampValidator = (SchemaValidator) handledAtTimestampMapping;
		traditionalScammerValidator = (SchemaValidator) traditionalScammerMapping;
		unbanRequestValidator = (SchemaValidator) unbanRequestMapping;
		subredditTraditionalListStatusValidator = (SchemaValidator) subredditTraditionalListStatusMapping;
		lastInfoPMValidator = (SchemaValidator) lastInfoPMMapping;
		siteSessionValidator = (SchemaValidator) siteSessionMapping;
	}

	/**
	 * Ensures the database is disconnected and will not return invalid
	 * mappings (instead they will return null until the next connect)
	 */
	public void disconnect() {
		try {
			connection.close();
		} catch (SQLException e) {
			logger.throwing(e);
		}

		fullnameMapping = null;
		monitoredSubredditMapping = null;
		personMapping = null;
		handledModActionMapping = null;
		banHistoryMapping = null;
		responseMapping = null;
		subscribedHashtagMapping = null;
		subredditModqueueProgressMapping = null;
		subredditPropagateStatusMapping = null;
		unbanHistoryMapping = null;
		handledAtTimestampMapping = null;
		traditionalScammerMapping = null;
		unbanRequestMapping = null;
		subredditTraditionalListStatusMapping = null;
		lastInfoPMMapping = null;
		siteSessionMapping = null;
		
		fullnameValidator = null;
		monitoredSubredditValidator = null;
		personValidator = null;
		handledModActionValidator = null;
		banHistoryValidator = null;
		responseValidator = null;
		subscribedHashtagValidator = null;
		subredditModqueueProgressValidator = null;
		subredditPropagateStatusValidator = null;
		unbanHistoryValidator = null;
		handledAtTimestampValidator = null;
		traditionalScammerValidator = null;
		unbanRequestValidator = null;
		subredditTraditionalListStatusValidator = null;
		lastInfoPMValidator = null;
		siteSessionValidator = null;
	}


	/**
	 * Purges everything from everything. Scary stuff.
	 * 
	 * @see me.timothy.bots.database.SchemaValidator#purgeSchema()
	 */
	public void purgeAll() {
		/* REVERSE ORDER of validateTableState */
		siteSessionValidator.purgeSchema();
		lastInfoPMValidator.purgeSchema();
		subredditTraditionalListStatusValidator.purgeSchema();
		unbanRequestValidator.purgeSchema();
		traditionalScammerValidator.purgeSchema();
		handledAtTimestampValidator.purgeSchema();
		unbanHistoryValidator.purgeSchema();
		subredditPropagateStatusValidator.purgeSchema();
		subredditModqueueProgressValidator.purgeSchema();
		subscribedHashtagValidator.purgeSchema();
		responseValidator.purgeSchema();
		banHistoryValidator.purgeSchema();
		handledModActionValidator.purgeSchema();
		personValidator.purgeSchema();
		monitoredSubredditValidator.purgeSchema();
		fullnameValidator.purgeSchema();
	}
	
	/**
	 * <p>Validates the tables in the database match what are expected. If the tables
	 * cannot be found, they are created. Throws an error if the tables already exist
	 * but are not in the expected state.</p>
	 * 
	 * @throws IllegalStateException if the tables are in the wrong state
	 * @see me.timothy.bots.database.SchemaValidator#validateSchema()
	 */
	public void validateTableState() {
		fullnameValidator.validateSchema();
		monitoredSubredditValidator.validateSchema();
		personValidator.validateSchema();
		handledModActionValidator.validateSchema();
		banHistoryValidator.validateSchema();
		responseValidator.validateSchema();
		subscribedHashtagValidator.validateSchema();
		subredditModqueueProgressValidator.validateSchema();
		subredditPropagateStatusValidator.validateSchema();
		unbanHistoryValidator.validateSchema();
		handledAtTimestampValidator.validateSchema();
		traditionalScammerValidator.validateSchema();
		unbanRequestValidator.validateSchema();
		subredditTraditionalListStatusValidator.validateSchema();
		lastInfoPMValidator.validateSchema();
		siteSessionValidator.validateSchema();
	}
	
	@Override
	public FullnameMapping getFullnameMapping() {
		return fullnameMapping;
	}
	
	@Override
	public MonitoredSubredditMapping getMonitoredSubredditMapping() {
		return monitoredSubredditMapping;
	}
	
	@Override
	public PersonMapping getPersonMapping() {
		return personMapping;
	}
	
	@Override
	public HandledModActionMapping getHandledModActionMapping() {
		return handledModActionMapping;
	}
	
	@Override
	public BanHistoryMapping getBanHistoryMapping() {
		return banHistoryMapping;
	}
	
	@Override
	public ResponseMapping getResponseMapping() {
		return responseMapping;
	}
	
	@Override
	public SubscribedHashtagMapping getSubscribedHashtagMapping() {
		return subscribedHashtagMapping;
	}
	
	@Override
	public SubredditModqueueProgressMapping getSubredditModqueueProgressMapping() {
		return subredditModqueueProgressMapping;
	}
	
	@Override
	public SubredditPropagateStatusMapping getSubredditPropagateStatusMapping() {
		return subredditPropagateStatusMapping;
	}
	
	@Override
	public UnbanHistoryMapping getUnbanHistoryMapping() {
		return unbanHistoryMapping;
	}
	
	@Override
	public HandledAtTimestampMapping getHandledAtTimestampMapping() {
		return handledAtTimestampMapping;
	}
	
	@Override
	public TraditionalScammerMapping getTraditionalScammerMapping() {
		return traditionalScammerMapping;
	}
	
	@Override
	public UnbanRequestMapping getUnbanRequestMapping() {
		return unbanRequestMapping;
	}
	
	@Override
	public SubredditTraditionalListStatusMapping getSubredditTraditionalListStatusMapping() {
		return subredditTraditionalListStatusMapping;
	}
	
	@Override
	public LastInfoPMMapping getLastInfoPMMapping() {
		return lastInfoPMMapping;
	}
	
	@Override
	public SiteSessionMapping getSiteSessionMapping() {
		return siteSessionMapping;
	}
	
	/**
	 * Adds a fullname to the database
	 * @param id the fullname to add
	 */
	@Override
	public void addFullname(String id) {
		fullnameMapping.save(new Fullname(-1, id));
	}

	/**
	 * Scans the database for ids matching the specified id
	 * 
	 * @param id the id to scan for
	 * @return if the database has that id
	 */
	@Override
	public boolean containsFullname(String id) {
		return fullnameMapping.contains(id);
	}


}
