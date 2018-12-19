package me.timothy.bots.models;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import me.timothy.bots.database.MappingDatabase;

/*
 * Originally I set it up that this class was not necessary, because I could
 * use the mod action id provided by the BanHistory to keep track, however
 * then I realized I wanted to also save UnbanHistory and that meant I would
 * have different types being fetched from about/log so I needed a joining
 * table on the mod action.
 */
/**
 * Describes a modaction which was already handled. This is used
 * by the SubredditModqueueProgress to keep track of which modactions
 * we have already looked at.
 * 
 * @author Timtohy
 */
public class HandledModAction {
	/** Our local database id */
	public int id;
	/** Monitored subreddit id */
	public int monitoredSubredditID;
	/** Reddits long unique identifier for the mod action. Typically 47 characters  */
	public String modActionID;
	/** When this action occurred according to reddit */
	public Timestamp occurredAt;
	
	/**
	 * @param id local database id or -1 if not in database
	 * @param monitoredSubredditID the monitored subreddit id
	 * @param modActionID the mod action id on reddit
	 * @param occurredAt the time this occurred from reddit
	 */
	public HandledModAction(int id, int monitoredSubredditID, String modActionID, Timestamp occurredAt) {
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.modActionID = modActionID;
		this.occurredAt = occurredAt;
	}
	
	/**
	 * Ensures this passes a sanity check for enterring into the database
	 * @return if this passes sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 && modActionID != null && occurredAt != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + monitoredSubredditID;
		result = prime * result + ((modActionID == null) ? 0 : modActionID.hashCode());
		result = prime * result + ((occurredAt == null) ? 0 : occurredAt.hashCode());
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
		HandledModAction other = (HandledModAction) obj;
		if (id != other.id)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		if (modActionID == null) {
			if (other.modActionID != null)
				return false;
		} else if (!modActionID.equals(other.modActionID))
			return false;
		if (occurredAt == null) {
			if (other.occurredAt != null)
				return false;
		} else if (!occurredAt.equals(other.occurredAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HandledModAction [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID
				+ ", modActionID=" + modActionID + ", occurredAt=" + occurredAt + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[id=" + id + ", sub=" + db.getMonitoredSubredditMapping().fetchByID(monitoredSubredditID).subreddit 
				+ ", occurredAt=" + SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(occurredAt) + "]";
	}
}
