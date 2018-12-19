package me.timothy.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.Random;

import org.junit.Test;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.RedditToMeaningProgressMapping;
import me.timothy.bots.database.custom.CustomRedditToMeaningProgressMapping;

public class RedditToMeaningProgressMappingTest {
	protected MappingDatabase database;
	
	@Test
	public void testTest() {
		assertNotNull(database);
	}
	
	@Test
	public void testInMemory() {
		RedditToMeaningProgressMapping map = database.getRedditToMeaningProgressMapping();
		
		assertNull(map.fetch());
		assertNull(map.fetch());
		
		Timestamp stamp = new Timestamp(100000);
		map.set(stamp);
		
		assertEquals(stamp, map.fetch());
		assertEquals(stamp, map.fetch());
		
		map.set(stamp);
		
		assertEquals(stamp, map.fetch());
		
		map.set(null);
		assertNull(map.fetch());
		
		stamp = new Timestamp(900000);
		map.set(stamp);
		assertEquals(stamp, map.fetch());
		
		stamp = new Timestamp(System.currentTimeMillis() - 10000);
		map.set(stamp);
		assertEquals(stamp, map.fetch());
	}
	
	@Test
	public void testRandomCrashing() {
		CustomRedditToMeaningProgressMapping map = (CustomRedditToMeaningProgressMapping) database.getRedditToMeaningProgressMapping();
		
		Random rand = new Random();
		Timestamp current = new Timestamp(0);
		
		for(int i = 0; i < 10; i++) {
			current = new Timestamp(rand.nextLong());
			map.set(current);
			if(!current.equals(map.fetch()))
				assertEquals(current, map.fetch());
			map.close();
			map.recover();
			if(!current.equals(map.fetch()))
				assertEquals(current, map.fetch());
		}
		
		int iters = rand.nextInt(4000) + 2000;
		for(int loop = 0; loop < 3; loop++) {
			for(int i = 0; i < iters; i++) {
				current.setTime(rand.nextLong());
				map.set(current);
				if(!current.equals(map.fetch()))
					assertEquals(current, map.fetch());
			}
			map.close();
			map.recover();
			assertEquals(current, map.fetch());
		}
		
		long start = System.currentTimeMillis();
		for(int i = 0; i < iters; i++) {
			current.setTime(rand.nextLong());
			map.set(current);
			if(!current.equals(map.fetch()))
				assertEquals(current, map.fetch());
		}
		long time = System.currentTimeMillis() - start;
		double millisPer = (time / (double)iters);
		System.out.println("Milliseconds per insert + fetch (iters=" + iters + "): " + millisPer);
		map.close();
		map.recover();
		assertEquals(current, map.fetch());
		
		iters = rand.nextInt(4000) + 2000;
		for(int loop = 0; loop < 3; loop++) {
			for(int i = 0; i < iters; i++) {
				current.setTime(rand.nextLong());
				map.set(current);
			}
			map.close();
			map.recover();
			assertEquals(current, map.fetch());
		}
		
		start = System.currentTimeMillis();
		for(int i = 0; i < iters; i++) {
			current.setTime(rand.nextLong());
			map.set(current);
		}
		time = System.currentTimeMillis() - start;
		millisPer = (time / (double)iters);
		System.out.println("Milliseconds per insert (iters=" + iters + "): " + millisPer);
		
		double insertsPerMS = iters / (double)time;
		double insertsPerSecond = 1000 * insertsPerMS;
		double bytesPerSecond = 12 * insertsPerSecond;
		double kbPerSecond = bytesPerSecond / 1024;
		double mbPerSecond = kbPerSecond / 1024;
		
		System.out.println("Throughput (To File): ");
		System.out.println("  Bytes / Second: " + bytesPerSecond);
		System.out.println("  KB / Second:    " + kbPerSecond);
		System.out.println("  MB / Second:    " + mbPerSecond);

		bytesPerSecond = 8 * insertsPerSecond;
		kbPerSecond = bytesPerSecond / 1024;
		mbPerSecond = kbPerSecond / 1024;
		System.out.println("Throughput (Real Data): ");
		System.out.println("  Bytes / Second: " + bytesPerSecond);
		System.out.println("  KB / Second:    " + kbPerSecond);
		System.out.println("  MB / Second:    " + mbPerSecond);
	}
}
