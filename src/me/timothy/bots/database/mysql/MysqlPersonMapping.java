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
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.Person;

/**
 * Describes a mapping between persons in memory and in the database.
 * 
 * @author Timothy
 */
public class MysqlPersonMapping extends MysqlObjectMapping<Person> implements SchemaValidator, PersonMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlPersonMapping(USLDatabase database, Connection connection) {
		super(database, connection, "persons", new MysqlColumn[] {
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "username"),
				new MysqlColumn(Types.LONGVARCHAR, "password_hash"),
				new MysqlColumn(Types.LONGVARCHAR, "email"),
				new MysqlColumn(Types.INTEGER, "auth"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "updated_at")
		});
	}

	@Override
	public void save(Person person) throws IllegalArgumentException {
		if(!person.isValid())
			throw new IllegalArgumentException("Person " + person + " is not valid");

		if(person.createdAt != null) { person.createdAt.setNanos(0); }
		if(person.updatedAt != null) { person.updatedAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(person.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET username=?, password_hash=?, email=?, auth=?, created_at=?, updated_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (username, password_hash, email, auth, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setString(counter++, person.username);
			statement.setString(counter++, person.passwordHash);
			statement.setString(counter++, person.email);
			statement.setInt(counter++, person.authLevel);
			statement.setTimestamp(counter++, person.createdAt);
			statement.setTimestamp(counter++, person.updatedAt);
			
			if(person.id > 0) {
				statement.setInt(counter++, person.id);
				statement.execute();
			}else {
				statement.execute();
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) 
					throw new RuntimeException("expected " + table + " to have generated keys!");
				person.id = keys.getInt(1);
				keys.close();
			}
			statement.close();
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Person> fetchAll() {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
			
			List<Person> persons = new ArrayList<>();
			ResultSet results = statement.executeQuery();
			while(results.next()) {
				persons.add(fetchFromSet(results));
			}
			results.close();
			statement.close();
			return persons;
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public Person fetchOrCreateByUsername(String username) {
		Person existing = fetchByUsername(username);
		if(existing != null)
			return existing;
		
		long now = System.currentTimeMillis();
		Person newPerson = new Person(-1, username, null, null, -1, new Timestamp(now), new Timestamp(now));
		save(newPerson);
		return newPerson;
	}

	@Override
	public Person fetchByUsername(String username) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE username=?");
			
			int counter = 1;
			statement.setString(counter++, username);
			
			ResultSet results = statement.executeQuery();
			
			Person person = null;
			if(results.next()) {
				person = fetchFromSet(results);
			}
			results.close();
			statement.close();
			return person;
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Person fetchByEmail(String email) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE email=? LIMIT 1");

			int counter = 1;
			statement.setString(counter++, email);

			ResultSet results = statement.executeQuery();
			Person person = null;
			if(results.next()) {
				person = fetchFromSet(results);
			}
			results.close();
			statement.close();
			return person;
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Fetch the person in the current row of the set.
	 * 
	 * @param set the set
	 * @return the person in the current row
	 */
	protected Person fetchFromSet(ResultSet set) throws SQLException {
		return new Person(set.getInt("id"), set.getString("username"), set.getString("password_hash"),
				set.getString("email"), set.getInt("auth"), set.getTimestamp("created_at"),
				set.getTimestamp("updated_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "username VARCHAR(255) NOT NULL, "
				+ "password_hash TEXT, "
				+ "email TEXT, "
				+ "auth INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY(username))");
		statement.close();
	}
}
