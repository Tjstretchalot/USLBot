package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import me.timothy.bots.USLDatabase;

public abstract class MysqlObjectWithIDMapping<A> extends MysqlObjectMapping<A> {
	protected MysqlObjectWithIDMapping(USLDatabase database, Connection connection, String table,
			MysqlColumn... columns) {
		super(database, connection, table, columns);
	}

	
	/**
	 * Fetches A from the current table 
	 * @param id
	 * @return
	 */
	public A fetchByID(int id) {
		return fetchByAction("SELECT * FROM " + table + " WHERE id=? LIMIT 1", new PreparedStatementSetVars() {

			@Override
			public void setVars(PreparedStatement statement) throws SQLException {
				int counter = 1;
				statement.setInt(counter++, id);
			}
			
		}, fetchFromSetFunction());
	}
}
