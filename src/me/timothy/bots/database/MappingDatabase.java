package me.timothy.bots.database;

/**
 * A database that contains a mapping for all inmemory models
 * to and from the database of choice.
 * 
 * @author Timothy Moore
 */
public interface MappingDatabase {
	public FullnameMapping getFullnameMapping();
	public MonitoredSubredditMapping getMonitoredSubredditMapping();
	public PersonMapping getPersonMapping();
	public BanHistoryMapping getBanHistoryMapping();
	public ResponseMapping getResponseMapping();
	public SubscribedHashtagMapping getSubscribedHashtagMapping();
	public SubredditModqueueProgressMapping getSubredditModqueueProgressMapping();
}
