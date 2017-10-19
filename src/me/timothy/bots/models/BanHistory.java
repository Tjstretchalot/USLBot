package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Links a person to being banned on a particular subreddit. Subreddits
 * are referred to by monitored subreddit id. It is assumed that if we
 * stop monitoring a subreddit, we don't care about its bans anymore. 
 * If we really want to, we can backup the database.
 * 
 * @author Timothy
 */
public class BanHistory {
	/**
	 * These are used as shorthand for describing why a user was banned
	 *
	 * @author Timothy
	 */
	public enum BanReasonIdentifier {
		/**
		 * We banned the user on this subreddit because he was banned
		 * as a scammer on another subreddit. The source should be 
		 * specified in the additional information text
		 */
		Propagate(0),
		
		/**
		 * The subreddit banned the user
		 */
		SubBan(1),
		;
		
		/**
		 * The database identifier
		 */
		public final int id;
		
		BanReasonIdentifier(int id) {
			this.id = id;
		}
		
		/**
		 * Fetch the ban reason identifier corresponding to the specified id,
		 * or throw an exception if none exists.
		 * 
		 * @param id the id
		 * @return the corresponding ban reason identifier
		 * @throws IllegalArgumentException if there is no corresponding ban reason identifier
		 */
		public static BanReasonIdentifier getByID(int id) {
			for(BanReasonIdentifier val : values()) {
				if(val.id == id) {
					return val;
				}
			}
			throw new IllegalArgumentException("There does not exist a ban reason identifier with id=" + id + "!");
		}
	}
	
	public int id;
	public int monitoredSubredditID;
	public int modPersonID;
	public int bannedPersonID;
	public int banReasonID;
	public String banDescription;
	public String banReasonAdditional;
	public boolean suppressed;
	public Timestamp occurredAt;
	public Timestamp createdAt;
	public Timestamp updatedAt;
	
	/**
	 * Create a new ban history with the specified information
	 * 
	 * @param id the unique identifier for this ban history in the database, or -1 if not a row yet
	 * @param monitoringSubredditID the id for the MonitoredSubreddit that this ban is on
	 * @param modPersonID the id for the person who banned this user
	 * @param bannedPersonID the id for the person who was banned
	 * @param banReasonID the id of the closest BanReasonIdentifier that describes why this ban happened
	 * @param banDescription the description provided by the banning user
	 * @param banReasonAdditional any additional notes regarding this ban
	 * @param suppressed if this ban was not propagated due to write-only access
	 * @param createdAt when this row was added
	 * @param occurredAt when this occurred according to reddit
	 * @param updatedAt when this row was last updated
	 */
	public BanHistory(int id, int monitoringSubredditID, int modPersonID, int bannedPersonID, int banReasonID, 
			String banDescription, String banReasonAdditional, boolean suppressed, Timestamp createdAt, Timestamp occurredAt,
			Timestamp updatedAt) {
		super();
		this.id = id;
		this.monitoredSubredditID = monitoringSubredditID;
		this.modPersonID = modPersonID;
		this.bannedPersonID = bannedPersonID;
		this.banReasonID = banReasonID;
		this.banDescription = banDescription;
		this.banReasonAdditional = banReasonAdditional;
		this.suppressed = suppressed;
		this.occurredAt = occurredAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determine if this ban history is a potentially valid row
	 * in the database
	 * @return if this is maybe valid
	 */
	public boolean isValid() {
		return (monitoredSubredditID > 0 && modPersonID > 0 && bannedPersonID > 0 && banReasonID > 0 
				&& banDescription != null && occurredAt != null && createdAt != null && updatedAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((banDescription == null) ? 0 : banDescription.hashCode());
		result = prime * result + ((banReasonAdditional == null) ? 0 : banReasonAdditional.hashCode());
		result = prime * result + banReasonID;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		result = prime * result + monitoredSubredditID;
		result = prime * result + Boolean.hashCode(suppressed);
		result = prime * result + ((occurredAt == null) ? 0 : occurredAt.hashCode());
		result = prime * result + modPersonID;
		result = prime * result + bannedPersonID;
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
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
		if (banReasonAdditional == null) {
			if (other.banReasonAdditional != null)
				return false;
		} else if (!banReasonAdditional.equals(other.banReasonAdditional))
			return false;
		if (banReasonID != other.banReasonID)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (suppressed != other.suppressed)
			return false;
		if (id != other.id)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (occurredAt == null) {
			if (other.occurredAt != null)
				return false;
		} else if (!occurredAt.equals(other.occurredAt))
			return false;
		if (modPersonID != other.modPersonID)
			return false;
		if (bannedPersonID != other.bannedPersonID)
			return false;
		if (updatedAt == null) {
			if (other.updatedAt != null)
				return false;
		} else if (!updatedAt.equals(other.updatedAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BanHistory [id=" + id + ", monitoringSubredditID=" + monitoredSubredditID + ", modPersonID="
				+ modPersonID + ", bannedPersonID=" + bannedPersonID + ", banReasonID=" + banReasonID
				+ ", banDescription=" + banDescription + ", banReasonAdditional=" + banReasonAdditional
				+ ", suppressed=" + suppressed + ", occurredAt=" + occurredAt + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + "]";
	}
}
