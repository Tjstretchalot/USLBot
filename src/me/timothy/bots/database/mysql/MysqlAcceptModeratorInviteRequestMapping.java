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
import me.timothy.bots.database.AcceptModeratorInviteRequestMapping;
import me.timothy.bots.models.AcceptModeratorInviteRequest;

public class MysqlAcceptModeratorInviteRequestMapping extends MysqlObjectWithIDMapping<AcceptModeratorInviteRequest> implements AcceptModeratorInviteRequestMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlAcceptModeratorInviteRequestMapping(USLDatabase database, Connection connection) {
		super(database, connection, "accept_mod_inv_requests", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.VARCHAR, "subreddit"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "fulfilled_at"),
				new MysqlColumn(Types.BIT, "success"));
	}

	@Override
	public void save(AcceptModeratorInviteRequest a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.fulfilledAt != null) { a.fulfilledAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			
			if(a.id < 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (mod_person_id, subreddit, created_at, "
						+ "fulfilled_at, success) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET mod_person_id=?, subreddit=?, created_at=?, "
						+ "fulfilled_at=?, success=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.modPersonId);
			statement.setString(counter++, a.subreddit);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.fulfilledAt);
			statement.setBoolean(counter++, a.success);
			
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
	public List<AcceptModeratorInviteRequest> fetchUnfulfilled(int limit) {
		return fetchByAction("SELECT * FROM " + table + " WHERE fulfilled_at IS NULL ORDER BY created_at ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, limit)), 
				fetchListFromSetFunction());
	}

	@Override
	protected AcceptModeratorInviteRequest fetchFromSet(ResultSet set) throws SQLException {
		return new AcceptModeratorInviteRequest(set.getInt("id"), set.getInt("mod_person_id"), 
				set.getString("subreddit"), set.getTimestamp("created_at"), set.getTimestamp("fulfilled_at"), 
				set.getBoolean("success"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "mod_person_id INT NOT NULL, "
				+ "subreddit VARCHAR(63) NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "fulfilled_at TIMESTAMP NULL DEFAULT NULL, "
				+ "success BIT NOT NULL DEFAULT 0, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_accmodinvreq_muserid (mod_person_id), "
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
