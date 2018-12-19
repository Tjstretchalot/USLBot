package me.timothy.bots.database.mysql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

/**
 * This handles the implementation of creating a mapping. A mapping of this sort has two columns,
 * both of which have the ids of the columns.
 * 
 * @author Timothy
 *
 * @param <A>
 */
public class MysqlManyToManyMapping<A> extends MysqlObjectMapping<A> {
	private static final Logger logger = LogManager.getLogger();
	
	protected String column1;
	protected String column1References;
	protected String column2;
	protected String column2References;
	
	// Since we don't have to code this a million times let's do the performant thing.
	protected final String saveUpdate;
	protected final String fetchByColumn1Query;
	protected final String fetchByColumn2Query;
	protected final String fetchByBothQuery;
	protected final String deleteQuery;
	
	protected Constructor<A> constructor;
	protected Field column1Field;
	protected Field column2Field;
	
	protected MysqlManyToManyMapping(USLDatabase database, Connection connection, 
			Class<A> genClass,
			String table, String column1, String column1References, String column1Camel,
			String column2, String column2References, String column2Camel) {
		super(database, connection, table, 
				new MysqlColumn(Types.INTEGER, column1),
				new MysqlColumn(Types.INTEGER, column2));
		
		this.column1 = column1;
		this.column1References = column1References;
		this.column2 = column2;
		this.column2References = column2References;
		
		saveUpdate = "INSERT INTO " + table + " (" + column1 + ", " + column2 + ") VALUES (?, ?)";
		fetchByColumn1Query = "SELECT " + column1 + ", " + column2 + " FROM " + table + " WHERE " + column1 + "=?";
		fetchByColumn2Query = "SELECT " + column1 + ", " + column2 + " FROM " + table + " WHERE " + column2 + "=?";
		fetchByBothQuery = "SELECT " + column1 + ", " + column2 + " FROM " + table + " WHERE " + column1 + "=? AND " + column2 + "=?";
		deleteQuery = "DELETE FROM " + table + " WHERE " + column1 + "=? AND " + column2 + "=?";
		
		try {
			constructor = genClass.getConstructor(Integer.TYPE, Integer.TYPE);
			column1Field = genClass.getField(column1Camel);
			column2Field = genClass.getField(column2Camel);
		}catch(NoSuchMethodException | SecurityException | NoSuchFieldException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void save(A a) throws IllegalArgumentException {
		try (PreparedStatement statement = connection.prepareStatement(saveUpdate)) {
			statement.setInt(1, column1Field.getInt(a));
			statement.setInt(2, column2Field.getInt(a));
			statement.execute();
		}catch(SQLException | IllegalAccessException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	public List<A> fetchByCol1(int col1) {
		return fetchByAction(fetchByColumn1Query, 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, col1)),
				fetchListFromSetFunction());
	}
	
	public List<A> fetchByCol2(int col2) {
		return fetchByAction(fetchByColumn2Query,
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, col2)),
				fetchListFromSetFunction());
	}
	
	public List<A> fetchByBoth(int col1, int col2) {
		return fetchByAction(fetchByBothQuery, 
				new PreparedStatementSetVarsUnsafe(new MysqlTypeValueTuple(Types.INTEGER, col1), new MysqlTypeValueTuple(Types.INTEGER, col2)),
				fetchListFromSetFunction());
	}
	
	public List<A> fetch(Integer column1, Integer column2) throws IllegalArgumentException {
		if(column1 == null) {
			if(column2 == null)
				throw new IllegalArgumentException("both columns are null! need at least 1 specified");
			return fetchByCol2(column2.intValue());
		}else if(column2 == null) { 
			return fetchByCol1(column1.intValue());
		}
		return fetchByBoth(column1.intValue(), column2.intValue());
	}
	
	public void delete(int column1, int column2) {
		runStatement(deleteQuery,
				new PreparedStatementSetVarsUnsafe(
						new MysqlTypeValueTuple(Types.INTEGER, column1),
						new MysqlTypeValueTuple(Types.INTEGER, column2)));
	}

	@Override
	protected A fetchFromSet(ResultSet set) throws SQLException {
		try {
			return constructor.newInstance(set.getInt(1), set.getInt(2));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void createTable() throws SQLException {
		try(Statement statement = connection.createStatement()) { 
			statement.execute("CREATE TABLE " + table + " ("
					+ column1 + " INT NOT NULL, "
					+ column2 + " INT NOT NULL, "
					+ "CONSTRAINT PK_" + table + " PRIMARY KEY (" + column1 + ", " + column2 + "), "
					+ "INDEX ind_" + table + "_pk_reverse (" + column2 + ", " + column1 + ")"
					+ ")");
			// skip foreign key constraints for performance. we don't want to do 4 indexes per insert that's madness
		}
	}
}
