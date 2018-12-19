package me.timothy.bots.database.custom;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.RedditToMeaningProgressMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.RedditToMeaningProgress;

/**
 * A custom implementation of the RedditToMeaningProgressMapping. This keeps the timestamp
 * in memory (of course) and will fill the file until it has more than 8kb of stuff or 
 * we detect a long gap in activity, or we just restarted.
 * 
 * @author Timothy
 */
public class CustomRedditToMeaningProgressMapping extends RandomAccessFileMapping<RedditToMeaningProgress>
												  implements RedditToMeaningProgressMapping, SchemaValidator {
	private static final Logger logger = LogManager.getLogger();
	private static final long MAX_FILE_SIZE = 1024 * 16;
	
	private int counter;
	private Timestamp timestamp;
	private long fileSize;

	private ByteBuffer buffer;
	
	public CustomRedditToMeaningProgressMapping(File file) {
		super(file);
		
		buffer = ByteBuffer.allocate(16);
		counter = 0;
	}

	@Override
	public List<RedditToMeaningProgress> fetchAll() {
		if(timestamp == null)
			return Collections.emptyList();
		return Collections.singletonList(new RedditToMeaningProgress(fetch()));
	}

	@Override
	public Timestamp fetch() {
		if(timestamp == null)
			return null;
		return new Timestamp(timestamp.getTime());
	}

	@Override
	public void set(Timestamp stamp) {
		if(fileSize >= MAX_FILE_SIZE - 12) {
			counter++;
			try {
				raf.seek(0);
				fileSize = 0;
				writeFirst();
			} catch (IOException e) {
				logger.throwing(e);
				throw new RuntimeException(e);
			}
		}

		long time = stamp == null ? 0 : stamp.getTime();
		timestamp = stamp == null ? null : new Timestamp(time);
		
		buffer.clear();
		buffer.putLong(time);
		buffer.putInt(counter);
		buffer.flip();
		
		try {
			raf.write(buffer.array(), 0, 12);
			fileSize += 12;
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void saveImpl(RedditToMeaningProgress a) throws IllegalArgumentException, IOException {
		set(a.lastEndedAt);
	}
	
	@Override
	protected void clearMemory() {
		timestamp = null;
		counter = 0;
		fileSize = 0;
	}

	@Override
	protected int recoverImpl(DataInputStream dis) throws IOException {
		int seekPos = 0;
		
		try {
			counter = dis.readInt();
			seekPos += 4;
			
			while(true) {
				long time = dis.readLong();
				int count = dis.readInt();
				if(count != counter)
					return seekPos;
				
				seekPos += 12;
				if(time == 0) {
					timestamp = null;
				}else {
					if(timestamp == null)
						timestamp = new Timestamp(time);
					else
						timestamp.setTime(time);
				}
			}
		}catch(EOFException e) {
			return seekPos;
		}
	}

	@Override
	protected void writeFirst() throws IOException {
		buffer.clear();
		buffer.putInt(counter);
		buffer.flip();
		
		raf.write(buffer.array(), 0, 4);
		fileSize += 4;
	}
}
