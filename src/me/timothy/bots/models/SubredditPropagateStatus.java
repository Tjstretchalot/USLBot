package me.timothy.bots.models;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import me.timothy.bots.database.MappingDatabase;

/**
 * Keeps track of where we are when it comes to propagating usl actions to a subreddit.
 * 
 * @author Timothy
 */
public class SubredditPropagateStatus {
	/** The id of the row in the database, or -1 if not in the database yet. */
	public int id;
	/** The id of the monitored subreddit that this is referring to */
	public int monitoredSubredditID;
	/** 
	 * The biggest id of the action that we have already propagated to this subreddit. 0 for nothing
	 * propagated yet
	 */
	public int actionID;
	/** When we last updated this row */
	public Timestamp updatedAt;
	
	/**
	 * @param id the id of the row in the database, or -1 if not in the database yet
	 * @param monitoredSubredditID the id of the subreddit this is a status for
	 * @param actionID the biggest USLAction id that has been propagated to this subreddit. 0 for nothing propagated yet
	 * @param updatedAt when this row was last updated
	 */
	public SubredditPropagateStatus(int id, int monitoredSubredditID, int actionID, Timestamp updatedAt) {
		super();
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.actionID = actionID;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determines if this is in a potentially viable state for saving to the database
	 * @return if this passes a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actionID;
		result = prime * result + id;
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
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
		SubredditPropagateStatus other = (SubredditPropagateStatus) obj;
		if (actionID != other.actionID)
			return false;
		if (id != other.id)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (updatedAt == null) {
			if (other.updatedAt != null)
				return false;
		} else if (!updatedAt.equals(other.updatedAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubredditPropagateStatus [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID + ", actionID="
				+ actionID + ", updatedAt=" + updatedAt + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[id=" + id + ", sub=" + db.getMonitoredSubredditMapping().fetchByID(monitoredSubredditID).subreddit
				+ ", actionID=" + actionID 
				+ ", updatedAt=" + updatedAt == null ? null : SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(updatedAt) + "]";
	}
}
