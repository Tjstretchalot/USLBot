package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * When we try to ban a user that was #scammer tagged on one subreddit, who
 * was banned on another subreddit already, we want to send the banning moderator
 * a pm describing the situation.
 * 
 * However, since this is done during propagating, and no mod history is formed,
 * this breaks the rule that we can always repropagate without duplicating work
 * unless we have some other way to remember we already made that pm.
 * 
 * This is that other way of remembering we made that pm.
 * 
 * @author Timothy
 */
public class LastInfoPM {
	/**
	 * Our identifier for this pm
	 */
	public int id;
	
	/**
	 * The id of the user who we sent the informative pm to
	 */
	public int modPersonID;
	
	/**
	 * The user we sent the pm about
	 */
	public int bannedPersonID;
	
	/**
	 * When we sent that pm
	 */
	public Timestamp createdAt;

	/**
	 * @param id
	 * @param modUserID
	 * @param bannedUserID
	 * @param pmSentAt
	 */
	public LastInfoPM(int id, int modUserID, int bannedUserID, Timestamp pmSentAt) {
		super();
		this.id = id;
		this.modPersonID = modUserID;
		this.bannedPersonID = bannedUserID;
		this.createdAt = pmSentAt;
	}
	
	/**
	 * Determines if this is probably valid
	 * 
	 * @return sanity check passed
	 */
	public boolean isValid() {
		return (modPersonID > 0 && bannedPersonID > 0 && createdAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bannedPersonID;
		result = prime * result + id;
		result = prime * result + modPersonID;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LastInfoPM))
			return false;
		LastInfoPM other = (LastInfoPM) obj;
		if (bannedPersonID != other.bannedPersonID)
			return false;
		if (id != other.id)
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LastInfoPM [id=" + id + ", modPersonID=" + modPersonID + ", bannedPersonID=" + bannedPersonID
				+ ", createdAt=" + createdAt + "]";
	}
}
