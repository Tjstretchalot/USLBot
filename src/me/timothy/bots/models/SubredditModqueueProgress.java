package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * This table is one-to-one with MonitoredSubreddits. It provides information
 * on the progress for the monitored subreddit in retrieving ban actions from
 * the modqueue, as well as enough information to continue from where it let
 * off. 
 * 
 * @author Timothy
 */
public class SubredditModqueueProgress {
	/** Unique identifier in database */
	public int id;
	/** Which subreddit */
	public int monitoredSubredditID;
	/** True if latestBanHistoryID is not the latest one on reddit */
	public boolean searchForward;
	/** The latest ban history id that has been retrieved from reddit */
	public Integer latestBanHistoryID;
	/** The newest ban history id that has been retrieved from reddit */
	public Integer newestBanHistoryID;
	/** When this was last updated. */
	public Timestamp updatedAt;
	
	/**
	 * @param id id (or -1 if not in database yet)
	 * @param monitoredSubredditID the id of the monitoredsubreddit
	 * @param searchForward if latestBanHistoryID is null or less than the latest one on reddit
	 * @param latestBanHistoryID the latest/OLDEST BanHistory id that was retrieved from this subreddit. may be null
	 * @param newestBanHistoryID the newest/YOUNGEST BanHistory id that was retrieved from this subreddit. may be null
	 * @param updatedAt when this was last updated
	 */
	public SubredditModqueueProgress(int id, int monitoredSubredditID, boolean searchForward, Integer latestBanHistoryID,
			Integer newestBanHistoryID, Timestamp updatedAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.searchForward = searchForward;
		this.latestBanHistoryID = latestBanHistoryID;
		this.newestBanHistoryID = newestBanHistoryID;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determines if this is a potentially reasonable set of info
	 * to put into the database.
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 
				&& (latestBanHistoryID == null || latestBanHistoryID > 0)
				&& (newestBanHistoryID == null || newestBanHistoryID > 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((latestBanHistoryID == null) ? 0 : latestBanHistoryID.hashCode());
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((newestBanHistoryID == null) ? 0 : newestBanHistoryID.hashCode());
		result = prime * result + (searchForward ? 1231 : 1237);
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
		SubredditModqueueProgress other = (SubredditModqueueProgress) obj;
		if (id != other.id)
			return false;
		if (latestBanHistoryID == null) {
			if (other.latestBanHistoryID != null)
				return false;
		} else if (!latestBanHistoryID.equals(other.latestBanHistoryID))
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (newestBanHistoryID == null) {
			if (other.newestBanHistoryID != null)
				return false;
		} else if (!newestBanHistoryID.equals(other.newestBanHistoryID))
			return false;
		if (searchForward != other.searchForward)
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
		return "SubredditModqueueProgress [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", searchForward=" + searchForward + ", latestBanHistoryID=" + latestBanHistoryID
				+ ", newestBanHistoryID=" + newestBanHistoryID + ", updatedAt=" + updatedAt + "]";
	}
}
