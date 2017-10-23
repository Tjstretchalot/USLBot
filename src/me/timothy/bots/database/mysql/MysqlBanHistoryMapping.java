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
				new MysqlColumn(Types.VARCHAR, "modaction_id"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_description"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_details"),
				new MysqlColumn(Types.TIMESTAMP, "occurred_at")
		});
	}

	@Override
	public void save(BanHistory banHistory) throws IllegalArgumentException {
		if(!banHistory.isValid())
			throw new IllegalArgumentException(banHistory + " is not valid");
		
		if(banHistory.occurredAt != null) { banHistory.occurredAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(banHistory.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, mod_person_id=?, banned_person_id=?, "
						+ "modaction_id=?, ban_description=?, ban_details=?, occurred_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, mod_person_id, banned_person_id, "
						+ "modaction_id, ban_description, ban_details, occurred_at) VALUES (?, ?, ?, "
						+ "?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, banHistory.monitoredSubredditID);
			statement.setInt(counter++, banHistory.modPersonID);
			statement.setInt(counter++, banHistory.bannedPersonID);
			statement.setString(counter++, banHistory.modActionID);
			statement.setString(counter++, banHistory.banDescription);
			statement.setString(counter++, banHistory.banDetails);
			statement.setTimestamp(counter++, banHistory.occurredAt);
			
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

	@Override
	public BanHistory fetchByID(int id) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE id=?");
			int counter = 1;
			statement.setInt(counter++, id);
			
			ResultSet results = statement.executeQuery();
			BanHistory ban = null;
			if(results.next()) {
				ban = fetchFromSet(results);
			}
			results.close();
			statement.close();
			
			return ban;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public BanHistory fetchByModActionID(String modActionID) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE modaction_id=?");
			int counter = 1;
			statement.setString(counter++, modActionID);
			
			ResultSet results = statement.executeQuery();
			BanHistory ban = null;
			if(results.next()) {
				ban = fetchFromSet(results);
			}
			results.close();
			statement.close();
			
			return ban;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public BanHistory fetchBanHistoryByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE banned_person_id=? AND monitored_subreddit_id=?");
			int counter = 1;
			statement.setInt(counter++, bannedPersonId);
			statement.setInt(counter++, monitoredSubredditId);
			
			ResultSet results = statement.executeQuery();
			BanHistory ban = null;
			if(results.next()) {
				ban = fetchFromSet(results);
			}
			results.close();
			statement.close();
			
			return ban;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<BanHistory> fetchBanHistoriesAboveIDSortedByIDAsc(int id, int num) {
		try 
		{
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE id>? ORDER BY id ASC LIMIT ?");
			int counter = 1;
			statement.setInt(counter++, id);
			statement.setInt(counter++, num);
			
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
				set.getInt("banned_person_id"), set.getString("modaction_id"), set.getString("ban_description"), 
				set.getString("ban_details"), set.getTimestamp("occurred_at"));
	}
	
	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "mod_person_id INT NOT NULL, "
				+ "banned_person_id INT NOT NULL, "
				+ "modaction_id VARCHAR(50) NOT NULL, "
				+ "ban_description TEXT NOT NULL, "
				+ "ban_details TEXT NOT NULL, "
				+ "occurred_at TIMESTAMP NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(modaction_id), "
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
