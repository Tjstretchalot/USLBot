package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Links a person to being banned on a particular subreddit. Subreddits
 * are referred to by monitored subreddit id. It is assumed that if we
 * stop monitoring a subreddit, we don't care about its bans anymore. 
 * If we really want to, we can backup the database.
 * 
 * BanHistory's are *always* information provided by the reddit server,
 * or information parsed from that. If a BanHistory is generated there
 * MUST be a corresponding ModAction.
 * 
 * @author Timothy
 */
public class BanHistory {
	public int id;
	public int monitoredSubredditID;
	public int modPersonID;
	public int bannedPersonID;
	public String modActionID;
	public String banDescription;
	public String banDetails;
	public Timestamp occurredAt;
	
	
	/**
	 * Create a new ban history from reddits information
	 * 
	 * @param id the id of this ban history in our database (or -1 if not in database)
	 * @param monitoredSubredditID the subreddit (identified by "subreddit") as the id the monitoredsubreddit
	 * @param modPersonID the person who performed the ban (identified by "mod") as the id of the person in our database
	 * @param bannedPersonID the person was banned (identified by "target_author") as the id of the person in our database
	 * @param modActionID the id of the ModAction (reddits database) that this data came from
	 * @param banDescription the description (provided by moderator when banning on reddit) of the ban 
	 * @param banDetails the duration of the ban (reddit calls this "details"), such as "permanent" or "90 days"
	 * @param occurredAt when the ban occurred at (identified by "created_at" on reddit)
	 */
	public BanHistory(int id, int monitoredSubredditID, int modPersonID, int bannedPersonID, String modActionID,
			String banDescription, String banDetails, Timestamp occurredAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.modPersonID = modPersonID;
		this.bannedPersonID = bannedPersonID;
		this.modActionID = modActionID;
		this.banDescription = banDescription;
		this.banDetails = banDetails;
		this.occurredAt = occurredAt;
	}

	public boolean isValid() {
		return (monitoredSubredditID > 0 && modPersonID > 0 && bannedPersonID > 0 && modActionID != null
				&& banDetails != null && banDescription != null && banDetails != null && occurredAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((banDescription == null) ? 0 : banDescription.hashCode());
		result = prime * result + ((banDetails == null) ? 0 : banDetails.hashCode());
		result = prime * result + bannedPersonID;
		result = prime * result + id;
		result = prime * result + ((modActionID == null) ? 0 : modActionID.hashCode());
		result = prime * result + modPersonID;
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((occurredAt == null) ? 0 : occurredAt.hashCode());
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
		if (id != other.id)
			return false;
		if (modActionID == null) {
			if (other.modActionID != null)
				return false;
		} else if (!modActionID.equals(other.modActionID))
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (occurredAt == null) {
			if (other.occurredAt != null)
				return false;
		} else if (!occurredAt.equals(other.occurredAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BanHistory [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID + ", modPersonID="
				+ modPersonID + ", bannedPersonID=" + bannedPersonID + ", modActionID=" + modActionID
				+ ", banDescription=" + banDescription + ", banDetails=" + banDetails + ", occurredAt=" + occurredAt
				+ "]";
	}
}
