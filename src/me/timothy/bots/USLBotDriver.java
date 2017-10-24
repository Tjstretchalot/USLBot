package me.timothy.bots;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.json.simple.parser.ParseException;

import me.timothy.bots.memory.BanHistoryPropagateResult;
import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubredditPropagateStatus;
import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMSummon;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.ModAction;
import me.timothy.jreddit.info.Thing;

/**
 * The bot driver for the universal scammer list bot. Contains
 * the high-level code for running the bot after it has been
 * initialized.
 * 
 * @author Timothy Moore
 */
public class USLBotDriver extends BotDriver {
	protected List<MonitoredSubreddit> monitoredSubreddits;
	protected USLDatabaseBackupManager backupManager;
	protected USLBanHistoryPropagator propagator;
	
	/**
	 * Creates a new bot driver that has the specified context to
	 * run in.
	 * 
	 * @param database database
	 * @param config file level configuration
	 * @param bot the bot, not logged in
	 * @param commentSummons the list of comment summons
	 * @param pmSummons the list of pm summons
	 * @param submissionSummons the list of submission summons
	 */
	public USLBotDriver(USLDatabase database, USLFileConfiguration config, Bot bot, CommentSummon[] commentSummons,
			PMSummon[] pmSummons, LinkSummon[] submissionSummons) {
		super(database, config, bot, commentSummons, pmSummons, submissionSummons);
		backupManager = new USLDatabaseBackupManager(database, config);
		propagator = new USLBanHistoryPropagator(database, config);
	}

	@Override
	protected void doLoop() throws IOException, ParseException, java.text.ParseException {
		logger.trace("Considering relogging in..");
		maybeLoginAgain();
		
		logger.trace("Updating tracked subreddits..");
		updateTrackedSubreddits();
		
		logger.trace("Scanning for new ban reports..");
		scanForBans();
		
		logger.trace("Propagating bans..");
		propagateBans();
		
		logger.trace("Considering backing up database..");
		considerBackupDatabase();
		
		logger.trace("Sleeping for a while..");
		sleepFor(30000);
	}

	/**
	 * Update the list of tracked subreddits from the database.
	 */
	protected void updateTrackedSubreddits() {
		USLDatabase db = (USLDatabase) database;
		
		monitoredSubreddits = db.getMonitoredSubredditMapping().fetchAll();
		bot.setSubreddit(String.join("+", monitoredSubreddits.toArray(new String[]{})));
	}

	/**
	 * Loops through the subreddits and scans them for bans
	 * @see #scanSubForBans(MonitoredSubreddit)
	 */
	protected void scanForBans() {
		for(MonitoredSubreddit sub : monitoredSubreddits) {
			logger.trace("Scanning " + sub.subreddit + " for any new bans...");
			scanSubForBans(sub);
		}
	}
	
	/**
	 * Scans a particular moderated subreddits moderation log for any bans. Uses
	 * the SubredditModqueueProgressMapping in order to ensure no work is duplicated
	 * and that the subreddit is completely scanned.
	 * 
	 * @param sub the subreddit to scan
	 */
	protected void scanSubForBans(MonitoredSubreddit sub) {
		USLDatabase db = (USLDatabase) database;
		
		SubredditModqueueProgress progress = db.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		
		if(progress == null) {
			logger.warn("SubredditModqueueProgress was not found for subreddit " + sub.subreddit + ", autogenerating!");
			progress = new SubredditModqueueProgress(-1, sub.id, true, null, null, null);
			db.getSubredditModqueueProgressMapping().save(progress);
		}
		
		// no more than 3 pages at a time
		for(int page = 0; page < 3; page++) {
			if(progress.searchForward) {
				scanSubForBansForwardSearch(sub, progress);
			}else {
				boolean finished = scanSubForBansReverseSearch(sub, progress);
				
				if(finished)
					break;
			}
		}
	}
	
