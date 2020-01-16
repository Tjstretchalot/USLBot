package me.timothy.bots.models;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import me.timothy.bots.USLDatabase;

/**
 * This describes some action we took due to to hardwareswap bans. This contains all the 
 * bans that we placed on the main subreddit because of a row in the hardware swap wiki.
 * Once we request an unban, we remove this row.
 * 
 * @author Timothy
 */
public class HardwareSwapAction {
	/** This should be the value of actionId */
	public static final int BAN_ACTION = 1;
	
	/** The database identifier for this row, or -1 if not in the database yet */
	public int id;
	/** The id of the person we banned */
	public int personID;
	/** Unused, should be 1 */
	public int actionID;
	/** The time when we performed this action */
	public Timestamp createdAt;
	
	/**
	 * @param id the database row identifier for this row or -1 if not in the database yet
	 * @param personID the id of the person who we banned
	 * @param actionID unused, should be "BAN_ACTION"
	 * @param createdAt when we created this row
	 */
	public HardwareSwapAction(int id, int personID, int actionID, Timestamp createdAt) {
		super();
		this.id = id;
		this.personID = personID;
		this.actionID = actionID;
		this.createdAt = createdAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actionID;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		result = prime * result + personID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HardwareSwapAction))
			return false;
		HardwareSwapAction other = (HardwareSwapAction) obj;
		if (actionID != other.actionID)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (id != other.id)
			return false;
		if (personID != other.personID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HardwareSwapAction [id=" + id + ", personID=" + personID + ", actionID=" + actionID + ", createdAt="
				+ createdAt + "]";
	}
	
	/**
	 * Creates a slightly prettier string representation of this row
	 * 
	 * @param db the database that this row is in
	 * @return a string representation of this action
	 */
	public String toPrettyString(USLDatabase db) {
		Person person = db.getPersonMapping().fetchByID(personID);
		String personName = person != null ? person.username : "[INVALID ROW]";
		String actionName = actionID == BAN_ACTION ? "BAN_ACTION" : "[INVALID ACTION (val=" + actionID + ")]";
		String createdAtStr = createdAt == null ? null : SimpleDateFormat.getDateTimeInstance().format(createdAt);
		
		return "HardwareSwapAction [id=" + id + ", person=[username='" + personName + "', id=" + personID + "], action=" 
			+ actionName + ", createdAt=" + createdAtStr + "]";
	}
}
