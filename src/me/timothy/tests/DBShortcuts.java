package me.timothy.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;

/**
 * This class should only be used for testing code. It wraps a database reference
 * and adds shortcuts for doing things that are common in testing.
 * @author Timothy
 *
 */
public class DBShortcuts {
	/** The database that is being wrapped */
	public MappingDatabase database;
	
	/** The configuration being wrapped */
	public USLFileConfiguration config;
	
	/** The time when this instance was initialized */
	public long now;
	
	/** This lets us create new hmas on the fly */
	public int hmaCounter;
	
	/** The earliest timestamp which can be in the mysql database */
	public final Timestamp epoch = new Timestamp(1000);
	
	public DBShortcuts(MappingDatabase database, USLFileConfiguration config) { 
		this.database = database;
		this.config = config;
		this.now = System.currentTimeMillis();
		this.hmaCounter = 0;
	}
	
	public DBShortcuts(MappingDatabase database) {
		this(database, null);
	}
	
	public Person person(String nm) {
		return database.getPersonMapping().fetchOrCreateByUsername(nm);
	}
	
	public Person mod() {
		return person("mod");
	}
	
	public Person mod2() {
		return person("mod2");
	}
	
	public Person user1() {
		return person("user1");
	}
	
	public Person bot() {
		return person(config.getProperty("user.username"));
	}
	
	public MonitoredSubreddit sub(String nm, boolean silent, boolean readOnly, boolean writeOnly) {
		MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByName(nm);
		if(sub != null)
			return sub;
		
		sub = new MonitoredSubreddit(-1, nm, silent, readOnly, writeOnly);
		database.getMonitoredSubredditMapping().save(sub);
		return sub;
	}
	
	public MonitoredSubreddit sub(String nm) {
		return sub(nm, true, false, false);
	}
	
	public MonitoredSubreddit primarySub() {
		return sub(config.getProperty("user.main_sub"));
	}
	
	public MonitoredSubreddit sub() {
		return sub("sub");
	}
	
	public MonitoredSubreddit sub2() {
		return sub("sub2");
	}
	
	public MonitoredSubreddit sub3() {
		return sub("sub3");
	}
	
	public Timestamp now() {
		return new Timestamp(now);
	}
	
	public Timestamp now(long delta) {
		return new Timestamp(now + delta);
	}
	
	public HandledModAction hma(MonitoredSubreddit sub, String id, Timestamp time) { 
		HandledModAction hma = new HandledModAction(-1, sub.id, id, time);
		database.getHandledModActionMapping().save(hma);
		return hma;
	}
	
	public HandledModAction hma(MonitoredSubreddit sub, String id) {
		return hma(sub, id, now());
	}
	
	public HandledModAction hma(MonitoredSubreddit sub, Timestamp time) {
		hmaCounter++;
		String id = "ModAction_ID" + hmaCounter;
		return hma(sub, id, time);
	}
	
	public HandledModAction hma(MonitoredSubreddit sub) {
		return hma(sub, now());
	}
	
	
	public BanHistory bh(Person mod, Person banned, HandledModAction hma, String msg, boolean perm) {
		BanHistory bh = new BanHistory(-1, mod.id, banned.id, hma.id, msg, perm ? "permanent" : "30 days");
		database.getBanHistoryMapping().save(bh);
		return bh;
	}
	
	public UnbanHistory ubh(Person mod, Person unbanned, HandledModAction hma) {
		UnbanHistory ubh = new UnbanHistory(-1, mod.id, unbanned.id, hma.id);
		database.getUnbanHistoryMapping().save(ubh);
		return ubh;
	}
	
	public Hashtag tag(String nm) {
		Person mod = mod();
		Hashtag tag = new Hashtag(-1, nm, "gen " + nm, mod.id, mod.id, now(), now());
		database.getHashtagMapping().save(tag);
		return tag;
	}
	
	public Hashtag scammerTag() {
		return tag("#scammer");
	}
	
	public Hashtag sketchyTag() {
		return tag("#sketchy");
	}

	public SubscribedHashtag attach(MonitoredSubreddit sub, Hashtag tag) {
		SubscribedHashtag subTag = new SubscribedHashtag(-1, sub.id, tag.id, now(), null);
		database.getSubscribedHashtagMapping().save(subTag);
		return subTag;
	}
	
