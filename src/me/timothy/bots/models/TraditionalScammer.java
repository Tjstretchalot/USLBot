package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * This class maps persons to the traditional scammer 
 * list, which is to say banned users that don't correspond
 * with an actual mod action.
 * 
 * @author Timothy
 */
public class TraditionalScammer {
	/**
	 * The identifier for this model in our database
	 */
	public int id;
	
	/**
	 * The person who has been banned
	 */
	public int personID;
	
	/**
	 * The reason given, if any
	 */
	public String reason;
	
	/**
	 * This description can be processed in the same way that we process
	 * ban descriptions to get the list of tags.
	 */
	public String description;
	
	/**
	 * When this user was added to the traditional scammer list
	 */
	public Timestamp createdAt;
	
	
	/**
	 * Create a new entry in the traditional scammer list
	 * 
	 * @param id the local database identifier (or -1 if not saved yet)
	 * @param bannedPersonID the person who is banned
	 * @param reason the reason he's on the traditional list
	 * @param description the equivalent of the ban description
	 * @param createdAt when this user was added to the list
	 */
	public TraditionalScammer(int id, int bannedPersonID, String reason, String description, Timestamp createdAt) {
		this.id = id;
		this.personID = bannedPersonID;
		this.reason = reason;
		this.description = description;
		this.createdAt = createdAt;
	}
	
	/**
	 * Ensures this is probably a valid entry
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return personID > 0 && reason != null && description != null && createdAt != null;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + personID;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
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
		TraditionalScammer other = (TraditionalScammer) obj;
		if (personID != other.personID)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id != other.id)
			return false;
		if (reason == null) {
			if (other.reason != null)
				return false;
		} else if (!reason.equals(other.reason))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "TraditionalScammer [id=" + id + ", personID=" + personID + ", reason=" + reason + ", description="
				+ description + ", createdAt=" + createdAt + "]";
	}
}
