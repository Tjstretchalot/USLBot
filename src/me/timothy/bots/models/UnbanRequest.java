package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * These are added when a moderator with appropriate authorization
 * removes someone from the universal scammer list AND requests that
 * the bot unban them on all subreddits he banned them on
 * 
 * @author Timothy
 */
public class UnbanRequest {
	/**
	 * The identifier for this request in our database
	 */
	public int id;
	
	/**
	 * The person who made this request
	 */
	public int modPersonID;
	
	/**
	 * The person who is currently banned that should be unbanned
	 */
	public int bannedPersonID;
	
	/**
	 * When this request was made
	 */
	public Timestamp createdAt;
	
	/**
	 * When this request was handled
	 */
	public Timestamp handledAt;
	
	/**
	 * If this request was determined to be invalid
	 */
	public boolean invalid;
	
	/**
	 * Creates a new request to unban a user
	 * 
	 * @param id our database identifier (-1 if not saved yet)
	 * @param modPersonID the moderator who made the request
	 * @param bannedPersonID the currently banned person to unban
	 * @param createdAt when this request was created
	 * @param handledAt when this request was handled
	 * @param invalid if this request is invalid
	 */
	public UnbanRequest(int id, int modPersonID, int bannedPersonID, Timestamp createdAt, Timestamp handledAt, boolean invalid) {
		this.id = id;
		this.modPersonID = modPersonID;
		this.bannedPersonID = bannedPersonID;
		this.createdAt = createdAt;
		this.handledAt = handledAt;
		this.invalid = invalid;
	}

	/**
	 * Check if this is a reasonable entry for the database
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return modPersonID > 0 && bannedPersonID > 0 && createdAt != null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bannedPersonID;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((handledAt == null) ? 0 : handledAt.hashCode());
		result = prime * result + id;
		result = prime * result + (invalid ? 1231 : 1237);
		result = prime * result + modPersonID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UnbanRequest))
			return false;
		UnbanRequest other = (UnbanRequest) obj;
		if (bannedPersonID != other.bannedPersonID)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (handledAt == null) {
			if (other.handledAt != null)
				return false;
		} else if (!handledAt.equals(other.handledAt))
			return false;
		if (id != other.id)
			return false;
		if (invalid != other.invalid)
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UnbanRequest [id=" + id + ", modPersonID=" + modPersonID + ", bannedPersonID=" + bannedPersonID
				+ ", createdAt=" + createdAt + ", handledAt=" + handledAt + ", invalid=" + invalid + "]";
	}
}
