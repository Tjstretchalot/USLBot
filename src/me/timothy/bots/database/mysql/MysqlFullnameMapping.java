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
import me.timothy.bots.database.FullnameMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.Fullname;

public class MysqlFullnameMapping extends MysqlObjectMapping<Fullname> implements FullnameMapping, SchemaValidator {
	private static Logger logger = LogManager.getLogger();
	public MysqlFullnameMapping(USLDatabase database, Connection connection) {
		super(database, connection, "fullnames", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "fullname"));
	}

	@Override
	public void save(Fullname fullname) throws IllegalArgumentException {
		try {
			PreparedStatement statement;
			if(fullname.id > 0) {
				statement = connection.prepareStatement("UPDATE fullnames SET fullname=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO fullnames (fullname) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setString(counter++, fullname.fullname);
			
			if(fullname.id > 0) {
				statement.setInt(counter++, fullname.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(keys.next()) {
					fullname.id = keys.getInt(1);
				}else {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys for fullname table, but none found");
				}
				keys.close();
			}
			statement.close();
			
		}catch(SQLException sqlE) {
			logger.throwing(sqlE);
			throw new RuntimeException(sqlE);
		}
	}

	@Override
	public boolean contains(String fullname) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM fullnames WHERE fullname=?");
			statement.setString(1, fullname);
			
			ResultSet results = statement.executeQuery();
			boolean contains = results.next();
			results.close();
			
			statement.close();
			
			return contains;
		}catch(SQLException sqlE) {
			logger.throwing(sqlE);
			throw new RuntimeException(sqlE);
		}
	}

	@Override
	public List<Fullname> fetchAll() {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM fullnames");
			
			List<Fullname> fullnames = new ArrayList<>();
			ResultSet results = statement.executeQuery();
			while(results.next()) {
				fullnames.add(fetchFromSet(results));
			}
			results.close();
			statement.close();
			
			return fullnames;
		} catch (SQLException sqlE) {
			logger.throwing(sqlE);
			throw new RuntimeException(sqlE);
		}
	}

	/**
	 * Fetches a fullname from the result set
	 * 
	 * @param set the result set to fetch from
	 * @return the fullname in the current row of the set
	 * @throws SQLException if one occurs
	 */
	protected Fullname fetchFromSet(ResultSet set) throws SQLException {
		return new Fullname(set.getInt("id"), set.getString("fullname"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE fullnames ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "fullname VARCHAR(50) NOT NULL, "
				+ "PRIMARY KEY(id))");
		statement.close();
	}
}
