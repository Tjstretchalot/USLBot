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
	/** The latest mod action id (from our database) that has been retrieved from reddit */
	public Integer latestHandledModActionID;
	/** The newest mod action id (from our database) that has been retrieved from reddit */
	public Integer newestHandledModActionID;
	/** When this was last updated. */
	public Timestamp updatedAt;
	/** The last time when we had a complete history of this history, or null */
	public Timestamp lastTimeHadFullHistory;
	
	/**
	 * @param id id (or -1 if not in database yet)
	 * @param monitoredSubredditID the id of the monitoredsubreddit
	 * @param searchForward if latestHandledModActionID is null or less than the latest one on reddit
	 * @param latestHandledModActionID the latest/OLDEST HandledModAction id that was retrieved from this subreddit. may be null
	 * @param newestHandledModActionID the newest/YOUNGEST HandledModAction id that was retrieved from this subreddit. may be null
	 * @param updatedAt when this was last updated
	 * @param lastTimeHadFullHistory the last time that we had a full history of this subreddit
	 */
	public SubredditModqueueProgress(int id, int monitoredSubredditID, boolean searchForward, Integer latestHandledModActionID,
			Integer newestHandledModActionID, Timestamp updatedAt, Timestamp lastTimeHadFullHistory) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.searchForward = searchForward;
		this.latestHandledModActionID = latestHandledModActionID;
		this.newestHandledModActionID = newestHandledModActionID;
		this.updatedAt = updatedAt;
		this.lastTimeHadFullHistory = lastTimeHadFullHistory;
	}
	
	/**
	 * Determines if this is a potentially reasonable set of info
	 * to put into the database.
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 && 
				(latestHandledModActionID == null || latestHandledModActionID > 0) &&
				(newestHandledModActionID == null || newestHandledModActionID > 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((latestHandledModActionID == null) ? 0 : latestHandledModActionID.hashCode());
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((newestHandledModActionID == null) ? 0 : newestHandledModActionID.hashCode());
		result = prime * result + (searchForward ? 1231 : 1237);
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
		result = prime * result + ((lastTimeHadFullHistory == null) ? 0 : lastTimeHadFullHistory.hashCode());
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
		if (latestHandledModActionID == null) {
			if (other.latestHandledModActionID != null)
				return false;
		} else if (!latestHandledModActionID.equals(other.latestHandledModActionID))
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (newestHandledModActionID == null) {
			if (other.newestHandledModActionID != null)
				return false;
		} else if (!newestHandledModActionID.equals(other.newestHandledModActionID))
			return false;
		if (searchForward != other.searchForward)
			return false;
		if (updatedAt == null) {
			if (other.updatedAt != null)
				return false;
		} else if (!updatedAt.equals(other.updatedAt))
			return false;
		if (lastTimeHadFullHistory == null) {
			if (other.lastTimeHadFullHistory != null)
				return false;
		} else if (!lastTimeHadFullHistory.equals(other.lastTimeHadFullHistory))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubredditModqueueProgress [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", searchForward=" + searchForward + ", latestHandledModActionID=" + latestHandledModActionID
				+ ", newestHandledModActionID=" + newestHandledModActionID + ", updatedAt=" + updatedAt + ""
				+ ", lastTimeHadFullHistory=" + lastTimeHadFullHistory + "]";
	}
}
