package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.USLActionHashtagMapping;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class USLActionHashtagMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		final long now = System.currentTimeMillis();
		
		Person paul = database.getPersonMapping().fetchOrCreateByUsername("paul");
		Person john = database.getPersonMapping().fetchOrCreateByUsername("john");
		
		USLAction action = database.getUSLActionMapping().create(true, john.id, new Timestamp(now));
		database.getUSLActionMapping().save(action);
		
		Hashtag tag = new Hashtag(-1, "#scammer", "the famous tag", paul.id, paul.id, new Timestamp(now), new Timestamp(now));
		database.getHashtagMapping().save(tag);
		
		USLActionHashtagMapping map = database.getUSLActionHashtagMapping();
		MysqlTestUtils.assertListContents(map.fetchAll());
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(action.id));
		
		USLActionHashtag actionTag = new USLActionHashtag(action.id, tag.id);
		map.save(actionTag);
		MysqlTestUtils.assertListContents(map.fetchAll(), actionTag);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(action.id), actionTag);
		MysqlTestUtils.assertListContents(map.fetchByUSLActionID(action.id + 1));
	}
}
