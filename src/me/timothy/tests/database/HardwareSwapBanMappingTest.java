package me.timothy.tests.database;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import me.timothy.bots.database.HardwareSwapBanMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.HardwareSwapAction;
import me.timothy.bots.models.HardwareSwapBan;
import me.timothy.tests.DBShortcuts;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class HardwareSwapBanMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	private static Set<Integer> newSet(int... ids) {
		return Arrays.stream(ids).boxed().collect(Collectors.toSet());
	}
	
	@Test
	public void testNotIn() {
		DBShortcuts db = new DBShortcuts(database);
		HardwareSwapBanMapping map = database.getHardwareSwapBanMapping();
		
		MysqlTestUtils.assertListContents(map.fetchAll());
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.emptySet()));
		
		HardwareSwapBan ban = new HardwareSwapBan(-1, db.user1().id, "reason", db.now());
		map.save(ban);
		
		assertTrue(ban.id > 0);
		MysqlTestUtils.assertListContents(map.fetchAll(), ban);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.emptySet()), ban);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.singleton(ban.personID)));
		
		HardwareSwapBan ban2 = new HardwareSwapBan(-1, db.person("user2").id, "reason2", db.now(1000));
		map.save(ban2);
		
		assertNotEquals(ban.id, ban2.id);
		assertTrue(ban2.id > 0);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), ban, ban2);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.emptySet()), ban, ban2);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.singleton(ban.personID)), ban2);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.singleton(ban2.personID)), ban);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban.personID, ban2.personID)));
		
		HardwareSwapBan ban3 = new HardwareSwapBan(-1, db.person("user3").id, "reason3", db.now(2000));
		map.save(ban3);
		
		assertNotEquals(ban.id, ban3.id);
		assertNotEquals(ban2.id, ban3.id);
		
		MysqlTestUtils.assertListContents(map.fetchAll(), ban, ban2, ban3);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(Collections.emptySet()), ban, ban2, ban3);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban.personID)), ban2, ban3);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban2.personID)), ban, ban3);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban3.personID)), ban, ban2);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban.personID, ban2.personID)), ban3);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban.personID, ban3.personID)), ban2);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban2.personID, ban3.personID)), ban);
		MysqlTestUtils.assertListContents(map.fetchWherePersonNotIn(newSet(ban.personID, ban2.personID, ban3.personID)));
		
		MysqlTestUtils.assertListContents(map.fetchWithoutAction(5), ban, ban2, ban3);
		
		database.getHardwareSwapActionMapping().save(new HardwareSwapAction(-1, ban2.personID, HardwareSwapAction.BAN_ACTION, db.now()));
		
		MysqlTestUtils.assertListContents(map.fetchWithoutAction(5), ban, ban3);
		
	}
}
