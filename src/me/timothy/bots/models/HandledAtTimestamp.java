package me.timothy.bots.models;

/**
 * This is a joining table used for propagating handled mod actions.
 * Since timestamps are not granular enough to be considered unique
 * on reddit, you must also search at the last checked timestamp for
 * any new modactions that occurred at that timestamp. However, you 
 * dont' want to handle mod actions multiple times. This joining table
 * maps the one-to-many relationship between a subreddit and the mod actions
 * that were handled at SubredditProgressAction#latestPropagatedActionTime
 * @author Tmoor
 *
 */
public class HandledAtTimestamp {
	public int monitoredSubredditID;
	public int handledModActionID;
	
	/**
	 * @param monitoredSubredditID which subreddit
	 * @param handledModActionID which mod action
	 */
	public HandledAtTimestamp(int monitoredSubredditID, int handledModActionID) {
		super();
		this.monitoredSubredditID = monitoredSubredditID;
		this.handledModActionID = handledModActionID;
	}
	
	/**
	 * Determines if this is probably a viable entry to the database
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 && handledModActionID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + handledModActionID;
		result = prime * result + monitoredSubredditID;
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
		HandledAtTimestamp other = (HandledAtTimestamp) obj;
		if (handledModActionID != other.handledModActionID)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HandledAtTimestamp [monitoredSubredditID=" + monitoredSubredditID + ", handledModActionID="
				+ handledModActionID + "]";
	}
}
