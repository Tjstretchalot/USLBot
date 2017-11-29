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
	/** If this token has been consumed */
	public boolean consumed;
	/** When the request was made */
	public Timestamp createdAt;
	/** When the request was sent (or null if not sent yet) */
	public Timestamp sentAt;
	/**
	 * @param id the database identifier or -1
	 * @param personID the id of the person 
	 * @param token the unique token
	 * @param consumed if this has been consumed already
	 * @param createdAt when this was created
	 * @param sentAt when this was sent
	 */
	public RegisterAccountRequest(int id, int personID, String token, boolean consumed, Timestamp createdAt, Timestamp sentAt) {
		super();
		this.id = id;
		this.personID = personID;
		this.token = token;
		this.consumed = consumed;
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
		result = prime * result + (consumed ? 1231 : 1237);
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
		if (consumed != other.consumed)
			return false;
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
		return "RegisterAccountRequest [id=" + id + ", personID=" + personID + ", token="
				+ (token == null ? "null" : "<redacted not null>") + ", consumed="
				+ consumed + ", createdAt=" + createdAt + ", sentAt=" + sentAt + "]";
	}
	
	
}
