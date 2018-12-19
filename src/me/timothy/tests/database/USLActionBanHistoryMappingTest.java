package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.USLActionBanHistoryMapping;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class USLActionBanHistoryMappingTest {

	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testAll() {
		final long now = System.currentTimeMillis();
		Person mod = database.getPersonMapping().fetchOrCreateByUsername("eric");
		Person bot = database.getPersonMapping().fetchOrCreateByUsername("USLBot");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		MonitoredSubreddit usl = new MonitoredSubreddit(-1, "universalscammerlist", true, false, false);
		database.getMonitoredSubredditMapping().save(usl);
		
		MonitoredSubreddit ericSub = new MonitoredSubreddit(-1, "ericssub", true, false, false);
		database.getMonitoredSubredditMapping().save(ericSub);
		
		HandledModAction hmaModBansJohn = new HandledModAction(-1, ericSub.id, "ModAction_ID1", new Timestamp(now));
		database.getHandledModActionMapping().save(hmaModBansJohn);
		
		BanHistory hisModBansJohn = new BanHistory(-1, mod.id, john.id, hmaModBansJohn.id, "i hate you", "permanent");
		database.getBanHistoryMapping().save(hisModBansJohn);
		
		HandledModAction hmaBotBansJohn = new HandledModAction(-1, usl.id, "ModAction_ID2", new Timestamp(now));
		database.getHandledModActionMapping().save(hmaBotBansJohn);
		
		BanHistory hisBotBansJohn = new BanHistory(-1, bot.id, john.id, hmaBotBansJohn.id, "i hate you from ericssub", "permanent");
		database.getBanHistoryMapping().save(hisBotBansJohn);
		
		HandledModAction hmaModBansPaul = new HandledModAction(-1, ericSub.id, "ModAction_ID3", new Timestamp(now));
		database.getHandledModActionMapping().save(hmaModBansPaul);
		
		BanHistory hisModBansPaul = new BanHistory(-1, mod.id, paul.id, hmaModBansPaul.id, "friend of john", "permanent");
		database.getBanHistoryMapping().save(hisModBansPaul);
		
		HandledModAction hmaBotBansPaul = new HandledModAction(-1, usl.id, "ModAction_ID4", new Timestamp(now));
		database.getHandledModActionMapping().save(hmaBotBansPaul);
		
		BanHistory hisBotBansPaul = new BanHistory(-1, bot.id, paul.id, hmaBotBansPaul.id, "friend of john from ericssub", "permanent");
		database.getBanHistoryMapping().save(hisBotBansPaul);
		
		
		
		USLAction johnBanAction = database.getUSLActionMapping().create(true, john.id, new Timestamp(now));
		USLAction paulBanAction = database.getUSLActionMapping().create(true, paul.id, new Timestamp(now));
		
		USLActionBanHistoryMapping map = database.getUSLActionBanHistoryMapping();
		
		USLActionBanHistory actBanModBansJohn = new USLActionBanHistory(johnBanAction.id, hisModBansJohn.id);
		map.save(actBanModBansJohn);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(johnBanAction.id), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansJohn.id), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansJohn.id));
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(paulBanAction.id));
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansPaul.id));
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansPaul.id));
		
		USLActionBanHistory actBanBotBansJohn = new USLActionBanHistory(johnBanAction.id, hisBotBansJohn.id);
		map.save(actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(johnBanAction.id), actBanModBansJohn, actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansJohn.id), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansJohn.id), actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(paulBanAction.id));
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansPaul.id));
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansPaul.id));

		USLActionBanHistory actBanModBansPaul = new USLActionBanHistory(paulBanAction.id, hisModBansPaul.id);
		map.save(actBanModBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(johnBanAction.id), actBanModBansJohn, actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansJohn.id), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansJohn.id), actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(paulBanAction.id), actBanModBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansPaul.id), actBanModBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansPaul.id));
		
		USLActionBanHistory actBanBotBansPaul = new USLActionBanHistory(paulBanAction.id, hisBotBansPaul.id);
		map.save(actBanBotBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(johnBanAction.id), actBanModBansJohn, actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansJohn.id), actBanModBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansJohn.id), actBanBotBansJohn);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(paulBanAction.id), actBanModBansPaul, actBanBotBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisModBansPaul.id), actBanModBansPaul);
		MysqlTestUtils.assertListContents(map.fetchByBanHistoryID(hisBotBansPaul.id), actBanBotBansPaul);
	}
}
