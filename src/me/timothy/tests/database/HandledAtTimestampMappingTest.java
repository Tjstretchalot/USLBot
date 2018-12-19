package me.timothy.tests.database;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.custom.CustomHandledAtTimestampMapping;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.tests.DBShortcuts;

public class HandledAtTimestampMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testClearRecover() {
		DBShortcuts db = new DBShortcuts(database);
		CustomHandledAtTimestampMapping map = (CustomHandledAtTimestampMapping) database.getHandledAtTimestampMapping();
		
		map.clear(db.now());
		
		int bh1a = db.bh(db.mod(), db.user1(), db.hma(db.sub(), db.now(-10000)), "msg", true).id;
		int bh1b = db.bh(db.mod(), db.user1(), db.hma(db.sub(), db.now(-10000)), "msg", true).id;
		int bh2a = db.bh(db.mod(), db.user1(), db.hma(db.sub()), "msg", true).id;
		
		int ubh1 = db.ubh(db.mod(), db.user1(), db.hma(db.sub(), db.now(-10000))).id;
		int ubh2 = db.ubh(db.mod(), db.user1(), db.hma(db.sub())).id;
		
		int ubr1 = db.unbanRequest(db.mod(), db.user1(), db.now(-20000), db.now(-10000), false).id;
		db.unbanRequest(db.mod(), db.user1(), db.now(), db.now(-10000), false);
		int ubr2 = db.unbanRequest(db.mod(), db.user1(), db.now(), db.now(), false).id;
		
		map.purgeSchema();
		map.verifyContents(new int[0], new int[0], new int[0]);
		
		map.clear(db.now(-10000));
		
		map.addBanHistory(bh1a);
		map.verifyContents(new int[] { bh1a }, new int[] {}, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh1a }, new int[] {}, new int[] {});
		
		map.addUnbanHistory(ubh1);
		map.verifyContents(new int[] { bh1a }, new int[] { ubh1 }, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh1a }, new int[] { ubh1 }, new int[] {});
		
		map.addBanHistory(bh1b);
		map.verifyContents(new int[] { bh1a, bh1b }, new int[] { ubh1 }, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh1a, bh1b }, new int[] { ubh1 }, new int[] {});

		map.addUnbanRequest(ubr1);
		map.verifyContents(new int[] { bh1a, bh1b }, new int[] { ubh1 }, new int[] { ubr1 });
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh1a, bh1b }, new int[] { ubh1 }, new int[] { ubr1 });
		
		map.clear(db.now());
		map.verifyContents(new int[] {}, new int[] {}, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] {}, new int[] {}, new int[] {});
		
		map.addBanHistory(bh2a);
		map.verifyContents(new int[] { bh2a }, new int[] {}, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh2a }, new int[] {}, new int[] {});
		
		map.addUnbanRequest(ubr2);
		map.verifyContents(new int[] { bh2a }, new int[] {}, new int[] { ubr2 });
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh2a }, new int[] {}, new int[] { ubr2 });
		
		map.addUnbanHistory(ubh2);
		map.verifyContents(new int[] { bh2a }, new int[] { ubh2 }, new int[] { ubr2 });
		map.close();
		map.recover();
		map.verifyContents(new int[] { bh2a }, new int[] { ubh2 }, new int[] { ubr2 });
		
		map.clear(db.now(10000));
		map.verifyContents(new int[] { }, new int[] {}, new int[] {});
		map.close();
		map.recover();
		map.verifyContents(new int[] { }, new int[] {}, new int[] {});
	}
	
	@Test
	public void testAll() {
		DBShortcuts db = new DBShortcuts(database);
		HandledAtTimestampMapping map = database.getHandledAtTimestampMapping();
		
		map.clear(db.now());
		
		db.hma(db.sub());
		HandledModAction hma2 = db.hma(db.sub());
		
		BanHistory bh1 = db.bh(db.mod(), db.user1(), hma2, "msg", true);
		assertFalse(map.containsBanHistory(bh1.id));
		map.addBanHistory(bh1.id);
		assertTrue(map.containsBanHistory(bh1.id));
		
		HandledModAction hma3 = db.hma(db.sub());
		UnbanHistory ubh1 = db.ubh(db.mod(), db.user1(), hma3);
		assertFalse(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		map.addUnbanHistory(ubh1.id);
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		HandledModAction hma4 = db.hma(db.sub());
		UnbanHistory ubh2 = db.ubh(db.mod(), db.person("user2"), hma4);
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		HandledModAction hma5 = db.hma(db.sub());
		UnbanHistory ubh3 = db.ubh(db.mod(), db.person("user3"), hma5);
		assertFalse(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		map.addUnbanHistory(ubh3.id);
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		UnbanRequest ubReq = db.unbanRequest(db.mod(), db.user1(), db.now(), db.now(), false);
		assertFalse(map.containsUnbanRequest(ubReq.id));
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		map.addUnbanRequest(ubReq.id);
		assertTrue(map.containsUnbanRequest(ubReq.id));
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		((CustomHandledAtTimestampMapping) map).close();
		((CustomHandledAtTimestampMapping) map).recover();

		assertTrue(map.containsUnbanRequest(ubReq.id));
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		HandledModAction hma6 = db.hma(db.sub());
		BanHistory bh2 = db.bh(db.mod(), db.user1(), hma6, "msg", true);
		assertFalse(map.containsBanHistory(bh2.id));
		assertTrue(map.containsUnbanRequest(ubReq.id));
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		map.addBanHistory(bh2.id);
		assertTrue(map.containsBanHistory(bh2.id));
		assertTrue(map.containsUnbanRequest(ubReq.id));
		assertTrue(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertTrue(map.containsUnbanHistory(ubh1.id));
		assertTrue(map.containsBanHistory(bh1.id));
		
		map.clear(db.now(5000));

		assertFalse(map.containsBanHistory(bh2.id));
		assertFalse(map.containsUnbanRequest(ubReq.id));
		assertFalse(map.containsUnbanHistory(ubh3.id));
		assertFalse(map.containsUnbanHistory(ubh2.id));
		assertFalse(map.containsUnbanHistory(ubh1.id));
		assertFalse(map.containsBanHistory(bh1.id));
	}
}
