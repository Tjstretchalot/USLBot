package me.timothy.log4j.rollover;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.core.appender.rolling.RolloverDescription;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;

import me.timothy.bots.USLBackupTransferManager;
import me.timothy.bots.USLBotMain;
import me.timothy.bots.USLFileConfiguration;

public class CompressAndFTPTransfer extends AbstractAction implements RolloverDescription {
	private String fileName;
	
	public CompressAndFTPTransfer(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String getActiveFileName() {
		return fileName;
	}

	@Override
	public boolean getAppend() {
		return false;
	}

	@Override
	public Action getAsynchronous() {
		return null;
	}

	@Override
	public Action getSynchronous() {
		return this;
	}

	@Override
	public boolean execute() throws IOException {
		USLFileConfiguration config = USLBotMain.mainConfig;
		if(config == null) {
			config = new USLFileConfiguration();
			config.load();
		}
		
		final String remoteLogsFolder = config.getProperty("ftpbackups.logsfolder");
		final long now = System.currentTimeMillis();
		
		File source = new File(fileName);
		File destination = new File(fileName + ".gz");
		
		if(!GzCompressAction.execute(source, destination, true))
			return false;
		
		LOGGER.info("Sending compressed log file via FTP");
		USLBackupTransferManager.backupFileFTP(config, destination,  
				USLBackupTransferManager.navigateToDirectory(remoteLogsFolder), 
				USLBackupTransferManager.chooseUnusedFilenameUsingCounter("usl-log-ending-at-" + now, "txt.gz")
				);
		
		Files.delete(destination.toPath());
		
		return true;
	}


}
