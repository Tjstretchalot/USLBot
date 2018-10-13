package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.SubredditTraditionalListStatusMapping;
import me.timothy.bots.models.SubredditTraditionalListStatus;

public class MysqlSubredditTraditionalListStatusMapping extends MysqlObjectWithIDMapping<SubredditTraditionalListStatus> implements SubredditTraditionalListStatusMapping
{
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlSubredditTraditionalListStatusMapping(USLDatabase database, Connection connection) {
		super(database, connection, "subreddit_tradlist_status", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "last_handled_id"),
				new MysqlColumn(Types.TIMESTAMP, "last_handled_at"));
	}

	@Override
	public void save(SubredditTraditionalListStatus a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.lastHandledAt != null) { a.lastHandledAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, last_handled_id=?, last_handled_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, last_handled_id, last_handled_at) VALUES (?, ?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			
			if(a.lastHandledID != null)
				statement.setInt(counter++, a.lastHandledID);
			else
				statement.setNull(counter++, Types.INTEGER);
			
			statement.setTimestamp(counter++, a.lastHandledAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) { 
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys for " + table);
				}
				
				a.id = keys.getInt(1);
				keys.close();
			}
			
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public SubredditTraditionalListStatus fetchBySubredditID(int monitoredSubredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=?",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID)), 
				fetchFromSetFunction());
	}

	@Override
	protected SubredditTraditionalListStatus fetchFromSet(ResultSet set) throws SQLException {
		Integer lastHandledID = set.getInt("last_handled_id");
		if(set.wasNull())
			lastHandledID = null;
		
		return new SubredditTraditionalListStatus(set.getInt("id"), set.getInt("monitored_subreddit_id"), 
				lastHandledID, set.getTimestamp("last_handled_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "last_handled_id INT NULL DEFAULT NULL, "
				+ "last_handled_at TIMESTAMP NULL DEFAULT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(monitored_subreddit_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
				+ ")");
		statement.close();
	}
}
