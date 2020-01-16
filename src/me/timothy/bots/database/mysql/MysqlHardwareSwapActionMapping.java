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
import me.timothy.bots.database.HardwareSwapActionMapping;
import me.timothy.bots.models.HardwareSwapAction;

public class MysqlHardwareSwapActionMapping extends MysqlObjectWithIDMapping<HardwareSwapAction> implements HardwareSwapActionMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlHardwareSwapActionMapping(USLDatabase database, Connection connection) {
		super(database, connection, "hardwareswap_actions",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.INTEGER, "action_id"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(HardwareSwapAction a) throws IllegalArgumentException {
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, action_id, created_at) VALUES (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, action_id=?, created_at=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.personID);
			statement.setInt(counter++, a.actionID);
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
					throw new RuntimeException("Expected generated keys for table " + table);
				}
				a.id = keys.getInt(1);
				keys.close();
			}
			statement.close();
		}catch(SQLException ex) {
			logger.throwing(ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public HardwareSwapAction fetchByPersonID(int personID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=? LIMIT 1",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personID)),
				fetchFromSetFunction());
	}
	
	@Override
	public void deleteByID(int id) {
		runStatement("DELETE FROM " + table + " WHERE id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, id)));
	}

	@Override
	public List<HardwareSwapAction> fetchActionsOnUnbanned() {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id NOT IN (SELECT person_id FROM hardwareswap_bans)",
				new PreparedStatementSetVarsUnsafe(), fetchListFromSetFunction());
	}
	
	@Override
	protected HardwareSwapAction fetchFromSet(ResultSet set) throws SQLException {
		return new HardwareSwapAction(set.getInt("id"), set.getInt("person_id"), set.getInt("action_id"),
				set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "person_id INT NOT NULL, "
					+ "action_id INT NOT NULL, "
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "PRIMARY KEY(id),"
					+ "INDEX ind_hwswapactionpers_id (person_id), "
					+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
					+ ")");
		}
	}

}
