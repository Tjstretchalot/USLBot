package me.timothy.bots.models;

/**
 * Describes a subreddit that is monitored by the bot.
 * 
 * @author Timothy
 *
 */
public class MonitoredSubreddit {
	public int id;
	public String subreddit;
	public boolean silent;
	public boolean readOnly;
	public boolean writeOnly;
	
	/**
	 * Initializes a new monitored subreddit using the given information.
	 * 
	 * @param id id (use -1 if you don't have one)
	 * @param subreddit the subreddit
	 * @param silent if the subreddit does not receieve pm alerts
	 * @param readOnly if the subreddit does not ban from the list, only gives bans to the list
	 * @param writeOnly if the subreddit only bans from the list, but cannot write to the list
	 */
	public MonitoredSubreddit(int id, String subreddit, boolean silent, boolean readOnly, boolean writeOnly) {
		this.id = id;
		this.subreddit = subreddit;
		this.silent = silent;
		this.readOnly = readOnly;
		this.writeOnly = writeOnly;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((subreddit == null) ? 0 : subreddit.hashCode());
		result = prime * result + Boolean.hashCode(silent);
		result = prime * result + Boolean.hashCode(readOnly);
		result = prime * result + Boolean.hashCode(writeOnly);
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
		MonitoredSubreddit other = (MonitoredSubreddit) obj;
		if (id != other.id)
			return false;
		if (subreddit == null) {
			if (other.subreddit != null)
				return false;
		} else if (!subreddit.equals(other.subreddit))
			return false;
		if (silent != other.silent)
			return false;
		if (readOnly != other.readOnly)
			return false;
		if (writeOnly != other.writeOnly)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MonitoredSubreddit [id=" + id + ", subreddit=" + subreddit + ", silent=" + silent + ", readOnly="
				+ readOnly + ", writeOnly=" + writeOnly + "]";
	}
}
