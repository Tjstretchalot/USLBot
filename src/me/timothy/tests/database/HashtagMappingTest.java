package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.Person;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class HashtagMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		long now = System.currentTimeMillis();
		Hashtag tag = new Hashtag(-1, "#scammer", "The most famous tag", john.id, john.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(tag);
		
		MysqlTestUtils.assertListContents(database.getHashtagMapping().fetchAll(), tag);
		
		assertEquals(tag, database.getHashtagMapping().fetchByID(tag.id));
		
		Hashtag tag2 = new Hashtag(-1, "#sketchy", "The less strict #scammer", john.id, john.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(tag2);
		

		MysqlTestUtils.assertListContents(database.getHashtagMapping().fetchAll(), tag, tag2);
		
		assertEquals(tag, database.getHashtagMapping().fetchByID(tag.id));
		assertEquals(tag2, database.getHashtagMapping().fetchByID(tag2.id));
	}
	
}