	/**
	 * Scans one page of a subreddits modactions using a forward search technique.
	 * 
	 * @param sub the subreddit to search
	 * @param progress current progress information
	 */
	protected void scanSubForBansForwardSearch(MonitoredSubreddit sub, SubredditModqueueProgress progress) {
		USLDatabase db = (USLDatabase) database;
		BanHistory latestBanHistory = null;
		if(progress.latestBanHistoryID != null) {
			latestBanHistory = db.getBanHistoryMapping().fetchByID(progress.latestBanHistoryID);
		}
		
		Listing bans = getSubBans(sub, null, latestBanHistory != null ? latestBanHistory.modActionID : null);
		sleepFor(2000);
		
		BanHistory afterBan = null;
		BanHistory latestBan = null;
		BanHistory earliestBan = null;
		for(int i = 0; i < bans.numChildren(); i++) {
			Thing child = bans.getChild(i);
			if(!(child instanceof ModAction)) {
				logger.warn("Got weird child from getSubBans: type=" + child.getClass().getCanonicalName());
				continue;
			}
			
			ModAction action = (ModAction) child;
			if(db.getBanHistoryMapping().fetchByModActionID(action.id()) != null) {
				continue;
			}

			if(action.action().equalsIgnoreCase("banuser")) {
				BanHistory banHistory = createAndSaveBanHistoryFromAction(db, sub, action);
				logger.printf(Level.INFO, "Detected new ban: %s", banHistory.toString());
				
				if(bans.after() != null && action.id().equalsIgnoreCase(bans.after())) {
					afterBan = banHistory;
				}
				
				if(latestBan == null || banHistory.occurredAt.getTime() > latestBan.occurredAt.getTime()) {
					latestBan = banHistory;
				}
				
				if(earliestBan == null || banHistory.occurredAt.getTime() < earliestBan.occurredAt.getTime()) {
					earliestBan = banHistory;
				}
			}
		}
		
		if(progress.newestBanHistoryID == null) {
			if(earliestBan == null) {
				logger.error("Either there are 0 bans in " + sub.subreddit + " or something has gone horribly wrong. Pming usl");
				sendModmail("universalscammerlist", "USLBot Unrecoverable Errors", "When monitoring /r/" + sub.subreddit + " I "
						+ "found no bans. I think it's more likely something went wrong. Shutting down.");
				logger.error("Initiating shutdown..");
				System.exit(1);
			}else {
				progress.newestBanHistoryID = earliestBan.id;
			}
		}
		
		if(bans.after() != null) {
			if(afterBan != latestBan) { 
				logger.warn("Listing after() is not equal to the ban in the listing with the latest timestamp!");
				logger.warn(bans.toString());
			}
			
			if(afterBan == null) {
				logger.warn("Listing after() references an id that isn't in the listing!");
				logger.warn(bans.toString());
				
				if(latestBan != null) {
					progress.latestBanHistoryID = latestBan.id; 
				}else {
					logger.error("Could not fallback to latest ban by timestamp; that was null too!");
					logger.error("This position is unrecoverable and will have the subreddit looping. Sending modmail to USL");
					sendModmail("universalscammerlist", "USLBot Unrecoverable Errors", "When monitoring /r/" + sub.subreddit + " I "
							+ "recieved a listing of BanActions. That listing was *completely empty*, but it returned after() not null! "
							+ "Most likely I have incorrect assumptions about how the reddit api handles pagination. "
							+ "So I will shut down until someone can check me out.\n"
							+ "\n"
							+ "---\n"
							+ "\n"
							+ "I was using after=" + latestBanHistory != null ? latestBanHistory.modActionID : null);
					logger.error("Initiating shutdown..");
					System.exit(1);
				}
			}else {
				progress.latestBanHistoryID = afterBan.id;
			}
			
			db.getSubredditModqueueProgressMapping().save(progress);
		}else {
			logger.info("Reached end of /r/" + sub.subreddit + " modqueue actions.");
			
			if(latestBan != null) {
				progress.latestBanHistoryID = latestBan.id;
			}else {
				logger.warn("The last page was empty for " + sub.subreddit + ". There is a chance this means that reddits pagination failed!");
			}
			
			progress.searchForward = false;
			db.getSubredditModqueueProgressMapping().save(progress);
		}
	}

