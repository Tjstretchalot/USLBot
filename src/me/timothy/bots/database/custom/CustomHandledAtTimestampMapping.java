package me.timothy.bots.database.custom;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledAtTimestamp;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;

/**
 * This is a specialized mapping for the HandledAtTimestamp that is persisted to 
 * file constantly but the file is only used for recovery (otherwise the operations
 * are performed in memory). This specifically is designed to make the operation
 * as fast as possible except during recovery, which is allowed to be as slow as
 * necessary.
 * 
 * @author Timothy
 */
public class CustomHandledAtTimestampMapping implements HandledAtTimestampMapping, SchemaValidator, CustomMapping<HandledAtTimestamp> {
	private static final Logger logger = LogManager.getLogger();
	
	private File file;
	
	private USLDatabase database;
	private RandomAccessFile writer;
	
	private Timestamp expectedTime;
	private Set<Integer> banHistories;
	private Set<Integer> unbanHistories;
	private Set<Integer> unbanRequests;
	
	private ByteBuffer buffer;
	
	public CustomHandledAtTimestampMapping(USLDatabase database, File file) {
		this.database = database;
		this.file = file;
		
		banHistories = new HashSet<>();
		unbanHistories = new HashSet<>();
		unbanRequests = new HashSet<>();
		
		buffer = ByteBuffer.allocate(8);
	}
	
	/**
	 * Recovers the database by reading from the file if it exists, loading our actual memory,
	 * then setting up the file handle.
	 */
	public void recover() {
		banHistories.clear();
		unbanHistories.clear();
		unbanRequests.clear();
		
		try {
			if(writer != null) {
				writer.close();
				writer = null;
			}
			
			File dir = file.getParentFile();
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					throw new IllegalStateException("Failed to create directory " + dir.toString());
				}
			}
			if(!file.exists()) {
				writer = new RandomAccessFile(file, "rwd");
				writer.seek(0);
				return;
			}
			
			int seekTo = 0;
			try(DataInputStream reader = new DataInputStream(new FileInputStream(file))) {
				long expectedTimestamp = reader.readLong();
				seekTo += 8;
				expectedTime = new Timestamp(expectedTimestamp);
				
				boolean finished = false;
				while(!finished) {
					byte type = reader.readByte();
					int primaryKey = reader.readInt();
					
					HandledAtTimestamp.HandledAtTimestampType typeEnum = HandledAtTimestamp.HandledAtTimestampType.getByDatabaseValue(type);
					
					HandledModAction hma;
					switch(typeEnum) {
					case BAN_HISTORY:
						BanHistory bh = database.getBanHistoryMapping().fetchByID(primaryKey);
						if(bh == null) {
							finished = true;
							break;
						}
						hma = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
						if(hma.occurredAt.getTime() != expectedTimestamp) {
							finished = true;
							break;
						}
						banHistories.add(primaryKey);
						break;
					case UNBAN_HISTORY:
						UnbanHistory ubh = database.getUnbanHistoryMapping().fetchByID(primaryKey);
						if(ubh == null) {
							finished = true;
							break;
						}
						hma = database.getHandledModActionMapping().fetchByID(ubh.handledModActionID);
						if(hma.occurredAt.getTime() != expectedTimestamp) {
							finished = true;
							break;
						}

						unbanHistories.add(primaryKey);
						break;
					case VALID_UNBAN_REQUEST:
						UnbanRequest req = database.getUnbanRequestMapping().fetchByID(primaryKey);
						if(req == null) {
							finished = true;
							break;
						}
						if(req.handledAt.getTime() != expectedTimestamp) {
							finished = true;
							break;
						}

						unbanRequests.add(primaryKey);
						break;
					}
					
					if(!finished)
						seekTo += 5;
				}
			}catch(EOFException e) {
			}
			
