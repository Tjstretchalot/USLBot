package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Specifies how much progress we've made on propagating the
 * traditional scammer list to a subreddit.
 * 
 * @author Timothy
 */
public class SubredditTraditionalListStatus {
	/** Internal database identifier */
	public int id;
	
	/** Which subreddit this is a status of */
	public int monitoredSubredditID;
	
	/** The largest id in traditional scammers that has been handled */
	public Integer lastHandledID;
	
	/** The last time we handled some stuff from the traditional list */
	public Timestamp lastHandledAt;

	/**
	 * @param id internal database identifier or -1 if not yet in database
	 * @param monitoredSubredditID the id of the subreddit
	 * @param lastHandledID the highest id that has been handled (work in ascending order)
	 * @param lastHandledAt when we last worked on propagating the traditional list to the subreddit
	 */
	public SubredditTraditionalListStatus(int id, int monitoredSubredditID, Integer lastHandledID, Timestamp lastHandledAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.lastHandledID = lastHandledID;
		this.lastHandledAt = lastHandledAt;
	}
	
	/**
	 * Determine if we might have enough information to save this as an
	 * entry in the database
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((lastHandledAt == null) ? Integer.hashCode(-1) : lastHandledAt.hashCode());
		result = prime * result + ((lastHandledID == null) ? 0 : lastHandledID.hashCode());
		result = prime * result + monitoredSubredditID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SubredditTraditionalListStatus))
			return false;
		SubredditTraditionalListStatus other = (SubredditTraditionalListStatus) obj;
		if (id != other.id)
			return false;
		if (lastHandledAt == null) {
			if (other.lastHandledAt != null)
				return false;
		} else if (!lastHandledAt.equals(other.lastHandledAt))
			return false;
		if (lastHandledID == null) {
			if (other.lastHandledID != null)
				return false;
		} else if (!lastHandledID.equals(other.lastHandledID))
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubredditTraditionalListStatus [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", lastHandledID=" + lastHandledID + ", lastHandledAt=" + lastHandledAt + "]";
	}
}
