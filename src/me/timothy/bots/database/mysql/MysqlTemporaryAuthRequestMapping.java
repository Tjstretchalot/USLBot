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
import me.timothy.bots.database.TemporaryAuthRequestMapping;
import me.timothy.bots.models.TemporaryAuthRequest;

public class MysqlTemporaryAuthRequestMapping extends MysqlObjectWithIDMapping<TemporaryAuthRequest> implements TemporaryAuthRequestMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlTemporaryAuthRequestMapping(USLDatabase database, Connection connection) {
		super(database, connection, "temporary_auth_requests",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"), 
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(TemporaryAuthRequest a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, created_at) VALUES (?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, created_at=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.personId);
			statement.setTimestamp(counter++, a.createdAt);

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
	public TemporaryAuthRequest fetchOldestRequest() {
		return fetchByAction("SELECT * FROM " + table + " ORDER BY created_at ASC LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(), 
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
	protected TemporaryAuthRequest fetchFromSet(ResultSet set) throws SQLException {
		return new TemporaryAuthRequest(set.getInt("id"), set.getInt("person_id"), set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "person_id INT NOT NULL,"
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_tar_pid (person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