	public USLAction action(boolean isBan, Person person, Hashtag[] tags, BanHistory[] bhs, UnbanHistory[] ubhs) {
		USLAction action = database.getUSLActionMapping().create(isBan, person.id, now());
		
		if(tags != null) {
			for(Hashtag tag : tags) {
				database.getUSLActionHashtagMapping().save(new USLActionHashtag(action.id, tag.id));
			}
		}
		
		if(bhs != null) {
			for(BanHistory bh : bhs) {
				database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(action.id, bh.id));
			}
		}
		
		if(ubhs != null) {
			for(UnbanHistory ubh : ubhs) {
				database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(action.id, ubh.id));
			}
		}
		
		return action;
	}
	
	public UnbanRequest unbanRequest(Person mod, Person user, Timestamp created, Timestamp handled, boolean invalid) {
		UnbanRequest req = new UnbanRequest(-1, mod.id, user.id, created, handled, invalid);
		database.getUnbanRequestMapping().save(req);
		return req;
	}
	
	public UnbanRequest unbanRequest(Person user) {
		return unbanRequest(mod(), user, now(), null, true);
	}
	
	public UnbanRequest unbanRequest(Person user, Timestamp time) {
		return unbanRequest(mod(), user, time, null, true);
	}
	
	public void handle(UnbanRequest req, Timestamp time, boolean invalid) {
		req.handledAt = time;
		req.invalid = invalid;
		database.getUnbanRequestMapping().save(req);
	}
	
	public SubredditModqueueProgress emptyProgress(MonitoredSubreddit sub) {
		SubredditModqueueProgress existing = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		if(existing != null) {
			existing.searchForward = true;
			existing.latestModActionID = null;
			existing.newestModActionID = null;
			existing.updatedAt = now();
			existing.lastTimeHadFullHistory = null;
			database.getSubredditModqueueProgressMapping().save(existing);
			return existing;
		}
		SubredditModqueueProgress res = new SubredditModqueueProgress(-1, sub.id, true, null, null, now(), null);
		database.getSubredditModqueueProgressMapping().save(res);
		return res;
	}
	
	public SubredditModqueueProgress fullProgress(MonitoredSubreddit sub, HandledModAction oldestHma, HandledModAction latestHma, Timestamp time) {
		SubredditModqueueProgress existing = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		if(existing != null) {
			existing.searchForward = false;
			existing.latestModActionID = oldestHma.modActionID;
			existing.newestModActionID = latestHma.modActionID;
			existing.updatedAt = time;
			existing.lastTimeHadFullHistory = time;
			database.getSubredditModqueueProgressMapping().save(existing);
			return existing;
		}
		SubredditModqueueProgress res = new SubredditModqueueProgress(-1, sub.id, false, oldestHma.modActionID, latestHma.modActionID, time, time);
		database.getSubredditModqueueProgressMapping().save(res);
		return res;
	}
	
	public SubredditModqueueProgress fullProgress(MonitoredSubreddit sub, Timestamp time) {
		List<HandledModAction> allSorted = database.getHandledModActionMapping().fetchLatestForSubreddit(sub.id, epoch, now(10000), 1000);
		assertTrue(allSorted.size() < 1000);
		assertFalse(allSorted.isEmpty());
		
		return fullProgress(sub, allSorted.get(0), allSorted.get(allSorted.size() - 1), time);
	}
	
	public SubredditModqueueProgress partialProgress(MonitoredSubreddit sub, HandledModAction oldest, HandledModAction newest, Timestamp time) { 
		SubredditModqueueProgress existing = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		if(existing != null) {
			existing.searchForward = true;
			existing.latestModActionID = oldest.modActionID;
			existing.newestModActionID = newest.modActionID;
			existing.updatedAt = time;
			existing.lastTimeHadFullHistory = null;
			database.getSubredditModqueueProgressMapping().save(existing);
			return existing;
		}
		SubredditModqueueProgress res = new SubredditModqueueProgress(-1, sub.id, true, oldest.modActionID, newest.modActionID, time, null);
		database.getSubredditModqueueProgressMapping().save(res);
		return res;
	}
	
	public Response response(String name, String val) {
		Response resp = new Response(-1, name, val, now(), now());
		database.getResponseMapping().save(resp);
		return resp;
	}
	
	public Response response(String name) {
		return response(name, name + " value");
	}
}
