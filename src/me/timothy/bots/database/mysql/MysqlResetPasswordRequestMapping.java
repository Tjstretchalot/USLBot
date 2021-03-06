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
import me.timothy.bots.database.ResetPasswordRequestMapping;
import me.timothy.bots.models.ResetPasswordRequest;

public class MysqlResetPasswordRequestMapping extends MysqlObjectWithIDMapping<ResetPasswordRequest> implements ResetPasswordRequestMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlResetPasswordRequestMapping(USLDatabase database, Connection connection) {
		super(database, connection, "reset_password_requests", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.VARCHAR, "token"),
				new MysqlColumn(Types.BIT, "consumed"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "sent_at"));
	}

	@Override
	public void save(ResetPasswordRequest a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.sentAt != null) { a.sentAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) 
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, token=?, consumed=?, created_at=?, sent_at=? WHERE id=?");
			else 
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, token, consumed, created_at, sent_at) "
						+ "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			
			int counter = 1;
			statement.setInt(counter++, a.personID);
			statement.setString(counter++, a.token);
			statement.setBoolean(counter++, a.consumed);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.sentAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) 
					throw new RuntimeException("Expected generated keys from " + table);
				
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
	public List<ResetPasswordRequest> fetchUnsent(int limit) {
		return fetchByAction("SELECT * FROM " + table + " WHERE sent_at IS NULL ORDER BY created_at ASC LIMIT ?",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, limit)),
				fetchListFromSetFunction());
	}

	@Override
	protected ResetPasswordRequest fetchFromSet(ResultSet set) throws SQLException {
		return new ResetPasswordRequest(set.getInt("id"), set.getInt("person_id"),
				set.getString("token"), set.getBoolean("consumed"), set.getTimestamp("created_at"), 
				set.getTimestamp("sent_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "person_id INT NOT NULL, "
				+ "token VARCHAR(255) NOT NULL, "
				+ "consumed TINYINT(1) NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sent_at TIMESTAMP NULL DEFAULT NULL, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_respas_pers_id (person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
