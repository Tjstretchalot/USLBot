package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.models.HandledAtTimestamp;

public class MysqlHandledAtTimestampMapping extends MysqlObjectMapping<HandledAtTimestamp> implements HandledAtTimestampMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlHandledAtTimestampMapping(USLDatabase database, Connection connection) {
		super(database, connection, "handled_at_timestamps", 
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "handled_modaction_id"));
	}

	@Override
	public void save(HandledAtTimestamp a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		try {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, "
					+ "handled_modaction_id) VALUES (?, ?)");
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setInt(counter++, a.handledModActionID);
			
			statement.executeUpdate();
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<HandledAtTimestamp> fetchByMonitoredSubredditID(int monitoredSubredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=?", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID)),
				fetchListFromSetFunction());
	}

	@Override
	public void deleteByMonitoredSubredditID(int monitoredSubredditID) {

		try {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE monitored_subreddit_id=?");
			int counter = 1;
			statement.setInt(counter++, monitoredSubredditID);
			
			statement.executeUpdate();
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected HandledAtTimestamp fetchFromSet(ResultSet set) throws SQLException {
		return new HandledAtTimestamp(set.getInt("monitored_subreddit_id"), set.getInt("handled_modaction_id"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "handled_modaction_id INT NOT NULL, "
				+ "INDEX ind_handattime_monsub_id (monitored_subreddit_id), "
				+ "INDEX ind_handattime_modact_id (handled_modaction_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id), "
				+ "FOREIGN KEY (handled_modaction_id) REFERENCES handled_modactions(id)"
				+ ")");
		statement.close();
	}

}