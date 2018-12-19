package me.timothy.bots.models;

import me.timothy.bots.database.MappingDatabase;

/**
 * This is a one-to-many relationship. A USLAction may have multiple ban histories that correspond
 * to the action. This should be all the *latest* bans. There should be no bans that are mapped to
 * an action that are not current. If we want that information we'll just search the ban history
 * table directly
 * 
 * @author Timothy
 */
public class USLActionBanHistory {
	/** The id of the USLAction row */
	public int actionID;
	/** The id of the ban history that is being paired with this action */
	public int banHistoryID;
	
	/**
	 * Creates a new mapping. Context is required to know if there are in the database or not.
	 * @param actionId the row of the action
	 * @param banHistId the row of the ban history
	 */
	public USLActionBanHistory(int actionId, int banHistId) {
		this.actionID = actionId;
		this.banHistoryID = banHistId;
	}
	
	/**
	 * Determines if this is potentially valid for saving into the database
	 * @return if this passes the sanity check
	 */
	public boolean isValid() {
		return actionID > 0 && banHistoryID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actionID;
		result = prime * result + banHistoryID;
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
		USLActionBanHistory other = (USLActionBanHistory) obj;
		if (actionID != other.actionID)
			return false;
		if (banHistoryID != other.banHistoryID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "USLActionBanHistory [actionId=" + actionID + ", banHistoryId=" + banHistoryID + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[action=" + db.getUSLActionMapping().fetchByID(actionID).toPrettyString(db) 
				+ ", ban_history=" + db.getBanHistoryMapping().fetchByID(banHistoryID).toPrettyString(db) + "]";
	}
}
