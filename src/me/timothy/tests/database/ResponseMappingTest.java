package me.timothy.tests.database;

import static org.junit.Assert.*;
import static me.timothy.tests.database.mysql.MysqlTestUtils.assertListContents;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Response;

/**
 * Describes a test focused on testing a ResponseMapping 
 * inside a MappingDatabase. The database must be cleared prior to
 * each test. The database will be modified. Do not run against a
 * production database.
 * 
 * @author Timothy
 */
public class ResponseMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testSave() {
		Response response = new Response();
		response.id = -1;
		response.name = "test";
		response.responseBody = "this is a test <user>";
		response.createdAt = new Timestamp(System.currentTimeMillis());
		response.updatedAt = new Timestamp(System.currentTimeMillis());
		database.getResponseMapping().save(response);
		assertTrue(response.id > 0);
		
		List<Response> fromDb = database.getResponseMapping().fetchAll();
		assertListContents(fromDb, response);
	}

	@Test
	public void testFetchByName() {
		Response test = new Response();
		test.id = -1;
		test.name = "test";
		test.responseBody = "this is a test response";
		test.createdAt = new Timestamp(System.currentTimeMillis());
		test.updatedAt = new Timestamp(System.currentTimeMillis());
		database.getResponseMapping().save(test);
		
		Response fromDb = database.getResponseMapping().fetchByName("test");
		assertEquals(test, fromDb);
		
		fromDb = database.getResponseMapping().fetchByName("not_a_response");
		assertNull(fromDb);
		
		Response hello = new Response();
		hello.id = -1;
		hello.name = "hello";
		hello.responseBody = "Hello! I'm a bot, are you <name>?";
		hello.createdAt = new Timestamp(System.currentTimeMillis());
		hello.updatedAt = new Timestamp(System.currentTimeMillis());
		database.getResponseMapping().save(hello);
		
		fromDb = database.getResponseMapping().fetchByName("test");
		assertEquals(test, fromDb);
		
		fromDb = database.getResponseMapping().fetchByName("hello");
		assertEquals(hello, fromDb);
		
		fromDb = database.getResponseMapping().fetchByName("shenanigans");
		assertNull(fromDb);
	}
}
