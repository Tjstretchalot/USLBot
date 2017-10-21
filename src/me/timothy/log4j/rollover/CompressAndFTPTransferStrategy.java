package me.timothy.log4j.rollover;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescription;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "CompressAndFTPTransferStrategy", category=Core.CATEGORY_NAME, printObject=true)
public class CompressAndFTPTransferStrategy implements RolloverStrategy {

	@Override
	public RolloverDescription rollover(RollingFileManager manager) throws SecurityException {
		return new CompressAndFTPTransfer(manager.getFileName());
	}
	
	@PluginFactory
	public static CompressAndFTPTransferStrategy createStrategy() {
		return new CompressAndFTPTransferStrategy();
	}
}
