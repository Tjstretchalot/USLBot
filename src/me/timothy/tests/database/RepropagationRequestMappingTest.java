package me.timothy.tests.database;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.RepropagationRequestMapping;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.RepropagationRequest;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class RepropagationRequestMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		DBShortcuts db = new DBShortcuts(database);
		RepropagationRequestMapping map = database.getRepropagationRequestMapping();
		
		Person mod = db.mod();
		
		MysqlTestUtils.assertListContents(map.fetchAll());
		
		RepropagationRequest req = new RepropagationRequest(-1, mod.id, "for testing", false, db.now(), null);
		map.save(req);
		assertTrue(req.id > 0);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), req);
		MysqlTestUtils.assertListContents(map.fetchUnhandled(), req);
		
		RepropagationRequest req2 = new RepropagationRequest(-1, mod.id, "again for testing", false, db.now(), null);
		map.save(req2);
		assertTrue(req2.id > 0);
		assertNotEquals(req.id, req2.id);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnhandled(), req, req2);
		
		req2.approved = true;
		req2.handledAt = db.now(1000);
		map.save(req2);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnhandled(), req);
		
		req.approved = false;
		req.handledAt = db.now(3000);
		map.save(req);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), req, req2);
		MysqlTestUtils.assertListContents(map.fetchUnhandled());
	}
}
