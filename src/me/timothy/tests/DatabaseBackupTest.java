package me.timothy.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLDatabaseBackupManager;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.tests.database.mysql.MysqlTestUtils;

/**
 * Tests focusing on the USLDatabaseBackupManager. This requires ftpbackups and database
 * be provided under test config. 
 * 
 * The system must have "mysqldump" and "gzip" as valid commands in command line. This can
 * be tricky in windows. 
 * 
 * @author Timothy
 */
public class DatabaseBackupTest {
	private USLDatabase database;
	private USLFileConfiguration config;
	private USLDatabaseBackupManager backupManager;
	
	@Before
	public void setUp() throws NullPointerException, IOException {
		System.out.println("--------------------");
		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		backupManager = new USLDatabaseBackupManager(database, config);
		
		MysqlTestUtils.clearDatabase(database);
	}
	
	@After 
	public void cleanUp() {
		((USLDatabase) database).disconnect();
		database = null;
		config = null;
		backupManager = null;
	}
	
	/**
	 * Ensure that database and config are not null
	 */
	@Test
	public void testTest() {
		assertNotNull(database);
		assertNotNull(config);
		assertNotNull(backupManager);
	}
	
	@Test
	public void testBackupCausesNoErrors() {
		backupManager.considerBackup();
		
		// This is tricky to test without some very complicated spoofing. But running this test you should
		// see the backup is generated. Right now I doubt it's worth the rather immense effort to try to
		// automate more than this. Any errors with the backup should be reproducible with this test.
	}
}
