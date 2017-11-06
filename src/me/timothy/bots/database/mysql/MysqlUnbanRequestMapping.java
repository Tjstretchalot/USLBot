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
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.models.UnbanRequest;

public class MysqlUnbanRequestMapping extends MysqlObjectWithIDMapping<UnbanRequest> implements UnbanRequestMapping {
	private static final Logger logger = LogManager.getLogger();
	
	public MysqlUnbanRequestMapping(USLDatabase database, Connection connection) {
		super(database, connection, "unban_requests",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "mod_person_id"),
				new MysqlColumn(Types.INTEGER, "banned_person_id"), 
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "handled_at"), 
				new MysqlColumn(Types.BIT, "invalid"));
	}

	@Override
	public void save(UnbanRequest a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.handledAt != null) { a.handledAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0) {
				statement = connection.prepareStatement("UPDATE " + table + " SET mod_person_id=?, banned_person_id=?, created_at=?, handled_at=?, invalid=? WHERE id=?");
			}else {
				statement = connection.prepareStatement("INSERT INTO " + table + " (mod_person_id, banned_person_id, created_at, handled_at, invalid) VALUES (?, ?, ?, ?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			}
			
			int counter = 1;
			statement.setInt(counter++, a.modPersonID);
			statement.setInt(counter++, a.bannedPersonID);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.handledAt);
			statement.setBoolean(counter++, a.invalid);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.executeUpdate();
			}else {
				statement.executeUpdate();
				
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
	public List<UnbanRequest> fetchUnhandled(int limit) {
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_at IS NULL LIMIT ?", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, limit)),
				fetchListFromSetFunction());
	}

	@Override
	public UnbanRequest fetchLatestValidByBannedPerson(int personID) {
		return fetchByAction("SELECT * FROM " + table + " WHERE banned_person_id=? AND (handled_at IS NULL OR invalid=0) ORDER BY created_at DESC LIMIT 1", 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personID)),
				fetchFromSetFunction());
	}

	@Override
	protected UnbanRequest fetchFromSet(ResultSet set) throws SQLException {
		return new UnbanRequest(set.getInt("id"), set.getInt("mod_person_id"), set.getInt("banned_person_id"), 
				set.getTimestamp("created_at"), set.getTimestamp("handled_at"), set.getBoolean("invalid"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "mod_person_id INT NOT NULL, "
				+ "banned_person_id INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "handled_at TIMESTAMP NULL DEFAULT NULL, "
				+ "invalid TINYINT(1) NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_banreqmodpers_id (mod_person_id), "
				+ "INDEX ind_banreqbanpers_id (banned_person_id), "
				+ "FOREIGN KEY (mod_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (banned_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
