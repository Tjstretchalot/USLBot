package me.timothy.bots.models;

import java.sql.Timestamp;

import me.timothy.bots.database.MappingDatabase;

/**
 * Subreddits are allowed to specify what to do in the event the
 * USLBot recieves a new ban message. This is done with 
 * string-literals that are generally (but not always) prefixed
 * with a hashtag.
 * 
 * For example, subreddit johnssub might join the USL-coalition.
 * Subreddit john is given read-only permissions from the subreddit.
 * Additionally, subreddit john chooses to subscribe to the 
 * following hashtags:
 *   - "#scammer" 
 *   - "#sketchy"
 *   - "#compromised account"
 * 
 * So if a user is added to the scammer list with the reason
 * "this guy is #sketchy"
 * johnssub would ban the user.
 * 
 * If the user is added to the scammer list with the reason
 * "this guy is #aggressive", he would not be banned on johnssub.
 * 
 * These hashtags are never updated. They can be flagged deleted by
 * setting the deletedAt to a non-null timestamp.
 * 
 * These used to be directly string literals, but now we go through a hashtag mapping layer for 
 * performance reasons and to improve coordination between subreddits.
 * 
 * @author Timothy
 */
public class SubscribedHashtag {
	public int id;
	public int monitoredSubredditID;
	public int hashtagID;
	public Timestamp createdAt;
	public Timestamp deletedAt;
	
	/**
	 * Create a new subscribed-hashtag
	 * 
	 * @param id id (or -1 if not in database)
	 * @param monitoredSubredditID relevant monitored subreddit id
	 * @param hashtagID the id of the hashtag
	 * @param createdAt created at timestamp
	 * @param deletedAt deleted at timestamp (or null if not deleted)
	 */
	public SubscribedHashtag(int id, int monitoredSubredditID, int hashtagID, Timestamp createdAt,
			Timestamp deletedAt) {
		super();
		this.id = id;
		this.monitoredSubredditID = monitoredSubredditID;
		this.hashtagID = hashtagID;
		this.createdAt = createdAt;
		this.deletedAt = deletedAt;
	}
	
	/**
	 * Determines if there is sufficient data to enter into the database.
	 * 
	 * @return if this entry matches a sanity check
	 */
	public boolean isValid() {
		return monitoredSubredditID > 0 && hashtagID > 0 && createdAt != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((deletedAt == null) ? 0 : deletedAt.hashCode());
		result = prime * result + hashtagID;
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
		SubscribedHashtag other = (SubscribedHashtag) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (deletedAt == null) {
			if (other.deletedAt != null)
				return false;
		} else if (!deletedAt.equals(other.deletedAt))
			return false;
		if (hashtagID != other.hashtagID)
			return false;
		if (id != other.id)
			return false;
		if (monitoredSubredditID != other.monitoredSubredditID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubscribedHashtag [id=" + id + ", monitoredSubredditID=" + monitoredSubredditID + ", hashtag_id="
				+ hashtagID + ", createdAt=" + createdAt + ", deletedAt=" + deletedAt + "]";
	}

	public String toPrettyString(MappingDatabase db) {
		return "SubsTag [id=" + id + ", sub=" + db.getMonitoredSubredditMapping().fetchByID(this.monitoredSubredditID).subreddit
				+ ", hashtag=" + db.getHashtagMapping().fetchByID(hashtagID) + "]"; 
	}
}
