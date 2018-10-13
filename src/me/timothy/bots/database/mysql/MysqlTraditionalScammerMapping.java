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
import me.timothy.bots.database.TraditionalScammerMapping;
import me.timothy.bots.models.TraditionalScammer;

public class MysqlTraditionalScammerMapping extends MysqlObjectWithIDMapping<TraditionalScammer> implements TraditionalScammerMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlTraditionalScammerMapping(USLDatabase database, Connection connection) {
		super(database, connection, "traditional_scammers", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.LONGVARCHAR, "reason"),
				new MysqlColumn(Types.LONGVARCHAR, "description"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(TraditionalScammer a) throws IllegalArgumentException {
		if(!a.isValid()) 
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, reason=?, description=?, created_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, reason, description, created_at) VALUES (?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.personID);
			statement.setString(counter++, a.reason);
			statement.setString(counter++, a.description);
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
	public TraditionalScammer fetchByPersonID(int personID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=?", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personID)),
				fetchFromSetFunction());
	}

	@Override
	public void deleteByPersonID(int personID) {
		try {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE person_id=?");
			
			int counter = 1;
			statement.setInt(counter++, personID);
			
			statement.execute();
			
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<TraditionalScammer> fetchEntriesAfterID(int id, int limit) {
		return fetchByAction("SELECT * FROM " + table + " WHERE id>? ORDER BY id ASC LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, id),
						new MysqlTypeValueTuple(Types.INTEGER, limit)),
				fetchListFromSetFunction());
	}

	@Override
	protected TraditionalScammer fetchFromSet(ResultSet set) throws SQLException {
		return new TraditionalScammer(set.getInt("id"), set.getInt("person_id"), set.getString("reason"), 
				set.getString("description"), set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "person_id INT NOT NULL, "
				+ "reason TEXT NOT NULL, "
				+ "description TEXT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
