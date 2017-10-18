package me.timothy.bots.database.mysql;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.ObjectMapping;
import me.timothy.bots.database.SchemaValidator;

/**
 * Describes an ObjectMapping and SchemaValidator combination based on mysql
 * 
 * @author Timothy
 *
 * @param <A> What this mapping maps
 */
public abstract class MysqlObjectMapping<A> implements ObjectMapping<A>, SchemaValidator {
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Using other mappings is discouraged since it couples mappings, however
	 * the practicality occasionally offsets the principle here.
	 */
	protected USLDatabase database;
	protected Connection connection;
	
	/**
	 * The table name of the schema
	 */
	protected String table;
	/**
	 * The columns that are expected to be in the schema
	 */
	protected MysqlColumn[] columns;
	
	/**
	 * Sets the {@code connection} to the specified connection and the 
	 * {@code database} to the specified database, as well as enough information
	 * to do general column schema verification.
	 * 
	 * @param database the database 
	 * @param connection the mysql connection
	 * @param table the table this mapping maps to in the datbase
	 * @param columns a description of the columns that are expected.
	 */
	protected MysqlObjectMapping(USLDatabase database, Connection connection, String table, MysqlColumn... columns) {
		this.database = database;
		this.connection = connection;
		this.table = table;
		this.columns = columns;
	}
	
	@Override
	public void validateSchema() {
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			ResultSet results = metadata.getTables(null, null, table, null);
			boolean exists = results.next();
			results.close();
			
			if(exists) {
				verifyColumns(metadata);
			}else {
				createTable();
			}
		}catch(SQLException ex) {
			logger.throwing(ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void purgeSchema() {
		try {
			Statement statement = connection.createStatement();
			statement.execute("DROP TABLE IF EXISTS " + table);
			statement.close();
		}catch(SQLException ex) {
			logger.throwing(ex);
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates the table in the database
	 * 
	 * @throws SQLException if one occurs
	 */
	protected abstract void createTable() throws SQLException;
	
	/**
	 * Verifies the columns in the table using the metadata of the
	 * database.
	 * @param metadata the metadata
	 * @throws SQLException if one occurs
	 * @throws IllegalStateException if the columns aren't whats expected
	 */
	protected void verifyColumns(DatabaseMetaData metadata) throws SQLException, IllegalStateException {
		List<MysqlColumn> toFind = Arrays.asList(columns);
		List<String> errors = new ArrayList<>();
		
		ResultSet columns = metadata.getColumns(null, null, table, "%");
		
		while(columns.next()) {
			String name = columns.getString(4);
			int datatype = columns.getInt(5);
			String autoIncrement = columns.getString(23);
			
			MysqlColumn real = new MysqlColumn(datatype, name, autoIncrement.equals("YES"));
			Optional<MysqlColumn> found = toFind.stream().filter(col -> col.name.equals(name)).findFirst();
			
			if(!found.isPresent()) {
				errors.add(String.format("unexpected column %s", name));
			}else if(!real.equals(found.get())) {
				errors.add(found.get().getDifferencePretty(real));
			}
		}
		
		columns.close();
		
		if(errors.size() > 0) {
			String errorMess = errors.stream().collect(Collectors.joining("; "));
			throw new IllegalStateException(errorMess);
		}
	}
}
