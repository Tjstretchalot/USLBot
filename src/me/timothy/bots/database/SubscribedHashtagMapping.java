package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.SubscribedHashtag;

/**
 * Maps SubscribedHashtags.
 * 
 * @author Timothy
 */
public interface SubscribedHashtagMapping extends ObjectMapping<SubscribedHashtag> {
	/**
	 * Get all of the subcribed hashtags for a specific subreddit ID.
	 * 
	 * @param monitoredSubredditID the subreddit id
	 * @param deleted if true, deleted results are returned. if false, no deleted results are returned.
	 * @return the list of subscribed hashtags
	 */
	public List<SubscribedHashtag> fetchForSubreddit(int monitoredSubredditID, boolean deleted);
}
