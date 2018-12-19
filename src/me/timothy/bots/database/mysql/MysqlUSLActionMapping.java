package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.USLActionMapping;
import me.timothy.bots.models.USLAction;

public class MysqlUSLActionMapping extends MysqlObjectWithIDMapping<USLAction> implements USLActionMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlUSLActionMapping(USLDatabase database, Connection connection) {
		super(database, connection, "usl_actions",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.BIT, "is_ban"),
				new MysqlColumn(Types.BIT, "is_latest"),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(USLAction a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if (a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (is_ban, is_latest, person_id, created_at) "
						+ "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET is_ban=?, is_latest=?, person_id=?, created_at=? "
						+ "WHERE id=?");
			}
			
			int counter = 1;
			statement.setBoolean(counter++, a.isBan);
			statement.setBoolean(counter++, a.isLatest);
			statement.setInt(counter++, a.personID);
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
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<USLAction> getActionsAfter(int startId, int limit, boolean getNonLatest) {
		StringBuilder query = new StringBuilder("SELECT * FROM ");
		query.append(table);
		query.append(" WHERE id >= ? ");
		if(!getNonLatest)
			query.append("AND is_latest=1 ");
		query.append("LIMIT ?");
		return fetchByAction(query.toString(), 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, startId), new MysqlTypeValueTuple(Types.INTEGER, limit)),
				fetchListFromSetFunction());
	}

	@Override
	public USLAction fetchLatest(int personId) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=? AND is_latest=1 LIMIT 1",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personId)),
				fetchFromSetFunction());
	}

	@Override
	public List<USLAction> fetchByPerson(int personId) {
		return fetchByAction("SELECT * FROM " + table + " WHERE person_id=?",
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, personId)),
				fetchListFromSetFunction());
	}

	@Override
	public USLAction create(boolean isBan, int personId, Timestamp occurredAt) {
		try {
			PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET is_latest=0 WHERE person_id=?");
			statement.setInt(1, personId);
			statement.execute();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		USLAction row = new USLAction(-1, isBan, true, personId, new Timestamp(occurredAt.getTime()));
		save(row);
		return row;
	}

	@Override
	protected USLAction fetchFromSet(ResultSet set) throws SQLException {
		return new USLAction(set.getInt("id"), set.getBoolean("is_ban"), set.getBoolean("is_latest"), 
				set.getInt("person_id"), set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "is_ban TINYINT(1) NOT NULL, "
				+ "is_latest TINYINT(1) NOT NULL, "
				+ "person_id INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "PRIMARY KEY (id), "
				+ "INDEX ind_uslaction_person_id (person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
