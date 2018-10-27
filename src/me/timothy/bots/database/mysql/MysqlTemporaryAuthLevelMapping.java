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
import me.timothy.bots.database.TemporaryAuthLevelMapping;
import me.timothy.bots.models.TemporaryAuthLevel;

public class MysqlTemporaryAuthLevelMapping extends MysqlObjectWithIDMapping<TemporaryAuthLevel> implements TemporaryAuthLevelMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlTemporaryAuthLevelMapping(USLDatabase database, Connection connection) {
		super(database, connection, "temporary_auth_levels",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.INTEGER, "auth_level"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "expires_at"));
	}

	@Override
	public void save(TemporaryAuthLevel a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.expiresAt != null) { a.expiresAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, auth_level, created_at, expires_at) VALUES (?, ?, ?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, auth_level=?, created_at=?, expires_at=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.personID);
			statement.setInt(counter++, a.authLevel);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.expiresAt);

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
	public TemporaryAuthLevel fetchByPersonID(int personID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personID)), 
				fetchFromSetFunction());
	}

	@Override
	public void deleteById(int id) {
		try {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE id=?");
			
			int counter = 1;
			statement.setInt(counter++, id);
			
			statement.execute();
			
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected TemporaryAuthLevel fetchFromSet(ResultSet set) throws SQLException {
		return new TemporaryAuthLevel(set.getInt("id"), set.getInt("person_id"), set.getInt("auth_level"),
				set.getTimestamp("created_at"), set.getTimestamp("expires_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE temporary_auth_levels ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "person_id INT NOT NULL, "
				+ "auth_level INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "expires_at TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE INDEX ind_tal_pid (person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
	}

}
