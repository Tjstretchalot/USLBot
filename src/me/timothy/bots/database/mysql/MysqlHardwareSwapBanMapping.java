package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HardwareSwapBanMapping;
import me.timothy.bots.models.HardwareSwapBan;

public class MysqlHardwareSwapBanMapping extends MysqlObjectWithIDMapping<HardwareSwapBan> implements HardwareSwapBanMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlHardwareSwapBanMapping(USLDatabase database, Connection connection) {
		super(database, connection, "hardwareswap_bans", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.LONGVARCHAR, "note"),
				new MysqlColumn(Types.TIMESTAMP, "detected_at"));
	}

	@Override
	public void save(HardwareSwapBan a) throws IllegalArgumentException {
		if(a.detectedAt != null) { a.detectedAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id < 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (person_id, note, detected_at) values (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET person_id=?, note=?, detected_at=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.personID);
			statement.setString(counter++, a.note);
			statement.setTimestamp(counter++, a.detectedAt);
			
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
	public HardwareSwapBan fetchByPersonID(int personID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=? LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personID)),
				fetchFromSetFunction());
	}


	@Override
	public List<HardwareSwapBan> fetchWherePersonNotIn(Set<Integer> personIds) {
		try {
			try(Statement statement = connection.createStatement()) {
				statement.execute("CREATE TEMPORARY TABLE hwspact_personids (person_id int not null, primary key(person_id))");
			
				try(PreparedStatement prepared = connection.prepareStatement("INSERT INTO hwspact_personids (person_id) VALUES (?)")) {
					for(int personId : personIds) {
						prepared.setInt(1, personId);
						prepared.addBatch();
					}
					prepared.executeBatch();
				}
				
				List<HardwareSwapBan> res;
				try(ResultSet set = statement.executeQuery("SELECT * FROM " + table + " WHERE person_id NOT IN (SELECT person_id FROM hwspact_personids)")) {
					res = fetchListFromSet(set);
				}
				
				statement.execute("DROP TABLE hwspact_personids");
				
				return res;
			}
		}catch(SQLException e) { 
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}


	@Override
	public void deleteByID(int id) {
		runStatement("DELETE FROM " + table + " WHERE id=? LIMIT 1",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, id)));
	}

	@Override
	public List<HardwareSwapBan> fetchWithoutAction(int limit) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id NOT IN (SELECT person_id FROM hardwareswap_actions) LIMIT ?",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, limit)), fetchListFromSetFunction());
	}

	@Override
	protected HardwareSwapBan fetchFromSet(ResultSet set) throws SQLException {
		return new HardwareSwapBan(set.getInt("id"), set.getInt("person_id"), set.getString("note"), set.getTimestamp("detected_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "person_id INT NOT NULL, "
					+ "note TEXT, "
					+ "detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "PRIMARY KEY(id),"
					+ "INDEX ind_hwswpbanperson_id (person_id), "
					+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
					+ ")");
		}
	}

}
