package me.timothy.bots.models;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import me.timothy.bots.USLDatabase;

/**
 * Called via the website by a moderator of the USL requesting that the bot
 * accepts a moderator invite from the given subreddit.
 * 
 * @author Timothy
 */
public class AcceptModeratorInviteRequest {
	/** Database row id or -1 if not yet in the database */
	public int id;
	/** The id of the moderator user who initiated this request */
	public int modPersonId;
	/** The subreddit who we should accept the moderator invite from */
	public String subreddit;
	/** When the request was created */
	public Timestamp createdAt;
	/** When the request was fulfilled */
	public Timestamp fulfilledAt;
	/** True if there was a pending invite and it was accepted, false otherwise */
	public boolean success;
	
	/**
	 * @param id database row id or -1 if not yet in database
	 * @param modUserId the id of the moderator user who made the request
	 * @param subreddit the subreddit to accept a moderator invite from
	 * @param createdAt when the request was created
	 * @param fulfilledAt when the request was fulfilled
	 * @param success if the request was successful
	 */
	public AcceptModeratorInviteRequest(int id, int modUserId, String subreddit, Timestamp createdAt,
			Timestamp fulfilledAt, boolean success) {
		super();
		this.id = id;
		this.modPersonId = modUserId;
		this.subreddit = subreddit;
		this.createdAt = createdAt;
		this.fulfilledAt = fulfilledAt;
		this.success = success;
	}
	
	/**
	 * Determines if this is potentially viable for saving in the database without
	 * doing any foreign key or uniqueness checks
	 * 
	 * @return if this is a potentially viable entry in the database
	 */
	public boolean isValid() {
		return modPersonId > 0 && subreddit != null && createdAt != null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((fulfilledAt == null) ? 0 : fulfilledAt.hashCode());
		result = prime * result + id;
		result = prime * result + modPersonId;
		result = prime * result + ((subreddit == null) ? 0 : subreddit.hashCode());
		result = prime * result + (success ? 1231 : 1237);
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
		AcceptModeratorInviteRequest other = (AcceptModeratorInviteRequest) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (fulfilledAt == null) {
			if (other.fulfilledAt != null)
				return false;
		} else if (!fulfilledAt.equals(other.fulfilledAt))
			return false;
		if (id != other.id)
			return false;
		if (modPersonId != other.modPersonId)
			return false;
		if (subreddit == null) {
			if (other.subreddit != null)
				return false;
		} else if (!subreddit.equals(other.subreddit))
			return false;
		if (success != other.success)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "AcceptModeratorInviteRequest [id=" + id + ", modUserId=" + modPersonId + ", subreddit=" + subreddit
				+ ", createdAt=" + createdAt + ", fulfilledAt=" + fulfilledAt + ", success=" + success + "]";
	}
	
	/**
	 * A prettier toString done by fetching out the ids of the users and formatting
	 * the timestamps.
	 * 
	 * Example results:
	 * 	id=3: for borrow by tjstretchalot on 11/30/09 7:10 PM. not fulfilled
	 * 	id=5: for test by johndoe on 05/15/18 5:10 PM and fulfilled at 05/15/18 5:13 PM (success=true)
	 *   
	 * @param database the database to fetch data from
	 * @return a prettier representation
	 */
	public String prettyToString(USLDatabase database) {
		StringBuilder result = new StringBuilder();
		
		result.append("id=").append(id).append(": for ").append(subreddit).append(" by ");
		Person person = database.getPersonMapping().fetchByID(modPersonId);
		result.append(person.username).append(" at ");
		
		DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		result.append(format.format(createdAt));
		
		if(fulfilledAt == null) {
			result.append(". not fulfilled");
			return result.toString();
		}
		
		result.append(" and fulfilled at ").append(format.format(fulfilledAt));
		result.append(" (success=").append(success).append(")");
		return result.toString();
	}
}
