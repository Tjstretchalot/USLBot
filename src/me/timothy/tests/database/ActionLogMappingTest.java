package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.ActionLog;

public class ActionLogMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}

	@Test
	public void testAppend() {
		assertLog();
		
		database.getActionLogMapping().append("test 1");
		assertLog("test 1");
		
		database.getActionLogMapping().append("test 2");
		assertLog("test 1", "test 2");
		
		database.getActionLogMapping().append("test 3");
		assertLog("test 1", "test 2", "test 3");
	}
	
	private void assertLog(String... expected) {
		List<ActionLog> fromDB = database.getActionLogMapping().fetchOrderedByTime();
		
		String debug = "Expected log to be " + String.join(", ", expected) + " but it was " 
				+ fromDB.stream().map(al -> al.action).collect(Collectors.joining(", "));
		if(fromDB.size() != expected.length) {
			assertFalse(debug, true);
		}
		
		for(int i = 0; i < fromDB.size(); i++) {
			assertEquals(debug, expected[i], fromDB.get(i).action);
		}
	}
}
