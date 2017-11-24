package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a request for an account to reset their password.
 * 
 * @author Timothy
 */
public class ResetPasswordRequest {
	/** The unique local database id, or -1 if not in database */
	public int id;
	/** The person making the request to reset their password */
	public int personID;
	/** The token that is sent to them to prove they recieved our message */
	public String token;
	/** When this request was made */
	public Timestamp createdAt;
	/** When the pm was sent or null if it hasn't been set yet */
	public Timestamp sentAt;
	
	/**
	 * @param id unique db id or -1 if not in database
	 * @param personID the id of the person who made the request
	 * @param token the token used to prove identity
	 * @param createdAt when the request was made
	 * @param sentAt when the PM was sent
	 */
	public ResetPasswordRequest(int id, int personID, String token, Timestamp createdAt, Timestamp sentAt) {
		super();
		this.id = id;
		this.personID = personID;
		this.token = token;
		this.createdAt = createdAt;
		this.sentAt = sentAt;
	}
	
	/**
	 * Determines if this reset password request has enough information to
	 * save it in the database
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
		if (!(obj instanceof ResetPasswordRequest))
			return false;
		ResetPasswordRequest other = (ResetPasswordRequest) obj;
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
		return "ResetPasswordRequest [id=" + id + ", personID=" + personID + ", token=" 
				+ (token == null ? "null" : "<redacted not null>") + ", createdAt="
				+ createdAt + ", sentAt=" + sentAt + "]";
	}
}
