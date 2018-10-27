package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a request to have a person be granted a temporary authorization
 * level.
 * 
 * @author Timothy
 */
public class TemporaryAuthRequest {
	/** The id of this entry */
	public int id;
	/** The person who wants to get some authorization */
	public int personId;
	/** When the request was made */
	public Timestamp createdAt;
	
	/**
	 * A request to get temporary authorization
	 * 
	 * @param id the id of the request
	 * @param personId the person who made the request
	 * @param createdAt when the request was made
	 */
	public TemporaryAuthRequest(int id, int personId, Timestamp createdAt) {
		this.id = id;
		this.personId = personId;
		this.createdAt = createdAt;
	}
	
	/**
	 * Determine if this request is potentially viable for saving to the database.
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return (personId > 0 && createdAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		result = prime * result + personId;
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
		TemporaryAuthRequest other = (TemporaryAuthRequest) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (id != other.id)
			return false;
		if (personId != other.personId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TemporaryAuthRequests [id=" + id + ", personId=" + personId + ", createdAt=" + createdAt + "]";
	}
}
