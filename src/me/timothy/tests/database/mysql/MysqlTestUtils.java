package me.timothy.tests.database.mysql;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;

import me.timothy.bots.USLDatabase;

/**
 * Contains utility functions that are used in many MySQL database
 * tests, such as creating/clearing the test database
 * 
 * @author Timothy
 */
public class MysqlTestUtils {
	/**
	 * Fetches the test_database.properties file 
	 * 
	 * @return the test database property file
	 */
	public static Properties fetchTestDatabaseProperties() {
		Properties props = new Properties();
		
		Path path = Paths.get("tests/database.properties");
		if(!Files.exists(path)) {
			throw new IllegalStateException("tests/database.properties could not be found");
		}
		
		try(FileReader fr = new FileReader(path.toFile())) {
			props.load(fr);
		}catch(IOException ex) {
			throw new RuntimeException(ex);
		}
		
		return props;
	}
	
	/**
	 * Creates the LoansBot database from the database properties. Must
	 * include a "username", "password", and "url" key. "url" MUST contain
	 * "test".
	 * 
	 * @param properties the database properties
	 * @return the database connection
	 */
	public static USLDatabase getDatabase(Properties properties) {
		String username = properties.getProperty("username");
		String password = properties.getProperty("password");
		String url = properties.getProperty("url");
		
		if(username == null || password == null || url == null) {
			throw new IllegalArgumentException("username and password and url cannot be null");
		}
		
		if(!url.contains("test")) {
			throw new IllegalArgumentException("url does not contain \"test\"");
		}
		
		USLDatabase db = new USLDatabase();
		try {
			db.connect(username, password, url);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return db;
	}
	
	/**
	 * Ensures that the database is empty. 
	 * 
	 * @param database the database to clear
	 */
	public static void clearDatabase(USLDatabase database) {
		database.purgeAll();
		database.validateTableState();
		database.validateTableState(); // doing this twice is like a test of its own
	}
	
	/**
	 * Asserts that the {@code list} contains the {@code obj}, as 
	 * if by {@code assertTrue(list.contains(obj)}, except with a
	 * prettier error message.
	 * 
	 * @param list the list
	 * @param obj the object
	 * @param <A> the type of list / object
	 */
	public static <A> void assertContains(List<A> list, A obj) {
		Assert.assertTrue("expected " + list + " to contain " + obj, list.contains(obj));
	}
	
	/**
	 * Convenience function to verify that a list matches the 
	 * set of objects. Does not verify order.
	 * 
	 * @param list the list
	 * @param objs the objects
	 * @param <A> the type of list / object
	 */
	@SafeVarargs
	public static <A> void assertListContents(List<A> list, A... objs) {
		/*
		 * This function has SafeVarargs since it never inserts anything
		 * into the list, nor is any casting attempted.
		 */
		for(int i = 0; i < objs.length; i++) {
			assertContains(list, objs[i]);
		}
		
		Assert.assertEquals(objs.length, list.size());
	}
	
	/**
	 * Ensures that the specified string array contains the specified string
	 * using {@link Object#equals(Object)}.
	 * @param arr the array
	 * @param str the string
	 */
	public static void assertStringArrContains(String[] arr, String str) {
		for(int i = 0; i < arr.length; i++) {
			if(arr[i].equals(str))
				return;
		}
		Assert.fail("expected " + Arrays.toString(arr) + " to contain " + str);
	}
	

	/**
	 * Ensures that the specified string array contains the same number and same contents
	 * using {@link Object#equals(Object)}, but does not verify order
	 * @param str the string
	 * @param arr the array
	 */
	public static void assertStringArrContents(String[] arr, String... strs) {
		for(int i = 0; i < strs.length; i++) {
			assertStringArrContains(arr, strs[i]);
		}
		Assert.assertEquals("expected same length contents. got " + Arrays.toString(arr) + " expected " + Arrays.toString(strs), strs.length, arr.length);
	}
}
