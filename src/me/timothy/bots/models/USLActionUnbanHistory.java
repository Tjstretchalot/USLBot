package me.timothy.bots.models;

import me.timothy.bots.database.MappingDatabase;

/**
 * This is a many-to-many mapping of USLActions to UnbanHistory's. This just gives us the relevant
 * unban histories for the given person, at the time of the USLAction. This should contain ALL of 
 * the latest unbans for the given person.
 * 
 * If a person is neither in the USLActionBanHistory nor the USLActionUnbanHistory mapping for a given
 * subreddit, then he is either not banned or banned prior to the uslbots history for that subreddit.
 * Because of that "or" this should only be checked once during the propagation step.
 * 
 * @author Timothy
 */
public class USLActionUnbanHistory {
	/** The id of the USLAction that is mapped to the unban history */
	public int actionID;
	/** The id of the unban history that is mapped to the action */
	public int unbanHistoryID;
	
	/**
	 * @param actionID the id of the action to map to the unban history
	 * @param unbanHistoryID the id of the unban history to map to the action
	 */
	public USLActionUnbanHistory(int actionID, int unbanHistoryID) {
		super();
		this.actionID = actionID;
		this.unbanHistoryID = unbanHistoryID;
	}
	
	/**
	 * Determines if this is a potentially valid row for saving to the database
	 * 
	 * @return if this is a potentially valid row
	 */
	public boolean isValid() {
		return actionID > 0 && unbanHistoryID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actionID;
		result = prime * result + unbanHistoryID;
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
		USLActionUnbanHistory other = (USLActionUnbanHistory) obj;
		if (actionID != other.actionID)
			return false;
		if (unbanHistoryID != other.unbanHistoryID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "USLActionUnbanHistory [actionID=" + actionID + ", unbanHistoryID=" + unbanHistoryID + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[action=" + db.getUSLActionMapping().fetchByID(actionID).toPrettyString(db)
				+ ", unban=" + db.getUnbanHistoryMapping().fetchByID(unbanHistoryID).toPrettyString(db) + "]";
	}
}
