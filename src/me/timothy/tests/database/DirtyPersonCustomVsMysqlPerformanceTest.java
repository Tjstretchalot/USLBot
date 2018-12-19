package me.timothy.tests.database;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLFileConfiguration;
import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.database.custom.CustomDirtyPersonMapping;
import me.timothy.bots.database.mysql.MysqlDirtyPersonMapping;
import me.timothy.bots.models.DirtyPerson;
import me.timothy.tests.database.mysql.MysqlTestUtils;

public class DirtyPersonCustomVsMysqlPerformanceTest {
	private static USLFileConfiguration config;
	private static USLDatabase database;
	
	public static void main(String[] args) throws NullPointerException, IOException { 
		System.out.println("Loading database...");

		config = new USLFileConfiguration(Paths.get("tests"));
		config.load();
		database = MysqlTestUtils.getDatabase(config.getProperties().get("database"));
		
		MysqlTestUtils.clearDatabase(database);
		
		MysqlDirtyPersonMapping mysqlMap = new MysqlDirtyPersonMapping(database, database.getConnection());
		mysqlMap.validateSchema();
		CustomDirtyPersonMapping customMap = (CustomDirtyPersonMapping) database.getDirtyPersonMapping();
		
		System.out.println("Warming up for mysql insertion test...");
		for(int i = 0; i < 3; i++) {
			double res = getAverageInsertionSpeed(mysqlMap);
			System.out.println("Now just clearing..");
			mysqlMap.purgeSchema();
			mysqlMap.validateSchema();
			System.out.println("Finished iteration " + i + " (" + res + ")");
		}
		
		System.out.println("Performing mysql insertion test...");
		double mysqlAvgInsert = getAverageInsertionSpeed(mysqlMap);
		mysqlMap.purgeSchema();
		mysqlMap.validateSchema();
		System.out.println("Avg insert: " + mysqlAvgInsert);
		
		System.out.println();
		System.out.println("Warming up for custom insertion test...");
		for(int i = 0; i < 3; i++) {
			double res = getAverageInsertionSpeed(customMap);
			customMap.purgeSchema();
			customMap.validateSchema();
			System.out.println("Finished iteration " + i + "(" + res + ")");
		}
		
		System.out.println("Performing custom insertion test...");
		double customAvgInsert = getAverageInsertionSpeed(customMap);
		customMap.purgeSchema();
		customMap.validateSchema();
		System.out.println("Avg insert: " + customAvgInsert);
		
		if(mysqlAvgInsert > customAvgInsert) {
			double perc = ((mysqlAvgInsert - customAvgInsert) / customAvgInsert) * 100;
			System.out.println("  Winner: Custom (" + perc + "% better)");
		}else {
			double perc = ((customAvgInsert - mysqlAvgInsert) / mysqlAvgInsert) * 100;
			System.out.println("  Winner: MySQL (" + perc + "% better)");
		}
		
		System.out.println();
		System.out.println();
		System.out.println("Warming up for average sample speed...");
		
		for(int i = 0; i < 3; i++) {
			double res = getAverageSampleSpeed();
			System.out.println("Finished iteration " + i + " (" + res + ")");
		}
		
		System.out.println("Performing average sample speed test..");
		double avgSample = getAverageSampleSpeed();
		System.out.println("Average sample speed: " + avgSample);
		
		System.out.println();
		System.out.println();
		System.out.println("Warming up for mysql average speed for random...");
		for(int i = 0; i < 3; i++) {
			double res = getAverageSpeedForRandom(mysqlMap);
			System.out.println("Now just clearing..");
			mysqlMap.purgeSchema();
			mysqlMap.validateSchema();
			System.out.println("Finished iteration " + i + "(" + res + " / " + (res - avgSample) + ")");
		}
		
		System.out.println("Performing mysql average speed for random test..");
		double mysqlAvgRandom = getAverageSpeedForRandom(mysqlMap);
		System.out.println("  MySQL Average random loop:  " + mysqlAvgRandom);
		System.out.println("        Less the sample time: " + (mysqlAvgRandom - avgSample));

		System.out.println();
		System.out.println("Warming up for custom average speed for random...");
		for(int i = 0; i < 3; i++) {
			double res = getAverageSpeedForRandom(customMap);
			System.out.println("Now just clearing..");
			customMap.purgeSchema();
			customMap.validateSchema();
			System.out.println("Finished iteration " + i + "(" + res + " / " + (res - avgSample) + ")");
		}
		
		System.out.println("Performing custom average speed for random test..");
		double customAvgRandom = getAverageSpeedForRandom(customMap);
		System.out.println("  Custom Average random loop:  " + customAvgRandom);
		System.out.println("         Less the sample time: " + (customAvgRandom - avgSample));
		
		if(mysqlAvgRandom > customAvgRandom) {
			double perc = ((mysqlAvgRandom - customAvgRandom) / customAvgRandom) * 100;
			System.out.println("Winner: Custom (" + perc + "% better)");
		}else {
			double perc = ((customAvgRandom - mysqlAvgRandom) / mysqlAvgRandom) * 100;
			System.out.println("Winner: MySQL (" + perc + "% better)");
		}
		
		mysqlMap.purgeSchema();
	}
	
	private static double getAverageInsertionSpeed(DirtyPersonMapping map) {
		final long iterations = 1000;
		
		DirtyPerson pers = new DirtyPerson(0);
		long start = System.currentTimeMillis();
		for(int i = 0; i < iterations; i++) {
			pers.personID = i;
			map.save(pers);
		}
		long time = System.currentTimeMillis() - start;
		return (double)time / iterations;
	}
	
	/*
	private static double getAverageSpeedForSingleAndDelete(DirtyPersonMapping map) {
		final long iterations = 1000;
		
		DirtyPerson pers = new DirtyPerson(0);
		long start = System.currentTimeMillis();
		for(int i = 0; i < iterations; i++) {
			pers.personID = i;
			map.save(pers);
			map.delete(i);
		}
		long time = System.currentTimeMillis() - start;
		return ((double) time) / iterations;
	}
	*/
	
	private static int getNextExpo() {
		// lambda 1/12
		//return (int)Math.ceil(Math.log(1-rand.nextDouble()) * 12);
		return 2;
	}
	
	private static double getAverageSampleSpeed() {
		final long iterations = 10000;
		
		long sum = 0;
		long start = System.currentTimeMillis();
		for(int i = 0; i < iterations; i++) {
			sum += getNextExpo();
		}
		long time = System.currentTimeMillis() - start;
		System.err.println(sum); // prevent it from optimizing the loop
		return ((double) time) / iterations;
	}
	
	private static double getAverageSpeedForRandom(DirtyPersonMapping map) {
		final long iterations = 100;
		
		DirtyPerson pers = new DirtyPerson(0);
		long start = System.currentTimeMillis();
		for(int i = 0; i < iterations; i++) {
			final int num = getNextExpo();
			for(int j = 0; j < num; j++) {
				pers.personID = i + j;
				map.save(pers);
			}
			
			List<DirtyPerson> fromDB = map.fetchAll();
			for(int j = 0, len = fromDB.size(); j < len; j++) {
				map.delete(fromDB.get(j).personID);
			}
		}
		long time = System.currentTimeMillis() - start;
		return ((double) time) / iterations;
	}
}
