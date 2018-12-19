package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * This model lets us now where we are at mapping "reddit" to "meaning" in conjunction
 * with the HandledAtTimestamp
 * 
 * @author Timothy
 */
public class RedditToMeaningProgress {
	/**
	 * The most recent timestamp which we converted to USLAction. All events with a timestamp prior
	 * to this time can be assumed to have already been processed.
	 */
	public Timestamp lastEndedAt;

	/**
	 * @param lastEndedAt
	 */
	public RedditToMeaningProgress(Timestamp lastEndedAt) {
		super();
		this.lastEndedAt = lastEndedAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lastEndedAt == null) ? 0 : lastEndedAt.hashCode());
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
		RedditToMeaningProgress other = (RedditToMeaningProgress) obj;
		if (lastEndedAt == null) {
			if (other.lastEndedAt != null) {
				return false;
			}
		} else if (!lastEndedAt.equals(other.lastEndedAt)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "RedditToMeaningProgress [lastEndedAt=" + lastEndedAt + "]";
	}
}
