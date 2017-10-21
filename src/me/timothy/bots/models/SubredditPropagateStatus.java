package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Keeps track of what banactions have been considered propagating 
 * *to* a subreddit. This is a one-to-one relationship with 
 * MonitoredSubreddits. This is designed to utilize that autoincrement
 * only increases the id counter.
 * 
 * Thus, if you only work your way upwards from ban history ids and
 * you start at 1, you will receive all of the ban history ids. If you
 * finish at 37, and a new one gets added, its id will be no less than 38.
 * 
 * @author Timothy
 */
public class SubredditPropagateStatus {
	public int id;
	public int monitoredSubredditID;
	public Integer lastBanHistoryID;
	public Timestamp updatedAt;
	
	/**
	 * Create a new SubredditPropagateStatus either in preparation for adding to the database
	 * or as a result from the database.
	 * 
	 * @param id the unique identifier from the database (-1 if not in database)
	 * @param monitoredSubredditID the id of the monitored subreddit
	 * @param lastBanActionID the id of the most recent ban action processed in relation this subreddit or null 
	 * @param updatedAt when the database entry was last updated (null if not in database)
	 */
	public SubredditPropagateStatus(int id, int monitoredSubredditID, Integer lastBanActionID, Timestamp updatedAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.lastBanHistoryID = lastBanActionID;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determines if this is a plausible database entry
	 * 
	 * @return if this can probably be saved to the database
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 
				&& (lastBanHistoryID == null || lastBanHistoryID > 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((lastBanHistoryID == null) ? 0 : lastBanHistoryID.hashCode());
		result = prime * result + monitoredSubredditID;
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
		SubredditPropagateStatus other = (SubredditPropagateStatus) obj;
		if (id != other.id)
			return false;
		if (lastBanHistoryID == null) {
			if (other.lastBanHistoryID != null)
				return false;
		} else if (!lastBanHistoryID.equals(other.lastBanHistoryID))
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
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
		return "SubredditPropagateStatus [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", lastBanActionID=" + lastBanHistoryID + ", updatedAt=" + updatedAt + "]";
	}
}
