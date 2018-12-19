package me.timothy.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.USLValidUnbanRequestToMeaningProcessor;
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
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class ValidUnbanRequestToMeaningProcessorTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	
	private USLValidUnbanRequestToMeaningProcessor processor;
	
	private DBShortcuts db;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		
		MysqlTestUtils.clearDatabase(database);

		db = new DBShortcuts(database, config);
		processor = new USLValidUnbanRequestToMeaningProcessor(database, config);
	}
	
	@Test
	public void testMovesBansAndUnbansForward() {
		Hashtag scammer = db.scammerTag();
		
		Person mod = db.mod();
		Person mod2 = db.mod2();
		Person bot = db.bot();
		Person banned = db.user1();

		MonitoredSubreddit sub1 = db.sub();
		MonitoredSubreddit sub2 = db.sub2();
		MonitoredSubreddit sub3 = db.sub3();
		
		db.hma(sub1);
		db.hma(sub1);
		db.hma(sub2);
		db.hma(sub3);
		
		HandledModAction hma1 = db.hma(sub2, db.now(-20000));
		BanHistory ban1 = db.bh(mod, banned, hma1, "msg", true);
		
		HandledModAction hma2 = db.hma(sub1, db.now(-50000));
		BanHistory ban2 = db.bh(mod2, banned, hma2, "msg", true);
		
		HandledModAction hma3 = db.hma(sub3, db.now(-15000));
		db.bh(bot, banned, hma3, "msg", true);
		
		HandledModAction hma4 = db.hma(sub3, db.now(-5000));
		UnbanHistory unban = db.ubh(bot, banned, hma4);
		
		USLAction act = db.action(true, banned, new Hashtag[] { scammer }, new BanHistory[] { ban1, ban2 }, new UnbanHistory[] { unban });
		
		UnbanRequest ur = db.unbanRequest(banned);
		db.handle(ur, db.now(), false);
		
		processor.processUnbanRequest(ur);
		
		USLAction fromDb = database.getUSLActionMapping().fetchLatest(banned.id);
		assertNotEquals(act, fromDb);
		assertFalse(fromDb.isBan);
		
		List<USLActionHashtag> attachedTags = database.getUSLActionHashtagMapping().fetchByUSLActionID(fromDb.id);
		assertTrue(attachedTags.isEmpty());
		
		List<USLActionBanHistory> attachedBans = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(fromDb.id);
		MysqlTestUtils.assertListContentsPreds(attachedBans, 
				(a) -> a.actionID == fromDb.id && a.banHistoryID == ban1.id,
				(a) -> a.actionID == fromDb.id && a.banHistoryID == ban2.id);
		
		List<USLActionUnbanHistory> attachedUnbans = database.getUSLActionUnbanHistoryMapping().fetchByUSLActionID(fromDb.id);
		MysqlTestUtils.assertListContentsPreds(attachedUnbans,
				(a) -> a.actionID == fromDb.id && a.unbanHistoryID == unban.id);
	}
	
	@After 
	public void cleanUp() {
		database.disconnect();
		database = null;
		config = null;
		processor = null;
	}
}
