package me.timothy.bots;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.AcceptModeratorInviteRequestMapping;
import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.database.BanHistoryMapping;
import me.timothy.bots.database.DeletedPersonMapping;
import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.database.FullnameMapping;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.database.HashtagMapping;
import me.timothy.bots.database.LastInfoPMMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.MonitoredSubredditAltModMailMapping;
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.database.ObjectMapping;
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.PropagatorSettingMapping;
import me.timothy.bots.database.RedditToMeaningProgressMapping;
import me.timothy.bots.database.RegisterAccountRequestMapping;
import me.timothy.bots.database.RepropagationRequestMapping;
import me.timothy.bots.database.ResetPasswordRequestMapping;
import me.timothy.bots.database.ResponseMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.database.SiteSessionMapping;
import me.timothy.bots.database.SubredditModqueueProgressMapping;
import me.timothy.bots.database.SubredditPersonBannedReleaseMapping;
import me.timothy.bots.database.SubredditPropagateStatusMapping;
import me.timothy.bots.database.SubredditTraditionalListStatusMapping;
import me.timothy.bots.database.SubscribedHashtagMapping;
import me.timothy.bots.database.TemporaryAuthLevelMapping;
import me.timothy.bots.database.TemporaryAuthRequestMapping;
import me.timothy.bots.database.TraditionalScammerMapping;
import me.timothy.bots.database.USLActionBanHistoryMapping;
import me.timothy.bots.database.USLActionHashtagMapping;
import me.timothy.bots.database.USLActionMapping;
import me.timothy.bots.database.USLActionUnbanHistoryMapping;
import me.timothy.bots.database.UnbanHistoryMapping;
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.database.custom.CustomDirtyPersonMapping;
import me.timothy.bots.database.custom.CustomHandledAtTimestampMapping;
import me.timothy.bots.database.custom.CustomMapping;
import me.timothy.bots.database.custom.CustomRedditToMeaningProgressMapping;
import me.timothy.bots.database.mysql.MysqlAcceptModeratorInviteRequestMapping;
import me.timothy.bots.database.mysql.MysqlActionLogMapping;
import me.timothy.bots.database.mysql.MysqlBanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlDeletedPersonMapping;
import me.timothy.bots.database.mysql.MysqlFullnameMapping;
import me.timothy.bots.database.mysql.MysqlHandledModActionMapping;
import me.timothy.bots.database.mysql.MysqlHashtagMapping;
import me.timothy.bots.database.mysql.MysqlLastInfoPMMapping;
import me.timothy.bots.database.mysql.MysqlMonitoredSubredditAltModMailMapping;
import me.timothy.bots.database.mysql.MysqlMonitoredSubredditMapping;
import me.timothy.bots.database.mysql.MysqlObjectMapping;
import me.timothy.bots.database.mysql.MysqlPersonMapping;
import me.timothy.bots.database.mysql.MysqlPropagatorSettingMapping;
import me.timothy.bots.database.mysql.MysqlRegisterAccountRequestMapping;
import me.timothy.bots.database.mysql.MysqlRepropagationRequestMapping;
import me.timothy.bots.database.mysql.MysqlResetPasswordRequestMapping;
import me.timothy.bots.database.mysql.MysqlResponseMapping;
import me.timothy.bots.database.mysql.MysqlSiteSessionMapping;
import me.timothy.bots.database.mysql.MysqlSubredditModqueueProgressMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPersonBannedReleaseMapping;
import me.timothy.bots.database.mysql.MysqlSubredditPropagateStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubredditTraditionalListStatusMapping;
import me.timothy.bots.database.mysql.MysqlSubscribedHashtagMapping;
import me.timothy.bots.database.mysql.MysqlTemporaryAuthLevelMapping;
import me.timothy.bots.database.mysql.MysqlTemporaryAuthRequestMapping;
import me.timothy.bots.database.mysql.MysqlTraditionalScammerMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionBanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionHashtagMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionMapping;
import me.timothy.bots.database.mysql.MysqlUSLActionUnbanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUnbanHistoryMapping;
import me.timothy.bots.database.mysql.MysqlUnbanRequestMapping;
import me.timothy.bots.models.AcceptModeratorInviteRequest;
import me.timothy.bots.models.ActionLog;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.DeletedPerson;
import me.timothy.bots.models.DirtyPerson;
import me.timothy.bots.models.Fullname;
import me.timothy.bots.models.HandledAtTimestamp;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.LastInfoPM;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.MonitoredSubredditAltModMail;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.PropagatorSetting;
import me.timothy.bots.models.RedditToMeaningProgress;
import me.timothy.bots.models.RegisterAccountRequest;
import me.timothy.bots.models.RepropagationRequest;
import me.timothy.bots.models.ResetPasswordRequest;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SiteSession;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubredditPersonBannedRelease;
import me.timothy.bots.models.SubredditPropagateStatus;
import me.timothy.bots.models.SubredditTraditionalListStatus;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TemporaryAuthLevel;
import me.timothy.bots.models.TemporaryAuthRequest;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
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
	
	private List<ObjectMapping<?>> mysqlMappings;
	private List<CustomMapping<?>> customMappings;
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
	 * @param flatFileFolder
	 * 			  the folder where flat files should be saved            
	 * @throws SQLException
	 *             if a sql-related exception occurs
	 */
	public void connect(String username, String password, String url, File flatFileFolder) 
			throws SQLException {
		if (connection != null) {
			disconnect();
		}
		
		connection = DriverManager.getConnection(url, username, password);
		connection.setAutoCommit(true);

		mysqlMappings = new ArrayList<>();
		customMappings = new ArrayList<>();
		mappingsDict = new HashMap<>();
		addMapping(Fullname.class, new MysqlFullnameMapping(this, connection));
		addMapping(MonitoredSubreddit.class, new MysqlMonitoredSubredditMapping(this, connection));
		addMapping(MonitoredSubredditAltModMail.class, new MysqlMonitoredSubredditAltModMailMapping(this, connection));
		addMapping(Person.class, new MysqlPersonMapping(this, connection));
		addMapping(DeletedPerson.class, new MysqlDeletedPersonMapping(this, connection));
		addMapping(HandledModAction.class, new MysqlHandledModActionMapping(this, connection));
		addMapping(BanHistory.class, new MysqlBanHistoryMapping(this, connection));
		addMapping(Response.class, new MysqlResponseMapping(this, connection));
		addMapping(Hashtag.class, new MysqlHashtagMapping(this, connection));
		addMapping(SubscribedHashtag.class, new MysqlSubscribedHashtagMapping(this, connection));
		addMapping(USLAction.class, new MysqlUSLActionMapping(this, connection));
		addMapping(USLActionBanHistory.class, new MysqlUSLActionBanHistoryMapping(this, connection));
		addMapping(USLActionHashtag.class, new MysqlUSLActionHashtagMapping(this, connection));
		addMapping(USLActionUnbanHistory.class, new MysqlUSLActionUnbanHistoryMapping(this, connection));
		addMapping(SubredditModqueueProgress.class, new MysqlSubredditModqueueProgressMapping(this, connection));
		addMapping(SubredditPropagateStatus.class, new MysqlSubredditPropagateStatusMapping(this, connection));
		addMapping(UnbanHistory.class, new MysqlUnbanHistoryMapping(this, connection));
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
		addMapping(PropagatorSetting.class, new MysqlPropagatorSettingMapping(this, connection));
		addMapping(RepropagationRequest.class, new MysqlRepropagationRequestMapping(this, connection));
		addMapping(SubredditPersonBannedRelease.class, new MysqlSubredditPersonBannedReleaseMapping(this, connection));
		
		addCustomMapping(HandledAtTimestamp.class, new CustomHandledAtTimestampMapping(this, Paths.get(flatFileFolder.getPath(), "handled_at_timestamps.dat").toFile()));
		addCustomMapping(DirtyPerson.class, new CustomDirtyPersonMapping(Paths.get(flatFileFolder.getPath(), "dirty_persons.dat").toFile()));
		addCustomMapping(RedditToMeaningProgress.class, new CustomRedditToMeaningProgressMapping(Paths.get(flatFileFolder.getPath(), "reddit_to_meaning.dat").toFile()));
	}
	
	/**
	 * Get the raw connection that this is using to connect. This should *only* be used for
	 * one-time conversion tools.
	 * @return The underlying connection
	 */
	public Connection getConnection() {
		return connection;
	}

	private <A> void addMapping(Class<A> cl, ObjectMapping<A> mapping) {
		mysqlMappings.add(mapping);
		mappingsDict.put(cl, mapping);
	}
	
	private <A> void addCustomMapping(Class<A> cl, CustomMapping<A> mapping) {
		mapping.recover();
		customMappings.add(mapping);
		mappingsDict.put(cl, mapping);
	}

	/**
	 * Purges everything from everything. Scary stuff.
	 * 
	 * @see me.timothy.bots.database.SchemaValidator#purgeSchema()
	 */
	public void purgeAll() {
		purgeCustom();
		
		for(int i = mysqlMappings.size() - 1; i >= 0; i--) {
			((SchemaValidator)mysqlMappings.get(i)).purgeSchema();
		}
	}
	
	/**
	 * Truncates everything from mysql. Scary stuff.
	 */
	public void truncateMySQL() {
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET FOREIGN_KEY_CHECKS = 0");
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		for(int i = mysqlMappings.size() - 1; i >= 0; i--) {
			((MysqlObjectMapping<?>)mysqlMappings.get(i)).truncate();
		}

		try (Statement statement = connection.createStatement()) {
			statement.execute("SET FOREIGN_KEY_CHECKS = 1");
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Purge everything from the custom mappings. Scary stuff
	 * 
	 * @see me.timothy.bots.database.SchemaValidator#purgeSchema()
	 */
	public void purgeCustom() {
		for(int i = customMappings.size() - 1; i >= 0; i--) {
			((SchemaValidator)customMappings.get(i)).purgeSchema();
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
		for(int i = 0; i < mysqlMappings.size(); i++) {
			((SchemaValidator)mysqlMappings.get(i)).validateSchema();
		}
		for(CustomMapping<?> map : customMappings) {
			((SchemaValidator)map).validateSchema();
		}
	}
	
	/**
	 * Ensures the database is disconnected and will not return invalid
	 * mappings (instead they will return null until the next connect)
	 */
	public void disconnect() {
		if(connection == null)
			return;
		
		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			logger.throwing(e);
		}
		
		for(CustomMapping<?> cm : customMappings) {
			cm.close();
		}
		
		mysqlMappings = null;
		customMappings = null;
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
	public HashtagMapping getHashtagMapping() {
		return (HashtagMapping) mappingsDict.get(Hashtag.class);
	}
	
	@Override
	public SubscribedHashtagMapping getSubscribedHashtagMapping() {
		return (SubscribedHashtagMapping) mappingsDict.get(SubscribedHashtag.class);
	}
	
	@Override
	public USLActionMapping getUSLActionMapping() {
		return (USLActionMapping) mappingsDict.get(USLAction.class);
	}
	
	@Override
	public USLActionBanHistoryMapping getUSLActionBanHistoryMapping() {
		return (USLActionBanHistoryMapping) mappingsDict.get(USLActionBanHistory.class);
	}
	
	@Override
	public USLActionHashtagMapping getUSLActionHashtagMapping() {
		return (USLActionHashtagMapping) mappingsDict.get(USLActionHashtag.class);
	}

	@Override
	public USLActionUnbanHistoryMapping getUSLActionUnbanHistoryMapping() {
		return (USLActionUnbanHistoryMapping) mappingsDict.get(USLActionUnbanHistory.class);
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
	
	@Override
	public DirtyPersonMapping getDirtyPersonMapping() {
		return (DirtyPersonMapping) mappingsDict.get(DirtyPerson.class);
	}
	
	@Override
	public RedditToMeaningProgressMapping getRedditToMeaningProgressMapping() {
		return (RedditToMeaningProgressMapping) mappingsDict.get(RedditToMeaningProgress.class);
	}
	
	@Override
	public DeletedPersonMapping getDeletedPersonMapping() {
		return (DeletedPersonMapping) mappingsDict.get(DeletedPerson.class);
	}
	
	@Override
	public PropagatorSettingMapping getPropagatorSettingMapping() {
		return (PropagatorSettingMapping) mappingsDict.get(PropagatorSetting.class);
	}
	
	@Override
	public MonitoredSubredditAltModMailMapping getMonitoredSubredditAltModMailMapping() {
		return (MonitoredSubredditAltModMailMapping) mappingsDict.get(MonitoredSubredditAltModMail.class);
	}

	@Override
	public RepropagationRequestMapping getRepropagationRequestMapping() {
		return (RepropagationRequestMapping) mappingsDict.get(RepropagationRequest.class);
	}
	
	@Override
	public SubredditPersonBannedReleaseMapping getSubredditPersonBannedReleaseMapping() {
		return (SubredditPersonBannedReleaseMapping) mappingsDict.get(SubredditPersonBannedRelease.class);
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
