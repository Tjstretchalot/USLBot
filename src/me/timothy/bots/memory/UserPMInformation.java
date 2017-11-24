package me.timothy.bots.memory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.timothy.bots.models.Person;

/**
 * Describe how a user should be pmd. This is meant to be a memory-only
 * collection of information that is used to standardize function arguments and 
 * return types, and should not be stored.
 * 
 * @author Timothy
 */
public class UserPMInformation {
	/** The person to pm */
	public final Person person;
	
	/** The title of the pm */
	public final String title;
	
	/** The body of the pm */
	public final String body;

	/** List of callbacks to call after successfully sending this pm */
	public final List<Runnable> callbacks;
	
	/**
	 * @param person person to pm
	 * @param title title of pm
	 * @param body body of pm
	 * @param the callbacks after successfully sending a pm
	 */
	public UserPMInformation(Person person, String title, String body, Runnable... callbacks) {
		super();
		this.person = person;
		this.title = title;
		this.body = body;
		this.callbacks = Collections.unmodifiableList(Arrays.asList(callbacks));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((person == null) ? 0 : person.hashCode());
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
		UserPMInformation other = (UserPMInformation) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (person == null) {
			if (other.person != null)
				return false;
		} else if (!person.equals(other.person))
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
		return "UserPMInformation [person=" + person + ", title=" + title + ", body=" + body + "]";
	}
	
	
	
}
