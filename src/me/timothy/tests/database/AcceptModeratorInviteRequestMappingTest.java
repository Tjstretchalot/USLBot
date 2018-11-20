package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;

import me.timothy.bots.database.AcceptModeratorInviteRequestMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.AcceptModeratorInviteRequest;
import me.timothy.bots.models.Person;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class AcceptModeratorInviteRequestMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testSaveUpdateFetch() {
		final long now = System.currentTimeMillis();
		
		AcceptModeratorInviteRequestMapping map = database.getAcceptModeratorInviteRequestMapping();
		
		MysqlTestUtils.assertListContents(map.fetchAll());
		
		Person mod = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		AcceptModeratorInviteRequest req = new AcceptModeratorInviteRequest(-1, mod.id, "test", new Timestamp(now), null, false);
		map.save(req);
		
		assertNotEquals(-1, req.id);
		MysqlTestUtils.assertListContents(map.fetchAll(), req);
		assertEquals(0, req.createdAt.getNanos());
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(1), req);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(10), req);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(0));
		
		AcceptModeratorInviteRequest req2 = new AcceptModeratorInviteRequest(-1, mod.id, "test2", new Timestamp(now + 5000), null, false);
		map.save(req2);
		
		assertNotEquals(-1, req2.id);
		assertNotEquals(req.id, req2.id);
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(1), req);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(10), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(0));
		
		req.fulfilledAt = new Timestamp(now + 10000);
		req.success = true;
		map.save(req);
		assertEquals(true, req.success);
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(1), req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(10), req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(0));
		
		req2.fulfilledAt = new Timestamp(now + 15000);
		map.save(req2);
		assertEquals(false, req2.success);
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(1));
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(10));
		MysqlTestUtils.assertListContents(map.fetchUnfulfilled(0));
	}
}
