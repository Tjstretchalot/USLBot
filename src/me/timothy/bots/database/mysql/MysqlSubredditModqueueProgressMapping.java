package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
			new MysqlColumn(Types.VARCHAR, "latest_modaction_id"),
			new MysqlColumn(Types.VARCHAR, "newest_modaction_id"),
			new MysqlColumn(Types.TIMESTAMP, "updated_at"),
			new MysqlColumn(Types.TIMESTAMP, "last_time_had_full_history")
		});
	}

	@Override
	public void save(SubredditModqueueProgress a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.lastTimeHadFullHistory != null) { a.lastTimeHadFullHistory.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, search_forward=?, latest_modaction_id=?, newest_modaction_id=?, last_time_had_full_history=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, search_forward, latest_modaction_id, newest_modaction_id, last_time_had_full_history) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setBoolean(counter++, a.searchForward);
			statement.setString(counter++, a.latestModActionID);
			statement.setString(counter++, a.newestModActionID);
			statement.setTimestamp(counter++, a.lastTimeHadFullHistory);
			
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
	
	@Deprecated
	public boolean anySearchingForward() {
		return fetchByAction("SELECT * FROM " + table + " WHERE search_forward=1 LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(),
				resultHasRowFunction());
	}

	@Deprecated
	public boolean anyNullLastFullHistoryTime() {
		return fetchByAction("SELECT * FROM " + table + " WHERE last_time_had_full_history IS NULL LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(),
				resultHasRowFunction());
	}

	@Deprecated
	public Timestamp fetchLeastRecentFullHistoryTime() {
		return fetchByAction("SELECT last_time_had_full_history FROM " + table + " WHERE last_time_had_full_history IS NOT NULL ORDER BY last_time_had_full_history ASC LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(),
				new PreparedStatementFetchResult<Timestamp>() {
					@Override
					public Timestamp fetchResult(ResultSet set) throws SQLException {
						if(!set.next())
							return null;
						return set.getTimestamp(1);
					}
				});
	}

	private PreparedStatementFetchResult<Boolean> resultHasRowFunction() {
		return new PreparedStatementFetchResult<Boolean>() {
			@Override
			public Boolean fetchResult(ResultSet set) throws SQLException {
				return set.next();
			}
		};
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
		return new SubredditModqueueProgress(set.getInt("id"), set.getInt("monitored_subreddit_id"), set.getBoolean("search_forward"), 
				set.getString("latest_modaction_id"), set.getString("newest_modaction_id"), set.getTimestamp("updated_at"), set.getTimestamp("last_time_had_full_history"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "search_forward TINYINT(1) NOT NULL, "
				+ "latest_modaction_id VARCHAR(50) NULL, "
				+ "newest_modaction_id VARCHAR(50) NULL, "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "last_time_had_full_history TIMESTAMP NULL DEFAULT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(monitored_subreddit_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
				+ ")");
		statement.close();
	}

}
