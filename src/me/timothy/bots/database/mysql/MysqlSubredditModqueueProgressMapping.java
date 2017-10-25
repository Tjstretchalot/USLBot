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
import me.timothy.bots.database.SubredditModqueueProgressMapping;
import me.timothy.bots.models.SubredditModqueueProgress;

public class MysqlSubredditModqueueProgressMapping extends MysqlObjectWithIDMapping<SubredditModqueueProgress>
		implements SubredditModqueueProgressMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlSubredditModqueueProgressMapping(USLDatabase database, Connection connection) {
		super(database, connection, "subreddit_modqueue_progress", new MysqlColumn[] {
			new MysqlColumn(Types.INTEGER, "id", true),
			new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
			new MysqlColumn(Types.BIT, "search_forward"),
			new MysqlColumn(Types.INTEGER, "latest_handled_modaction_id"),
			new MysqlColumn(Types.INTEGER, "newest_handled_modaction_id"),
			new MysqlColumn(Types.TIMESTAMP, "updated_at")
		});
	}

	@Override
	public void save(SubredditModqueueProgress a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, search_forward=?, latest_handled_modaction_id=?, newest_handled_modaction_id=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, search_forward, latest_handled_modaction_id, newest_handled_modaction_id) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setBoolean(counter++, a.searchForward);
			if(a.latestHandledModActionID == null)
				statement.setNull(counter++, Types.INTEGER);
			else
				statement.setInt(counter++, a.latestHandledModActionID);
			if(a.newestHandledModActionID == null)
				statement.setNull(counter++, Types.INTEGER);
			else
				statement.setInt(counter++, a.newestHandledModActionID);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.executeUpdate();
			}else {
				statement.executeUpdate();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new IllegalStateException("expected generated keys from " + table);
				}
				
				a.id = keys.getInt(1);
				keys.close();
			}
			
			statement.close();
			statement = connection.prepareStatement("SELECT updated_at FROM " + table + " WHERE id=?");
			counter = 1;
			statement.setInt(counter++, a.id);
			ResultSet results = statement.executeQuery();
			if(!results.next()) {
				results.close();
				statement.close();
				throw new IllegalStateException("something went horribly wrong retrieving updated_at from " + table);
			}
			a.updatedAt = results.getTimestamp(1);
			results.close();
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public SubredditModqueueProgress fetchForSubreddit(int monitoredSubredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID)),
				fetchFromSetFunction());
	}
	
	/**
	 * Fetch the SubredditModqueueProgress in the current row of the result set 
	 * 
	 * @param set the result set
	 * @return the SubredditModqueueProgress in the current row
	 * @throws SQLException if one occurs
	 */
	@Override
	protected SubredditModqueueProgress fetchFromSet(ResultSet set) throws SQLException {
		Integer latestBanHistoryID = set.getInt("latest_handled_modaction_id");
		if(set.wasNull())
			latestBanHistoryID = null;
		Integer newestBanHistoryID = set.getInt("newest_handled_modaction_id");
		if(set.wasNull())
			newestBanHistoryID = null;
		
		return new SubredditModqueueProgress(set.getInt("id"), set.getInt("monitored_subreddit_id"), set.getBoolean("search_forward"), 
				latestBanHistoryID, newestBanHistoryID, set.getTimestamp("updated_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "search_forward TINYINT(1) NOT NULL, "
				+ "latest_handled_modaction_id INT NULL, "
				+ "newest_handled_modaction_id INT NULL, "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(monitored_subreddit_id), "
				+ "INDEX ind_subrmodqueprog_latestbh_id (latest_handled_modaction_id), "
				+ "INDEX ind_subrmodqueprog_newestbh_id (newest_handled_modaction_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id), "
				+ "FOREIGN KEY (latest_handled_modaction_id) REFERENCES handled_modactions(id), "
				+ "FOREIGN KEY (newest_handled_modaction_id) REFERENCES handled_modactions(id)"
				+ ")");
		statement.close();
	}

}
