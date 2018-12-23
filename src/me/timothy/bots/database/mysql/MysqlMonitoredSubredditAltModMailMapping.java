package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.MonitoredSubredditAltModMailMapping;
import me.timothy.bots.models.MonitoredSubredditAltModMail;

public class MysqlMonitoredSubredditAltModMailMapping extends MysqlObjectWithIDMapping<MonitoredSubredditAltModMail> implements MonitoredSubredditAltModMailMapping{
	private static final Logger logger = LogManager.getLogger();

	public MysqlMonitoredSubredditAltModMailMapping(USLDatabase database, Connection connection) {
		super(database, connection, "subreddit_alt_modmail", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.VARCHAR, "subreddit"));
	}

	@Override
	public void save(MonitoredSubredditAltModMail a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		try {
			PreparedStatement statement;
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, subreddit) VALUES (?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, subreddit=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setString(counter++, a.alternateSubreddit);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys for table " + table);
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
	public List<String> fetchForSubreddit(int monitoredSubredditID) {
		try(PreparedStatement statement = connection.prepareStatement("SELECT subreddit FROM " + table + " WHERE monitored_subreddit_id=?")) {
			statement.setInt(1, monitoredSubredditID);

			try(ResultSet set = statement.executeQuery()) {
				List<String> res = new ArrayList<>();
				while(set.next()) {
					res.add(set.getString(1));
				}
				return res;
			}
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected MonitoredSubredditAltModMail fetchFromSet(ResultSet set) throws SQLException {
		return new MonitoredSubredditAltModMail(set.getInt("id"), set.getInt("monitored_subreddit_id"),
				set.getString("subreddit"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "monitored_subreddit_id INT NOT NULL, "
					+ "subreddit VARCHAR(255) NOT NULL, "
					+ "PRIMARY KEY (id), "
					+ "INDEX (monitored_subreddit_id)"
					+ ")");
		}
	}

}
