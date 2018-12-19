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
import me.timothy.bots.database.SubscribedHashtagMapping;
import me.timothy.bots.models.SubscribedHashtag;

public class MysqlSubscribedHashtagMapping extends MysqlObjectWithIDMapping<SubscribedHashtag> implements SubscribedHashtagMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlSubscribedHashtagMapping(USLDatabase database, Connection connection) {
		super(database, connection, "subscribed_hashtags", new MysqlColumn[] {
			new MysqlColumn(Types.INTEGER, "id", true),	
			new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
			new MysqlColumn(Types.INTEGER, "hashtag_id"),
			new MysqlColumn(Types.TIMESTAMP, "created_at"),
			new MysqlColumn(Types.TIMESTAMP, "deleted_at")
		});
	}

	@Override
	public void save(SubscribedHashtag a) throws IllegalArgumentException {
		if(!a.isValid()) {
			throw new RuntimeException(a + " is not valid!");
		}
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.deletedAt != null) { a.deletedAt.setNanos(0); }
		
		PreparedStatement statement;
		try {
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, "
						+ "hashtag_id=?, created_at=?, deleted_at=? WHERE id=?"); 
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + 
						" (monitored_subreddit_id, hashtag_id, created_at, deleted_at) VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setInt(counter++, a.hashtagID);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.deletedAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new RuntimeException("expected generated keys when inserting into " + table);
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
	public List<SubscribedHashtag> fetchForSubreddit(int monitoredSubredditID, boolean deleted) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ").append(table).append(" WHERE monitored_subreddit_id=?");
		if(!deleted) {
			sql.append(" AND deleted_at IS NULL");
		}
		return fetchByAction(sql.toString(), new PreparedStatementSetVars() {

			@Override
			public void setVars(PreparedStatement statement) throws SQLException {
				int counter = 1;
				statement.setInt(counter++, monitoredSubredditID);
			}
			
		}, fetchListFromSetFunction());
	}
	
	/**
	 * Fetch the subscribedhashtag in the current row of the set.
	 * 
	 * @param set the set
	 * @return the subscribedhashtag in the current row
	 * @throws SQLException if one occurs
	 */
	@Override
	protected SubscribedHashtag fetchFromSet(ResultSet set) throws SQLException {
		return new SubscribedHashtag(set.getInt("id"), set.getInt("monitored_subreddit_id"), set.getInt("hashtag_id"),
				set.getTimestamp("created_at"), set.getTimestamp("deleted_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "hashtag_id INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "deleted_at TIMESTAMP NULL DEFAULT NULL, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_subschashtag_monsub_id (monitored_subreddit_id), "
				+ "INDEX ind_subschashtag_hashtag_id (hashtag_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id), "
				+ "FOREIGN KEY (hashtag_id) REFERENCES hashtags(id)"
				+ ")");
		statement.close();
	}

}
