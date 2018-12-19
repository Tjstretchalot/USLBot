package me.timothy.tests.database.mysql;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
		String flatFolder = properties.getProperty("flat_folder");
		
		if(username == null || password == null || url == null) {
			throw new IllegalArgumentException("username and password and url cannot be null");
		}
		
		if(!url.contains("test")) {
			throw new IllegalArgumentException("url does not contain \"test\"");
		}
		
		if(!flatFolder.contains("test")) {
			throw new IllegalArgumentException("flat_folder does not contain \"test\"");
		}
		
		USLDatabase db = new USLDatabase();
		try {
			db.connect(username, password, url, new File(flatFolder));
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
		database.truncateMySQL();
		database.purgeCustom();
		/*
		database.purgeAll();
		database.validateTableState();
		database.validateTableState(); // doing this twice is like a test of its own
		*/
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
		assertContains(null, list, obj);
	}
	
	/**
	 * Asserts that the {@code list} contains the {@code obj}, as 
	 * if by {@code assertTrue(list.contains(obj)}, except with a
	 * prettier error message.
	 * 
	 * @param msg custom error message or null
	 * @param list the list
	 * @param obj the object
	 * @param <A> the type of list / object
	 */
	public static <A> void assertContains(String msg, List<A> list, A obj) {
		if(msg == null)
			msg = "expected " + list + " to contain " + obj;
		Assert.assertTrue(msg, list.contains(obj));
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
		assertListContents(null, list, objs);
	}

	/**
	 * Convenience function to verify that a list matches the 
	 * set of objects. Does not verify order.
	 * 
	 * @param msg the message on error
	 * @param list the list
	 * @param objs the objects
	 * @param <A> the type of list / object
	 */
	@SafeVarargs
	public static <A> void assertListContents(String msg, List<A> list, A... objs) {
		/*
		 * This function has SafeVarargs since it never inserts anything
		 * into the list, nor is any casting attempted.
		 */
		for(int i = 0; i < objs.length; i++) {
			assertContains(msg, list, objs[i]);
		}
		
		Assert.assertEquals(msg, objs.length, list.size());
	}
	
	/**
	 * Ensure that every predicate is met by at least one item in the list, not repeating
	 * items (one item cannot satisfy multiple predicates), and that there are no extra
	 * items.
	 * 
	 * @param list the list that you want to verify
	 * @param preds the things you want to verify are in the list
	 */
	@SafeVarargs
	public static <A> void assertListContentsPreds(List<A> list, Predicate<A>... preds) {
		assertListContentsPreds("", list, preds);
	}
	
	/**
	 * Ensure that every predicate is met by at least one item in the list, not repeating
	 * items (one item cannot satisfy multiple predicates), and that there are no extra
	 * items.
	 * 
	 * @param msg the message to print upon failure
	 * @param list the list
	 * @param preds the predicates to match
	 */
	@SafeVarargs
	public static <A> void assertListContentsPreds(String msg, List<A> list, Predicate<A>... preds) {
		List<Integer> remainingInds = new ArrayList<>(preds.length);
		for(int i = 0; i < preds.length; i++) {
			remainingInds.add(i);
		}
		
		for(int i = 0; i < list.size(); i++) {
			A a = list.get(i);
			boolean found = false;
			for(int j = 0; j < remainingInds.size(); j++) {
				if(preds[remainingInds.get(j)].test(a)) {
					remainingInds.remove(j);
					found = true;
					break;
				}
			}
			if(!found) {
				throw new AssertionError(list.stream().map((e) -> e.toString()).collect(Collectors.joining(", ")) + "; no matching pred for index=" + i);
			}
		}
		
		if(remainingInds.size() != 0) {
			throw new AssertionError(list.stream().map((e) -> e.toString()).collect(Collectors.joining(", ")) + "; no item matches predicates " + remainingInds.toString());
		}
	}
	
	/**
	 * Assert list contents without a message and return the list ordered in the same order as the predicates.
	 * 
	 * @param list the list
	 * @param preds the predicates
	 * @return a new list with the same items as list but ordered to match preds
	 */
	@SafeVarargs
	public static <A> List<A> orderToMatchPreds(List<A> list, Predicate<A>... preds) {
		List<Integer> remainingInds = new ArrayList<>(preds.length);
		for(int i = 0; i < preds.length; i++) {
			remainingInds.add(i);
		}
		
		List<A> newList = new ArrayList<>();
		for(int i = 0; i < list.size(); i++) {
			A a = list.get(i);
			boolean found = false;
			for(int j = 0; j < remainingInds.size(); j++) {
				if(preds[remainingInds.get(j)].test(a)) {
					remainingInds.remove(j);
					newList.add(a);
					found = true;
					break;
				}
			}
			
			if(!found) {
				throw new AssertionError(list.stream().map((e) -> e.toString()).collect(Collectors.joining(", ")) + "; no matching pred for index=" + i);
			}
		}
		
		assertEquals(" no item matches predicates " + remainingInds.toString(), 0, remainingInds.size());
		return newList;
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
