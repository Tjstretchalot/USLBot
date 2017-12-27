package me.timothy.bots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import me.timothy.bots.database.ActionLogMapping;

/**
 * Manages disconnecting the database and managing backups. This handles
 * both backing up the generated log4j file as well as the database.
 * 
 * @author Timothy
 */
public class USLDatabaseBackupManager {
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * The database that this is managing.
	 */
	protected USLDatabase database;
	
	/**
	 * The file configuration
	 */
	protected USLFileConfiguration config;
	
	/**
	 * The next time a backup is due. 0 if no backup has been done
	 * by this manager yet.
	 */
	protected long nextBackupUTC;
	
	/**
	 * Initializes a new backup manager that is attached to the specified database
	 * based on the specified configuration. A backup is queued for the next 
	 * considerBackup call.
	 * 
	 * @param database the database
	 * @param config the configuration
	 */
	public USLDatabaseBackupManager(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
		
		this.nextBackupUTC = 0;
	}
	
	/**
	 * Backs up the database and logs if it is appropriate to do so.
	 */
	public void considerBackup() {
		long now = System.currentTimeMillis();
		if(nextBackupUTC > now) {
			return;
		}
		ActionLogMapping al = database.getActionLogMapping();
		al.append("Initiating database backup..");
		
		logger.info("Initiating database backup..");
		logger.debug("Disconnecting from MySQL database..");
		database.disconnect();
		
		logger.debug("Locating unused filename for database backup...");
		File backupFile = decideBackupFile(now);
		
		logger.debug("Generating database backup file..");
		generateDatabaseBackupFile(backupFile);
		
		logger.debug("Sending database backup file..");
		boolean succ = sendDatabaseBackupFile(backupFile, now);
		if(!succ) {
			logger.error("Sending database backup file was unsuccessful!");
		}
		
		
		logger.debug("Deleting local backup..");
		deleteLocalDatabaseBackupFile(backupFile);
		
		logger.debug("Choosing new backup time..");
		selectNextBackupTime(now, succ);
		logger.debug("Next backup time is " + nextBackupUTC);
		
		logger.debug("Reconnecting to MySQL database..");
		try {
			database.connect(config.getProperty("database.username"), config.getProperty("database.password"), config.getProperty("database.url"));
			if(!succ) {
				al.append("Failed to send the database backup file to the backup server! It may be offline.");
			}
			Date asDate = new Date(nextBackupUTC);
			DateFormat df = DateFormat.getDateTimeInstance();
			al.append("Next backup attempt is at " + df.format(asDate));
			al.append("Database backup complete.");
		} catch (SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	/**
	 * Decides where to save the backup file initially.
	 * 
	 * @param now the current timestamp
	 * @return the backup file
	 */
	protected File decideBackupFile(long now) {
		File file = new File("usl-db-backup-" + now + ".sql.gz");
		int counter = 2;
		while(file.exists()) {
			file = new File("usl-db-backup-" + now + " (" + counter + ").sql.gz");
			counter++;
		}
		return file;
	}
	
	/**
	 * Generates the backup file, which will be located at file.
	 * 
	 * @param file where to save database backup
	 */
	protected void generateDatabaseBackupFile(File file) {
		final boolean windows = System.getProperty("os.name").startsWith("Windows");
		logger.trace("Creating command..");
		StringBuilder commandBuilder = new StringBuilder();
		if(windows) {
			commandBuilder.append("cmd.exe /c ");
		}
		commandBuilder.append("mysqldump -u ");
		commandBuilder.append(config.getProperty("database.username"));
		commandBuilder.append(" -p");
		commandBuilder.append(config.getProperty("database.password"));
		commandBuilder.append(" ").append(config.getProperty("database.database"));
		commandBuilder.append(" | ");
		if(windows) {
			commandBuilder.append("cmd.exe /c ");
		}
		commandBuilder.append("gzip > \"").append(file.getAbsolutePath()).append("\"");
		
		String command = commandBuilder.toString();
		String commandClean = command.replace("-p" + config.getProperty("database.password"), "-pREDACTED");
		logger.trace("Executing " + commandClean);
		
		Runtime runtime = Runtime.getRuntime();
		try {
			Process pc = null;
			if(windows) {
				pc = runtime.exec(command);
			}else {
				pc = runtime.exec(new String[] { "/bin/sh", "-c", command });
			}
			
			InputStream stdInp = pc.getInputStream();
			if(stdInp != null) {
				BufferedReader stdInput = new BufferedReader(new 
		                 InputStreamReader(stdInp));
	
				logger.trace("Reading from standard output..");
				String ln;
				while((ln = stdInput.readLine()) != null) {
					logger.trace(ln);
				}
			}else {
				logger.trace("Standard output was null (redirected?)");
			}
			
			InputStream errInp = pc.getErrorStream();
			if(errInp != null) {
	            BufferedReader stdError = new BufferedReader(new 
		                 InputStreamReader(pc.getErrorStream()));
	            logger.trace("Reading from error output..");
	            String ln;
	            while((ln = stdError.readLine()) != null) {
	            	logger.trace(ln);
	            }
			}else {
				logger.trace("Error output was null (redirected?)");
			}
			
            logger.trace("Awaiting result..");
			int result = pc.waitFor();
			
			logger.trace("Got result " + result);
			
			logger.trace("Waiting a bit to allow files to process..");
			Thread.sleep(1000);
			logger.trace("Verifying backup file exists...");
			if(!file.exists()) {
				logger.error("Backup file did not exist after executing command " + commandClean);
				logger.error("Result code was " + result);
				throw new RuntimeException("Expected backup file to exist after executing " + commandClean);
			}
		} catch (IOException | InterruptedException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Send the backup file to the server described by the configuration file.
	 * 
	 * @param backupFile the backup file to send
	 * @param now the current timestamp
	 */
	protected boolean sendDatabaseBackupFile(File backupFile, long now) {
		final boolean secure = Boolean.valueOf(config.getProperty("ftpbackups.secure"));
		
		if(secure) {
			sendDatabaseBackupFileSFTP(backupFile, now);
			return true; // todo
		}else {
			return sendDatabaseBackupFileFTP(backupFile, now);
		}
	}
	
	/**
	 * Send the database backup file to the FTP server.
	 * 
	 * @param backupFile the backup file
	 * @param now the current timestamp
	 * @return if the backup file was sent successfully
	 */
	protected boolean sendDatabaseBackupFileFTP(File backupFile, long now) {
		final String dbFolder = config.getProperty("ftpbackups.dbfolder");
		
		return USLBackupTransferManager.backupFileFTP(config, backupFile, 
				USLBackupTransferManager.navigateToDirectory(dbFolder), 
				USLBackupTransferManager.chooseUnusedFilenameUsingCounter("usl-db-backup-" + now, "sql.gz")
				);
	}
	
	/**
	 * Send the database backup file to the SFTP server.
	 * 
	 * @param backupFile the file to send.
	 * @param now the current timestamp
	 */
	protected void sendDatabaseBackupFileSFTP(File backupFile, long now) {
		final String sftpHost = config.getProperty("ftpbackups.host");
		final String sftpUser = config.getProperty("ftpbackups.username");
		final String sftpPass = config.getProperty("ftpbackups.password");
		final int sftpPort = Integer.parseInt(config.getProperty("ftpbackups.port"));
		final String sftpKnownHostsFile = config.getProperty("ftpbackups.knownhostsfile");
		final String dbFolder = config.getProperty("ftpbackups.dbfolder");
		
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		logger.trace("Initiating connection with host");
		try {
			JSch jsch = new JSch();
			jsch.setKnownHosts(sftpKnownHostsFile);
			session = jsch.getSession(sftpUser, sftpHost, sftpPort);
			session.setPassword(sftpPass);
			session.connect();
			
			logger.trace("Successfully connected to host. Generating secure channel..");
			channel = session.openChannel("sftp");
			channel.connect();
			
			logger.trace("Secure file transfer channel established. Navigating to backup folder");
			channelSftp = (ChannelSftp) channel;
			
			channelSftp.cd(dbFolder);
			
			logger.trace("Navigation successful. Generating unused remote filename");
			
			@SuppressWarnings("unchecked")
			Vector<LsEntry> lsResult = channelSftp.ls(".");
			
			int counter = 2;
			String remoteName = "usl-db-backup-" + now + ".sql.gz";
			
			while(true) {
				boolean found = false;
				for(LsEntry entry : lsResult) {
					if(entry.getFilename().equals(remoteName)) {
						found = true;
						break;
					}
				}
				
				if(found)
					break;
				
				remoteName = "usl-db-backup-" + now + " (" + counter + ").sql.gz";
				counter++;
			}
			
			logger.trace("Chose filename " + remoteName + ". Uploading..");
			
			FileInputStream inpStream = null;
			try {
				inpStream = new FileInputStream(backupFile);
				channelSftp.put(inpStream, remoteName);
			}finally {
				if(inpStream != null) {
					logger.trace("Disconnecting from backup file..");
					inpStream.close();
				}
				inpStream = null;
			}
			
			logger.trace("Upload successful!");
		}catch(JSchException | SftpException | IOException exc) {
			logger.throwing(exc);
			throw new RuntimeException(exc);
		}finally {
			if(channelSftp != null) {
				logger.trace("Disconnecting from SFTP channel..");
				channelSftp.exit();
				channelSftp = null;
				channel = null;
			}
			
			if(channel != null) {
				logger.trace("Disconnecting from SFTP channel (uncasted!)..");
				channel.disconnect();
				channel = null;
			}
			
			if(session != null) {
				logger.trace("Disconnecting from host..");
				session.disconnect();
				session = null;
			}
		}
	}

	/**
	 * Delete the local database backup file
	 * @param backupFile the backup file
	 */
	protected void deleteLocalDatabaseBackupFile(File backupFile) {
		try {
			Files.delete(backupFile.toPath());
		}catch(IOException ex) {
			logger.error("Failed to delete local backup file! This error is going to be caught, logged, and then ignored.");
			logger.catching(ex);
		}
	}

	/**
	 * Selects the next backup time
	 * @param now the current timestamp
	 */
	protected void selectNextBackupTime(long now, boolean succ) {
		if(succ) {
			nextBackupUTC = now + Long.parseLong(config.getProperty("ftpbackups.intervalms"));
		}else {
			nextBackupUTC = now + Long.parseLong(config.getProperty("ftpbackups.failintervalms"));
		}
	}
}
