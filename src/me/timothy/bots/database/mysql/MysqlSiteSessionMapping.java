package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.SiteSessionMapping;
import me.timothy.bots.models.SiteSession;

public class MysqlSiteSessionMapping extends MysqlObjectWithIDMapping<SiteSession> implements SiteSessionMapping {

	public MysqlSiteSessionMapping(USLDatabase database, Connection connection) {
		super(database, connection, "site_sessions", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "session_id"),
				new MysqlColumn(Types.INTEGER, "person_id"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "expires_at"));
	}

	@Override
	public void save(SiteSession a) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Site sessions database operations are not implemented yet");
	}

	@Override
	protected SiteSession fetchFromSet(ResultSet set) throws SQLException {
		throw new UnsupportedOperationException("Site sessions database operations are not implemented yet");
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "session_id VARCHAR(255) NOT NULL, "
				+ "person_id INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', "
				+ "expires_at TIMESTAMP NULL DEFAULT NULL, "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE (session_id), "
				+ "INDEX ind_siteses_pers_id (person_id), "
				+ "FOREIGN KEY (person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
