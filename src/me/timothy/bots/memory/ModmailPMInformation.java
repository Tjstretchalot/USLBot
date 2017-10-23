package me.timothy.bots.memory;

import me.timothy.bots.models.MonitoredSubreddit;

/**
 * Contains the necessary information to describe a modmail pm 
 * that should take place. This is meant as a short-term collection
 * for in-progress actions.
 * 
 * @author Timothy
 */
public class ModmailPMInformation {
	/**
	 * The subreddit to pm
	 */
	public final MonitoredSubreddit subreddit;
	
	/**
	 * The title of the message
	 */
	public final String title;
	
	/**
	 * The body of the message
	 */
	public final String body;

	/**
	 * @param subreddit
	 * @param title
	 * @param body
	 */
	public ModmailPMInformation(MonitoredSubreddit subreddit, String title, String body) {
		super();
		this.subreddit = subreddit;
		this.title = title;
		this.body = body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((subreddit == null) ? 0 : subreddit.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
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
		ModmailPMInformation other = (ModmailPMInformation) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (subreddit == null) {
			if (other.subreddit != null)
				return false;
		} else if (!subreddit.equals(other.subreddit))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ModmailPMInformation [subreddit=" + subreddit + ", title=" + title + ", body=" + body + "]";
	}
}
