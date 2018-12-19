package me.timothy.bots.database.custom;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.SchemaValidator;

/**
 * Abstract base class for making a mapping that relies on a single random access file and memory,
 * great for tables which we still need guarranteed persistence through restarts but thats really
 * the only thing we need out of the mapping.
 * 
 * @author Timothy
 */
public abstract class RandomAccessFileMapping<A> implements CustomMapping<A>, SchemaValidator {
	private static final Logger logger = LogManager.getLogger();
	
	protected File file;
	protected RandomAccessFile raf;
	
	/**
	 * Create a new random access file mapping attached to the given file
	 * @param file the file of interest
	 */
	public RandomAccessFileMapping(File file) {
		this.file = file;
	}
	
	/**
	 * Recover the database. This will create or open the file then just delegate
	 * to recoverImpl
	 */
	public void recover() {
		close();
		clearMemory();
		
		try {
			if(!file.exists()) {
				File dir = file.getParentFile();
				if(!dir.exists()) {
					if(!dir.mkdirs()) {
						throw new IllegalStateException("Failed to create directory " + dir.toString());
					}
				}
				
				raf = new RandomAccessFile(file, "rwd");
				writeFirst();
				return;
			}
			
			int seekPos = 0;
			try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
				seekPos = recoverImpl(dis);
			}catch(EOFException eof) {}
			
			postRecoverPreOpenWrite(seekPos);
			
			if(raf == null) {
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(seekPos);
			}
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Close the random access file if it is open.
	 */
	public void close() {
		if(raf == null)
			return;
		
		try {
			preClose();
			raf.close();
			raf = null;
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		clearMemory();
	}
	
	/**
	 * Wraps the save inside a try/catch and calls saveImpl
	 */
	public void save(A a) throws IllegalArgumentException {
		try {
			saveImpl(a);
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
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
	
	@Override
	public void validateSchema() {}
	
	/**
	 * This is called writer after we create the file for the first time. It may
	 * write any headers required.
	 * 
	 * @throws IOException If one occurs
	 */
	protected void writeFirst() throws IOException {}
	
	/**
	 * Clear any information that is stored in memory
	 */
	protected abstract void clearMemory();
	
	/**
	 * Save the given record without having to handle errors
	 * 
	 * @param a the record to save
	 * @throws IllegalArgumentException if the record is not valid
	 * @throws IOException if one occurs
	 */
	protected abstract void saveImpl(A a) throws IllegalArgumentException, IOException;
	
	/**
	 * This is called immediately after opening the appropriate stream. It should
	 * recover the data that was saved in the file. It may throw an EOFException
	 * to indicate that it is complete.
	 * 
	 * @param dis the data input stream to load from
	 * @throws IOException if one occurs
	 * @throws EOFException when it reaches the end of the file
	 * @returns the location to seek to once we open the writer
	 */
	protected abstract int recoverImpl(DataInputStream dis) throws IOException;
	
	/**
	 * This is called right after we recover the internal data and right before we
	 * open the random access file for writing. This may do any compression steps
	 * that it wants.
	 * 
	 * It may open the writer if it likes. If recover sees that the writer has 
	 * already been initialized, it will leave it.
	 * 
	 * @param seekPos the result from recoverImpl
	 * @throws IOException if one occurs
	 */
	protected void postRecoverPreOpenWrite(int seekPos) throws IOException {}
	
	/**
	 * This is called immediately prior to the connection being closed. It may do any file
	 * writes that are helpful, though it should take into account that in the event of a
	 * crash this won't get called. This is best used for compressing the file / deleting
	 * empty stuff
	 * 
	 * @throws IOException if one occurs
	 */
	protected void preClose() throws IOException {}
}
