package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.ResetPasswordRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class ResetPasswordRequestMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		long now = System.currentTimeMillis();
		ResetPasswordRequest req = new ResetPasswordRequest(-1, john.id, "alskdfj", true, new Timestamp(now), null);
		database.getResetPasswordRequestMapping().save(req);
		
		assertTrue(req.id > 0);
		assertEquals(0, req.createdAt.getNanos());
		
		int oldId = req.id;
		List<ResetPasswordRequest> fromDB = database.getResetPasswordRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, req);
		
		req.sentAt = new Timestamp(now);
		database.getResetPasswordRequestMapping().save(req);
		assertEquals(oldId, req.id);
		fromDB = database.getResetPasswordRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, req);
	}
	
	@Test
	public void testFetchUnsent(){ 
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		long now = System.currentTimeMillis();
		ResetPasswordRequest req = new ResetPasswordRequest(-1, john.id, "asldkfj", false, new Timestamp(now), null);
		database.getResetPasswordRequestMapping().save(req);
		
		List<ResetPasswordRequest> fromDB = database.getResetPasswordRequestMapping().fetchUnsent(2);
		assertEquals(1, fromDB.size());
		assertEquals(req, fromDB.get(0));
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		ResetPasswordRequest req2 = new ResetPasswordRequest(-1, paul.id, "aljk;", false, new Timestamp(now + 5000), null);
		database.getResetPasswordRequestMapping().save(req2);
		
		fromDB = database.getResetPasswordRequestMapping().fetchUnsent(2);
		assertEquals(2, fromDB.size());
		assertEquals(req, fromDB.get(0));
		assertEquals(req2, fromDB.get(1));
		
		fromDB = database.getResetPasswordRequestMapping().fetchUnsent(1);
		assertEquals(1, fromDB.size());
		assertEquals(req, fromDB.get(0));
		
		req.sentAt = new Timestamp(now);
		database.getResetPasswordRequestMapping().save(req);

		fromDB = database.getResetPasswordRequestMapping().fetchUnsent(2);
		assertEquals(1, fromDB.size());
		assertEquals(req2, fromDB.get(0));
	}
}
