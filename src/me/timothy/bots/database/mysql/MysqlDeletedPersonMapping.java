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
import me.timothy.bots.database.DeletedPersonMapping;
import me.timothy.bots.models.DeletedPerson;

public class MysqlDeletedPersonMapping extends MysqlObjectMapping<DeletedPerson> implements DeletedPersonMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlDeletedPersonMapping(USLDatabase database, Connection connection) {
		super(database, connection,
				"deleted_persons",
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.TIMESTAMP, "detected_deleted_at"));
	}

	@Override
	public void save(DeletedPerson a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.detectedDeletedAt != null) { a.detectedDeletedAt.setNanos(0); }
		if(contains(a.personID)) {
			throw new IllegalArgumentException("Changing the detectedDeletedAt timestamp is not supported at this time");
		}
		
		try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, detected_deleted_at) VALUES (?, ?)")) {
			statement.setInt(1, a.personID);
			statement.setTimestamp(2, a.detectedDeletedAt);
			statement.execute();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addIfNotExists(int personID) {
		try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (person_id) VALUES (?) ON DUPLICATE KEY UPDATE person_id=person_id")) {
			statement.setInt(1, personID);
			statement.execute();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean contains(int personID) {
		try(PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE person_id=? LIMIT 1")) {
			statement.setInt(1, personID);
			try(ResultSet set = statement.executeQuery()) {
				return set.next();
			}
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected DeletedPerson fetchFromSet(ResultSet set) throws SQLException {
		return new DeletedPerson(set.getInt("id"), set.getTimestamp("detected_deleted_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "person_id INT NOT NULL, "
					+ "detected_deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "UNIQUE KEY (person_id), "
					+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
					+ ")");
		}
	}

}
