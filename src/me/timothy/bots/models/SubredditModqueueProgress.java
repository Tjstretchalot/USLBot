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
	/** The latest mod action id that has been retrieved from reddit */
	public String latestModActionID;
	/** The newest mod action id that has been retrieved from reddit */
	public String newestModActionID;
	/** When this was last updated. */
	public Timestamp updatedAt;
	/** The last time when we had a complete history of this history, or null */
	public Timestamp lastTimeHadFullHistory;
	
	/**
	 * @param id id (or -1 if not in database yet)
	 * @param monitoredSubredditID the id of the monitoredsubreddit
	 * @param searchForward if latestHandledModActionID is null or less than the latest one on reddit
	 * @param latestModActionID the latest/OLDEST HandledModAction id that was retrieved from this subreddit. may be null
	 * @param newestModActionID the newest/YOUNGEST HandledModAction id that was retrieved from this subreddit. may be null
	 * @param updatedAt when this was last updated
	 * @param lastTimeHadFullHistory the last time that we had a full history of this subreddit
	 */
	public SubredditModqueueProgress(int id, int monitoredSubredditID, boolean searchForward, String latestModActionID,
			String newestModActionID, Timestamp updatedAt, Timestamp lastTimeHadFullHistory) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.searchForward = searchForward;
		this.latestModActionID = latestModActionID;
		this.newestModActionID = newestModActionID;
		this.updatedAt = updatedAt;
		this.lastTimeHadFullHistory = lastTimeHadFullHistory;
	}
	
	/**
	 * Determines if this is a potentially reasonable set of info
	 * to put into the database.
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
		result = prime * result + ((latestModActionID == null) ? 0 : latestModActionID.hashCode());
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((newestModActionID == null) ? 0 : newestModActionID.hashCode());
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
		if (latestModActionID == null) {
			if (other.latestModActionID != null)
				return false;
		} else if (!latestModActionID.equals(other.latestModActionID))
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (newestModActionID == null) {
			if (other.newestModActionID != null)
				return false;
		} else if (!newestModActionID.equals(other.newestModActionID))
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
				+ ", searchForward=" + searchForward + ", latestHandledModActionID=" + latestModActionID
				+ ", newestHandledModActionID=" + newestModActionID + ", updatedAt=" + updatedAt + ""
				+ ", lastTimeHadFullHistory=" + lastTimeHadFullHistory + "]";
	}
}
