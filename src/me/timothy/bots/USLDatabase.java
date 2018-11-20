package me.timothy.bots;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.AcceptModeratorInviteRequestMapping;
import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.database.BanHistoryMapping;
import me.timothy.bots.database.FullnameMapping;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.database.LastInfoPMMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.database.ObjectMapping;
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.RegisterAccountRequestMapping;
import me.timothy.bots.database.ResetPasswordRequestMapping;
import me.timothy.bots.database.ResponseMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.database.SiteSessionMapping;
import me.timothy.bots.database.SubredditModqueueProgressMapping;
import me.timothy.bots.database.SubredditPropagateStatusMapping;
import me.timothy.bots.database.SubredditTraditionalListStatusMapping;
import me.timothy.bots.database.SubscribedHashtagMapping;
import me.timothy.bots.database.TemporaryAuthLevelMapping;
import me.timothy.bots.database.TemporaryAuthRequestMapping;
import me.timothy.bots.database.TraditionalScammerMapping;
import me.timothy.bots.database.UnbanHistoryMapping;
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.database.mysql.MysqlAcceptModeratorInviteRequestMapping;
import me.timothy.bots.database.mysql.MysqlActionLogMapping;
import me.timothy.bots.database.mysql.MysqlBanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlFullnameMapping;
import me.timothy.bots.database.mysql.MysqlHandledAtTimestampMapping;
import me.timothy.bots.database.mysql.MysqlHandledModActionMapping;
import me.timothy.bots.database.mysql.MysqlLastInfoPMMapping;
import me.timothy.bots.database.mysql.MysqlMonitoredSubredditMapping;
import me.timothy.bots.database.mysql.MysqlPersonMapping;
import me.timothy.bots.database.mysql.MysqlRegisterAccountRequestMapping;
import me.timothy.bots.database.mysql.MysqlResetPasswordRequestMapping;
import me.timothy.bots.database.mysql.MysqlResponseMapping;
import me.timothy.bots.database.mysql.MysqlSiteSessionMapping;
import me.timothy.bots.database.mysql.MysqlSubredditModqueueProgressMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPropagateStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubredditTraditionalListStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubscribedHashtagMapping;
import me.timothy.bots.database.mysql.MysqlTemporaryAuthLevelMapping;
import me.timothy.bots.database.mysql.MysqlTemporaryAuthRequestMapping;
import me.timothy.bots.database.mysql.MysqlTraditionalScammerMapping;
import me.timothy.bots.database.mysql.MysqlUnbanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUnbanRequestMapping;
import me.timothy.bots.models.AcceptModeratorInviteRequest;
import me.timothy.bots.models.ActionLog;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.Fullname;
import me.timothy.bots.models.HandledAtTimestamp;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.LastInfoPM;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.RegisterAccountRequest;
import me.timothy.bots.models.ResetPasswordRequest;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SiteSession;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubredditPropagateStatus;
import me.timothy.bots.models.SubredditTraditionalListStatus;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TemporaryAuthLevel;
import me.timothy.bots.models.TemporaryAuthRequest;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;

/**
 * The database for the universal scammer list.
 * Uses a MYSql connection and acts as a MappingDatabase.
 * 
 * @author Timothy Moore
 */
public class USLDatabase extends Database implements MappingDatabase {
	private Logger logger;
	private Connection connection;
	
	private List<ObjectMapping<?>> mappings;
	private Map<Class<?>, ObjectMapping<?>> mappingsDict;
	
	
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
		connection.setAutoCommit(true);

