package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * A temporary authorization level for the website, which was given to them by
 * the LoansBot after verifying they are a moderator for the universal scammer
 * list subreddit.
 * 
 * @author Timothy
 */
public class TemporaryAuthLevel {
	/** The id for this entry */
	public int id;
	/** The person who is being granted an auth level */
	public int personID;
	/** The auth level the person is being granted */
	public int authLevel;
	/** When this permission was granted */
	public Timestamp createdAt;
	/** When this permission expires */
	public Timestamp expiresAt;
	
	/**
	 * Create a new temporary auth level
	 * 
	 * @param id the id of this entry or -1 if not in the database
	 * @param personID the person
	 * @param authLevel the authorization level
	 * @param createdAt when this authorization level was created
	 * @param expiresAt when it expires
	 */
	public TemporaryAuthLevel(int id, int personID, int authLevel, Timestamp createdAt, Timestamp expiresAt) {
		this.id = id;
		this.personID = personID;
		this.authLevel = authLevel;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}
	
	/**
	 * Determines if this temporary auth level is potentially saveable to the database
	 * 
	 * @return true if potentially saveable, false otherwise
	 */
	public boolean isValid() {
		return (personID > 0 && authLevel >= -1 && createdAt != null && expiresAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + authLevel;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((expiresAt == null) ? 0 : expiresAt.hashCode());
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
		if (getClass() != obj.getClass())
			return false;
		TemporaryAuthLevel other = (TemporaryAuthLevel) obj;
		if (authLevel != other.authLevel)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (expiresAt == null) {
			if (other.expiresAt != null)
				return false;
		} else if (!expiresAt.equals(other.expiresAt))
			return false;
		if (id != other.id)
			return false;
		if (personID != other.personID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TemporaryAuthLevel [id=" + id + ", personID=" + personID + ", authLevel=" + authLevel + ", createdAt="
				+ createdAt + ", expiresAt=" + expiresAt + "]";
	}
}
