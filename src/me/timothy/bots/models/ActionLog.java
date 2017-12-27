package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * The action log is a curated logging tool that is used to help
 * visualize what the USLBot is doing without having to connect
 * to the USLBot. It's fairly similiar in theory to INFO level logging,
 * but it is kept distinct to help ensure nothing that shouldn't be
 * publicly visible is sent to it.
 * 
 * The ActionLog is expected to only last one loop, then be cleared. Each
 * item in the log is simply a string with a timestamp. This does NOT
 * serve as an acceptable "logging" tool in the sense of being able to
 * view into the past.
 *
 * To improve readablity, the following are special commands to the outputter:
 *   {link person ID} - this should display to identify the person with the specified id
 *   {link subreddit ID} - this should display to identify the subreddit with the specified id
 *   {SIGSTART} - this text alone should be done at the very top of the loop
 *   
 * 
 * @author Timothy
 */
public class ActionLog {
	public int id;
	public String action;
	public Timestamp createdAt;
	
	/**
	 * @param action the action string
	 * @param createdAt when this log happened
	 */
	public ActionLog(int id, String action, Timestamp createdAt) {
		this.id = id;
		this.action = action;
		this.createdAt = createdAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ActionLog))
			return false;
		ActionLog other = (ActionLog) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ActionLog [id=" + id + ", action=" + action + ", createdAt=" + createdAt + "]";
	}
}
