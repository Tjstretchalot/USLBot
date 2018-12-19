package me.timothy.bots.memory;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.HandledModActionMapping;
import me.timothy.bots.database.MappingDatabase;

/**
 * This class just lets you iterate over handled mod actions joined with their history. This
 * requires a buffering step from mysql which this handles smoothly.
 * 
 * This only returns items which are NOT in the handled at timestamp mapping.
 * 
 * @author Timothy
 */
public class BufferedHandledModActionJoinHistoryIter {
	private static final Logger logger = LogManager.getLogger();
	private static final int bufferSizeIncrement = 250;
	
	private int bufferSize;
	private HandledModActionMapping hmaMap;
	private HandledAtTimestampMapping hatMap;
	
	private Timestamp timeBeforeWhichIgnore;
	private Timestamp timeAtOrAfterIgnore;
	
	private List<HandledModActionJoinHistory> buffer;
	private int bufferInd;
	private boolean sawNew;
	
	public BufferedHandledModActionJoinHistoryIter(MappingDatabase database, Timestamp timeBeforeWhichIgnore, Timestamp timeAtOrAfterIgnore) {
		this.bufferSize = 250;
		this.hmaMap = database.getHandledModActionMapping();
		this.hatMap = database.getHandledAtTimestampMapping();
		
		this.timeBeforeWhichIgnore = timeBeforeWhichIgnore;
		this.timeAtOrAfterIgnore = timeAtOrAfterIgnore;
		
		this.sawNew = false;
		
		updateBuffer();
	}
	
	private void updateBuffer() {
		buffer = hmaMap.fetchLatestJoined(timeBeforeWhichIgnore, timeAtOrAfterIgnore, bufferSize);
		if(buffer.isEmpty()) 
			bufferInd = -1;
		else
			bufferInd = 0;
	}
	
	private void increaseBuffer() {
		logger.printf(Level.WARN, "Iter HMAs -> Detected buffer size %d insufficient, increasing by %d", bufferSize, bufferSizeIncrement);
		bufferSize += bufferSizeIncrement;
	}
	
	public HandledModActionJoinHistory next() {
		if(bufferInd == -1)
			return null;
		
		HandledModActionJoinHistory current = buffer.get(bufferInd);
		bufferInd++;
		while(true) {
			if(current.isBan() && !hatMap.containsBanHistory(current.banHistory.id))
				break;
			if(current.isUnban() && !hatMap.containsUnbanHistory(current.unbanHistory.id))
				break;
			if(bufferInd == buffer.size()) {
				if(buffer.size() < bufferSize)
				{
					bufferInd = -1;
					return null;
				}
				
				if(!sawNew) {
					increaseBuffer();
				}
				updateBuffer();
			}
			current = buffer.get(bufferInd);
			bufferInd++;
		}
		
		if(!current.handledModAction.occurredAt.equals(timeBeforeWhichIgnore)) {
			timeBeforeWhichIgnore = new Timestamp(current.handledModAction.occurredAt.getTime());
			sawNew = true;
		}
		
		if(bufferInd == buffer.size()) {
			if(!sawNew) {
				increaseBuffer();
			}
			updateBuffer();
		}
		
		return current;
	}
}
