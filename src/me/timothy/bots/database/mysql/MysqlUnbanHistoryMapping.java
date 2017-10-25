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
import me.timothy.bots.database.UnbanHistoryMapping;
import me.timothy.bots.models.UnbanHistory;

public class MysqlUnbanHistoryMapping extends MysqlObjectWithIDMapping<UnbanHistory> implements UnbanHistoryMapping {

	private static Logger logger = LogManager.getLogger();
	
	public MysqlUnbanHistoryMapping(USLDatabase database, Connection connection) {
		super(database, connection, "unban_histories", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.INTEGER, "unbanned_person_id"),
				new MysqlColumn(Types.INTEGER, "handled_modaction_id"));
	}

	@Override
	public void save(UnbanHistory a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET mod_person_id=?, "
						+ "unbanned_person_id=?, handled_modaction_id=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (mod_person_id, "
						+ "unbanned_person_id, handled_modaction_id) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.modPersonID);
			statement.setInt(counter++, a.unbannedPersonID);
			statement.setInt(counter++, a.handledModActionID);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.executeUpdate();
			}else {
				statement.executeUpdate();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys from " + table);
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
	public UnbanHistory fetchByHandledModActionID(int handledModActionID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_modaction_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, handledModActionID)), 
				fetchFromSetFunction());
	}

	@Override
	public List<UnbanHistory> fetchByHandledModActionIDS(Collection<Integer> handledModActionIDs) {
		if(handledModActionIDs == null)
			throw new NullPointerException("handledModActionIDs cannot be null");
		
		if(handledModActionIDs.size() == 0)
			return new ArrayList<>();
		
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_modaction_id IN (" + createPlaceholders(handledModActionIDs.size()) + ")",
				new PreparedStatementSetVars() {

					@Override
					public void setVars(PreparedStatement statement) throws SQLException {
						int counter = 1;
						for(int hmaID : handledModActionIDs) {
							statement.setInt(counter++, hmaID);
						}
					}
			
				}, fetchListFromSetFunction());
	}

	@Override
	public UnbanHistory fetchUnbanHistoryByPersonAndSubreddit(int unbannedPersonId, int monitoredSubredditId) {
		return fetchByAction("SELECT ubh.id, ubh.mod_person_id, ubh.unbanned_person_id, ubh.handled_modaction_id FROM " + table + " AS ubh "
				+ "INNER JOIN handled_modactions AS hma ON ubh.handled_modaction_id = hma.id "
				+ "WHERE ubh.unbanned_person_id=? AND hma.monitored_subreddit_id=? ORDER BY occurred_at DESC LIMIT 1",
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, unbannedPersonId),
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditId)),
				new PreparedStatementFetchResult<UnbanHistory>() {

					@Override
					public UnbanHistory fetchResult(ResultSet set) throws SQLException {
						if(!set.next())
							return null;
						return new UnbanHistory(set.getInt(1), set.getInt(2), set.getInt(3), set.getInt(4));
					}
			
				});
	}

	@Override
	public List<UnbanHistory> fetchUnbanHistoriesByPersonAndSubreddit(int unbannedPersonId, int monitoredSubredditId) {
		return fetchByAction("SELECT ubh.id, ubh.mod_person_id, ubh.unbanned_person_id, ubh.handled_modaction_id FROM " + table + " AS ubh "
				+ "INNER JOIN handled_modactions AS hma ON ubh.handled_modaction_id = hma.id "
				+ "WHERE ubh.unbanned_person_id=? AND hma.monitored_subreddit_id=?",
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, unbannedPersonId),
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditId)),
				new PreparedStatementFetchResult<List<UnbanHistory>>() {

					@Override
					public List<UnbanHistory> fetchResult(ResultSet set) throws SQLException {
						List<UnbanHistory> result = new ArrayList<>();
						while(set.next()) {
							result.add(new UnbanHistory(set.getInt(1), set.getInt(2), set.getInt(3), set.getInt(4)));
						}
						return result;
					}
			
				});
	}

	@Override
	protected UnbanHistory fetchFromSet(ResultSet set) throws SQLException {
		return new UnbanHistory(set.getInt("id"), set.getInt("mod_person_id"),
				set.getInt("unbanned_person_id"), set.getInt("handled_modaction_id"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "mod_person_id INT NOT NULL, "
				+ "unbanned_person_id INT NOT NULL, "
				+ "handled_modaction_id INT NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(handled_modaction_id), "
				+ "INDEX ind_unbanhist_modper_id (mod_person_id), "
				+ "INDEX ind_unbanhist_unbanper_id (unbanned_person_id), "
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (unbanned_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (handled_modaction_id) REFERENCES handled_modactions(id)"
				+ ")");
		statement.close();
		
	}

}
