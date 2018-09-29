package me.timothy.bots;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.jreddit.info.ModAction;

/**
 * The goal of this class is to take in a single handled mod action and determine
 * what we should do with it. This must be able to handle recieving the same mod
 * action multiple times in a row without duplicating database rows.
 * 
 * @author Timothy
 */
public class USLModActionProcessor {
	private static boolean extremeTrace = false;
	
	private static Logger logger = LogManager.getLogger();
	
	/**
	 * Takes the given mod action and stores it in the database, if appropriate to do so. This
	 * will process the mod action into a ban history or unban history if it fits those.
	 * 
	 * If it has already seen the mod action, this will do nothing.
	 * 
	 * @param bot the bot
	 * @param database the database
	 * @param config file config  / settings
	 * @param ma the action to process
	 */
	public static void processModAction(Bot bot, USLDatabase database, USLFileConfiguration config, ModAction ma) {
		// only update extreme trace at the start
		extremeTrace = config.getProperty("general.ma_processor_extreme_trace").equals("true");
		
		extremeTraceLog("start");
		extremeTraceLog("  ma = [ModAction id=%s, action=%s, desc=%s, det=%s, utc=%d]", ma.id(), ma.action(), ma.description(), ma.details(), ma.createdUTC());
		realProcessModAction(bot, database, config, ma);
		extremeTraceLog("end");
		
	}
	
	private static void realProcessModAction(Bot bot, USLDatabase database, USLFileConfiguration config, ModAction ma) {
		if(haveSeen(database, ma)) {
			extremeTraceLog("  already in database = already seen");
			return;
		}
		
		extremeTraceLog("  switching on ma.action()=\"%s\"", ma.action());
		switch(ma.action()) {
		case "banuser":
			processBanUser(bot, database, config, ma);
			break;
		case "unbanuser":
			processUnbanUser(bot, database, config, ma);
			break;
		}
	}
	
	private static void processBanUser(Bot bot, USLDatabase database, USLFileConfiguration config, ModAction ma) {
		extremeTraceLog("  detected this is a new ban by %s on %s", ma.mod(), ma.targetAuthor());
		
		HandledModAction hma = saveModActionToDB(database, ma);

		Person mod = database.getPersonMapping().fetchOrCreateByUsername(ma.mod());
		Person banned = database.getPersonMapping().fetchOrCreateByUsername(ma.targetAuthor());
		
		
		BanHistory history = new BanHistory(-1, mod.id, banned.id, hma.id, ma.description(), ma.details());
		database.getBanHistoryMapping().save(history);
		
		// THE REST IS JUST LOGGING
		
		extremeTraceLog("  saved %s", history.toString());
		
		logger.printf(Level.INFO, "Detected that %s (id=%d) banned %s (id=%d) on subreddit %s (id=%d); description=\"%s\", details=\"%s\"",
				mod.username, mod.id, banned.username, banned.id, database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID).subreddit,
				hma.monitoredSubredditID, history.banDescription, history.banDetails);
		
		extremeTraceLog("  scanning for matching unbans...");
		List<UnbanHistory> matching = database.getUnbanHistoryMapping().fetchUnbanHistoriesByPersonAndSubreddit(banned.id, hma.monitoredSubredditID);
		for(UnbanHistory ubh : matching) {
			HandledModAction ubhHma = database.getHandledModActionMapping().fetchByID(ubh.handledModActionID);
			
			if(ubhHma.occurredAt.after(hma.occurredAt)) {
				extremeTraceLog("    found matching: %s (hma=%s)", ubh.toString(), ubhHma.toString());
				logger.printf(Level.INFO, "  Note: %s was unbanned by %s on %s so this ban is not in effect",
						database.getPersonMapping().fetchByID(ubh.unbannedPersonID).username, 
						database.getPersonMapping().fetchByID(ubh.modPersonID).username, 
						ubhHma.occurredAt.toString());
			}
		}
		
		if(matching.size() == 0) {
			extremeTraceLog("    no matching unbans");
		}
	}
	
	private static void processUnbanUser(Bot bot, USLDatabase database, USLFileConfiguration config, ModAction ma) {
		extremeTraceLog("  detected this is %s unbanning %s", ma.mod(), ma.targetAuthor());
		
		HandledModAction hma = saveModActionToDB(database, ma);

		Person mod = database.getPersonMapping().fetchOrCreateByUsername(ma.mod());
		Person unbanned = database.getPersonMapping().fetchOrCreateByUsername(ma.targetAuthor());
		
		UnbanHistory history = new UnbanHistory(-1, mod.id, unbanned.id, hma.id);
		database.getUnbanHistoryMapping().save(history);
		
		// THE REST IS JUST LOGGING
		
		extremeTraceLog("  saved %s", history.toString());
		
		logger.printf(Level.INFO, "Detected that %s (id=%d) unbanned %s (id=%d) on subreddit %s (id=%d)", 
				mod.username, mod.id, unbanned.username, unbanned.id, 
				database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID).subreddit, 
				hma.monitoredSubredditID);
		
		
		extremeTraceLog("  scanning for matching bans...");
		List<BanHistory> matching = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(unbanned.id, hma.monitoredSubredditID);
		for(BanHistory bh : matching) {
			HandledModAction bhHma = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
			
			if(bhHma.occurredAt.before(hma.occurredAt)) {
				extremeTraceLog("    found matching: %s (hma = %s)", bh.toString(), bhHma.toString());
				logger.printf(Level.INFO, "  This could be related to %s banning %s at %s - desc: \"%s\", details: \"%s\"",
						database.getPersonMapping().fetchByID(bh.modPersonID).username, database.getPersonMapping().fetchByID(bh.bannedPersonID).username,
						bhHma.occurredAt.toString(), bh.banDescription, bh.banDetails);
			}
		}
		
		if(matching.size() == 0) {
			extremeTraceLog("  no matching bans!");
			logger.printf(Level.INFO, "  There are no bans that we know of that might correspond with this.");
		}
	}
	
	private static HandledModAction saveModActionToDB(USLDatabase database, ModAction ma) {
		Timestamp occurredAt = new Timestamp((long)(ma.createdUTC() * 1000));
		extremeTraceLog("  saving mod action to file; subreddit = %s, occurredAt=%s", ma.subreddit(), occurredAt.toString());
		
		MonitoredSubreddit ms = database.getMonitoredSubredditMapping().fetchByName(ma.subreddit());
		if(ms == null)
		{
			extremeTraceLog("That's strange... we got a mod action on a non monitored subreddit?");
			logger.printf(Level.ERROR, "saveModActionToDB with mod action id=%s on subreddit %s is NOT monitored", ma.id(), ma.subreddit());
			throw new RuntimeException("subreddit " + ma.subreddit() + " is not monitored");
		}
		HandledModAction hma = new HandledModAction(-1, ms.id, ma.id(), occurredAt);
		database.getHandledModActionMapping().save(hma);
		return hma;
	}
	
	private static void extremeTraceLog(String msg, Object... args) {
		if(extremeTrace) {
			logger.printf(Level.TRACE, msg, args);
		}
	}
	
	private static boolean haveSeen(USLDatabase db, ModAction ma) {
		return db.getHandledModActionMapping().fetchByModActionID(ma.id()) != null;
	}
}
