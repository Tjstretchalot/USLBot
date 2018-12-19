package me.timothy.bots.memory;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.database.UnbanRequestMapping;
import me.timothy.bots.models.UnbanRequest;

public class BufferedUnbanRequestIter {
	private static final Logger logger = LogManager.getLogger();
	private static final int bufferSizeIncrement = 10;
	
	private int bufferSize;
	private UnbanRequestMapping urMap;
	private HandledAtTimestampMapping hatMap;

	private Timestamp timeBeforeWhichIgnore;
	private Timestamp timeAtOrAfterIgnore;
	
	private List<UnbanRequest> buffer;
	private int bufferInd;
	private boolean sawNew;
	
	public BufferedUnbanRequestIter(MappingDatabase database, Timestamp timeBeforeWhichIgnore, Timestamp timeAtOrAfterIgnore) {
		this.bufferSize = 10;
		this.urMap = database.getUnbanRequestMapping();
		this.hatMap = database.getHandledAtTimestampMapping();
		
		this.timeBeforeWhichIgnore = timeBeforeWhichIgnore;
		this.timeAtOrAfterIgnore = timeAtOrAfterIgnore;
		
		this.sawNew = false;
		
		updateBuffer();
	}
	
	private void updateBuffer() {
		buffer = urMap.fetchLatestValid(timeBeforeWhichIgnore, timeAtOrAfterIgnore, bufferSize);
		if(buffer.isEmpty()) {
			bufferInd = -1;
		}else {
			bufferInd = 0;
		}
	}
	
	private void increaseBuffer() {
		logger.printf(Level.WARN, "Iter URs -> Detected buffer size %d insufficient, increasing by %d", bufferSize, bufferSizeIncrement);
		bufferSize += bufferSizeIncrement;
	}
	
	public UnbanRequest next() {
		if(bufferInd == -1)
			return null;
		
		UnbanRequest current = buffer.get(bufferInd);
		bufferInd++;
		while(hatMap.containsUnbanRequest(current.id)) {
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
		
		if(!current.handledAt.equals(timeBeforeWhichIgnore)) {
			timeBeforeWhichIgnore = new Timestamp(current.handledAt.getTime());
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
