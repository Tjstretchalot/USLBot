/**
 * 
 */
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
import me.timothy.bots.database.SubredditPropagateStatusMapping;
import me.timothy.bots.models.SubredditPropagateStatus;

public class MysqlSubredditPropagateStatusMapping extends MysqlObjectWithIDMapping<SubredditPropagateStatus>
		implements SubredditPropagateStatusMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlSubredditPropagateStatusMapping(USLDatabase database, Connection connection) {
		super(database, connection, "subreddit_propagate_status", new MysqlColumn[] {
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "major_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "minor_subreddit_id"),
				new MysqlColumn(Types.TIMESTAMP, "latest_propagated_action_time"),
				new MysqlColumn(Types.TIMESTAMP, "updated_at")
		});
	}

	@Override
	public void save(SubredditPropagateStatus a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET major_subreddit_id=?, minor_subreddit_id=?, latest_propagated_action_time=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (major_subreddit_id, minor_subreddit_id, latest_propagated_action_time) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.majorSubredditID);
			statement.setInt(counter++, a.minorSubredditID);
			statement.setTimestamp(counter++, a.latestPropagatedActionTime);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new IllegalStateException("expected " + table + " to return generated keys");
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
				throw new IllegalStateException("something went horribly wrong when fetching updated at from " + table);
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
	public SubredditPropagateStatus fetchForSubredditPair(int majorSubredditID, int minorSubredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE major_subreddit_id=? AND minor_subreddit_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, majorSubredditID),
						new MysqlTypeValueTuple(Types.INTEGER, minorSubredditID)),
				fetchFromSetFunction());
	}
	
	/**
	 * Fetch the SubredditPropagateStatus in the current row of the set
	 * 
	 * @param set the set
	 * @return the SubredditPropagateStatus in the current row of the set
	 * @throws SQLException if one occurs
	 */
	@Override
	protected SubredditPropagateStatus fetchFromSet(ResultSet set) throws SQLException {
		return new SubredditPropagateStatus(set.getInt("id"), set.getInt("major_subreddit_id"),
				set.getInt("minor_subreddit_id"), set.getTimestamp("latest_propagated_action_time"), 
				set.getTimestamp("updated_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
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
	}

}
