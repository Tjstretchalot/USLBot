package me.timothy.tests.database;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.SubredditPersonBannedReleaseMapping;
import me.timothy.bots.models.SubredditPersonBannedRelease;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class SubredditPersonBannedReleaseMappingTest {
	/**
	 * The {@link me.timothy.bots.database.MappingDatabase MappingDatabase} that contains
	 * the {@link me.timothy.bots.database.FullnameMapping FullnameMapping} to test.
	 */
	protected MappingDatabase database;
	
	/**
	 * Verifies the test is setup correctly by ensuring the {@link #database} is not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	/**
	 * Verifies that we can save new entries and fetch all existing entries
	 */
	@Test
	public void testSaveFetchAll() {
		DBShortcuts db = new DBShortcuts(database);
		SubredditPersonBannedReleaseMapping map = database.getSubredditPersonBannedReleaseMapping();
		
		MysqlTestUtils.assertListContents(map.fetchAll());
		SubredditPersonBannedRelease release1 = new SubredditPersonBannedRelease(db.sub().id, db.user1().id);
		map.save(release1);
		MysqlTestUtils.assertListContents(map.fetchAll(), release1);
		
		SubredditPersonBannedRelease release2 = new SubredditPersonBannedRelease(db.sub2().id, db.user1().id);
		map.save(release2);
		MysqlTestUtils.assertListContents(map.fetchAll(), release1, release2);
	}
}
