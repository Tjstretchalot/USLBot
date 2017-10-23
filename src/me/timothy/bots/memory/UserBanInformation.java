package me.timothy.bots.memory;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;

/**
 * Describes one ban which should take place. This is meant as short-term container 
 * object for in-progress operations.
 * 
 * @author Timothy
 */
public final class UserBanInformation {
	/** The person that should be banned */
	public final Person person;
	/** The subreddit they should be banned on */
	public final MonitoredSubreddit subreddit;
	/** The message provided to the user about the ban */
	public final String banMessage;
	/** The subreddit ban reason string (usually "other") */
	public final String banReason; 
	/** The note provided to the moderators */
	public final String banNote;
	
	/**
	 * @param person person to ban
	 * @param subreddit subreddit to ban on
	 * @param banMessage message reddit will pass to user
	 * @param banReason usually the string "other"
	 * @param banNote the note provided to moderators (&lt; 50 chars)
	 */
	public UserBanInformation(Person person, MonitoredSubreddit subreddit, String banMessage, String banReason,
			String banNote) {
		super();
		this.person = person;
		this.subreddit = subreddit;
		this.banMessage = banMessage;
		this.banReason = banReason;
		this.banNote = banNote;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((banMessage == null) ? 0 : banMessage.hashCode());
		result = prime * result + ((banNote == null) ? 0 : banNote.hashCode());
		result = prime * result + ((banReason == null) ? 0 : banReason.hashCode());
		result = prime * result + ((person == null) ? 0 : person.hashCode());
		result = prime * result + ((subreddit == null) ? 0 : subreddit.hashCode());
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
		UserBanInformation other = (UserBanInformation) obj;
		if (banMessage == null) {
			if (other.banMessage != null)
				return false;
		} else if (!banMessage.equals(other.banMessage))
			return false;
		if (banNote == null) {
			if (other.banNote != null)
				return false;
		} else if (!banNote.equals(other.banNote))
			return false;
		if (banReason == null) {
			if (other.banReason != null)
				return false;
		} else if (!banReason.equals(other.banReason))
			return false;
		if (person == null) {
			if (other.person != null)
				return false;
		} else if (!person.equals(other.person))
			return false;
		if (subreddit == null) {
			if (other.subreddit != null)
				return false;
		} else if (!subreddit.equals(other.subreddit))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UserBanInformation [person=" + person + ", subreddit=" + subreddit + ", banMessage=" + banMessage
				+ ", banReason=" + banReason + ", banNote=" + banNote + "]";
	}
}
