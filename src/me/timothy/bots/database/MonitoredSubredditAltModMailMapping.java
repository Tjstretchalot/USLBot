package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.MonitoredSubredditAltModMail;

/**
 * Maps subreddits to an alternative subreddit for modmail purposes
 * 
 * @author Timothy
 */
public interface MonitoredSubredditAltModMailMapping extends ObjectMapping<MonitoredSubredditAltModMail> {
	/**
	 * Fetch the subreddit that should be posted to instead of the modmail for the
	 * given monitored subreddit if such a subreddit exists, otherwise 
	 * @param monitoredSubredditID
	 * @return the subreddits to post on instead of modmailing the given subreddit, or an empty list
	 * if they have no alternate subreddits.
	 */
	public List<String> fetchForSubreddit(int monitoredSubredditID);
}
