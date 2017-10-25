package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.BanHistoryMapping;
import me.timothy.bots.models.BanHistory;

public class MysqlBanHistoryMapping extends MysqlObjectWithIDMapping<BanHistory> implements BanHistoryMapping {
	private static Logger logger = LogManager.getLogger();

	public MysqlBanHistoryMapping(USLDatabase database, Connection connection) {
		super(database, connection, "ban_histories", new MysqlColumn[] {
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.INTEGER, "banned_person_id"),
				new MysqlColumn(Types.INTEGER, "handled_modaction_id"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_description"),
				new MysqlColumn(Types.LONGVARCHAR, "ban_details")
		});
	}

	@Override
	public void save(BanHistory banHistory) throws IllegalArgumentException {
		if(!banHistory.isValid())
			throw new IllegalArgumentException(banHistory + " is not valid");
		
		try {
			PreparedStatement statement;
			if(banHistory.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET mod_person_id=?, banned_person_id=?, "
						+ "handled_modaction_id=?, ban_description=?, ban_details=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (mod_person_id, banned_person_id, "
						+ "handled_modaction_id, ban_description, ban_details) VALUES (?, ?, "
						+ "?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, banHistory.modPersonID);
			statement.setInt(counter++, banHistory.bannedPersonID);
			statement.setInt(counter++, banHistory.handledModActionID);
			statement.setString(counter++, banHistory.banDescription);
			statement.setString(counter++, banHistory.banDetails);
			
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
	public BanHistory fetchByHandledModActionID(int handledModActionID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_modaction_id=? LIMIT 1",
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, handledModActionID)
						),
				fetchFromSetFunction());
	}

	@Override
	public List<BanHistory> fetchByHandledModActionIDs(Collection<Integer> handledModActionIDs) {
		if(handledModActionIDs == null)
			throw new NullPointerException("handledModActionIDs cannot be null");
		
		if(handledModActionIDs.size() == 0)
			return new ArrayList<BanHistory>();
		
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_modaction_id IN (" + createPlaceholders(handledModActionIDs.size()) + ")",
				new PreparedStatementSetVars() {

					@Override
					public void setVars(PreparedStatement statement) throws SQLException {
						int counter = 1;
						for(int i : handledModActionIDs) {
							statement.setInt(counter++, i);
						}
					}
		
				}, fetchListFromSetFunction());
	}

	@Override
	public BanHistory fetchBanHistoryByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId) {
		return fetchByAction("SELECT bh.id, bh.mod_person_id, bh.banned_person_id, bh.handled_modaction_id, "
				+ "bh.ban_description, bh.ban_details FROM " + table + " AS bh "
						+ "INNER JOIN handled_modactions AS hma ON hma.id = bh.handled_modaction_id "
						+ "WHERE bh.banned_person_id=? AND hma.monitored_subreddit_id=? "
				+ "ORDER BY hma.occurred_at DESC LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, bannedPersonId),
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditId)
						), 
				new PreparedStatementFetchResult<BanHistory>() {

					@Override
					public BanHistory fetchResult(ResultSet set) throws SQLException {
						if(!set.next())
							return null;
						return new BanHistory(set.getInt(1), set.getInt(2), set.getInt(3), set.getInt(4), 
								set.getString(5), set.getString(6));
					}
					
				});
	}

	@Override
	public List<BanHistory> fetchBanHistoriesByPersonAndSubreddit(int bannedPersonId, int monitoredSubredditId) {
		return fetchByAction("SELECT bh.id, bh.mod_person_id, bh.banned_person_id, bh.handled_modaction_id, "
				+ "bh.ban_description, bh.ban_details FROM " + table + " AS bh "
						+ "INNER JOIN handled_modactions AS hma ON hma.id = bh.handled_modaction_id "
						+ "WHERE bh.banned_person_id=? AND hma.monitored_subreddit_id=?",
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, bannedPersonId),
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditId)
						), 
				new PreparedStatementFetchResult<List<BanHistory>>() {

					@Override
					public List<BanHistory> fetchResult(ResultSet set) throws SQLException {
						List<BanHistory> result = new ArrayList<>();
						while(set.next()) {
							result.add(new BanHistory(set.getInt(1), set.getInt(2), set.getInt(3), set.getInt(4), 
								set.getString(5), set.getString(6)));
						}
						return result;
					}
					
				});
	}
	
	@Override
	protected BanHistory fetchFromSet(ResultSet set) throws SQLException {
		return new BanHistory(set.getInt("id"), set.getInt("mod_person_id"), 
				set.getInt("banned_person_id"), set.getInt("handled_modaction_id"), 
				set.getString("ban_description"), set.getString("ban_details"));
	}
	
	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "mod_person_id INT NOT NULL, "
				+ "banned_person_id INT NOT NULL, "
				+ "handled_modaction_id INT NOT NULL, "
				+ "ban_description TEXT NOT NULL, "
				+ "ban_details TEXT NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(handled_modaction_id), "
				+ "INDEX ind_banhist_modper_id (mod_person_id), "
				+ "INDEX ind_banhist_banper_id (banned_person_id), "
				+ "FOREIGN KEY (handled_modaction_id) REFERENCES handled_modactions(id), "
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (banned_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
