package me.timothy.bots;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.info.Account;

/**
 * A convenience wrapper tha manages keeping an up-to-date list of accounts which
 * have been deleted.
 * 
 * @author Timothy
 */
public class DeletedPersonManager {
	private static final long TIME_BETWEEN_PRUNES = 1000 * 60 * 60;
	private static final long CACHE_TIME = 1000 * 60 * 30;
	
	private Map<String, Timestamp> cacheExistentUsers;
	
	private MappingDatabase database;
	private BotDriver driver;
	private Runnable maybeLoginAgainRunnable;
	
	private long nextPruneTime;
	
	public DeletedPersonManager(MappingDatabase database, BotDriver driver, Runnable maybeLoginAgainRunnable) {
		this.database = database;
		this.driver = driver;
		this.maybeLoginAgainRunnable = maybeLoginAgainRunnable;
		this.nextPruneTime = System.currentTimeMillis() + TIME_BETWEEN_PRUNES;
		
		cacheExistentUsers = new HashMap<>();
	}
	
	public boolean isDeleted(String user) {
		long now = System.currentTimeMillis();
		if(now > nextPruneTime) {
			prune();
			nextPruneTime = now + TIME_BETWEEN_PRUNES;
		}
		
		Timestamp checkedAt = cacheExistentUsers.getOrDefault(user, null);
		if(checkedAt == null) {
			return checkNoMemoryCacheAndCache(user);
		}
		
		Timestamp viableCacheTime = new Timestamp(now - CACHE_TIME);
		if(checkedAt.before(viableCacheTime)) {
			cacheExistentUsers.remove(user);
			return checkNoMemoryCacheAndCache(user);
		}
		
		// In the existent user cache
		return false;
	}
	
	protected boolean checkNoMemoryCacheAndCache(String user) {
		Person pers = database.getPersonMapping().fetchByUsername(user);
		if(pers == null || !database.getDeletedPersonMapping().contains(pers.id)) {
			return reallyCheckAndCache(user);
		}
		// person exists and is in the deleted person mapping
		return true;
	}
	
	protected boolean reallyCheckAndCache(String user) {
		boolean deleted = reallyCheckIsDeleted(user);
		if(deleted) {
			Person pers = database.getPersonMapping().fetchOrCreateByUsername(user);
			database.getDeletedPersonMapping().addIfNotExists(pers.id);
			return true;
		}else {
			cacheExistentUsers.put(user, new Timestamp(System.currentTimeMillis()));
			return false;
		}
	}
	
	protected void prune() {
		Map<String, Timestamp> newCache = new HashMap<>();
		
		Timestamp purgeBefore = new Timestamp(System.currentTimeMillis() - CACHE_TIME);
		for(Entry<String, Timestamp> kvp : cacheExistentUsers.entrySet()) {
			Timestamp checkedAt = kvp.getValue();
			if(checkedAt.after(purgeBefore)) {
				newCache.put(kvp.getKey(), checkedAt);
			}
		}
		
		cacheExistentUsers = newCache;
	}
	
	protected boolean reallyCheckIsDeleted(String user) {
		Account[] result = new Account[1];
		new Retryable<Boolean>("Get Account", maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				result[0] = RedditUtils.getAccountFor(driver.bot.getUser(), user);
				return true;
			}

		}.run();
		driver.sleepFor(BotDriver.BRIEF_PAUSE_MS);
		return result[0] == null;
	}
}
