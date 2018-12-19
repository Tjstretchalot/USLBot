package me.timothy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLRedditToMeaningProcessor;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class USLRedditToMeaningProcessorTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLRedditToMeaningProcessor processor;
	
	private long now;
	
	private Person bot;
	private Person mod;
	private Person mod2;
	private Person banned1;
	
	private MonitoredSubreddit usl;
	private MonitoredSubreddit borrow;
	private MonitoredSubreddit giftCardExchange;
	
	private List<Hashtag> tags;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		processor = new USLRedditToMeaningProcessor(database, config);
		
		MysqlTestUtils.clearDatabase(database);
		
		bot = database.getPersonMapping().fetchOrCreateByUsername(config.getProperty("user.username"));
		mod = database.getPersonMapping().fetchOrCreateByUsername("tjstretchalot");
		mod2 = database.getPersonMapping().fetchOrCreateByUsername("nter");
		banned1 = database.getPersonMapping().fetchOrCreateByUsername("badguy1998");
		
		usl = new MonitoredSubreddit(-1, "universalscammerlist", true, false, false);
		database.getMonitoredSubredditMapping().save(usl);
		
		borrow = new MonitoredSubreddit(-1, "borrow", true, false, false);
		database.getMonitoredSubredditMapping().save(borrow);
		
		giftCardExchange = new MonitoredSubreddit(-1, "giftcardexchange", true, false, false);
		database.getMonitoredSubredditMapping().save(giftCardExchange);
		
		now = System.currentTimeMillis();
		
		tags = new ArrayList<>();
	}
	
	private Timestamp now() {
		return new Timestamp(now);
	}
	
	private Hashtag scammerTag() {
		Hashtag tag = new Hashtag(-1, "#scammer", "scammer tag", mod.id, mod.id, now(), now());
		database.getHashtagMapping().save(tag);
		tags.add(tag);
		return tag;
	}
	
	private Hashtag sketchyTag() {
		Hashtag tag = new Hashtag(-1, "#sketchy", "sketchy tag", mod.id, mod.id, now(), now());
		database.getHashtagMapping().save(tag);
		tags.add(tag);
		return tag;
	}
	
	/**
	 * Make sure setup got everything not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(processor);
	}
	
	/**
	 * If someone is not banned, then we ban them for a temporary duration, no usl action is produced.
	 */
	@Test
	public void testTemporaryBanDoesNothing() {
		Hashtag scammer = scammerTag();
		
		HandledModAction hma = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, mod.id, banned1.id, hma.id, scammer.tag, "30 days");
		database.getBanHistoryMapping().save(ban);
		
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
		
		processor.processBan(tags, hma, ban);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
	}
	
	/**
	 * If someone is banned permanently, but not with any tags we are aware of, no usl action is produced
	 */
	@Test
	public void testTaglessBanDoesNothing() {
		scammerTag();

		HandledModAction hma = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, mod.id, banned1.id, hma.id, "notag", "permanent");
		database.getBanHistoryMapping().save(ban);
		
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
		
		processor.processBan(tags, hma, ban);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
	}
	
	/**
	 * If someone is banned permanently with a tag, we should get a USLAction
	 */
	@Test
	public void testBanWithTagProducesAction() {
		Hashtag scammer = scammerTag();
		
		HandledModAction hma = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, mod.id, banned1.id, hma.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban);
		
		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
		
		processor.processBan(tags, hma, ban);
		
		List<USLAction> lFromDb = database.getUSLActionMapping().fetchAll();
		assertEquals(1, lFromDb.size());
		
		USLAction fromDb = lFromDb.get(0);
		assertEquals(banned1.id, fromDb.personID);
		assertEquals(true, fromDb.isBan);
		assertEquals(true, fromDb.isLatest);
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		assertEquals(1, lBHFromDb.size());
		
		USLActionBanHistory bhFromDB = lBHFromDb.get(0);
		assertEquals(fromDb.id, bhFromDB.actionID);
		assertEquals(ban.id, bhFromDB.banHistoryID);
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		assertEquals(1, lHTFromDb.size());
		
		USLActionHashtag htFromDb = lHTFromDb.get(0);
		assertEquals(fromDb.id, htFromDb.actionID);
		assertEquals(scammer.id, htFromDb.hashtagID);
	}
	
	/**
	 * If someone is banned permanently with multiple tags, we should get a usl action
	 * with each tag attached
	 */
	@Test
	public void testBanWithMultipleTagsProducesAction() {
		Hashtag sketchy = sketchyTag();
		Hashtag scammer = scammerTag();
		
		HandledModAction hma = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma);
		
		BanHistory ban = new BanHistory(-1, mod.id, banned1.id, hma.id, scammer.tag + " " + sketchy.tag, "permanent");
		database.getBanHistoryMapping().save(ban);

		MysqlTestUtils.assertListContents(database.getUSLActionMapping().fetchAll());
		
		processor.processBan(tags, hma, ban);
		
		List<USLAction> lFromDb = database.getUSLActionMapping().fetchAll();
		assertEquals(1, lFromDb.size());
		
		USLAction fromDb = lFromDb.get(0);
		assertEquals(banned1.id, fromDb.personID);
		assertEquals(true, fromDb.isBan);
		assertEquals(true, fromDb.isLatest);
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban.id)));
		
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == fromDb.id && a.hashtagID == scammer.id)), 
				((a) -> (a.actionID == fromDb.id && a.hashtagID == sketchy.id)));
	}
	
	
	/**
	 * If someone is banned permanently with a tag, then the bot bans that person on another subreddit, it is 
	 * simply attached to the current action.
	 */
	@Test
	public void testBanWithTagAndBotBansProducesSingleAction() {
		Hashtag scammer = scammerTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, usl.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, bot.id, banned1.id, hma2.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		processor.processBan(tags, hma2, ban2);

		List<USLAction> lFromDb = database.getUSLActionMapping().fetchAll();
		assertEquals(1, lFromDb.size());
		
		USLAction fromDb = lFromDb.get(0);
		assertEquals(banned1.id, fromDb.personID);
		assertEquals(true, fromDb.isBan);
		assertEquals(true, fromDb.isLatest);
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban1.id)),
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban2.id)));
		
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == fromDb.id && a.hashtagID == scammer.id)));
		
		
	}
	
	/**
	 * If someone bans with a tag, then the bot bans them on another subreddit, we now have one action. If 
	 * someone bans with the same tag, then that ban is simply attached to the existing action.
	 */
	@Test
	public void testBanWithTagAndBotBansAndConflictingBanProducesSingleAction() {
		Hashtag scammer = scammerTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, usl.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, bot.id, banned1.id, hma2.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		processor.processBan(tags, hma2, ban2);
		
		HandledModAction hma3 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID3", now());
		database.getHandledModActionMapping().save(hma3);
		
		BanHistory ban3 = new BanHistory(-1, mod2.id, banned1.id, hma3.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban3);
		
		processor.processBan(tags, hma3, ban3);

		List<USLAction> lFromDb = database.getUSLActionMapping().fetchAll();
		assertEquals(1, lFromDb.size());
		
		USLAction fromDb = lFromDb.get(0);
		assertEquals(banned1.id, fromDb.personID);
		assertEquals(true, fromDb.isBan);
		assertEquals(true, fromDb.isLatest);
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban1.id)),
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban2.id)),
				((a) -> (a.actionID == fromDb.id && a.banHistoryID == ban3.id)));
		
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == fromDb.id && a.hashtagID == scammer.id)));
	}
	
	/**
	 * If someone bans with a tag, then someone bans with a different tag, then we end up getting two actions
	 * and we now have both tags attached.
	 */
	@Test
	public void testBanWithTagThenSecondBanWithSecondTagProducesTwoActions() {
		Hashtag scammer = scammerTag();
		Hashtag sketchy = sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, mod2.id, banned1.id, hma2.id, sketchy.tag, "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		processor.processBan(tags, hma2, ban2);

		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && !a.isLatest)),
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban1.id)),
				((a) -> (a.actionID == lFromDb.get(1).id && a.banHistoryID == ban1.id)), 
				((a) -> (a.actionID == lFromDb.get(1).id && a.banHistoryID == ban2.id)));
		
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == scammer.id)),
				((a) -> (a.actionID == lFromDb.get(1).id && a.hashtagID == scammer.id)),
				((a) -> (a.actionID == lFromDb.get(1).id && a.hashtagID == sketchy.id)));
		
	}
	
	/**
	 * If someone bans them with a tag, then unbans them, we end up with one action. The action has no bans
	 * attached and one unban attached, but maintains the original tag.
	 */
	@Test
	public void testBanWithTagThenUnbanDetachesBanAndAttachesUnban() {
		Hashtag scammer = scammerTag();
		sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, borrow.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod.id, banned1.id, hma1.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		processor.processUnban(tags, hma2, unban1);

		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb);
		
		List<USLActionUnbanHistory> lUHFromDb = database.getUSLActionUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lUHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.unbanHistoryID == unban1.id)));
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == scammer.id)));
	}
	
	/**
	 * If someone bans someone without a tag, then another subreddit bans that person with a tag, we end
	 * up with one usl action with that tag attached and both bans attached.
	 */
	@Test
	public void testBanWithoutTagThenOtherSubBansWithTagAttachesOldBan() {
		scammerTag();
		Hashtag sketchy = sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, "notag", "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID3", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, mod2.id, banned1.id, hma2.id, sketchy.tag, "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		processor.processBan(tags, hma2, ban2);

		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban1.id)),
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban2.id)));
		
		List<USLActionUnbanHistory> lUHFromDb = database.getUSLActionUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lUHFromDb);
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == sketchy.id)));
	}
	
	
	/**
	 * If someone bans someone with a tag, then that gets propagated by the bot, and one of the subreddits
	 * its propagated to unbans the user, we end up with only 1 action that has all the bans EXCEPT for 
	 * the unbanning subreddit attached, and the unban attached.
	 * 
	 * This also has the history scanned after it's all produced rather than one-at-a-time, which shouldn't
	 * have any major effect but might explain why this one fails if it does.
	 */
	@Test
	public void testBanWithTagThenPropagateThenUnbanGetsUnbanAttached() {
		Hashtag scammer = scammerTag();
		sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		HandledModAction hma2 = new HandledModAction(-1, usl.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban2 = new BanHistory(-1, bot.id, banned1.id, hma2.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban2);
		
		HandledModAction hma3 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID3", now());
		database.getHandledModActionMapping().save(hma3);
		
		BanHistory ban3 = new BanHistory(-1, bot.id, banned1.id, hma3.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban3);
		
		HandledModAction hma4 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID4", now());
		database.getHandledModActionMapping().save(hma4);
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod2.id, banned1.id, hma4.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		processor.processBan(tags, hma1, ban1);
		processor.processBan(tags, hma2, ban2);
		processor.processBan(tags, hma3, ban3);
		processor.processUnban(tags, hma4, unban1);


		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban1.id)),
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban2.id)));
		
		List<USLActionUnbanHistory> lUHFromDb = database.getUSLActionUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lUHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.unbanHistoryID == unban1.id)));
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == scammer.id)));
	}
	
	/**
	 * If we didn't have the original ban information, but then a user gets unbanned and we see it, then 
	 * that same subreddit bans them, we get a single action as if we didn't know about the unattached
	 * unban.
	 */
	@Test
	public void testUnbanThenBanWithTagGetsAction() {
		Hashtag scammer = scammerTag();
		sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", new Timestamp(now - 3000));
		database.getHandledModActionMapping().save(hma1);
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod.id, banned1.id, hma1.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		HandledModAction hma2 = new HandledModAction(-1, borrow.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma2.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processUnban(tags, hma1, unban1);
		processor.processBan(tags, hma2, ban1);


		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban1.id)));
		
		List<USLActionUnbanHistory> lUHFromDb = database.getUSLActionUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lUHFromDb);
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == scammer.id)));
	}
	
	/**
	 * If we didn't have the original ban information, then another subreddit bans them with a tag, then 
	 * we get an action with the unban attached and the ban attached and the appropriate tag
	 */
	@Test
	public void testUnbanThenOtherSubBanWithTagGetsAction() {
		Hashtag scammer = scammerTag();
		sketchyTag();
		
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", now());
		database.getHandledModActionMapping().save(hma1);
		
		UnbanHistory unban1 = new UnbanHistory(-1, mod.id, banned1.id, hma1.id);
		database.getUnbanHistoryMapping().save(unban1);
		
		HandledModAction hma2 = new HandledModAction(-1, giftCardExchange.id, "ModAction_ID2", now());
		database.getHandledModActionMapping().save(hma2);
		
		BanHistory ban1 = new BanHistory(-1, mod2.id, banned1.id, hma2.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processUnban(tags, hma1, unban1);
		processor.processBan(tags, hma2, ban1);


		List<USLAction> lFromDb = MysqlTestUtils.orderToMatchPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest)));
		
		List<USLActionBanHistory> lBHFromDb = database.getUSLActionBanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lBHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.banHistoryID == ban1.id)));
		
		List<USLActionUnbanHistory> lUHFromDb = database.getUSLActionUnbanHistoryMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lUHFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.unbanHistoryID == unban1.id)));
		
		List<USLActionHashtag> lHTFromDb = database.getUSLActionHashtagMapping().fetchAll();
		MysqlTestUtils.assertListContentsPreds(lHTFromDb,
				((a) -> (a.actionID == lFromDb.get(0).id && a.hashtagID == scammer.id)));
	}
	
	@Test
	public void testBanWithOldTimeProducesActionAtOldTime() {
		Hashtag scammer = scammerTag();
		
		long oldTime = now - 60000;
		HandledModAction hma1 = new HandledModAction(-1, borrow.id, "ModAction_ID1", new Timestamp(oldTime));
		database.getHandledModActionMapping().save(hma1);
		
		BanHistory ban1 = new BanHistory(-1, mod.id, banned1.id, hma1.id, scammer.tag, "permanent");
		database.getBanHistoryMapping().save(ban1);
		
		processor.processBan(tags, hma1, ban1);

		MysqlTestUtils.assertListContentsPreds(database.getUSLActionMapping().fetchAll(), 
				((a) -> (a.personID == banned1.id && a.isBan && a.isLatest && Math.abs(a.createdAt.getTime() - oldTime) < 1000)));
	}
	
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		processor = null;
	}
}
