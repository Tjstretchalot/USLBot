package me.timothy.tests.database.mysql;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import me.timothy.bots.USLDatabase;
import me.timothy.tests.database.UnbanRequestMappingTest;

public class MysqlUnbanRequestMappingTest extends UnbanRequestMappingTest {
	@Before
	public void setUp() {
		Properties testDBProperties = MysqlTestUtils.fetchTestDatabaseProperties();
		USLDatabase testDb = MysqlTestUtils.getDatabase(testDBProperties);
		MysqlTestUtils.clearDatabase(testDb);
		
		super.database = testDb;
	}
	
	@After 
	public void cleanUp() {
		((USLDatabase) database).disconnect();
	}
}
