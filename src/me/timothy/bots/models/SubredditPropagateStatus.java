package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Keeps track of what banactions have been considered propagating 
 * *to* a subreddit. This is a one-to-one relationship with 
 * MonitoredSubreddits. 
 * 
 * This database cannot be used without also using the "latest handled"
 * joining table. This is because modactions must be processed forward
 * in time, but timestamps are not granular enough on reddit to be 
 * considered unique.
 * 
 * @author Timothy
 */
public class SubredditPropagateStatus {
	public int id;
	public int monitoredSubredditID;
	public Timestamp latestPropagatedActionTime;
	public Timestamp updatedAt;
	
	/**
	 * Create a new SubredditPropagateStatus either in preparation for adding to the database
	 * or as a result from the database.
	 * 
	 * @param id the unique identifier from the database (-1 if not in database)
	 * @param monitoredSubredditID the id of the monitored subreddit
	 * @param latestPropagatedActionTime the occurred_at of the last propagated handled mod action 
	 * @param updatedAt when the database entry was last updated (null if not in database)
	 */
	public SubredditPropagateStatus(int id, int monitoredSubredditID, Timestamp latestPropagatedActionTime, Timestamp updatedAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.latestPropagatedActionTime = latestPropagatedActionTime;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determines if this is a plausible database entry
	 * 
	 * @return if this can probably be saved to the database
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((latestPropagatedActionTime == null) ? 0 : latestPropagatedActionTime.hashCode());
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
		if (latestPropagatedActionTime == null) {
			if (other.latestPropagatedActionTime != null)
				return false;
		} else if (!latestPropagatedActionTime.equals(other.latestPropagatedActionTime))
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
				+ ", latestPropagatedActionTime=" + latestPropagatedActionTime + ", updatedAt=" + updatedAt + "]";
	}
}
