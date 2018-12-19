package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLPropagator;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests the USLBanHistoryPropagator
 * 
 * @author Timothy
 */
public class PropagatorTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLPropagator propagator;
	
	private Person bot;
	private Person mod;
	private Person banned;
	
	private MonitoredSubreddit sub1;
	private MonitoredSubreddit sub2;
	private MonitoredSubreddit sub3;
	
	private long now;
	private int hmaCounter;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		propagator = new USLPropagator(database, config);
		
		MysqlTestUtils.clearDatabase(database);
		
		now = System.currentTimeMillis();
		
		bot = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		mod = database.getPersonMapping().fetchOrCreateByUsername("mod");
		banned = database.getPersonMapping().fetchOrCreateByUsername("banned");
		
		sub1 = new MonitoredSubreddit(-1, "sub1", true, false, false);
		database.getMonitoredSubredditMapping().save(sub1);
		
		sub2 = new MonitoredSubreddit(-1, "sub2", true, false, false);
		database.getMonitoredSubredditMapping().save(sub2);
		
		sub3 = new MonitoredSubreddit(-1, "sub3", true, false, false);
		database.getMonitoredSubredditMapping().save(sub3);
		
		hmaCounter = 0;
	}
	
	/**
	 * Make sure setup got everything not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(propagator);
	}

	
	/**
	 * init required responses with filler data. only 1 thing should test responses
	 * substitutions, the rest should just test they are sent out
	 */
	private void initResponses() {
		database.getResponseMapping().save(new Response(-1, "propagated_ban_message", "ban message", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_note", "ban note", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_title", "ban modmail title", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagated_ban_modmail_body", "ban modmail body", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_override_unban_title", "override unban title", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_override_unban_body", "override unban body", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_ban_collision_to_collider_title", "ban collision title", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_ban_to_subreddit_ban_collision_to_collider_body", "ban collision body", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_std_modmail_title", "unban std. title", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_std_modmail_body", "unban std. body", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_primary_modmail_title", "unban prim. title", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_primary_modmail_body", "unban prim. body", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_failed_modmail_title", "unban failed", now(), now()));
		database.getResponseMapping().save(new Response(-1, "propagate_unban_failed_modmail_body", "unban failed", now(), now()));
	}
	
	private Timestamp now() {
		return new Timestamp(now);
	}
	
	private Hashtag scammerTag() {
		Hashtag res = new Hashtag(-1, "#scammer", "famous tag", mod.id, mod.id, now(), now());
		database.getHashtagMapping().save(res);
		return res;
	}
	
	private Hashtag sketchyTag() {
		Hashtag res = new Hashtag(-1, "#sketchy", "second tag", mod.id, mod.id, now(), now());
		database.getHashtagMapping().save(res);
		return res;
	}
	
	private SubscribedHashtag attach(MonitoredSubreddit sub, Hashtag tag) {
		SubscribedHashtag subTag = new SubscribedHashtag(-1, sub.id, tag.id, now(), null);
		database.getSubscribedHashtagMapping().save(subTag);
		return subTag;
	}

	private HandledModAction hma(MonitoredSubreddit sub, long time) {
		hmaCounter++;
		
		HandledModAction hma = new HandledModAction(-1, sub.id, "ModAction_ID" + hmaCounter, new Timestamp(time));
		database.getHandledModActionMapping().save(hma);
		return hma;
	}
	
	private HandledModAction hma(MonitoredSubreddit sub) {
		return hma(sub, now);
	}
	
	
	private BanHistory bh(Person mod, Person banned, HandledModAction hma, String msg, boolean perm) {
		BanHistory bh = new BanHistory(-1, mod.id, banned.id, hma.id, msg, perm ? "permanent" : "30 days");
		database.getBanHistoryMapping().save(bh);
		return bh;
	}
	
	private UnbanHistory ubh(Person mod, Person unbanned, HandledModAction hma) {
		UnbanHistory ubh = new UnbanHistory(-1, mod.id, unbanned.id, hma.id);
		database.getUnbanHistoryMapping().save(ubh);
		return ubh;
	}
	
	private USLAction action(boolean isBan, Person person, Hashtag[] tags, BanHistory[] bhs, UnbanHistory[] ubhs) {
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
	
	/**
	 * Tests that we won't try to ban a dude on a subreddit because the subreddit banned that dude
	 */
	@Test
	public void testDoesntPropagateSubredditBanToItself() {
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma1 = hma(sub1);
		BanHistory bh1 = bh(mod, banned, hma1, scammer.tag, true);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { bh1 }, null);
		
		PropagateResult result = propagator.propagateAction(action);
		assertEquals(action, result.action);
		assertTrue(result.bans.isEmpty());
		assertTrue(result.modmailPMs.isEmpty());
		assertTrue(result.userPMs.isEmpty());
	}
	
	/**
	 * Tests that it propagates to other subreddits that follow the tags
	 */
	@Test
	public void testPropagatesToOther() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		attach(sub3, scammer);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, null);
		
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans, 
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub3))));
	}
	
	/**
	 * Tests it correctly handles situations with different multiple people being banned
	 * for different reasons
	 */
	@Test
	public void testPropagatesDifferentHashtags() {
		initResponses();
		Hashtag scammer = scammerTag();
		Hashtag sketchy = sketchyTag();
		attach(sub1, scammer);
		attach(sub1, sketchy);
		attach(sub2, scammer);
		attach(sub3, sketchy);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, null);
		action.createdAt = now();
		database.getUSLActionMapping().save(action);
		
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans, 
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub2))));
		
		
		USLAction action2 = action(true, banned, new Hashtag[] { sketchy }, null, null);
		
		result = propagator.propagateAction(action2);
		MysqlTestUtils.assertListContentsPreds(result.bans, 
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub3))));
		
		USLAction action3 = action(true, banned, new Hashtag[] { scammer, sketchy }, null, null);

		result = propagator.propagateAction(action3);
		MysqlTestUtils.assertListContentsPreds(result.bans, 
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub2))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub3))));
	}
	
	/**
	 * Verify that it won't propagate to a subreddit for a tag it no longer subscribes to
	 */
	@Test
	public void testDoesntPropagateDeletedHashtags() {
		initResponses();
		Hashtag scammer = scammerTag();
		
		SubscribedHashtag st = attach(sub1, scammer);
		st.deletedAt = now();
		database.getSubscribedHashtagMapping().save(st);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, null);
		
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
	}
	
	/**
	 * Tests that it won't send modmail to a subreddit about banning someone unless they aren't
	 * in silent mode.
	 */
	@Test
	public void testDoesntMailSilent() {
		sub2.silent = false;
		database.getMonitoredSubredditMapping().save(sub2);
		
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		attach(sub2, scammer);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, null);
		
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans,
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub2))));
		MysqlTestUtils.assertListContentsPreds(result.modmailPMs,
				((a) -> (a.subreddit.equals(sub2))));
	}
	
	/**
	 * Verifies that it won't try to propagate a ban to a write-only subreddit.
	 */
	@Test
	public void testDoesntPropagateToWriteOnly() {
		sub3.writeOnly = true;
		database.getMonitoredSubredditMapping().save(sub3);
		
		initResponses();
		scammerTag();
		Hashtag sketchy = sketchyTag();
		attach(sub1, sketchy);
		attach(sub2, sketchy);
		attach(sub3, sketchy);
		
		USLAction action = action(true, banned, new Hashtag[] { sketchy }, null, null);
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans,
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))),
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub2))));
	}
	
	@Test
	public void testDoesntBanUnbannedUsers() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now + 5000);
		UnbanHistory ubh = ubh(mod, banned, hma);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, new UnbanHistory[] { ubh });

		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
	}
	
	@Test
	public void testDoesBanPreviouslyUnbannedUsers() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now - 5000);
		UnbanHistory ubh = ubh(mod, banned, hma);
		
		USLAction action = action(true, banned, new Hashtag[] { scammer }, null, new UnbanHistory[] { ubh });

		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans,
				((a) -> (a.person.equals(banned) && a.subreddit.equals(sub1))));
	}
	
	@Test
	public void testUnbansBotBansWhenStillFollowing() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now - 5000);
		BanHistory bh = bh(bot, banned, hma, scammer.tag, true);
		
		USLAction action = action(false, banned, null, new BanHistory[] { bh }, null);

		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
		MysqlTestUtils.assertListContentsPreds(result.unbans,
				(a) -> a.person.equals(banned) && a.subreddit.equals(sub1));
		MysqlTestUtils.assertListContentsPreds(result.modmailPMs);
	}
	
	@Test
	public void testDoesntOverrideReban() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now + 5000);
		BanHistory bh = bh(bot, banned, hma, scammer.tag, true);
		
		USLAction action = action(false, banned, null, new BanHistory[] { bh }, null);

		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
		MysqlTestUtils.assertListContentsPreds(result.unbans);
		MysqlTestUtils.assertListContentsPreds(result.modmailPMs);
	}
	
	@Test
	public void testDoesntUnbanUnrelatedButDoesSendMessage() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now - 5000);
		BanHistory bh = bh(mod, banned, hma, "notag", true);
		
		USLAction action = action(false, banned, null, new BanHistory[] { bh }, null);

		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
		MysqlTestUtils.assertListContentsPreds(result.unbans);
		MysqlTestUtils.assertListContentsPreds(result.modmailPMs, 
				(a) -> a.subreddit.equals(sub1));
	}
	
	@Test
	public void testDoesUnbanModBanWithTag() {
		initResponses();
		Hashtag scammer = scammerTag();
		attach(sub1, scammer);
		
		HandledModAction hma = hma(sub1, now - 5000);
		BanHistory bh = bh(mod, banned, hma, scammer.tag, true);
		
		USLAction action = action(false, banned, null, new BanHistory[] { bh }, null);
		
		PropagateResult result = propagator.propagateAction(action);
		MysqlTestUtils.assertListContentsPreds(result.bans);
		MysqlTestUtils.assertListContentsPreds(result.unbans, 
				(a) -> a.person.equals(banned) && a.subreddit.equals(sub1));
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		propagator = null;
	}
}
