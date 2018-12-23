package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.models.DirtyPerson;

public class MysqlDirtyPersonMapping extends MysqlObjectMapping<DirtyPerson> implements DirtyPersonMapping {
	private static final Logger logger = LogManager.getLogger();
	private final String insertQuery;

	public MysqlDirtyPersonMapping(USLDatabase database, Connection connection) {
		super(database, connection, "dirty_persons", 
				new MysqlColumn(Types.INTEGER, "person_id"));
		insertQuery = "INSERT INTO " + table + " (person_id) VALUES (?)";
	}

	@Override
	public void save(DirtyPerson a) throws IllegalArgumentException {
		try(PreparedStatement statement = connection.prepareStatement(insertQuery)) {
			statement.setInt(1, a.personID);
			statement.execute();
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<DirtyPerson> fetch(int limit) {
		try(PreparedStatement statement = connection.prepareStatement("SELECT person_id FROM " + table + " LIMIT ?")) {
			statement.setInt(1, limit);
			try(ResultSet set = statement.executeQuery()) {
				if(!set.next()) 
					return Collections.emptyList();
				
				List<DirtyPerson> result = new ArrayList<>();
				do {
					result.add(fetchFromSet(set));
				} while(set.next());
				return result;
			}
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean contains(int personId) {
		try(PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE person_id=? LIMIT 1")) {
			statement.setInt(1, personId);
			try(ResultSet set = statement.executeQuery()) {
				return set.next();
			}
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(int personId) {
		try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE person_id=? LIMIT 1")) {
			statement.setInt(1, personId);
			statement.execute();
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public int count() {
		try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table)) {
			try(ResultSet set = statement.executeQuery()) {
				if(!set.next())
					throw new RuntimeException("Should always get one row from select count(*)!");
				return set.getInt(1);
			}
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected DirtyPerson fetchFromSet(ResultSet set) throws SQLException {
		return new DirtyPerson(set.getInt(1));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) { 
			statement.execute("CREATE TABLE " + table + " ("
					+ "person_id INT NOT NULL, "
					+ "PRIMARY KEY(person_id)"
					+ ") ENGINE = INNODB");
		}
	}

}