			writer = new RandomAccessFile(file, "rwd");
			writer.seek(seekTo);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void save(HandledAtTimestamp a) throws IllegalArgumentException {
		if(expectedTime == null) {
			throw new IllegalStateException("You must call clear at least once before you can start saving/writing things");
		}
		
		buffer.clear();
		buffer.put(a.type.databaseValue);
		buffer.putInt(a.primaryKey);
		
		buffer.flip();
		try {
			writer.write(buffer.array(), 0, 5);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<HandledAtTimestamp> fetchAll() {
		throw new UnsupportedOperationException("This operation would be very slow");
	}

	@Override
	public boolean containsBanHistory(int banHistoryID) {
		return banHistories.contains(banHistoryID);
	}

	@Override
	public boolean containsUnbanHistory(int unbanHistoryID) {
		return unbanHistories.contains(unbanHistoryID);
	}

	@Override
	public boolean containsUnbanRequest(int unbanRequestID) {
		return unbanRequests.contains(unbanRequestID);
	}

	@Override
	public void addBanHistory(int banHistoryID) {
		if(expectedTime == null) {
			throw new IllegalStateException("You must call clear at least once before you can start saving/writing things");
		}
		
		buffer.clear();
		buffer.put((byte)HandledAtTimestamp.HandledAtTimestampType.BAN_HISTORY.databaseValue);
		buffer.putInt(banHistoryID);
		buffer.flip();

		try {
			writer.write(buffer.array(), 0, 5);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		banHistories.add(banHistoryID);
	}

	@Override
	public void addUnbanHistory(int unbanHistoryID) {
		if(expectedTime == null) {
			throw new IllegalStateException("You must call clear at least once before you can start saving/writing things");
		}
		
		buffer.clear();
		buffer.put((byte)HandledAtTimestamp.HandledAtTimestampType.UNBAN_HISTORY.databaseValue);
		buffer.putInt(unbanHistoryID);
		buffer.flip();

		try {
			writer.write(buffer.array(), 0, 5);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		unbanHistories.add(unbanHistoryID);
	}

	@Override
	public void addUnbanRequest(int unbanRequestID) {
		if(expectedTime == null) {
			throw new IllegalStateException("You must call clear at least once before you can start saving/writing things");
		}

		buffer.clear();
		buffer.put((byte)HandledAtTimestamp.HandledAtTimestampType.VALID_UNBAN_REQUEST.databaseValue);
		buffer.putInt(unbanRequestID);
		buffer.flip();

		try {
			writer.write(buffer.array(), 0, 5);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		unbanRequests.add(unbanRequestID);
	}
	
	/**
	 * This function is meant for testing. It should verify that the contents of this map
	 * are exactly the ids contains in the three lists. It does not need to be performant.
	 * 
	 * @param banHists the ban history ids
	 * @param unbanHists the unban history ids
	 * @param unbanRequests the unban request ids
	 * @throws AssertionError if this does not match the expected
	 */
	public void verifyContents(int[] banHists, int[] unbanHists, int[] unbRequests) {
		verifyContents(IntStream.of(banHists).boxed().collect(Collectors.toList()),
				IntStream.of(unbanHists).boxed().collect(Collectors.toList()),
				IntStream.of(unbRequests).boxed().collect(Collectors.toList()));
	}
	
	public void verifyContents(Collection<Integer> banHists, Collection<Integer> unbanHists, Collection<Integer> unbRequests) {
		org.junit.Assert.assertEquals(banHistories.size(), banHists.size());
		org.junit.Assert.assertTrue(banHistories.containsAll(banHists));

		org.junit.Assert.assertEquals(unbanHistories.size(), unbanHists.size());
		org.junit.Assert.assertTrue(unbanHistories.containsAll(unbanHists));

		org.junit.Assert.assertEquals(unbanRequests.size(), unbRequests.size());
		org.junit.Assert.assertTrue(unbanRequests.containsAll(unbRequests));
	}

	@Override
	public void clear(Timestamp time) {
		try {
			Timestamp cp = new Timestamp(time.getTime());
			cp.setNanos(0);
			long writeTime = cp.getTime();
			expectedTime = cp;
			
			buffer.clear();
			buffer.putLong(writeTime);
			buffer.flip();
			
			writer.seek(0);
			writer.write(buffer.array(), 0, 8);
		} catch (IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		banHistories.clear();
		unbanHistories.clear();
		unbanRequests.clear();
	}

	@Override
	public void validateSchema() throws IllegalStateException {
	}

	public void close() {
		if(writer != null) {
			try {
				writer.close();
				writer = null;
			}catch(IOException e) {
				logger.throwing(e);
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void purgeSchema() {
		close();
		
		try {
			Files.delete(file.toPath());
		}catch(IOException e) { 
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		recover();
	}
}
