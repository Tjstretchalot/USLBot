package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * This describes a session on the website. This isn't used inside the
 * USLBot but it's easier if we only have one thing that generates the
 * database instead of splitting it up.
 * 
 * @author Timothy
 */
public class SiteSession {
	/** The unique identifier for this session in the database */
	public int id;
	/** The unique string identifier that is passed to/from the client as a cookie */
	public String sessionIdentifier;
	/** The person for which this session is acting as a login token for */
	public int personID;
	/** When this session was created */
	public Timestamp createdAt;
	/** When this session should automatically expire (null for never) */
	public Timestamp expiresAt;
	
	/**
	 * @param id the database identifier or -1
	 * @param sessionIdentifier the session identifer
	 * @param personID which person this is a session for
	 * @param createdAt when this session was created
	 * @param expiresAt when this session should expire (-1 for never)
	 */
	public SiteSession(int id, String sessionIdentifier, int personID, Timestamp createdAt, Timestamp expiresAt) {
		super();
		this.id = id;
		this.sessionIdentifier = sessionIdentifier;
		this.personID = personID;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}
	
	/**
	 * Determines if this model passes a sanity check
	 * 
	 * @return if required fields are set
	 */
	public boolean isValid() {
		return (sessionIdentifier != null && sessionIdentifier.length() < 255 && personID > 0 && createdAt != null);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((expiresAt == null) ? 0 : expiresAt.hashCode());
		result = prime * result + id;
		result = prime * result + personID;
		result = prime * result + ((sessionIdentifier == null) ? 0 : sessionIdentifier.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SiteSession))
			return false;
		SiteSession other = (SiteSession) obj;
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
		if (sessionIdentifier == null) {
			if (other.sessionIdentifier != null)
				return false;
		} else if (!sessionIdentifier.equals(other.sessionIdentifier))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SiteSession [id=" + id + ", sessionIdentifier=" + (sessionIdentifier == null ? "null" : "<redacted not null>") + ", personID=" + personID
				+ ", createdAt=" + createdAt + ", expiresAt=" + expiresAt + "]";
	}
}
