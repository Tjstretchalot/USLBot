package me.timothy.tests.database;

import static org.junit.Assert.*;
import static me.timothy.tests.database.mysql.MysqlTestUtils.assertListContents;

import java.util.List;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Fullname;

/**
 * Describes tests focused on the FullnameMapping in 
 * a mapping database. The database <i>must</i> be completely
 * empty after setup every time. The database <i>will</i> be modified
 * - it should point at a test database.
 * 
 * @author Timothy
 */
public class FullnameMappingTest {
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
	 * Tests that {@link me.timothy.bots.database.ObjectMapping#save(Object) saving}
	 * fullnames will set their {@link Fullname#id id} to a strictly positive
	 * number, and that the fullname can be fetched again with
	 * {@link me.timothy.bots.database.ObjectMapping#fetchAll() fetchAll()}
	 */
	@Test
	public void testSave() {
		final String fullnameStr = "asdfgh";
		Fullname fullname = new Fullname(-1, fullnameStr);
		
		database.getFullnameMapping().save(fullname);
		
		assertTrue(fullname.id > 0);
		assertTrue(fullname.fullname.equals(fullnameStr));
		
		List<Fullname> fromDb = database.getFullnameMapping().fetchAll();
		assertListContents(fromDb, fullname);
	}
	
	/**
	 * Tests that
	 * {@link me.timothy.bots.database.FullnameMapping#contains(String)
	 * contains} will return true for Strings that are
	 * {@link Object#equals(Object) equal}, and false for {@link Fullname
	 * fullnames} that are not in the {@link #database database}, regardless of
	 * if they are alike in the MySQL sense.
	 */
	@Test
	public void testContains() {
		final String fullnameStr = "asdfgh";
		
		assertFalse(database.getFullnameMapping().contains(fullnameStr));
		
		database.getFullnameMapping().save(new Fullname(-1, fullnameStr));
		
		assertTrue(database.getFullnameMapping().contains(fullnameStr));
		assertFalse(database.getFullnameMapping().contains("asdf%"));
	}
}
