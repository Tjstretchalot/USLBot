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
import me.timothy.bots.database.RepropagationRequestMapping;
import me.timothy.bots.models.RepropagationRequest;

public class MysqlRepropagationRequestMapping extends MysqlObjectWithIDMapping<RepropagationRequest> implements RepropagationRequestMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlRepropagationRequestMapping(USLDatabase database, Connection connection) {
		super(database, connection, "repropagation_requests", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.INTEGER, "requester_person_id"),
				new MysqlColumn(Types.LONGVARCHAR, "reason"),
				new MysqlColumn(Types.BIT, "approved"),
				new MysqlColumn(Types.TIMESTAMP, "received_at"),
				new MysqlColumn(Types.TIMESTAMP, "handled_at"));
	}

	@Override
	public void save(RepropagationRequest a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.receivedAt != null) { a.receivedAt.setNanos(0); }
		if(a.handledAt != null) { a.handledAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (requester_person_id, reason, approved, received_at, handled_at) "
						+ "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET requester_person_id=?, reason=?, approved=?, received_at=?, handled_at=? "
						+ "WHERE id=?");
			}
			
			int counter = 1;
			statement.setInt(counter++, a.requestingPersonID);
			statement.setString(counter++, a.reason);
			statement.setBoolean(counter++, a.approved);
			statement.setTimestamp(counter++, a.receivedAt);
			statement.setTimestamp(counter++, a.handledAt);
			
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
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<RepropagationRequest> fetchUnhandled() {
		return fetchByAction("SELECT * FROM " + table + " WHERE handled_at IS NULL", 
				new PreparedStatementSetVarsUnsafe(),
				fetchListFromSetFunction());
	}

	@Override
	protected RepropagationRequest fetchFromSet(ResultSet set) throws SQLException {
		return new RepropagationRequest(set.getInt("id"), set.getInt("requester_person_id"), set.getString("reason"), 
				set.getBoolean("approved"), set.getTimestamp("received_at"), set.getTimestamp("handled_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + table + " ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "requester_person_id INT NOT NULL, "
					+ "reason TEXT NOT NULL, "
					+ "approved TINYINT(1) NOT NULL, "
					+ "received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "handled_at TIMESTAMP NULL DEFAULT NULL, "
					+ "PRIMARY KEY (id), "
					+ "INDEX ind_repropreq_pers_id (requester_person_id), "
					+ "FOREIGN KEY (requester_person_id) REFERENCES persons(id)"
					+ ")");
		}
	}

}
