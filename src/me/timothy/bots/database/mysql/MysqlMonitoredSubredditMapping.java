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
import me.timothy.bots.database.MonitoredSubredditMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.MonitoredSubreddit;

public class MysqlMonitoredSubredditMapping extends MysqlObjectWithIDMapping<MonitoredSubreddit> 
	implements MonitoredSubredditMapping, SchemaValidator {
	private static Logger logger = LogManager.getLogger();

	public MysqlMonitoredSubredditMapping(USLDatabase database, Connection connection) {
		super(database, connection, "monitored_subreddits", new MysqlColumn[] {
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "subreddit"),
				new MysqlColumn(Types.BIT, "silent"),
				new MysqlColumn(Types.BIT, "read_only"),
				new MysqlColumn(Types.BIT, "write_only")
		});
	}

	@Override
	public void save(MonitoredSubreddit monitoredSubreddit) throws IllegalArgumentException {
		try {
			PreparedStatement statement;
			if(monitoredSubreddit.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET subreddit=?, silent=?, read_only=?, write_only=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (subreddit, silent, read_only, write_only) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setString(counter++, monitoredSubreddit.subreddit);
			statement.setBoolean(counter++, monitoredSubreddit.silent);
			statement.setBoolean(counter++, monitoredSubreddit.readOnly);
			statement.setBoolean(counter++, monitoredSubreddit.writeOnly);
			
			if(monitoredSubreddit.id > 0) {
				statement.setInt(counter++, monitoredSubreddit.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(keys.next()) {
					monitoredSubreddit.id = keys.getInt(1);
				}else {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys for " + table + " table, but none found");
				}
				keys.close();
			}
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String fetchAllAndConcatenate() {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT subreddit FROM " + table);
			
			boolean first = true;
			StringBuilder monitoredSubreddits = new StringBuilder();
			ResultSet results = statement.executeQuery();
			while(results.next()) {
				if(first) {
					first = false;
				}else {
					monitoredSubreddits.append('+');
				}
				
				monitoredSubreddits.append(results.getString(1));
			}
			results.close();
			statement.close();
			
			return monitoredSubreddits.toString();
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches a monitored subreddit from the result set
	 * 
	 * @param set the result set to fetch from
	 * @return the monitored subreddit in the current row of the set
	 * @throws SQLException if one occurs
	 */
	@Override
	protected MonitoredSubreddit fetchFromSet(ResultSet set) throws SQLException {
		return new MonitoredSubreddit(set.getInt("id"), set.getString("subreddit"), set.getBoolean("silent"),
				set.getBoolean("read_only"), set.getBoolean("write_only"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "subreddit VARCHAR(50) NOT NULL, "
				+ "silent TINYINT(1) NOT NULL, "
				+ "read_only TINYINT(1) NOT NULL, "
				+ "write_only TINYINT(1) NOT NULL, "
				+ "PRIMARY KEY(id),"
				+ "UNIQUE KEY(subreddit))");
		statement.close();
	}
}
