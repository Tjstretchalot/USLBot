package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.models.HandledModAction;

public class MysqlHandledModActionMapping extends MysqlObjectWithIDMapping<HandledModAction> implements HandledModActionMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlHandledModActionMapping(USLDatabase database, Connection connection) {
		super(database, connection, "handled_modactions", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.VARCHAR, "modaction_id"),
				new MysqlColumn(Types.TIMESTAMP, "occurred_at"));
	}

	@Override
	public void save(HandledModAction a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.occurredAt != null) { a.occurredAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, modaction_id=?,"
						+ " occurred_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, modaction_id,"
						+ " occurred_at) VALUES (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setString(counter++, a.modActionID);
			statement.setTimestamp(counter++, a.occurredAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys from " + table);
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
	protected HandledModAction fetchFromSet(ResultSet set) throws SQLException {
		return new HandledModAction(set.getInt("id"), set.getInt("monitored_subreddit_id"),
				set.getString("modaction_id"), set.getTimestamp("occurred_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "modaction_id VARCHAR(50) NOT NULL, "
				+ "occurred_at TIMESTAMP NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(modaction_id), "
				+ "INDEX ind_handmodactions_monsub_id (monitored_subreddit_id), "
				+ "FOREIGN KEY(monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
				+ ")");
		statement.close();
	}

	@Override
	public HandledModAction fetchByModActionID(String modActionID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE modaction_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.VARCHAR, modActionID)),
				fetchFromSetFunction());
	}

	@Override
	public List<HandledModAction> fetchByTimestamp(Timestamp timestamp) {
		return fetchByAction("SELECT * FROM " + table + " WHERE occurred_at=?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.TIMESTAMP, timestamp)),
				fetchListFromSetFunction());
	}
	

	@Override
	public List<HandledModAction> fetchLatest(Timestamp after, Timestamp before, int num) {
		return fetchByAction("SELECT * FROM " + table + " WHERE occurred_at>? AND occurred_at<? ORDER BY occurred_at ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.TIMESTAMP, after),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.INTEGER, num)),
				fetchListFromSetFunction());
	}
	
	@Override
	public List<HandledModAction> fetchByTimestampAndSubreddit(Timestamp timestamp, int subredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=? AND occurred_at=?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, subredditID),
						new MysqlTypeValueTuple(Types.TIMESTAMP, timestamp)),
				fetchListFromSetFunction());
	}

	@Override
	public List<HandledModAction> fetchLatestForSubreddit(int monitoredSubredditID, Timestamp after, Timestamp before, int num) {
		return fetchByAction("SELECT * FROM " + table + " WHERE (monitored_subreddit_id=? AND occurred_at>=? AND (? IS NULL OR occurred_at<?)) ORDER BY occurred_at ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID),
						new MysqlTypeValueTuple(Types.TIMESTAMP, after),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.INTEGER, num)),
				fetchListFromSetFunction());
	}

}
