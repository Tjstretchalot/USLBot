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
import me.timothy.bots.database.SubredditPersonBannedReleaseMapping;
import me.timothy.bots.models.SubredditPersonBannedRelease;

public class MysqlSubredditPersonBannedReleaseMapping extends MysqlObjectMapping<SubredditPersonBannedRelease> implements SubredditPersonBannedReleaseMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlSubredditPersonBannedReleaseMapping(USLDatabase database, Connection connection) {
		super(database, connection, "sub_pers_banned_release",
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "person_id"));
	}

	@Override
	public void save(SubredditPersonBannedRelease a) throws IllegalArgumentException {
		try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, person_id) VALUES (?, ?)")) {
			statement.setInt(1, a.monitoredSubredditID);
			statement.setInt(2, a.personID);
			statement.execute();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected SubredditPersonBannedRelease fetchFromSet(ResultSet set) throws SQLException {
		return new SubredditPersonBannedRelease(set.getInt("monitored_subreddit_id"), set.getInt("person_id"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "monitored_subreddit_id INT NOT NULL, "
					+ "person_id INT NOT NULL, "
					+ "PRIMARY KEY (monitored_subreddit_id, person_id), "
					+ "INDEX ind_spbr_monsub (monitored_subreddit_id), "
					+ "INDEX ind_sbpr_persid (person_id), "
					+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id), "
					+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
					+ ")");
		}
	}

}
