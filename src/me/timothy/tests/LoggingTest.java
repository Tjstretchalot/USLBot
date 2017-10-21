package me.timothy.tests;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * This just logs stuff every second indefinitely. Used to test the log4j2.xml file
 * 
 * @author Timothy
 */
public class LoggingTest {
	@Test
	public void testPrintStuff() throws InterruptedException {
		Level[] levels = new Level[] {
				Level.TRACE,
				Level.DEBUG,
				Level.INFO,
				Level.WARN,
				Level.ERROR
		};
		
		int currIndex = 0;
		Logger logger = LogManager.getLogger();
		while(true) {
			logger.printf(levels[currIndex], "Printing some stuff at %d", System.currentTimeMillis());
			currIndex = (currIndex + 1) % levels.length;
			Thread.sleep(1000);
		}
	}
}
