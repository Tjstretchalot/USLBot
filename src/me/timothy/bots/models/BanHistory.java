package me.timothy.bots.models;

import me.timothy.bots.database.MappingDatabase;

/**
 * Links a person to being banned on a particular subreddit. Subreddits
 * are referred to by monitored subreddit id, through the handled mod
 * action id. It is assumed that if we stop monitoring a subreddit, 
 * we don't care about its bans anymore. 
 * If we really want to, we can backup the database.
 * 
 * BanHistory's are *always* information provided by the reddit server,
 * or information parsed from that. If a BanHistory is generated there
 * MUST be a corresponding ModAction (and HandledModAction)
 * 
 * @author Timothy
 */
public class BanHistory {
	public int id;
	public int modPersonID;
	public int bannedPersonID;
	public int handledModActionID;
	public String banDescription;
	public String banDetails;
	
	
	/**
	 * Create a new ban history from reddits information
	 * 
	 * @param id the id of this ban history in our database (or -1 if not in database)
	 * @param modPersonID the person who performed the ban (identified by "mod") as the id of the person in our database
	 * @param bannedPersonID the person was banned (identified by "target_author") as the id of the person in our database
	 * @param handledModActionID the id of the handledmodaction that this was parsed from
	 * @param banDescription the description (provided by moderator when banning on reddit) of the ban. may be null
	 * if banDetails is "changed to" followed by a time (such as "changed to permanent")
	 * @param banDetails the duration of the ban (reddit calls this "details"), such as "permanent" or "90 days". Alternatively,
	 * the string literal "changed to " followed by a duration
	 */
	public BanHistory(int id, int modPersonID, int bannedPersonID, int handledModActionID,
			String banDescription, String banDetails) {
		this.id = id;
		this.modPersonID = modPersonID;
		this.bannedPersonID = bannedPersonID;
		this.handledModActionID = handledModActionID;
		this.banDescription = banDescription;
		this.banDetails = banDetails;
	}

	public boolean isValid() {
		return (modPersonID > 0 && bannedPersonID > 0 && handledModActionID > 0
				&& banDetails != null && banDetails != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((banDescription == null) ? 0 : banDescription.hashCode());
		result = prime * result + ((banDetails == null) ? 0 : banDetails.hashCode());
		result = prime * result + bannedPersonID;
		result = prime * result + handledModActionID;
		result = prime * result + id;
		result = prime * result + modPersonID;
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
		BanHistory other = (BanHistory) obj;
		if (banDescription == null) {
			if (other.banDescription != null)
				return false;
		} else if (!banDescription.equals(other.banDescription))
			return false;
		if (banDetails == null) {
			if (other.banDetails != null)
				return false;
		} else if (!banDetails.equals(other.banDetails))
			return false;
		if (bannedPersonID != other.bannedPersonID)
			return false;
		if (handledModActionID != other.handledModActionID)
			return false;
		if (id != other.id)
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BanHistory [id=" + id + ", modPersonID="
				+ modPersonID + ", bannedPersonID=" + bannedPersonID + ", handledModActionID=" + handledModActionID
				+ ", banDescription=" + banDescription + ", banDetails=" + banDetails + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[id=" + id + ", mod=" + db.getPersonMapping().fetchByID(modPersonID).username + ", "
				+ "banned=" + db.getPersonMapping().fetchByID(bannedPersonID).username + ", "
				+ "description=" + banDescription + ", details=" + banDetails
				+ ", hma=" + db.getHandledModActionMapping().fetchByID(handledModActionID).toPrettyString(db) + "]";
	}
}
