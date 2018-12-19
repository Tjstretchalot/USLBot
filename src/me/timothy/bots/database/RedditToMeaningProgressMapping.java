package me.timothy.bots.database;

import java.sql.Timestamp;

import me.timothy.bots.models.RedditToMeaningProgress;

/**
 * Describes something which is capable of holding a single timestamp in such a way that we can
 * recover it immediately.
 * 
 * @author Timothy
 */
public interface RedditToMeaningProgressMapping extends ObjectMapping<RedditToMeaningProgress> {
	/**
	 * Fetch the timestamp that is held by this mapping, or return null if this was inited empty.
	 * @return the value held in the database
	 */
	public Timestamp fetch();
	
	/**
	 * Set the value in the database to the given timestamp
	 * @param stamp the timestamp to set the value to
	 */
	public void set(Timestamp stamp);
}
