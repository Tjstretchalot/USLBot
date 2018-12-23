package me.timothy.bots.models;

/**
 * The modmail on reddit still handles "many messages" poorly. In the event of a bug in the USLBot
 * or just an overzealous message, it makes it impossible for the moderators to see the normal queue.
 * For this reason, subreddits may setup an alternate subreddit which the bot posts to instead of
 * sending messages to their modmail.
 * 
 * @author Timothy
 */
public class MonitoredSubredditAltModMail {
	/** The id of this row. */
	public int id;
	/** The monitored subreddit which is having its modmail redirected */
	public int monitoredSubredditID;
	/** The subreddit to redirect the modmail to */
	public String alternateSubreddit;
	
	/**
	 * @param id the id of the row in the database or -1 if not in the database yet
	 * @param monitoredSubredditID the id of the monitored subreddit whose modmail is redirected
	 * @param alternateSubreddit the subreddit that the modmail should be posted on
	 */
	public MonitoredSubredditAltModMail(int id, int monitoredSubredditID, String alternateSubreddit) {
		super();
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.alternateSubreddit = alternateSubreddit;
	}
	
	/**
	 * Determines if this is a potentially valid row in the database
	 * @return if this is potentially valid
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 && alternateSubreddit != null && !alternateSubreddit.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alternateSubreddit == null) ? 0 : alternateSubreddit.hashCode());
		result = prime * result + id;
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
		MonitoredSubredditAltModMail other = (MonitoredSubredditAltModMail) obj;
		if (alternateSubreddit == null) {
			if (other.alternateSubreddit != null)
				return false;
		} else if (!alternateSubreddit.equals(other.alternateSubreddit))
			return false;
		if (id != other.id)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MonitoredSubredditAltModMail [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", alternateSubreddit=" + alternateSubreddit + "]";
	}
}