	/**
	 * Scans one page of a subreddits modactions using a reverse search technique. Returns true if
	 * the search finished (so there are no more pages to search)
	 * 
	 * @param sub subreddit to search
	 * @param progress progress information
	 * @return if there is no more new information on sub to find right now
	 */
	protected boolean scanSubForBansReverseSearch(MonitoredSubreddit sub, SubredditModqueueProgress progress) {
		USLDatabase db = (USLDatabase) database;
		
		if(progress.newestBanHistoryID == null) {
			logger.error("scanSubForBansReverseSearch and progress.newestBanHistory id == null. This is not going to work.");
			logger.error("Initiating shutdown..");
			System.exit(1);
		}
		
		BanHistory newestBanHistory = db.getBanHistoryMapping().fetchByID(progress.newestBanHistoryID);
		
		Listing beforeNewest = getSubBans(sub, newestBanHistory.modActionID, null);
		sleepFor(2000);
		
		BanHistory earliestBan = null;
		BanHistory beforeBan = null;
		for(int i = 0; i < beforeNewest.numChildren(); i++) {
			Thing thing = beforeNewest.getChild(i);
			if(!(thing instanceof ModAction)) {
				logger.warn("scanSubForBansReverseSearch got weird thing type=" + thing.getClass().getCanonicalName());
				continue;
			}
			
			ModAction action = (ModAction) thing;
			if(db.getBanHistoryMapping().fetchByModActionID(action.id()) != null) {
				continue;
			}
			
			if(action.action().equalsIgnoreCase("banuser")) {
				BanHistory banHistory = createAndSaveBanHistoryFromAction(db, sub, action);
				logger.printf(Level.INFO, "Detected new ban: %s", banHistory.toString());
				
				if(beforeNewest.before() != null && action.id().equalsIgnoreCase(beforeNewest.before())) {
					beforeBan = banHistory;
				}
				
				if(earliestBan == null || banHistory.occurredAt.getTime() < earliestBan.occurredAt.getTime()) {
					earliestBan = banHistory;
				}
			}
		}
		
		if(beforeNewest.before() != null) {
			if(beforeBan == null) {
				logger.error("beforeNewest.before() was not null and in the listing! I don't know how to recover!");
				logger.error("Shutting down..");
				System.exit(1);
			}
			
			if(beforeBan != earliestBan) {
				logger.warn("beforeNewest.before() is not the earliest ban in the listing. this should be recoverable but it's weird");
			}
			
			progress.newestBanHistoryID = beforeBan.id;
			db.getSubredditModqueueProgressMapping().save(progress);
			return false;
		}else {
			if(earliestBan == null) {
				return true; // got an empty listing. no new bans since last check
			}
			
			progress.newestBanHistoryID = earliestBan.id;
			db.getSubredditModqueueProgressMapping().save(progress);
			return true;
		}
	}
	
	/**
	 * Create a ban history from the specified mod action, save it to the database, and return it.
	 * 
	 * @param db database casted
	 * @param action the action to save
	 * @return the ban history
	 */
	protected BanHistory createAndSaveBanHistoryFromAction(USLDatabase db, MonitoredSubreddit sub, ModAction action) {
		Person mod = db.getPersonMapping().fetchOrCreateByUsername(action.mod());
		Person banned = db.getPersonMapping().fetchOrCreateByUsername(action.targetAuthor());
		
		BanHistory previousBanOnPerson = db.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(banned.id, sub.id);
		if(previousBanOnPerson != null) {
			logger.info("Had information on a ban for user " + banned + " on subreddit " + sub.subreddit + ", but a "
					+ "newer ban has occurred on that same person on that same subreddit. That could mean that the old "
					+ "ban was temporary (old details='" + previousBanOnPerson.banDetails + "') or the person was "
							+ "unbanned and rebanned on the subreddit. Either way, replacing our old information.");
		}
		
		BanHistory banHistory = new BanHistory(previousBanOnPerson == null ? -1 : previousBanOnPerson.id, sub.id, mod.id, banned.id, action.id(), action.description(), action.details(), 
				new Timestamp((long)(action.createdUTC() * 1000)));
		db.getBanHistoryMapping().save(banHistory);
		return banHistory;
	}
	
