package me.timothy.bots.models;

/**
 * This model is for the settings for the propagator. It gets fancier settings since
 * we want editing capability as well.
 * 
 * @author Timothy
 */
public class PropagatorSetting {
	public static enum PropagatorSettingKey {
		/** 
		 * Suppress no-operation messages, which are messages of the form "Not unbanning /u/XYZ on ABC because FGH" or
		 * "Not banning because reasons", which are messages stating things we *won't* do. This setting should be the
		 * string "true" or "false" and is automatically toggled off 
		 */
		SUPPRESS_NO_OP_MESSAGES("suppress_no_op")
		;
		
		public final String stringRepr;
		
		private PropagatorSettingKey(String stringRepr) {
			this.stringRepr = stringRepr;
		}
		
		public static PropagatorSettingKey fromDatabase(String dbVal) {
			if(dbVal.equals(SUPPRESS_NO_OP_MESSAGES.stringRepr))
				return SUPPRESS_NO_OP_MESSAGES;
			
			throw new IllegalArgumentException("Unknown propagator setting key: " + dbVal);
		}
	}
	
	/** The database row id */
	public int id;
	/** The parsed setting key */
	public PropagatorSettingKey key;
	/** The parsed setting value */
	public String value;
	
	/**
	 * Create a new setting from the parsed key
	 * @param id the row id or -1 if not in the database
	 * @param key the key
	 * @param value the value
	 */
	public PropagatorSetting(int id, PropagatorSettingKey key, String value) {
		super();
		this.id = id;
		this.key = key;
		this.value = value;
	}
	
	/**
	 * Create a setting from the database row
	 * @param id the row id or -1 if not in the database
	 * @param key the key
	 * @param value the value
	 */
	public PropagatorSetting(int id, String key, String value) {
		super();
		this.id = id;
		this.key = PropagatorSettingKey.fromDatabase(key);
		this.value = value;
	}
	
	/**
	 * Determines if this is a potentially valid row
	 * @return if this row passes the sniff test
	 */
	public boolean isValid() {
		return key != null && value != null && !value.isEmpty();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		PropagatorSetting other = (PropagatorSetting) obj;
		if (id != other.id)
			return false;
		if (key != other.key)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "PropagatorSetting [id=" + id + ", key=" + key.stringRepr + ", value=" + value + "]";
	}
}
