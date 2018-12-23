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
import me.timothy.bots.database.PropagatorSettingMapping;
import me.timothy.bots.models.PropagatorSetting;
import me.timothy.bots.models.PropagatorSetting.PropagatorSettingKey;

public class MysqlPropagatorSettingMapping extends MysqlObjectWithIDMapping<PropagatorSetting> implements PropagatorSettingMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlPropagatorSettingMapping(USLDatabase database, Connection connection) {
		super(database, connection,
				"propagator_settings", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "property_key"),
				new MysqlColumn(Types.VARCHAR, "property_value"));
	}

	@Override
	public void save(PropagatorSetting a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		try {
			PreparedStatement statement;
			if(a.id < 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (property_key, property_value) VALUES (?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET property_key=?, property_value=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setString(counter++, a.key.stringRepr);
			statement.setString(counter++, a.value);

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
	public void put(PropagatorSettingKey key, String value) {
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (property_key, property_value) VALUES (?, ?) "
				+ "ON DUPLICATE KEY UPDATE property_value=?")) {
			int counter = 1;
			statement.setString(counter++, key.stringRepr);
			statement.setString(counter++, value);
			statement.setString(counter++, value);
			statement.execute();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String get(PropagatorSettingKey key) {
		try (PreparedStatement statement = connection.prepareStatement("SELECT property_value FROM " + table + " WHERE property_key=?")) {
			statement.setString(1, key.stringRepr);
			
			try(ResultSet set = statement.executeQuery()) {
				if(!set.next())
					return null;
				return set.getString(1);
			}
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected PropagatorSetting fetchFromSet(ResultSet set) throws SQLException {
		return new PropagatorSetting(set.getInt("id"), set.getString("property_key"), set.getString("property_value"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "property_key VARCHAR(255) NOT NULL, "
					+ "property_value VARCHAR(255) NOT NULL, "
					+ "PRIMARY KEY (id), "
					+ "UNIQUE KEY (property_key)"
					+ ")");
		}
	}

}
