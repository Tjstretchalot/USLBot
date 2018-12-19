package me.timothy.bots.memory;

import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.UnbanHistory;

/**
 * This is the result of a query that joins handled mod actions with both ban histories
 * and unban histories in a single step. We are guarranteed a handled mod action and exactly
 * 1 of: ban history, unban history
 * 
 * @author Timothy
 */
public class HandledModActionJoinHistory {
	/** The handled mod action */
	public HandledModAction handledModAction;
	/** Either the ban history corresponding to the action or null */
	public BanHistory banHistory;
	/** Either the unban history corresponding to the action or null */
	public UnbanHistory unbanHistory;
	
	/**
	 * Create a new instance that of an object that describes either a ban
	 * history or unban history.
	 * 
	 * @param handledModAction the handled mod action with a paired ban or unban
	 * @param banHistory the ban the handled mod action is paired with or null
	 * @param unbanHistory the unban the handled mod action is paired with or null
	 */
	public HandledModActionJoinHistory(HandledModAction handledModAction, BanHistory banHistory,
			UnbanHistory unbanHistory) {
		super();
		this.handledModAction = handledModAction;
		this.banHistory = banHistory;
		this.unbanHistory = unbanHistory;
	}
	
	/**
	 * Determine if this join is representing a ban
	 * @return true if this instance represents a ban, false otherwise
	 */
	public boolean isBan() {
		return banHistory != null;
	}
	
	/**
	 * Determine if this join is representing an unban
	 * @return true if this instance represents an unban, false otherwise
	 */
	public boolean isUnban() {
		return banHistory == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((banHistory == null) ? 0 : banHistory.hashCode());
		result = prime * result + ((handledModAction == null) ? 0 : handledModAction.hashCode());
		result = prime * result + ((unbanHistory == null) ? 0 : unbanHistory.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HandledModActionJoinHistory other = (HandledModActionJoinHistory) obj;
		if (banHistory == null) {
			if (other.banHistory != null) {
				return false;
			}
		} else if (!banHistory.equals(other.banHistory)) {
			return false;
		}
		if (handledModAction == null) {
			if (other.handledModAction != null) {
				return false;
			}
		} else if (!handledModAction.equals(other.handledModAction)) {
			return false;
		}
		if (unbanHistory == null) {
			if (other.unbanHistory != null) {
				return false;
			}
		} else if (!unbanHistory.equals(other.unbanHistory)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HandledModActionJoinHistory [handledModAction=" + handledModAction + ", banHistory=" + banHistory
				+ ", unbanHistory=" + unbanHistory + "]";
	}
}
