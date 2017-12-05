package me.timothy.bots.models;

/**
 * This is a joining table used for propagating handled mod actions.
 * Since timestamps are not granular enough to be considered unique
 * on reddit, you must also search at the last checked timestamp for
 * any new modactions that occurred at that timestamp. However, you 
 * dont' want to handle mod actions multiple times. This joining table
 * maps the one-to-many relationship between two subreddits and the mod actions
 * that were handled at SubredditProgressAction#latestPropagatedActionTime
 * @author Tmoor
 *
 */
public class HandledAtTimestamp {
	public int majorSubredditID;
	public int minorSubredditID;
	public int handledModActionID;
	
	
	/**
	 * @param majorSubredditID the subreddit doing the propagating
	 * @param minorSubredditID the subreddit being propagated
	 * @param handledModActionID the action that was handled
	 */
	public HandledAtTimestamp(int majorSubredditID, int minorSubredditID, int handledModActionID) {
		super();
		this.majorSubredditID = majorSubredditID;
		this.minorSubredditID = minorSubredditID;
		this.handledModActionID = handledModActionID;
	}

	/**
	 * Determines if this is probably a viable entry to the database
	 * 
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return majorSubredditID > 0 && minorSubredditID > 0 && handledModActionID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + handledModActionID;
		result = prime * result + majorSubredditID;
		result = prime * result + minorSubredditID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HandledAtTimestamp))
			return false;
		HandledAtTimestamp other = (HandledAtTimestamp) obj;
		if (handledModActionID != other.handledModActionID)
			return false;
		if (majorSubredditID != other.majorSubredditID)
			return false;
		if (minorSubredditID != other.minorSubredditID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HandledAtTimestamp [majorSubredditID=" + majorSubredditID + ", minorSubredditID=" + minorSubredditID
				+ ", handledModActionID=" + handledModActionID + "]";
	}
}
