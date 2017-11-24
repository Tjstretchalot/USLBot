package me.timothy.tests.database;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.RegisterAccountRequest;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class RegisterAccountRequestMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testSave() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		long now = System.currentTimeMillis();
		RegisterAccountRequest req = new RegisterAccountRequest(-1, john.id, "alskdfj", new Timestamp(now), null);
		database.getRegisterAccountRequestMapping().save(req);
		
		assertTrue(req.id > 0);
		assertEquals(0, req.createdAt.getNanos());
		
		int oldId = req.id;
		List<RegisterAccountRequest> fromDB = database.getRegisterAccountRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, req);
		
		req.sentAt = new Timestamp(now);
		database.getRegisterAccountRequestMapping().save(req);
		assertEquals(oldId, req.id);
		fromDB = database.getRegisterAccountRequestMapping().fetchAll();
		MysqlTestUtils.assertListContents(fromDB, req);
	}
	
	@Test
	public void testFetchUnsent(){ 
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		long now = System.currentTimeMillis();
		RegisterAccountRequest req = new RegisterAccountRequest(-1, john.id, "asldkfj", new Timestamp(now), null);
		database.getRegisterAccountRequestMapping().save(req);
		
		List<RegisterAccountRequest> fromDB = database.getRegisterAccountRequestMapping().fetchUnsent(2);
		assertEquals(1, fromDB.size());
		assertEquals(req, fromDB.get(0));
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		
		RegisterAccountRequest req2 = new RegisterAccountRequest(-1, paul.id, "aljk;", new Timestamp(now + 5000), null);
		database.getRegisterAccountRequestMapping().save(req2);
		
		fromDB = database.getRegisterAccountRequestMapping().fetchUnsent(2);
		assertEquals(2, fromDB.size());
		assertEquals(req, fromDB.get(0));
		assertEquals(req2, fromDB.get(1));
		
		fromDB = database.getRegisterAccountRequestMapping().fetchUnsent(1);
		assertEquals(1, fromDB.size());
		assertEquals(req, fromDB.get(0));
		
		req.sentAt = new Timestamp(now);
		database.getRegisterAccountRequestMapping().save(req);

		fromDB = database.getRegisterAccountRequestMapping().fetchUnsent(2);
		assertEquals(1, fromDB.size());
		assertEquals(req2, fromDB.get(0));
	}
}
