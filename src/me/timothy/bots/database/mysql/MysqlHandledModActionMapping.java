package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.memory.HandledModActionJoinHistory;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.UnbanHistory;

public class MysqlHandledModActionMapping extends MysqlObjectWithIDMapping<HandledModAction> implements HandledModActionMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlHandledModActionMapping(USLDatabase database, Connection connection) {
		super(database, connection, "handled_modactions", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "monitored_subreddit_id"),
				new MysqlColumn(Types.VARCHAR, "modaction_id"),
				new MysqlColumn(Types.TIMESTAMP, "occurred_at"));
	}

	@Override
	public void save(HandledModAction a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.occurredAt != null) { a.occurredAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET monitored_subreddit_id=?, modaction_id=?,"
						+ " occurred_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (monitored_subreddit_id, modaction_id,"
						+ " occurred_at) VALUES (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.monitoredSubredditID);
			statement.setString(counter++, a.modActionID);
			statement.setTimestamp(counter++, a.occurredAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
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
	protected HandledModAction fetchFromSet(ResultSet set) throws SQLException {
		return new HandledModAction(set.getInt("id"), set.getInt("monitored_subreddit_id"),
				set.getString("modaction_id"), set.getTimestamp("occurred_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "monitored_subreddit_id INT NOT NULL, "
				+ "modaction_id VARCHAR(50) NOT NULL, "
				+ "occurred_at TIMESTAMP NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(modaction_id), "
				+ "INDEX ind_handmodactions_monsub_id (monitored_subreddit_id), "
				+ "FOREIGN KEY(monitored_subreddit_id) REFERENCES monitored_subreddits(id)"
				+ ")");
		statement.close();
	}

	@Override
	public HandledModAction fetchByModActionID(String modActionID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE modaction_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.VARCHAR, modActionID)),
				fetchFromSetFunction());
	}

	@Override
	public List<HandledModAction> fetchByTimestamp(Timestamp timestamp) {
		return fetchByAction("SELECT * FROM " + table + " WHERE occurred_at=?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.TIMESTAMP, timestamp)),
				fetchListFromSetFunction());
	}
	

	@Override
	public List<HandledModAction> fetchLatest(Timestamp after, Timestamp before, int num) {
		return fetchByAction("SELECT * FROM " + table + " WHERE occurred_at>? AND occurred_at<? ORDER BY occurred_at ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.TIMESTAMP, after),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.INTEGER, num)),
				fetchListFromSetFunction());
	}
	
	@Override
	public List<HandledModAction> fetchByTimestampAndSubreddit(Timestamp timestamp, int subredditID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE monitored_subreddit_id=? AND occurred_at=?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, subredditID),
						new MysqlTypeValueTuple(Types.TIMESTAMP, timestamp)),
				fetchListFromSetFunction());
	}

	@Override
	public List<HandledModAction> fetchLatestForSubreddit(int monitoredSubredditID, Timestamp after, Timestamp before, int num) {
		return fetchByAction("SELECT * FROM " + table + " WHERE (monitored_subreddit_id=? AND occurred_at>=? AND (? IS NULL OR occurred_at<?)) ORDER BY occurred_at ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, monitoredSubredditID),
						new MysqlTypeValueTuple(Types.TIMESTAMP, after),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.TIMESTAMP, before),
						new MysqlTypeValueTuple(Types.INTEGER, num)),
				fetchListFromSetFunction());
	}

	@Override
	public List<HandledModActionJoinHistory> fetchLatestJoined(Timestamp after, Timestamp before, int num) {
		Timestamp fixedAfter = new Timestamp(after.getTime());
		fixedAfter.setNanos(0);
		
		Timestamp fixedBefore = new Timestamp(before.getTime());
		fixedBefore.setNanos(0);
		
		try(PreparedStatement statement = connection.prepareStatement("SELECT handled_modactions.id, handled_modactions.monitored_subreddit_id, handled_modactions.modaction_id, "
				+ "handled_modactions.occurred_at, ban_histories.id, ban_histories.mod_person_id, ban_histories.banned_person_id, "
				+ "ban_histories.ban_description, ban_histories.ban_details, unban_histories.id, unban_histories.mod_person_id, "
				+ "unban_histories.unbanned_person_id "
				+ "FROM (SELECT * FROM handled_modactions ORDER BY handled_modactions.occurred_at ASC) handled_modactions "
				+ "LEFT JOIN ban_histories ON handled_modactions.id = ban_histories.handled_modaction_id "
				+ "LEFT JOIN unban_histories ON handled_modactions.id = unban_histories.handled_modaction_id "
				+ "WHERE handled_modactions.occurred_at >= ? AND handled_modactions.occurred_at < ? AND (ban_histories.id IS NOT NULL OR unban_histories.id IS NOT NULL) "
				+ "ORDER BY handled_modactions.occurred_at ASC LIMIT ?")) {
			statement.setTimestamp(1, fixedAfter);
			statement.setTimestamp(2, fixedBefore);
			statement.setInt(3, num);
			
			try(ResultSet set = statement.executeQuery()) {
				List<HandledModActionJoinHistory> result = new ArrayList<>();
				while(set.next()) {
					HandledModAction hma = new HandledModAction(set.getInt(1), set.getInt(2), set.getString(3), set.getTimestamp(4));
					set.getInt(5);
					if(set.wasNull()) {
						// Unban
						result.add(new HandledModActionJoinHistory(hma, null, new UnbanHistory(
								set.getInt(10), set.getInt(11), set.getInt(12), hma.id)));
					}else {
						// Ban
						BanHistory bh = new BanHistory(
								set.getInt(5), set.getInt(6), set.getInt(7), hma.id, set.getString(8), set.getString(9));
						
						if(bh.banDescription == null)
							bh.banDescription = "";
						
						result.add(new HandledModActionJoinHistory(hma, bh, null));
					}
				}
				return result;
			}
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

}