	/**
	 * Propagates bans that were scanned by scanSubForBans. Uses the SubredditPropagateStatusMapping
	 * in order to ensure that no work is duplicated and that all bans are propagated to all subreddits.
	 * 
	 * Uses the USLBanHistoryPropagator to decide how to propagate for each subreddit.
	 */
	protected void propagateBans() {
		for(MonitoredSubreddit subreddit : monitoredSubreddits) {
			logger.trace("Propagating bans to " + subreddit.subreddit);
			propagateBansForSubreddit(subreddit);
		}
	}
	
	/**
	 * Propagates the bans for the specified subreddit. Uses SubredditPropagateStatusMapping
	 * in order to ensure no work is duplicated and that all bans are propagated to the subreddit.
	 * 
	 * @param subreddit the subreddit
	 */
	protected void propagateBansForSubreddit(MonitoredSubreddit subreddit) {
		USLDatabase db = (USLDatabase)database;
		
		SubredditPropagateStatus status = db.getSubredditPropagateStatusMapping().fetchForSubreddit(subreddit.id);
		
		if(status == null) {
			logger.warn("SubredditPropagateStatus for subreddit " + subreddit.subreddit + " was not found, autogenerating!");
			status = new SubredditPropagateStatus(-1, subreddit.id, null, null);
			db.getSubredditPropagateStatusMapping().save(status);
		}
		
		
		// we will break out early after 250 ban histories or 3 ban histories
		// requiring reddit interactions, whichever comes first
		
		for(int i = 0; i < 5; i++) {
			int idAbove = status.lastBanHistoryID == null ? 0 : status.lastBanHistoryID;
			List<BanHistory> bansToPropagate = db.getBanHistoryMapping().fetchBanHistoriesAboveIDSortedByIDAsc(idAbove, 50);
			if(bansToPropagate.isEmpty()) {
				logger.trace("Out of things to propagate for " + subreddit.subreddit);
				return;
			}
			
			int didSomethingCounter = 0;
			for(BanHistory bh : bansToPropagate) {
				logger.printf(Level.DEBUG, "Propagating bh id=%d (%d banned on %d by %d) to %s", bh.id, bh.bannedPersonID, bh.monitoredSubredditID, bh.modPersonID, subreddit.subreddit);
				
				BanHistoryPropagateResult result = propagator.propagateBan(subreddit, bh);
				if(handlePropagateResult(result)) {
					didSomethingCounter++;
				}
				
				status.lastBanHistoryID = bh.id;
				db.getSubredditPropagateStatusMapping().save(status);
				
				if(didSomethingCounter > 3) {
					logger.trace("Detected that " + subreddit.subreddit + " is taking a long time to propagate (many reddit interactions). Going onto next thing.");
					return;
				}
			}
		}
		
		logger.trace("Detected that " + subreddit.subreddit + " is taking a long time to propagate (many ban histories checked). Going onto next thing.");
	}
	
