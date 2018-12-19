package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	/** Effectively acting as a function definition */
	protected interface PreparedStatementSetVars {
		/**
		 * Call statement.setInt like functions for the statement that this
		 * is paired with.
		 * 
		 * @param statement the statement
		 * @throws SQLException if one occurs
		 */
		public void setVars(PreparedStatement statement) throws SQLException;
	}
	
	/** Maps Types to values **/
	protected class MysqlTypeValueTuple {
		/** mysql type */
		public final int type;
		/** mysql value */
		public final Object value;
		
		/**
		 * Create a new mysql type/value tuple
		 * @param type the type (from Types)
		 * @param value the value
		 */
		public MysqlTypeValueTuple(int type, Object value) {
			this.type = type;
			this.value = value;
		}
	}
	
	/** This class uses a ton of casting to reduce boilerplate. Down-side is no compiler-type-checks  */
	protected class PreparedStatementSetVarsUnsafe implements PreparedStatementSetVars {
		protected MysqlTypeValueTuple[] typeValueTuples;
		
		/**
		 * Compiler thinks this can polute the heap. Seems like that would require a lot
		 * of effort since this class is protected and no generics should ever go in here
		 * 
		 * @param typeValueTuples the tuples
		 */
		@SafeVarargs
		public PreparedStatementSetVarsUnsafe(MysqlTypeValueTuple... typeValueTuples) {
			this.typeValueTuples = typeValueTuples;
		}
		
		@Override
		public void setVars(PreparedStatement statement) throws SQLException {
			int counter = 1;
			for(MysqlTypeValueTuple tuple : typeValueTuples) {
				switch(tuple.type) { 
				case Types.INTEGER:
					statement.setInt(counter++, (int)tuple.value);
					break;
				case Types.BIT:
					statement.setBoolean(counter++, (boolean)tuple.value);
					break;
				case Types.VARCHAR:
				case Types.LONGVARCHAR:
					statement.setString(counter++, (String)tuple.value);
					break;
				case Types.TIMESTAMP:
					statement.setTimestamp(counter++, (Timestamp)tuple.value);
					break;
				default:
					throw new IllegalArgumentException("Unknown type " + tuple.type + ", you probably just need to add it to this function.");
				}
			}
		}
	}
	/** Effectively acting as a function definition */
	protected interface PreparedStatementFetchResult<B> {
		/**
		 * Call fetchFromSet like functions for the statement that this
		 * is paired with. Should not close the set, and should skip
		 * one row.
		 * 
		 * @param set the set to fetch from
		 * @return the B the set described 
		 * @throws SQLException if one occurs
		 */
		public B fetchResult(ResultSet set) throws SQLException;
	}
	
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
	public List<A> fetchAll() {
		return fetchByAction("SELECT * FROM " + table, null, fetchListFromSetFunction());
	}
	
	/**
	 * Runs the given statement, setting the given variables if they are given, and ignores the result.
	 * 
	 * @param statement the SQL to execute
	 * @param setVars the variables to set, if any
	 */
	protected void runStatement(String statement, PreparedStatementSetVars setVars) {
		try {
			PreparedStatement pStatement = connection.prepareStatement(statement);
			if(setVars != null)
				setVars.setVars(pStatement);
			
			pStatement.execute();
			pStatement.close();
		}catch(SQLException e) {
			logger.error("SQLException occurred on MysqlObjectMapping<A>#runStatement. statement=" + statement + ", table=" + table);
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a preparedstatement statement, then calls setVars, then executes the prepared statement
	 * as a query, caches the result of the fetchFunc on that result, then closes the set and prepared
	 * statement and returns the cached result.
	 * 
	 * @param statement the statement
	 * @param setVars the setvars function or null
	 * @param fetchFunc the function that fetches the B from the result set 
	 * @return result from fetchFunc
	 */
	protected <B> B fetchByAction(String statement, PreparedStatementSetVars setVars, PreparedStatementFetchResult<B> fetchFunc) {
		try {
			PreparedStatement pStatement = connection.prepareStatement(statement);
			if(setVars != null)
				setVars.setVars(pStatement);
			
			ResultSet set = pStatement.executeQuery();
			B result = fetchFunc.fetchResult(set);
			set.close();
			
			pStatement.close();
			return result;
		}catch(SQLException e) {
			logger.error("SQLException occurred on MysqlObjectMapping<A>#fetchByAction. statement=" + statement + ", table=" + table);
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Wrapper around fetchListFromSet so it can be passed to fetchByAction
	 * 
	 * @return wrapped fetchListFromSet
	 */
	protected PreparedStatementFetchResult<List<A>> fetchListFromSetFunction() {
		return new PreparedStatementFetchResult<List<A>>() {

			@Override
			public List<A> fetchResult(ResultSet set) throws SQLException {
				return fetchListFromSet(set);
			}
			
		};
	}
	
	/**
	 * Wrapper around fetchFromSet so it can be passed to fetchByAction. Returns
	 * null if set.next() returns false.
	 * 
	 * @return wrapped fetchFromSet
	 */
	protected PreparedStatementFetchResult<A> fetchFromSetFunction() {
		return new PreparedStatementFetchResult<A>() {
			@Override
			public A fetchResult(ResultSet set) throws SQLException {
				if(set.next()) {
					return fetchFromSet(set);
				}
				return null;
			}
		};
	}
	
	/**
	 * Fetch the int in the first column of the first row of the set. Returns
	 * null if set.next() returns false.
	 * 
	 * @return A statement fetch result that just grabs the id in the first row first column
	 */
	protected PreparedStatementFetchResult<Integer> fetchFirstIntFromSetFunction() {
		return new PreparedStatementFetchResult<Integer>() {

			@Override
			public Integer fetchResult(ResultSet set) throws SQLException {
				if(set.next()) {
					return set.getInt(1);
				}
				return null;
			}
			
		};
	}
	
	/**
	 * Fetch the int in the first column for all the rows in the set. Returns an empty set
	 * if set.next() returns false on the first call.
	 * 
	 * @return A statement fetch result that just grabs all the ints in the first column
	 */
	protected PreparedStatementFetchResult<List<Integer>> fetchFirstColumnIntsFromSetFunction() {
		return new PreparedStatementFetchResult<List<Integer>>() {

			@Override
			public List<Integer> fetchResult(ResultSet set) throws SQLException {
				List<Integer> result = new ArrayList<>();
				while(set.next()) {
					result.add(set.getInt(1));
				}
				return result;
			}
			
		};
	}
	
	/**
	 * Creates ?, ?, ? etc., where there are num
	 * question marks in the resulting string.
	 * 
	 * @param num number of question marks
	 * @return string with num question marks delimited by commas
	 */
	protected String createPlaceholders(int num) {
		return String.join(", ", Collections.nCopies(num, "?"));
	}
	
	/**
	 * Fetches every a in the set. Starts with set.next(), so skips the current
	 * row if the set has already been accessed.
	 * 
	 * @param set the set to fetch from
	 * @return the list of as in the set
	 * @throws SQLException if one occurs
	 */
	protected List<A> fetchListFromSet(ResultSet set) throws SQLException 
	{
		List<A> as = new ArrayList<>();
		while(set.next()) {
			as.add(fetchFromSet(set));
		}
		return as;
	}

	/**
	 * Fetches the A in the current row of the set.
	 * 
	 * @param set the set to fetch from
	 * @return the a in the current row
	 * @throws SQLException if one occurs
	 */
	protected abstract A fetchFromSet(ResultSet set) throws SQLException;
	
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
	
	public void truncate() {
		try {
			Statement statement = connection.createStatement();
			statement.execute("TRUNCATE TABLE " + table);
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
		int foundCounter = 0;
		List<String> errors = new ArrayList<>();
		
		ResultSet columns = metadata.getColumns(null, null, table, "%");
		
		while(columns.next()) {
			MysqlColumn tmp = verifyColumn(toFind, errors, columns);
			if(tmp != null) {
				foundCounter++;
			}
		}
		
		columns.close();
		
		if(foundCounter != toFind.size()) {
			errors.add("Expected a total of " + toFind.size() + " columns but got " + foundCounter);
		}
		
		if(errors.size() > 0) {
			String errorMess = errors.stream().collect(Collectors.joining("; "));
			throw new IllegalStateException(errorMess);
		}
	}
	
	/**
	 * Verifies a column returned from DatabaseMetaData is expected. If the column is unexpected
	 * or there are differences between it and the expected value, errors should be appended with
	 * a string describing the issue.
	 * 
	 * @param toFind list wrapped version of columns
	 * @param errors the current errors
	 * @param columns the result set. you should check the current row of the result set
	 * @throws SQLException if one occurs
	 */
	protected MysqlColumn verifyColumn(List<MysqlColumn> toFind, List<String> errors, ResultSet columns) throws SQLException {
		String name = columns.getString(4);
		int datatype = columns.getInt(5);
		String autoIncrement = columns.getString(23);
		
		MysqlColumn real = new MysqlColumn(datatype, name, autoIncrement.equals("YES"));
		Optional<MysqlColumn> found = toFind.stream().filter(col -> col.name.equals(name)).findFirst();
		
		if(!found.isPresent()) {
			errors.add(String.format("unexpected column %s", name));
			return null;
		}else if(!real.equals(found.get())) {
			errors.add(found.get().getDifferencePretty(real));
		}
		return found.get();
	}
}
