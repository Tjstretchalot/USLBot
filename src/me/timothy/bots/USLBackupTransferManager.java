package me.timothy.bots;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is just a static class that allows for transferring
 * files to the backup manager.
 * 
 * @author Timothy Moore
 *
 */
public class USLBackupTransferManager {
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Backs up the specified file to the FTP backup server.
	 * 
	 * Connects via the information in config, verifying that "secure" is false. Then
	 * the remoteWorkingDirEffector should change the working director of the ftpclient to
	 * the appropriate resulting dir, using the utility functions provided in this class.
	 * 
	 * Then a filename should be selected. This is done by calling remoteFileNameSelector, 
	 * which also is probably usable through the utility functions provided in this class.
	 * 
	 * Finally the file is transferred.
	 * 
	 * @param config the file configuration
	 * @param backupFile the file to backup
	 * @param remoteWorkingDirEffector sets the remote working directory
	 * @param remoteFilenameSelector sets the remote filename
	 */
	public static void backupFileFTP(USLFileConfiguration config, File backupFile, Consumer<FTPClient> remoteWorkingDirEffector, 
			Function<FTPClient, String> remoteFilenameSelector) {
		if(Boolean.parseBoolean(config.getProperty("ftpbackups.secure"))) {
			throw new RuntimeException("backupFileFTP cannot be used with secure backups!");
		}
		
		final String ftpHost = config.getProperty("ftpbackups.host");
		final String ftpUser = config.getProperty("ftpbackups.username");
		final String ftpPass = config.getProperty("ftpbackups.password");
		final int ftpPort = Integer.parseInt(config.getProperty("ftpbackups.port"));
		

		FTPClient client = new FTPClient();
		try { 
			logger.printf(Level.TRACE, "Connecting to FTP server %s on port %s..", ftpHost, ftpPort);
			client.connect(ftpHost, ftpPort);
			
			int reply = client.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply)) {
				logger.printf(Level.ERROR, "FTP Server returned unexpected reply code %d. Aborting!", reply);
				throw new RuntimeException("Unexpected FTP reply code " + reply);
			}
			
			logger.printf(Level.TRACE, "Successfully connected. Logging in..");
			boolean loginSuccess = client.login(ftpUser, ftpPass);
			
			if(!loginSuccess) {
				logger.error("FTP Server rejected login information! Aborting!");
				throw new RuntimeException("FTP server rejected login information");
			}
			
			client.setFileType(FTP.BINARY_FILE_TYPE);
			
			logger.trace("Navigating to correct working directory..");
			remoteWorkingDirEffector.accept(client);
			
			logger.trace("Selecting remote filename..");
			String remoteName = remoteFilenameSelector.apply(client);
			
			logger.printf(Level.TRACE, "Selected remote filename %s", remoteName);
			logger.trace("Beginning upload...");
			
			boolean storeSuccess = false;
			try (InputStream input = new FileInputStream(backupFile)) {
				storeSuccess = client.storeFile(remoteName, input);
			}
			
			if(storeSuccess) {
				logger.info("Database backup successful");
			}else {
				logger.error("client.storeFile returned false!");
				throw new RuntimeException("client.storeFile returned false!");
			}
		}catch(IOException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}finally {
			try {
				logger.trace("Attempting to disconnect from ftp server..");
				client.disconnect();
			} catch (IOException e) {
				logger.trace("Failed to disconnect from ftp server");
				logger.throwing(e);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Navigates to a directory defined with folders deliminated by either
	 * forward or back slashes, or some combination.
	 * 
	 * This generates folders if they do not exist.
	 * 
	 * This is a valid input to backupFileFTP remoteWorkingDirEffector
	 * 
	 * @param directory the directory
	 * @return
	 */
	public static Consumer<FTPClient> navigateToDirectory(final String directory) {
		return new Consumer<FTPClient>() {

			@Override
			public void accept(FTPClient client) {
				try {
					List<String> dbFolderPath = getDBFolderPath(directory);
					String currentDir = "";
					for(String folder : dbFolderPath) {
						if(client.changeWorkingDirectory(folder)) {
							if(currentDir.isEmpty()) {
								currentDir += folder;
							}else {
								currentDir += "/" + folder;
							}
							logger.printf(Level.TRACE, "Succesfully navigated to %s, current working directory is now %s", folder, currentDir);
						}else {
							logger.printf(Level.WARN, "Failed to navigate to %s, attempting to generate directory..", folder);
							if(client.makeDirectory(folder)) {
								logger.printf(Level.INFO, "Successfully generated folder %s (current dir is %s)", folder, currentDir);
							}else {
								logger.printf(Level.ERROR, "Failed to generate folder %s (current dir is %s), aborting!", folder, currentDir);
								throw new RuntimeException("Failed to make directories to path " + directory);
							}
							
							logger.printf(Level.TRACE, "Navigating to newly generated folder..");
							if(client.changeWorkingDirectory(folder)) {
								if(currentDir.isEmpty()) {
									currentDir += folder;
								}else {
									currentDir += "/" + folder;
								}
								logger.printf(Level.TRACE, "Successfully navigated to newly generated folder, current working directory is now %s", currentDir);
							}else {
								logger.printf(Level.ERROR, "After successfully generating %s I failed to navigate to it. working directory: %s. Aborting!", folder, currentDir);
								throw new RuntimeException("Failed to navigate to " + folder + " at working directory " + currentDir);
							}
						}
					}
				}catch(IOException e) {
					logger.throwing(e);
					throw new RuntimeException(e);
				}
			}
			
		};
	}
	
	/**
	 * Selects an unusued filename in the working directory that is of the form "base.extension" where base is 
	 * filenameBase and extension is extension unless that file is taken and then it will search for an unused
	 * filename of the form "base (counter).extension" where counter is a number (starting at 2)
	 * 
	 * @param filenameBase the base of the filename
	 * @param extension the extension for the filename
	 * @return an unused filename
	 */
	public static Function<FTPClient, String> chooseUnusedFilenameUsingCounter(final String filenameBase, final String extension) {
		return new Function<FTPClient, String>() {

			@Override
			public String apply(FTPClient client) {
				
				String[] localNames;
				try {
					localNames = client.listNames();
				} catch(IOException e) {
					logger.throwing(e);
					throw new RuntimeException(e);
				}

				int counter = 2;
				String remoteName = filenameBase + "." + extension;
				
				while(true) {
					boolean found = false;
					for(String fileName : localNames) {
						if(fileName.equals(remoteName)) {
							found = true;
							break;
						}
					}
					
					if(!found)
						break;
					
					remoteName = filenameBase + " (" + counter + ")." + extension;
					counter++;
				}
				
				return remoteName;
			}
			
		};
	}
	
	/**
	 * Gets the path to the database folder such that the first element
	 * is the highest-level folder and the last element is the lowest-level
	 * folder.
	 * 
	 * For example
	 * 
	 * getDBFolder("one/two/three\\four/") - [ "one", "two", "three", "four" ]
	 * @param dbFolder
	 * @return
	 */
	protected static List<String> getDBFolderPath(String dbFolder) {
		return Arrays.asList(dbFolder.split("/|\\\\"));
	}
}
