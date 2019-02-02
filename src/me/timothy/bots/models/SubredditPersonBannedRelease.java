package me.timothy.bots.models;

/**
 * This model describes a relationship between a monitored subreddit and a user. The user is
 * given access to the banlist for the given subreddit regardless of tags via the website.
 * 
 * @author Timothy
 */
public class SubredditPersonBannedRelease {
	/**
	 * The monitored subreddit which released the data
	 */
	public int monitoredSubredditID;
	/**
	 * The person allowed to access the subreddits data
	 */
	public int personID;
	
	/**
	 * @param monitoredSubredditID the monitored subreddit whose data can be accessed
	 * @param personID the person who can access that data
	 */
	public SubredditPersonBannedRelease(int monitoredSubredditID, int personID) {
		super();
		this.monitoredSubredditID = monitoredSubredditID;
		this.personID = personID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + monitoredSubredditID;
		result = prime * result + personID;
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
		SubredditPersonBannedRelease other = (SubredditPersonBannedRelease) obj;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (personID != other.personID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubredditPersonBannedRelease [monitoredSubredditID=" + monitoredSubredditID + ", personID=" + personID
				+ "]";
	}
}
