package me.timothy.bots.database.custom;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.database.SchemaValidator;
import me.timothy.bots.models.DirtyPerson;

public class CustomDirtyPersonMapping implements DirtyPersonMapping, SchemaValidator, CustomMapping<DirtyPerson> {
	private static final Logger logger = LogManager.getLogger();
	
	private File file;
	
	private RandomAccessFile writer;
	
	private Set<Integer> personIds;
	
	private int counter;
	private ByteBuffer buffer;
	
	public CustomDirtyPersonMapping(File file) {
		this.file = file;
		
		counter = 0;
		personIds = new HashSet<Integer>();
		buffer = ByteBuffer.allocate(16);
	}
	
	@Override
	public void save(DirtyPerson a) throws IllegalArgumentException {
		if(personIds.contains(a.personID))
			return;
		
		personIds.add(a.personID);
		directWrite(a.personID);
	}
	
	protected void directWrite(int personID) {
		buffer.clear();
		buffer.putInt(counter);
		buffer.putInt(personID);
		buffer.flip();
		try {
			writer.write(buffer.array(), 0, 8);
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	public void recover() {
		if(writer != null)
			close();
		
		personIds.clear();
		try {
			File dir = file.getParentFile();
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					throw new IllegalStateException("Failed to create directory " + dir.toString());
				}
			}
			
			if(!file.exists()) {
				writer = new RandomAccessFile(file, "rwd");
				writer.seek(0);
				
				buffer.clear();
				buffer.putInt(counter);
				buffer.flip();
				writer.write(buffer.array(), 0, 4);
				return;
			}
			
			int seekPos = 0;
			try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
				counter = dis.readInt();
				seekPos += 4;
				
				while(dis.readInt() == counter) {
					int personId = dis.readInt();
					if(!personIds.add(personId)) {
						personIds.remove(personId);
					}
					seekPos += 8;
				}
			}catch(EOFException e) {
			}
			
			writer = new RandomAccessFile(file, "rwd");
			writer.seek(seekPos);
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	public void clear() {
		counter++;
		
		try {
			writer.seek(0);
			
			buffer.clear();
			buffer.putInt(counter);
			buffer.flip();
			writer.write(buffer.array(), 0, 4);
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		personIds.clear();
	}
	
	public void close() {
		try {
			writer.close();
			writer = null;
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<DirtyPerson> fetchAll() {
		return personIds.stream().map((i) -> new DirtyPerson(i)).collect(Collectors.toList());
	}

	@Override
	public List<DirtyPerson> fetch(int limit) {
		return personIds.stream().limit(limit).map((i) -> new DirtyPerson(i)).collect(Collectors.toList());
	}

	@Override
	public boolean contains(int personId) {
		return personIds.contains(personId);
	}

	@Override
	public void delete(int personId) {
		if(personIds.contains(personId)) {
			personIds.remove(personId);
			if(personIds.isEmpty()) {
				clear();
			}else {
				directWrite(personId);
			}
		}
	}

	@Override
	public void validateSchema() throws IllegalStateException {
	}

	@Override
	public void purgeSchema() {
		if(writer != null)
			close();
		
		counter = 0;
		try {
			Files.delete(file.toPath());
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		recover();
	}
}
