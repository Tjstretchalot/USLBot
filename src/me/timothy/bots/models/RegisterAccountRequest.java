package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a request to register your account on the website. This 
 * section refers specifically to the PM that should be sent to the user
 * to confirm their identity.
 * 
 * @author Timothy
 */
public class RegisterAccountRequest {
	/** The ID for the account */
	public int id;
	/** The ID of the person attempting to register */
	public int personID;
	/** The unique token to send to the user */
	public String token;
	/** When the request was made */
	public Timestamp createdAt;
	/** When the request was sent (or null if not sent yet) */
	public Timestamp sentAt;
	/**
	 * @param id
	 * @param personID
	 * @param token
	 * @param createdAt
	 * @param sentAt
	 */
	public RegisterAccountRequest(int id, int personID, String token, Timestamp createdAt, Timestamp sentAt) {
		super();
		this.id = id;
		this.personID = personID;
		this.token = token;
		this.createdAt = createdAt;
		this.sentAt = sentAt;
	}
	
	/**
	 * Determine if this is potentially a valid database entry
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return personID > 0 && token != null && createdAt != null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		result = prime * result + personID;
		result = prime * result + ((sentAt == null) ? 0 : sentAt.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RegisterAccountRequest))
			return false;
		RegisterAccountRequest other = (RegisterAccountRequest) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (id != other.id)
			return false;
		if (personID != other.personID)
			return false;
		if (sentAt == null) {
			if (other.sentAt != null)
				return false;
		} else if (!sentAt.equals(other.sentAt))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "RegisterAccountRequest [id=" + id + ", personID=" + personID + ", token=" + (token == null ? "null" : "<redacted not null>") + ", createdAt="
				+ createdAt + ", sentAt=" + sentAt + "]";
	}
	
	
}
