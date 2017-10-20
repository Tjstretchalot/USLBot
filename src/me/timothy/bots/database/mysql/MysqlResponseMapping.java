package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.ResponseMapping;
import me.timothy.bots.models.Response;

public class MysqlResponseMapping extends MysqlObjectMapping<Response> implements ResponseMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlResponseMapping(USLDatabase database, Connection connection) {
		super(database, connection, "responses",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "name"),
				new MysqlColumn(Types.LONGVARCHAR, "response_body"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "updated_at"));
	}

	@Override
	public void save(Response a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.updatedAt != null) { a.updatedAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE responses SET "
						+ "name=?, response_body=?, created_at=?, updated_at=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO responses (name, "
						+ "response_body, created_at, updated_at) VALUES (?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setString(counter++, a.name);
			statement.setString(counter++, a.responseBody);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.updatedAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(keys.next()) {
					a.id = keys.getInt(1);
				}else {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected responses to return generated keys, but didn't get any!");
				}
				keys.close();
			}
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Response fetchByName(String name) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM responses WHERE name=?");
			statement.setString(1, name);
			
			ResultSet results = statement.executeQuery();
			Response response = null;
			if(results.next()) {
				response = fetchFromSet(results);
			}
			results.close();
			
			statement.close();
			return response;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Response> fetchAll() {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM responses");
			
			ResultSet results = statement.executeQuery();
			List<Response> responses = new ArrayList<>();
			while(results.next()) {
				responses.add(fetchFromSet(results));
			}
			results.close();
			
			statement.close();
			return responses;
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches the response in the current row of the result set
	 * @param results the result set
	 * @return the response in the current row
	 * @throws SQLException if one occurs
	 */
	protected Response fetchFromSet(ResultSet results) throws SQLException {
		return new Response(results.getInt("id"), results.getString("name"), 
				results.getString("response_body"), results.getTimestamp("created_at"), 
				results.getTimestamp("updated_at"));
	}
	
	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE responses ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "name VARCHAR(255) NOT NULL, "
				+ "response_body TEXT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE (name)" 
				+ ")");
		statement.close();
	}

}