		mappings = new ArrayList<>();
		mappingsDict = new HashMap<>();
		addMapping(Fullname.class, new MysqlFullnameMapping(this, connection));
		addMapping(MonitoredSubreddit.class, new MysqlMonitoredSubredditMapping(this, connection));
		addMapping(Person.class, new MysqlPersonMapping(this, connection));
		addMapping(HandledModAction.class, new MysqlHandledModActionMapping(this, connection));
		addMapping(BanHistory.class, new MysqlBanHistoryMapping(this, connection));
		addMapping(Response.class, new MysqlResponseMapping(this, connection));
		addMapping(SubscribedHashtag.class, new MysqlSubscribedHashtagMapping(this, connection));
		addMapping(SubredditModqueueProgress.class, new MysqlSubredditModqueueProgressMapping(this, connection));
		addMapping(SubredditPropagateStatus.class, new MysqlSubredditPropagateStatusMapping(this, connection));
		addMapping(UnbanHistory.class, new MysqlUnbanHistoryMapping(this, connection));
		addMapping(HandledAtTimestamp.class, new MysqlHandledAtTimestampMapping(this, connection));
		addMapping(TraditionalScammer.class, new MysqlTraditionalScammerMapping(this, connection));
		addMapping(UnbanRequest.class, new MysqlUnbanRequestMapping(this, connection));
		addMapping(SubredditTraditionalListStatus.class, new MysqlSubredditTraditionalListStatusMapping(this, connection));
		addMapping(LastInfoPM.class, new MysqlLastInfoPMMapping(this, connection));
		addMapping(SiteSession.class, new MysqlSiteSessionMapping(this, connection));
		addMapping(RegisterAccountRequest.class, new MysqlRegisterAccountRequestMapping(this, connection));
		addMapping(ResetPasswordRequest.class, new MysqlResetPasswordRequestMapping(this, connection));
		addMapping(ActionLog.class, new MysqlActionLogMapping(this, connection));
		addMapping(TemporaryAuthLevel.class, new MysqlTemporaryAuthLevelMapping(this, connection));
		addMapping(TemporaryAuthRequest.class, new MysqlTemporaryAuthRequestMapping(this, connection));
		addMapping(AcceptModeratorInviteRequest.class, new MysqlAcceptModeratorInviteRequestMapping(this, connection));
	}

	private <A> void addMapping(Class<A> cl, ObjectMapping<A> mapping) {
		mappings.add(mapping);
		mappingsDict.put(cl, mapping);
	}

	/**
	 * Purges everything from everything. Scary stuff.
	 * 
	 * @see me.timothy.bots.database.SchemaValidator#purgeSchema()
	 */
	public void purgeAll() {
		for(int i = mappings.size() - 1; i >= 0; i--) {
			((SchemaValidator)mappings.get(i)).purgeSchema();
		}
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
		for(int i = 0; i < mappings.size(); i++) {
			((SchemaValidator)mappings.get(i)).validateSchema();
		}
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
		
		mappings = null;
		mappingsDict = null;
	}
	
	@Override 
	public FullnameMapping getFullnameMapping() {
		return (FullnameMapping) mappingsDict.get(Fullname.class);
	}
	
	@Override
	public MonitoredSubredditMapping getMonitoredSubredditMapping() {
		return (MonitoredSubredditMapping) mappingsDict.get(MonitoredSubreddit.class);
	}
	
	@Override
	public PersonMapping getPersonMapping() {
		return (PersonMapping) mappingsDict.get(Person.class);
	}
	
	@Override
	public HandledModActionMapping getHandledModActionMapping() {
		return (HandledModActionMapping) mappingsDict.get(HandledModAction.class);
	}
	
	@Override
	public BanHistoryMapping getBanHistoryMapping() {
		return (BanHistoryMapping) mappingsDict.get(BanHistory.class);
	}
	
	@Override
	public ResponseMapping getResponseMapping() {
		return (ResponseMapping) mappingsDict.get(Response.class);
	}
	
	@Override
	public SubscribedHashtagMapping getSubscribedHashtagMapping() {
		return (SubscribedHashtagMapping) mappingsDict.get(SubscribedHashtag.class);
	}
	
	@Override
	public SubredditModqueueProgressMapping getSubredditModqueueProgressMapping() {
		return (SubredditModqueueProgressMapping) mappingsDict.get(SubredditModqueueProgress.class);
	}
	
	@Override
	public SubredditPropagateStatusMapping getSubredditPropagateStatusMapping() {
		return (SubredditPropagateStatusMapping) mappingsDict.get(SubredditPropagateStatus.class);
	}
	
	@Override
	public UnbanHistoryMapping getUnbanHistoryMapping() {
		return (UnbanHistoryMapping) mappingsDict.get(UnbanHistory.class);
	}
	
	@Override
	public HandledAtTimestampMapping getHandledAtTimestampMapping() {
		return (HandledAtTimestampMapping) mappingsDict.get(HandledAtTimestamp.class);
	}
	
	@Override
	public TraditionalScammerMapping getTraditionalScammerMapping() {
		return (TraditionalScammerMapping) mappingsDict.get(TraditionalScammer.class);
	}
	
	@Override
	public UnbanRequestMapping getUnbanRequestMapping() {
		return (UnbanRequestMapping) mappingsDict.get(UnbanRequest.class);
	}
	
	@Override
	public SubredditTraditionalListStatusMapping getSubredditTraditionalListStatusMapping() {
		return (SubredditTraditionalListStatusMapping) mappingsDict.get(SubredditTraditionalListStatus.class);
	}
	
	@Override
	public LastInfoPMMapping getLastInfoPMMapping() {
		return (LastInfoPMMapping) mappingsDict.get(LastInfoPM.class);
	}
	
	@Override
	public SiteSessionMapping getSiteSessionMapping() {
		return (SiteSessionMapping) mappingsDict.get(SiteSession.class);
	}
	
	@Override
	public RegisterAccountRequestMapping getRegisterAccountRequestMapping() {
		return (RegisterAccountRequestMapping) mappingsDict.get(RegisterAccountRequest.class);
	}
	
	@Override
	public ResetPasswordRequestMapping getResetPasswordRequestMapping() {
		return (ResetPasswordRequestMapping) mappingsDict.get(ResetPasswordRequest.class);
	}
	
	@Override
	public ActionLogMapping getActionLogMapping() {
		return (ActionLogMapping) mappingsDict.get(ActionLog.class);
	}
	
	@Override
	public TemporaryAuthLevelMapping getTemporaryAuthLevelMapping() {
		return (TemporaryAuthLevelMapping) mappingsDict.get(TemporaryAuthLevel.class);
	}
	
	@Override
	public TemporaryAuthRequestMapping getTemporaryAuthRequestMapping() {
		return (TemporaryAuthRequestMapping) mappingsDict.get(TemporaryAuthRequest.class);
	}
	
	@Override
	public AcceptModeratorInviteRequestMapping getAcceptModeratorInviteRequestMapping() {
		return (AcceptModeratorInviteRequestMapping) mappingsDict.get(AcceptModeratorInviteRequest.class);
	}
	
	/**
	 * Adds a fullname to the database
	 * @param id the fullname to add
	 */
	@Override
	public void addFullname(String id) {
		getFullnameMapping().save(new Fullname(-1, id));
	}

	/**
	 * Scans the database for ids matching the specified id
	 * 
	 * @param id the id to scan for
	 * @return if the database has that id
	 */
	@Override
	public boolean containsFullname(String id) {
		return getFullnameMapping().contains(id);
	}


}
