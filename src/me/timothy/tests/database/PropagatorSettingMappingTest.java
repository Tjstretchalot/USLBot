package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.PropagatorSettingMapping;
import me.timothy.bots.models.PropagatorSetting.PropagatorSettingKey;

public class PropagatorSettingMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testAll() {
		PropagatorSettingMapping map = database.getPropagatorSettingMapping();
		
		assertNull(map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertNull(map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		
		map.put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "true");
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));

		map.put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "true");
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));

		map.put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "false");
		assertEquals("false", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertEquals("false", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));

		map.put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "true");
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertEquals("true", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));

		map.put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "false");
		assertEquals("false", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
		assertEquals("false", map.get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES));
	}
}
