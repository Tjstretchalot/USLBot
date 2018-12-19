package me.timothy.bots.models;

import me.timothy.bots.database.MappingDatabase;

/**
 * Links a persons to an unban on a particular subreddit, identified by
 * a modaction. This is so that when our USL information on a user is 
 * queried no important information is missed.
 * 
 * @author Timothy
 */
public class UnbanHistory {
	public int id;
	public int modPersonID;
	public int unbannedPersonID;
	public int handledModActionID;
	
	/**
	 * @param id internal database id or -1 if not in database
	 * @param modPersonID the database id of the person who unbanned 
	 * @param unbannedPersonID the database id of the person who was unbanned
	 * @param handledModActionID the database id of the handled mod action
	 */
	public UnbanHistory(int id, int modPersonID, int unbannedPersonID, int handledModActionID) {
		super();
		this.id = id;
		this.modPersonID = modPersonID;
		this.unbannedPersonID = unbannedPersonID;
		this.handledModActionID = handledModActionID;
	}
	
	/**
	 * Determines if this UnbanHistory is complete enough to save to the
	 * database
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return modPersonID > 0 && unbannedPersonID > 0 && handledModActionID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + modPersonID;
		result = prime * result + unbannedPersonID;
		result = prime * result + handledModActionID;
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
		UnbanHistory other = (UnbanHistory) obj;
		if (id != other.id)
			return false;
		if (handledModActionID != other.handledModActionID)
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		if (unbannedPersonID != other.unbannedPersonID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UnbanHistory [id=" + id + ", modPersonID=" + modPersonID + ", unbannedPersonID=" + unbannedPersonID
				+ ", handledModActionID=" + handledModActionID + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[id=" + id + ", mod=" + db.getPersonMapping().fetchByID(modPersonID).username 
				+ ", unbanned=" + db.getPersonMapping().fetchByID(unbannedPersonID).username 
				+ ", hma=" + db.getHandledModActionMapping().fetchByID(handledModActionID).toPrettyString(db) + "]";
	}
}
