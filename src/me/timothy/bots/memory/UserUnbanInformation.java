package me.timothy.bots.memory;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;

/**
 * In memory model that describes a user that should be unbanned from 
 * a subreddit. This is solely used for in-progress actions and is never
 * saved anywhere and doesn't correspond with anything
 * 
 * @author Timothy
 */
public final class UserUnbanInformation {
	/** The person to unban */
	public final Person person;
	
	/** The subreddit to unban on */
	public final MonitoredSubreddit subreddit;
	
	/**
	 * Create a new user unban information for unbanning person on subreddit
	 * 
	 * @param person the person to unban
	 * @param subreddit the subreddit to unban on
	 */
	public UserUnbanInformation(Person person, MonitoredSubreddit subreddit) {
		this.person = person;
		this.subreddit = subreddit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		UserUnbanInformation other = (UserUnbanInformation) obj;
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
		return "UserUnbanInformation [person=" + person + ", subreddit=" + subreddit + "]";
	}
}
