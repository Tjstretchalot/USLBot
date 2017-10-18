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
import me.timothy.bots.database.BanHistoryMapping;
import me.timothy.bots.models.BanHistory;

public class MysqlBanHistoryMapping extends MysqlObjectMapping<BanHistory> implements BanHistoryMapping {
	private static Logger logger = LogManager.getLogger();

	public MysqlBanHistoryMapping(USLDatabase database, Connection connection) {
		super(database, connection, "ban_histories", new MysqlColumn[] {
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.INTEGER, "banned_person_id"),
				new MysqlColumn(Types.INTEGER, "ban_reason_id"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_description"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_reason_additional"),
				new MysqlColumn(Types.BIT, "suppressed"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "occurred_at"),
				new MysqlColumn(Types.TIMESTAMP, "updated_at")
		});
		// TODO Auto-generated constructor stub
	}

	@Override
	public void save(BanHistory banHistory) throws IllegalArgumentException {
		if(!banHistory.isValid())
			throw new IllegalArgumentException(banHistory + " is not valid");
		
		if(banHistory.createdAt != null) { banHistory.createdAt.setNanos(0); }
		if(banHistory.occurredAt != null) { banHistory.occurredAt.setNanos(0); }
		if(banHistory.updatedAt != null) { banHistory.updatedAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(banHistory.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, mod_person_id=?, banned_person_id=?, "
						+ "ban_reason_id=?, ban_description=?, ban_reason_additional=?, suppressed=?, created_at=?, occurred_at=?, updated_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, mod_person_id, banned_person_id, "
						+ "ban_reason_id, ban_description, ban_reason_additional, suppressed, created_at, occurred_at, updated_at) VALUES (?, ?, ?, "
						+ "?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, banHistory.monitoredSubredditID);
			statement.setInt(counter++, banHistory.modPersonID);
			statement.setInt(counter++, banHistory.bannedPersonID);
			statement.setInt(counter++, banHistory.banReasonID);
			statement.setString(counter++, banHistory.banDescription);
			statement.setString(counter++, banHistory.banReasonAdditional);
			statement.setBoolean(counter++, banHistory.suppressed);
			statement.setTimestamp(counter++, banHistory.createdAt);
			statement.setTimestamp(counter++, banHistory.occurredAt);
			statement.setTimestamp(counter++, banHistory.updatedAt);
			
			if(banHistory.id > 0) {
				statement.setInt(counter++, banHistory.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					throw new RuntimeException("Expected generated keys from " + table + " but got none!");
				}
				banHistory.id = keys.getInt(1);
				keys.close();
			}
			statement.close();
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<BanHistory> fetchAll() {
		try 
		{
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
			
			ResultSet results = statement.executeQuery();
			List<BanHistory> bans = new ArrayList<>();
			while(results.next()) {
				bans.add(fetchFromSet(results));
			}
			results.close();
			statement.close();
			return bans;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	protected BanHistory fetchFromSet(ResultSet set) throws SQLException {
		return new BanHistory(set.getInt("id"), set.getInt("monitored_subreddit_id"), set.getInt("mod_person_id"), 
				set.getInt("banned_person_id"), set.getInt("ban_reason_id"), set.getString("ban_description"), 
				set.getString("ban_reason_additional"), set.getBoolean("suppressed"), set.getTimestamp("created_at"), 
				set.getTimestamp("occurred_at"), set.getTimestamp("updated_at"));
	}
	
	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "mod_person_id INT NOT NULL, "
				+ "banned_person_id INT NOT NULL, "
				+ "ban_reason_id INT NOT NULL, "
				+ "ban_description TEXT NOT NULL, "
				+ "ban_reason_additional TEXT, "
				+ "suppressed TINYINT(1) NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00', "
				+ "occurred_at TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00', "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_banhist_monsub_id (monitored_subreddit_id), "
				+ "INDEX ind_banhist_modper_id (mod_person_id), "
				+ "INDEX ind_banhist_banper_id (banned_person_id), "
				+ "FOREIGN KEY (monitored_subreddit_id) REFERENCES monitored_subreddits(id), "
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (banned_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
