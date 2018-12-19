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
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "action_id"),
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
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, action_id=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, action_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setInt(counter++, a.actionID);
			
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
	public SubredditPropagateStatus fetchForSubreddit(int monitoredSubredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=?",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID)),
				fetchFromSetFunction());
	}

	@Override
	public SubredditPropagateStatus fetchOrCreateForSubreddit(int monitoredSubredditID) {
		SubredditPropagateStatus res = fetchForSubreddit(monitoredSubredditID);
		if(res != null)
			return res;
		
		res = new SubredditPropagateStatus(-1, monitoredSubredditID, 0, null);
		save(res);
		return res;
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
		return new SubredditPropagateStatus(set.getInt("id"), set.getInt("monitored_subreddit_id"),
				set.getInt("action_id"), set.getTimestamp("updated_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + "("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "action_id INT NOT NULL, "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY (id), "
				+ "UNIQUE KEY (monitored_subreddit_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
				+ ")");
		statement.close();
	}
}
