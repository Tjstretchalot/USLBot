package me.timothy.bots.database.mysql;

import java.sql.Types;
import java.util.Arrays;
import java.util.Optional;

/**
 * Describes a MySQL Column
 * 
 * @author Timothy
 */
public class MysqlColumn {
	/**
	 * A way of prettifying column types from java.sql.Types
	 * 
	 * @author Timothy
	 */
	private enum ColumnType {
		INTEGER(Types.INTEGER, "int"),
		LONGNVARCHAR(Types.LONGNVARCHAR, "longnvarchar"),
		LONGVARCHAR(Types.LONGVARCHAR, "text"),
		VARCHAR(Types.VARCHAR, "varchar"),
		BIT(Types.BIT, "tinyint"),
		TIMESTAMP(Types.TIMESTAMP, "timestamp")
		
		;
		
		private int type;
		private String description;
		
		ColumnType(int type, String description) {
			this.type = type;
			this.description = description;
		}
		
		public static ColumnType getByType(int type) {
			Optional<ColumnType> res = Arrays.asList(values()).stream().filter(ctype -> ctype.type == type).findFirst();
			if(res.isPresent())
				return res.get();
			return null;
		}
		
		public String getDescription() {
			return description;
		}
	}
	/**
	 * The type of the column 
	 * 
	 * @see java.sql.Types
	 */
	public int type;
	
	/**
	 * The name of the column
	 */
	public String name;
	
	/**
	 * If autoincrement is expected
	 */
	public boolean autoIncrement;

	/**
	 * Creates the MySQL column
	 * @param type column type
	 * @param name column name
	 * @param autoIncrement autoincrement 
	 */
	public MysqlColumn(int type, String name, boolean autoIncrement) {
		super();
		this.type = type;
		this.name = name;
		this.autoIncrement = autoIncrement;
	}

	/**
	 * Creates a mysql column, assumes that autoincrement is false
	 * @param type the type of the column
	 * @param name the name of the column
	 */
	public MysqlColumn(int type, String name) {
		this(type, name, false);
	}

	/**
	 * Describes the difference between this column and <code>other</code>,
	 * where this column is the "expected" column and <code>other</code> is
	 * the actual column (presumably with the same name)
	 * 
	 * @param other the actual column
	 * @return the difference between the expected and actual, e.g. "expected id to be an int and autoincrement"
	 */
	public String getDifferencePretty(MysqlColumn other) {
		if(this.equals(other))
			return null;
		if(!this.name.equals(other.name))
			throw new IllegalArgumentException(this + " is not comparable to " + other);
		
		if(type == other.type && autoIncrement != other.autoIncrement) {
			return String.format("expected %s%s to autoincrement", name, autoIncrement ? "" : " not");
		}else if(type != other.type && autoIncrement == other.autoIncrement) {
			return String.format("expected %s to be of type %s (got %s)", name, ColumnType.getByType(type).getDescription(), ColumnType.getByType(other.type).getDescription());
		}else if(type != other.type && autoIncrement != other.autoIncrement) {
			return String.format("expected %s to be of type %s (got %s) and%s autoincrement", name, ColumnType.getByType(type).getDescription(), ColumnType.getByType(other.type).getDescription(), autoIncrement ? "" : " not");
		}
		
		throw new RuntimeException("this shouldn't happen");
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (autoIncrement ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MysqlColumn other = (MysqlColumn) obj;
		if (autoIncrement != other.autoIncrement)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MysqlColumn [type=" + type + ", name=" + name + ", autoIncrement=" + autoIncrement + "]";
	}
	
	
}
