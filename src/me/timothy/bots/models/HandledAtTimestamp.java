package me.timothy.bots.models;

/**
 * This lets us know what user activities we have already converted into USLAction's. We also keep
 * track of the timestamp that we've done all the conversion up to, so we only need to store the
 * stuff at that particular timestamp since we may have only gotten some of them but not all of
 * them.
 * 
 * @author Timothy
 */
public class HandledAtTimestamp {
	public enum HandledAtTimestampType {
		BAN_HISTORY((byte)0), UNBAN_HISTORY((byte)1), VALID_UNBAN_REQUEST((byte)2);
		
		/** The value that represents this type in the database */
		public final byte databaseValue;
		
		private HandledAtTimestampType(byte databaseValue) {
			this.databaseValue = databaseValue;
		}
		
		public static HandledAtTimestampType getByDatabaseValue(byte dbValue) {
			if(dbValue == 0)
				return BAN_HISTORY;
			else if(dbValue == 1)
				return UNBAN_HISTORY;
			
			if(dbValue != 2)
				throw new IllegalArgumentException("dbValue=" + dbValue);
			
			return VALID_UNBAN_REQUEST;
		}
	}
	
	/** The primary key of the thing that we already handled */
	public int primaryKey;
	/** The type of thing that we handled */
	public HandledAtTimestampType type;
	
	/**
	 * @param primaryKey the primary key for the thing we handled
	 * @param type which thing the primary key refers to
	 */
	public HandledAtTimestamp(int primaryKey, HandledAtTimestampType type) {
		super();
		this.primaryKey = primaryKey;
		this.type = type;
	}

	/**
	 * Create a handled at timestamp from the database row directly
	 * @param primaryKey the primary key for the thing we handled
	 * @param type which thing the primary key refers to
	 */
	public HandledAtTimestamp(int primaryKey, byte type) {
		this(primaryKey, HandledAtTimestampType.getByDatabaseValue(type));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + primaryKey;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HandledAtTimestamp other = (HandledAtTimestamp) obj;
		if (primaryKey != other.primaryKey) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HandledAtTimestamp [primaryKey=" + primaryKey + ", type=" + type + "]";
	}
}
