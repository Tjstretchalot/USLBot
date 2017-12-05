package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Keeps track of what banactions have been considered propagating 
 * *to* a subreddit. This is a one-to-many relationship with 
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
	public int majorSubredditID;
	public int minorSubredditID;
	public Timestamp latestPropagatedActionTime;
	public Timestamp updatedAt;
	
	
	/**
	 * @param id the id in the database (or -1 if not in database)
	 * @param majorSubredditID the major subreddit (the one doing the propagating for this status)
	 * @param minorSubredditID the minor subreddit (the one being propagated for this status)
	 * @param latestPropagatedActionTime the latest timestamp from minor sub that was propagated (or null for none)
	 * @param updatedAt when it was last updated
	 */
	public SubredditPropagateStatus(int id, int majorSubredditID, int minorSubredditID,
			Timestamp latestPropagatedActionTime, Timestamp updatedAt) {
		this.id = id;
		this.majorSubredditID = majorSubredditID;
		this.minorSubredditID = minorSubredditID;
		this.latestPropagatedActionTime = latestPropagatedActionTime;
		this.updatedAt = updatedAt;
	}

	/**
	 * Determines if this is a plausible database entry
	 * 
	 * @return if this can probably be saved to the database
	 */
	public boolean isValid() {
		return majorSubredditID > 0 && minorSubredditID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((latestPropagatedActionTime == null) ? 0 : latestPropagatedActionTime.hashCode());
		result = prime * result + majorSubredditID;
		result = prime * result + minorSubredditID;
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SubredditPropagateStatus))
			return false;
		SubredditPropagateStatus other = (SubredditPropagateStatus) obj;
		if (id != other.id)
			return false;
		if (latestPropagatedActionTime == null) {
			if (other.latestPropagatedActionTime != null)
				return false;
		} else if (!latestPropagatedActionTime.equals(other.latestPropagatedActionTime))
			return false;
		if (majorSubredditID != other.majorSubredditID)
			return false;
		if (minorSubredditID != other.minorSubredditID)
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
		return "SubredditPropagateStatus [id=" + id + ", majorSubredditID=" + majorSubredditID + ", minorSubredditID="
				+ minorSubredditID + ", latestPropagatedActionTime=" + latestPropagatedActionTime + ", updatedAt="
				+ updatedAt + "]";
	}
}
