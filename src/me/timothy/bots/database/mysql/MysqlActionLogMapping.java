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
import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.models.ActionLog;

/**
 * Maps action logs to/from the database
 * 
 * @author Timothy
 */
public class MysqlActionLogMapping extends MysqlObjectWithIDMapping<ActionLog> implements ActionLogMapping {
	private static Logger logger = LogManager.getLogger();

	public MysqlActionLogMapping(USLDatabase database, Connection connection) {
		super(database, connection, "actions_log",
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.LONGVARCHAR, "action"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"));
	}

	@Override
	public void save(ActionLog a) throws IllegalArgumentException {
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id > 0)
				statement = connection.prepareStatement("UPDATE " + table + " SET action=?, created_at=? WHERE id=?");
			else
				statement = connection.prepareStatement("INSERT INTO " + table + " (action, created_at) VALUES (?, ?)", 
						Statement.RETURN_GENERATED_KEYS);
			
			int counter = 1;
			statement.setString(counter++, a.action);
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
					throw new RuntimeException("Expected generated keys for " + table);
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
	public void append(String action) {
		try {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (action) VALUES (?)");
			statement.setString(1, action);
			statement.execute();
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		try {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table);
			statement.execute();
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ActionLog> fetchOrderedByTime() {
		return fetchByAction("SELECT * FROM " + table + " ORDER BY created_at DESC", 
				new PreparedStatementSetVarsUnsafe(), 
				fetchListFromSetFunction());
	}

	@Override
	protected ActionLog fetchFromSet(ResultSet set) throws SQLException {
		return new ActionLog(set.getInt("id"), set.getString("action"), set.getTimestamp("created_at"));
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "action TEXT NOT NULL, "
				+ "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "INDEX ind_actlog_crat (created_at))");
		statement.close();
	}

}