	/**
	 * Do the things that the result says to do.
	 * 
	 * @param result the result
	 * @return if any reddit requests were done
	 */
	protected boolean handlePropagateResult(BanHistoryPropagateResult result) {
		USLDatabase db = (USLDatabase)database;
		boolean didSomething = false;
		List<MonitoredSubreddit> suppressedSubredditPms = new ArrayList<>();
		for(UserBanInformation ban : result.bans) {
			BanHistory personBannedOnSubreddit = db.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(ban.person.id, ban.subreddit.id);
			if(personBannedOnSubreddit != null) {
				logger.printf(Level.TRACE, "Skipping banning %s on %s.. Already know he's banned from this: %s", 
						ban.person.username, ban.subreddit.subreddit, personBannedOnSubreddit.toString());
				suppressedSubredditPms.add(ban.subreddit);
				continue;
			}
			
			logger.printf(Level.INFO, "Banning %s on %s..", ban.person.username, ban.subreddit.subreddit);
			
			handleBanUser(ban);
			sleepFor(2000);
			didSomething = true;
		}
		
		for(ModmailPMInformation modPm : result.modmailPMs) {
			if(suppressedSubredditPms.contains(modPm.subreddit)) {
				logger.printf(Level.TRACE, "Skipping modmail pm to %s, had a ban that was suppressed", modPm.subreddit);
				continue;
			}
			
			logger.printf(Level.INFO, "Sending some modmail to %s (title=%s)", modPm.subreddit, modPm.title);
			logger.trace("body=" + modPm.body);
			sendModmail(modPm.subreddit.subreddit, modPm.title, modPm.body);
			sleepFor(2000);
			didSomething = true;
		}
		
		for(UserPMInformation userPm : result.userPMs) {
			logger.printf(Level.INFO, "Sending some mail to %s (title=%s)", userPm.person.username, userPm.title);
			sendMessage(userPm.person.username, userPm.title, userPm.body);
			sleepFor(2000);
			didSomething = true;
		}
		return didSomething;
	}
	
	/**
	 * Bans the specified user from the specified subreddit. This is just 
	 * a wrapper around handleBanUser(username, message, reason, note)
	 * 
	 * @param banInfo the information regarding the ban
	 */
	protected void handleBanUser(UserBanInformation banInfo) {
		super.handleBanUser(banInfo.person.username, banInfo.banMessage, banInfo.banReason, banInfo.banNote);
	}
	
	/**
	 * Sends modmail to the specified subreddit
	 * @param sub subreddit to message
	 * @param title the title of the message
	 * @param body the body of the mesage
	 */
	protected void sendModmail(String sub, String title, String body) {
		sendMessage("/r/" + sub, title, body);
	}
	
	/**
	 * Wrapper around {@link me.timothy.jreddit.RedditUtils#getModeratorLog(String, String, String, String, me.timothy.jreddit.User)}
	 * specific for getting bans for a subreddit inside a retryable
	 * 
	 * @param sub monitored subreddit
	 * @param before before (wont return this id)
	 * @param after after (wont return this id)
	 * @return the listing
	 */
	protected Listing getSubBans(MonitoredSubreddit sub, String before, String after) {
		return new Retryable<Listing>("get subreddit ban actions", maybeLoginAgainRunnable) {
			@Override
			protected Listing runImpl() throws Exception {
				return RedditUtils.getModeratorLog(sub.subreddit, null, "banuser", before, after, bot.getUser());
			}
		}.run();
	}
	
	/**
	 * Consider backing up the database. Delegates to USLDatabaseBackup
	 */
	protected void considerBackupDatabase() {
		backupManager.considerBackup();
	}
	
	/**
	 * Convert the specified time, as if returned from System.currentTimeMillis, to something
	 * more legible.
	 * @param timeMS time since epoch in milliseconds
	 * @return string representation
	 */
	public static String timeToPretty(long timeMS) {
		return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date(timeMS));
	}
	
	/**
	 * Convert the specified time, in seconds since epoch, to something more legible
	 * @param timeReddit time since epoch in seconds
	 * @return string representation
	 */
	public static String timeToPretty(double timeReddit) {
		return timeToPretty((long)(timeReddit * 1000));
	}
}
