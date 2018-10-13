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
import me.timothy.bots.database.LastInfoPMMapping;
import me.timothy.bots.models.LastInfoPM;

public class MysqlLastInfoPMMapping extends MysqlObjectWithIDMapping<LastInfoPM> implements LastInfoPMMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlLastInfoPMMapping(USLDatabase database, Connection connection) {
		super(database, connection, "last_info_pms", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.INTEGER, "banned_person_id"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(LastInfoPM a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0)
				statement = connection.prepareStatement("UPDATE " + table + " SET mod_person_id=?, banned_person_id=?, created_at=? WHERE id=?");
			else
				statement = connection.prepareStatement("INSERT INTO " + table + " (mod_person_id, banned_person_id, created_at) VALUES (?, ?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			
			int counter = 1;
			statement.setInt(counter++, a.modPersonID);
			statement.setInt(counter++, a.bannedPersonID);
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
	public LastInfoPM fetchByModAndUser(int modPersonID, int bannedPersonID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE mod_person_id=? AND banned_person_id=? ORDER BY created_at DESC LIMIT 1",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, modPersonID), new MysqlTypeValueTuple(Types.INTEGER, bannedPersonID)),
				fetchFromSetFunction());
	}

	@Override
	protected LastInfoPM fetchFromSet(ResultSet set) throws SQLException {
		return new LastInfoPM(set.getInt("id"), set.getInt("mod_person_id"), set.getInt("banned_person_id"), set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "banned_person_id INT NOT NULL, "
				+ "mod_person_id INT NOT NULL, "
				+ "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "
				+ "PRIMARY KEY(id),"
				+ "INDEX ind_linfmodpers_id (mod_person_id), "
				+ "INDEX ind_linfbanpers_id (banned_person_id),"
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (banned_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
