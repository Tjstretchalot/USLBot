package me.timothy.bots;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.json.simple.parser.ParseException;

import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.memory.TraditionalScammerHandlerResult;
import me.timothy.bots.memory.UnbanRequestResult;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.memory.UserUnbanInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubredditTraditionalListStatus;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;
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
	protected USLPropagator propagator;
	protected USLPropagatorManager propagatorManager;
	protected USLUnbanRequestHandler unbanRequestHandler;
	protected USLTraditionalScammerHandler scammerHandler;
	protected USLRegisterAccountRequestManager raqManager;
	protected USLResetPasswordRequestManager rprManager;
	
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
		propagator = new USLPropagator(database, config);
		propagatorManager = new USLPropagatorManager(database, config, propagator, (result) -> handlePropagateResult(result));
		unbanRequestHandler = new USLUnbanRequestHandler(database, config, (sub, person) -> isModerator(sub, person));
		scammerHandler = new USLTraditionalScammerHandler(database, config);
		raqManager = new USLRegisterAccountRequestManager(database, config);
		rprManager = new USLResetPasswordRequestManager(database, config);
	}

	@Override
	protected void doLoop() throws IOException, ParseException, java.text.ParseException {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		al.clear();
		
		al.append("{SIGSTART}");
		
		al.append("Considering relogging in..");
		logger.trace("Considering relogging in..");
		maybeLoginAgain();
		
		logger.trace("Updating tracked subreddits..");
		updateTrackedSubreddits();
		
		al.append("Sending out account request messages..");
		logger.trace("Sending out account request messages..");
		sendAccountRequestMessages();
		
		al.append("Sending out reset password messages..");
		logger.trace("Sending out reset password messages..");
		sendResetPasswordMessages();
		
		al.append("Checking personal messages..");
		logger.trace("Checking personal messages..");
		scanPersonalMessages();
		
		al.append("Handling unban requests..");
		logger.trace("Handling unban requests..");
		handleUnbanRequests();
		
		al.append("Propagating people that are on the traditional scammer list..");
		logger.trace("Handling people on old style list..");
		handleTraditionalScammers();
		
		al.append("Scanning for new mod actions on monitored subreddits..");
		logger.trace("Scanning for new ban reports..");
		scanForBans();
		
		al.append("Propagating bans to monitored subreddits..");
		logger.trace("Propagating bans..");
		propagateBans();
		
		al.append("Considering backing up database..");
		logger.trace("Considering backing up database..");
		considerBackupDatabase();
		al = ((USLDatabase)database).getActionLogMapping();
		
		al.append("Waiting for a bit..");
	}


	/**
	 * Send out the list of user pms to confirm that someone is an
	 * owner of an account
	 */
	protected void sendAccountRequestMessages() {
		List<UserPMInformation> userPms = raqManager.sendRegisterAccountRequests();
		
		handleUserPMs(userPms);
	}

	/**
	 * Send out the list of user pms to confirm that someone is the
	 * owner of an account for the purpose of resetting their usl password
	 */
	protected void sendResetPasswordMessages() {
		List<UserPMInformation> userPms = rprManager.sendResetPasswordRequests();
		
		handleUserPMs(userPms);
	}
	
	/**
	 * Update the list of tracked subreddits from the database.
	 */
	protected void updateTrackedSubreddits() {
		USLDatabase db = (USLDatabase) database;
		
		monitoredSubreddits = db.getMonitoredSubredditMapping().fetchAll();
		bot.setSubreddit(String.join("+", monitoredSubreddits.stream().map(ms -> ms.subreddit).collect(Collectors.toList())));
	}

	/**
	 * Go through all the unban requests and handle them.
	 */
	protected void handleUnbanRequests() {
		USLDatabase db = (USLDatabase) database;
		while(true) {
			List<UnbanRequest> unbanRequests = db.getUnbanRequestMapping().fetchUnhandled(50);
			if(unbanRequests.isEmpty())
				break;
			
			logger.printf(Level.DEBUG, "Handling %d unban requests (requested up to 50)", unbanRequests.size());
			
			for(UnbanRequest unbanReq : unbanRequests) {
				Person mod = db.getPersonMapping().fetchByID(unbanReq.modPersonID);
				Person toUnban = db.getPersonMapping().fetchByID(unbanReq.bannedPersonID);
				
				logger.printf(Level.TRACE, "Handling request by %s to unban %s", mod.username, toUnban.username);
				UnbanRequestResult result = unbanRequestHandler.handleUnbanRequest(unbanReq);
				
				handleUnbanRequestResult(unbanReq, result, mod, toUnban);
			}
		}
	}
	
	/**
	 * Do what needs to be done as described in the result
	 * 
	 * @param request the request that was made
	 * @param result what needs to be done
	 * @param mod person with id request.modPersonID
	 * @param toUnban person with id request.bannedPersonID
	 * @return if we needed to interact with reddit
	 */
	protected boolean handleUnbanRequestResult(UnbanRequest request, UnbanRequestResult result, Person mod, Person toUnban) {
		boolean didSomething = false;
		didSomething = handleUnbans(result.unbans) || didSomething;
		didSomething = handleModmailPMs(result.modmailPMs) || didSomething;
		didSomething = handleUserPMs(result.userPMs) || didSomething;

		USLDatabase db = (USLDatabase) database;
		if(result.scammerToRemove != null) {
			Person asPerson = db.getPersonMapping().fetchByID(result.scammerToRemove.personID);
			logger.printf(Level.INFO, "Removing %s from the Traditional Scammer List", asPerson.username);
			
			db.getTraditionalScammerMapping().deleteByPersonID(asPerson.id);
		}
		
		request.invalid = result.invalid;
		request.handledAt = new Timestamp(System.currentTimeMillis());
		db.getUnbanRequestMapping().save(request);
		
		return didSomething;
	}
	
	/**
	 * Go through each subreddit and see if we need to handle some traditional
	 * scammers
	 */
	protected void handleTraditionalScammers() {
		USLDatabase db = (USLDatabase) database;
		ActionLogMapping al = db.getActionLogMapping();
		for(MonitoredSubreddit sub : monitoredSubreddits) {
			al.append(String.format("Propagating traditional scammers to {link sub %d}..", sub.id));
			SubredditTraditionalListStatus status = db.getSubredditTraditionalListStatusMapping().fetchBySubredditID(sub.id);
			
			if(status == null) {
				logger.printf(Level.DEBUG, "No traditional list status for /r/%s - generating", sub.subreddit);
				status = new SubredditTraditionalListStatus(-1, sub.id, null, null);
				db.getSubredditTraditionalListStatusMapping().save(status);
			}
			
			List<TraditionalScammer> toHandle;
			if(status.lastHandledID == null) {
				toHandle = db.getTraditionalScammerMapping().fetchEntriesAfterID(-1, 10);
			}else {
				toHandle = db.getTraditionalScammerMapping().fetchEntriesAfterID(status.lastHandledID, 10);
			}
			
			int numRedditInteractions = 0;
			int loopCounter = 0;
			while(numRedditInteractions < 3 && toHandle.size() > 0 && loopCounter < 10) {
				loopCounter++;
				for(TraditionalScammer scammer : toHandle) {
					Person scammerPerson = db.getPersonMapping().fetchByID(scammer.personID);
					logger.printf(Level.TRACE, "Considering the traditional scammer %s on /r/%s", scammerPerson.username, sub.subreddit);
					TraditionalScammerHandlerResult result = scammerHandler.handleTraditionalScammer(scammer, sub);
					
					if(handleTraditionalScammerHandlerResult(scammer, sub, result)) {
						numRedditInteractions++;
					}
					
					status.lastHandledAt = new Timestamp(System.currentTimeMillis());
					status.lastHandledID = scammer.id;
					db.getSubredditTraditionalListStatusMapping().save(status);
					
					if(numRedditInteractions >= 3)
						break;
				}
				
				if(numRedditInteractions >= 3) {
					logger.printf(Level.DEBUG, "Detected it's requiring a lot of reddit interactions to update /r/%s on the traditional scammer list, postponing this until later", sub.subreddit);
					break;
				}

				if(loopCounter >= 10) {
					logger.printf(Level.DEBUG, "Detected we've done a lot of database interactions while updating /r/%s on the traditional scammer list, postponing this until later", sub.subreddit);
					break;
				}
				
				toHandle = db.getTraditionalScammerMapping().fetchEntriesAfterID(status.lastHandledID, 10);
			}
		}
	}
	
	/**
	 * Do what needs to be done as specified in the handler result
	 * @param scammer the scammer that was handled
	 * @param sub the subreddit it was handled on
	 * @param result what to do
	 * @return if a reddit interaction was required
	 */
	protected boolean handleTraditionalScammerHandlerResult(TraditionalScammer scammer, MonitoredSubreddit sub,
			TraditionalScammerHandlerResult result) {
		logger.printf(Level.TRACE, "Handling a TradScamRes with %d bans, %d modmail pms, and %d user pms", result.bans.size(), result.modmailPMs.size(), result.userPMs.size());
		
		boolean didSomething = false;
		didSomething = handleBans(result.bans) || didSomething;
		didSomething = handleModmailPMs(result.modmailPMs) || didSomething;
		didSomething = handleUserPMs(result.userPMs) || didSomething;
		return didSomething;
	}
	

	/**
	 * Loops through the subreddits and scans them for bans
	 * @see #scanSubForBans(MonitoredSubreddit)
	 */
	protected void scanForBans() {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		for(MonitoredSubreddit sub : monitoredSubreddits) {
			al.append(String.format("Scanning {link subreddit %d} for any new mod actions..", sub.id));
			logger.trace("Scanning " + sub.subreddit + " for any new bans..");
			scanSubForBans(sub);
		}
	}
	
	/**
	 * Scans a particular moderated subreddits moderation log for any bans. Uses
	 * the SubredditModqueueProgressMapping in order to ensure no work is duplicated
	 * and that the subreddit is completely scanned.
	 * 
	 * @param sub the subreddit to scan
	 * @return true if we have the entire current history of the subreddit, false otherwise
	 */
	protected void scanSubForBans(MonitoredSubreddit sub) {
		USLDatabase db = (USLDatabase) database;
		
		SubredditModqueueProgress progress = db.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		
		if(progress == null) {
			logger.warn("SubredditModqueueProgress was not found for subreddit " + sub.subreddit + ", autogenerating!");
			progress = new SubredditModqueueProgress(-1, sub.id, true, null, null, null, null);
			db.getSubredditModqueueProgressMapping().save(progress);
		}
		
		// no more than 3 pages at a time
		for(int page = 0; page < 3; page++) {
			if(progress.searchForward) {
				scanSubForBansForwardSearch(sub, progress);
			}else {
				long now = System.currentTimeMillis();
				boolean finished = scanSubForBansReverseSearch(sub, progress);
				
				if(finished) {
					progress.lastTimeHadFullHistory = new Timestamp(now);
					db.getSubredditModqueueProgressMapping().save(progress);
					return;
				}
			}
		}
		
		progress.lastTimeHadFullHistory = null;
		db.getSubredditModqueueProgressMapping().save(progress);
	}
	
	/**
	 * Scans one page of a subreddits modactions using a forward search technique.
	 * 
	 * @param sub the subreddit to search
	 * @param progress current progress information
	 */
	protected void scanSubForBansForwardSearch(MonitoredSubreddit sub, SubredditModqueueProgress progress) {
		USLDatabase db = (USLDatabase) database;
		HandledModAction latestHandledModAction = null;
		if(progress.latestHandledModActionID != null) {
			latestHandledModAction = db.getHandledModActionMapping().fetchByID(progress.latestHandledModActionID);
		}
		
		String after = latestHandledModAction != null ? latestHandledModAction.modActionID : null;
		logger.trace("Fetching moderator log for /r/" + sub.subreddit + ", before=null, after=" + after);
		Listing bans = getSubActions(sub, null, after);
		logger.trace("Got " + bans.numChildren() + " results");
		sleepFor(2000);
		
		HandledModAction afterAction = null;
		HandledModAction earliestInTimeAction = null;
		HandledModAction latestInTimeAction = null;
		for(int i = 0; i < bans.numChildren(); i++) {
			Thing child = bans.getChild(i);
			if(!(child instanceof ModAction)) {
				logger.warn("Got weird child from getSubBans: type=" + child.getClass().getCanonicalName());
				continue;
			}
			
			ModAction action = (ModAction) child;
			String prefix = "Child " + i + " [id=" + action.id() + "] ";
			HandledModAction handled = db.getHandledModActionMapping().fetchByModActionID(action.id());
			if(handled != null) {
				logger.trace(prefix + " we had already seen");
				continue;
			}else {
				handled = new HandledModAction(-1, sub.id, action.id(), new Timestamp((long)(action.createdUTC() * 1000)));
			
				db.getHandledModActionMapping().save(handled);
	
				if(action.action().equalsIgnoreCase("banuser")) {
					logger.trace(prefix + " was a new ban, saving it then will dump it");
					BanHistory banHistory = createAndSaveBanHistoryFromAction(db, sub, action, handled);
					logger.printf(Level.INFO, "Detected new ban: %s", banHistory.toString());
				}else if(action.action().equalsIgnoreCase("unbanuser")) {
					logger.trace(prefix + " was a new unban, saving it then will dump it");
					UnbanHistory unbanHistory = createAndSaveUnbanHistoryFromAction(db, sub, action, handled);
					logger.printf(Level.INFO, "Detected new unban: %s", unbanHistory.toString());
				}else {
					logger.trace(prefix + " was a " + action.action() + "; ignoring it");
				}
			}

			if(bans.after() != null && action.id().equalsIgnoreCase(bans.after())) {
				afterAction = handled;
			}
			
			if(earliestInTimeAction == null || handled.occurredAt.before(earliestInTimeAction.occurredAt)) {
				earliestInTimeAction = handled;
			}
			
			if(latestInTimeAction == null || handled.occurredAt.after(latestInTimeAction.occurredAt)) {
				latestInTimeAction = handled;
			}
		}
		
		if(progress.newestHandledModActionID == null) {
			if(latestInTimeAction == null) {
				logger.error("Either there are 0 bans in " + sub.subreddit + " or something has gone horribly wrong. Pming usl");
				sendModmail("universalscammerlist", "USLBot Unrecoverable Errors", "When monitoring /r/" + sub.subreddit + " I "
						+ "found no bans. I think it's more likely something went wrong.\n"
						+ "\n"
						+ "---\n"
						+ "\n"
						+ "Here is a dump of the Listing result that was returned:\n"
						+ "\n"
						+ bans.toString());
				logger.error("Initiating shutdown..");
				System.exit(1);
			}else {
				progress.newestHandledModActionID = latestInTimeAction.id;
			}
		}
		
		if(bans.after() != null) {
			if(afterAction != earliestInTimeAction) { 
				logger.warn("Listing after() is not equal to the ban in the listing with the least recent timestamp!");
				logger.warn(bans.toString());
			}
			
			if(afterAction == null) {
				logger.warn("Listing after() references an id that isn't in the listing!");
				logger.warn(bans.toString());
				
				if(earliestInTimeAction != null) {
					progress.latestHandledModActionID = earliestInTimeAction.id; 
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
							+ "I was using after=" + (earliestInTimeAction != null ? earliestInTimeAction.modActionID : null));
					logger.error("Initiating shutdown..");
					System.exit(1);
				}
			}else {
				progress.latestHandledModActionID = afterAction.id;
			}
			
			db.getSubredditModqueueProgressMapping().save(progress);
		}else {
			logger.info("Reached end of /r/" + sub.subreddit + " modqueue actions.");
			
			if(earliestInTimeAction != null) {
				progress.latestHandledModActionID = earliestInTimeAction.id;
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
		
		if(progress.newestHandledModActionID == null) {
			logger.error("scanSubForBansReverseSearch and progress.newestBanHistory id == null. This is not going to work.");
			logger.error("Initiating shutdown..");
			System.exit(1);
		}
		
		HandledModAction newestModAction = db.getHandledModActionMapping().fetchByID(progress.newestHandledModActionID);
		
		logger.trace("Fetching subreddit " + sub.subreddit + "s modqueue, before=" + newestModAction.modActionID + ", after=null");
		Listing beforeNewest = getSubActions(sub, newestModAction.modActionID, null);
		logger.trace("Got beforeNewest = " + beforeNewest);
		sleepFor(2000);
		
		HandledModAction mostRecentHandled = null;
		HandledModAction beforeHandled = null;
		for(int i = 0; i < beforeNewest.numChildren(); i++) {
			Thing thing = beforeNewest.getChild(i);
			if(!(thing instanceof ModAction)) {
				logger.warn("scanSubForBansReverseSearch got weird thing type=" + thing.getClass().getCanonicalName());
				continue;
			}
			
			ModAction action = (ModAction) thing;
			boolean alreadySaw = false;
			HandledModAction handled = db.getHandledModActionMapping().fetchByModActionID(action.id());
			
			String prefix = "Child " + i  + " [id=" + action.id() + "]";
			if (handled != null) {
				logger.trace(prefix + " we had already seen");
				alreadySaw = true;
			}
			
			if(!alreadySaw) {
				handled = new HandledModAction(-1, sub.id, action.id(), new Timestamp((long)(action.createdUTC() * 1000)));
				db.getHandledModActionMapping().save(handled);
				
				
				if(action.action().equalsIgnoreCase("banuser")) {
					logger.trace(prefix + " was a ban, saving then dumping");
					BanHistory banHistory = createAndSaveBanHistoryFromAction(db, sub, action, handled);
					logger.printf(Level.INFO, "Detected new ban: %s", banHistory.toString());
				}else if(action.action().equalsIgnoreCase("unbanuser")) {
					logger.trace(prefix + " was an unban, saving then dumping");
					UnbanHistory unbanHistory = createAndSaveUnbanHistoryFromAction(db, sub, action, handled);
					logger.printf(Level.INFO, "Detected new unban: %s", unbanHistory.toString());
				}else {
					logger.trace(prefix + " was a " + action.action() + "; ignoring");
				}
			}
			
			if(beforeNewest.before() != null && action.id().equalsIgnoreCase(beforeNewest.before())) {
				beforeHandled = handled;
			}
			
			if(mostRecentHandled == null || handled.occurredAt.after(mostRecentHandled.occurredAt)) {
				mostRecentHandled = handled;
			}
		}
		
		if(beforeNewest.before() != null) {
			if(beforeHandled == null) {
				logger.error("beforeNewest.before() was not null and in the listing! I don't know how to recover!");
				logger.error("Shutting down..");
				System.exit(1);
			}
			
			if(beforeHandled != mostRecentHandled) {
				logger.warn("beforeNewest.before() is not the most recent ban in the listing. this should be recoverable but it's weird");
			}
			
			logger.trace("Using reddits suggested before target of " + beforeNewest.before() + " which we found");
			progress.newestHandledModActionID = beforeHandled.id;
			db.getSubredditModqueueProgressMapping().save(progress);
			return false;
		}else {
			if(mostRecentHandled == null) {
				logger.trace("beforeNewest.before() is null and mostRecentHandled is null, this means that we got no children.");
				return true; // got an empty listing. no new bans since last check
			}
			
			logger.trace("Reddit gave us before=null, so using the one with the most recent timestamp " + timeToPretty(mostRecentHandled.occurredAt.getTime()) + " (id=" + mostRecentHandled.modActionID +")");
			progress.newestHandledModActionID = mostRecentHandled.id;
			db.getSubredditModqueueProgressMapping().save(progress);
			return true;
		}
	}
	
	/**
	 * Create a ban history from the specified mod action, save it to the database, and return it.
	 * 
	 * @param db database casted
	 * @param sub the subreddit it was fetched from
	 * @param action the action to save
	 * @param handled the handled mod action corresponding with action
	 * @return the ban history
	 */
	protected BanHistory createAndSaveBanHistoryFromAction(USLDatabase db, MonitoredSubreddit sub, ModAction action, HandledModAction handled) {
		Person mod = db.getPersonMapping().fetchOrCreateByUsername(action.mod());
		Person banned = db.getPersonMapping().fetchOrCreateByUsername(action.targetAuthor());
		
		BanHistory previousBanOnPerson = db.getBanHistoryMapping().fetchBanHistoryByPersonAndSubreddit(banned.id, sub.id);
		if(previousBanOnPerson != null) {
			logger.info("Had information on a ban for user " + banned.username + " on subreddit " + sub.subreddit + ", but a "
					+ "newer ban has occurred on that same person on that same subreddit. That could mean that the old "
					+ "ban was temporary (old details='" + previousBanOnPerson.banDetails + "') or the person was "
							+ "unbanned and rebanned on the subreddit.");
		}
		
		BanHistory banHistory = new BanHistory(-1, mod.id, banned.id, handled.id, action.description(), action.details());
		db.getBanHistoryMapping().save(banHistory);
		return banHistory;
	}
	
	/**
	 * Create an unban history from the specified mod action, save it to the database, and return it
	 * 
	 * @param db the database
	 * @param sub the monitored subreddit the action is on
	 * @param action the action
	 * @param handled the handled mod action corresponding with action
	 * @return the unban history
	 */
	protected UnbanHistory createAndSaveUnbanHistoryFromAction(USLDatabase db, MonitoredSubreddit sub, ModAction action, HandledModAction handled) {
		Person mod = db.getPersonMapping().fetchOrCreateByUsername(action.mod());
		Person unbanned = db.getPersonMapping().fetchOrCreateByUsername(action.targetAuthor());
		
		UnbanHistory unbanHistory = new UnbanHistory(-1, mod.id, unbanned.id, handled.id);
		db.getUnbanHistoryMapping().save(unbanHistory);
		return unbanHistory;
	}
	
	/**
	 * Propagates bans that were scanned by scanSubForBans. Uses the SubredditPropagateStatusMapping
	 * in order to ensure that no work is duplicated and that all bans are propagated to all subreddits.
	 * 
	 * Uses the USLBanHistoryPropagator to decide how to propagate for each subreddit.
	 */
	protected void propagateBans() {
		propagatorManager.managePropagating(monitoredSubreddits);
	}
	
	/**
	 * Do the things that the result says to do.
	 * 
	 * @param result the result
	 * @return if any reddit requests were done
	 */
	protected boolean handlePropagateResult(PropagateResult result) {
		boolean didSomething = false;
		didSomething = handleBans(result.bans) || didSomething;
		didSomething = handleModmailPMs(result.modmailPMs) || didSomething;
		didSomething = handleUserPMs(result.userPMs) || didSomething;
		return didSomething;
	}
	
	/**
	 * Handle the list of bans that need to be done
	 * 
	 * @param bans the bans to do
	 * @return if we did something with reddit
	 */
	protected boolean handleBans(List<UserBanInformation> bans) {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		boolean didSomething = false;
		for(UserBanInformation ban : bans) {
			al.append(String.format("Banning {link person %d} on {link subreddit %d}..", ban.person.id, ban.subreddit.id));
			logger.printf(Level.INFO, "Banning %s on %s..", ban.person.username, ban.subreddit.subreddit);
			
			handleBanUser(ban);
			sleepFor(2000);
			didSomething = true;
		}
		return didSomething;
	}
	
	/**
	 * Handle the list of unbans that need to be done
	 * 
	 * @param unbans the unbans to do
	 * @return if we did something with reddit
	 */
	protected boolean handleUnbans(List<UserUnbanInformation> unbans) {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		boolean didSomething = false;
		for(UserUnbanInformation unban : unbans) {
			al.append(String.format("Unbanning {link person %d} on {link subreddit %d}..", unban.person.id, unban.subreddit.id));
			logger.printf(Level.INFO, "Unbanning %s on %s..", unban.person.username, unban.subreddit.subreddit);
			
			handleUnbanUser(unban);
			sleepFor(2000);
			didSomething = true;
		}
		return didSomething;
	}
	
	/**
	 * Handle the list of modmail pms that need to be done
	 * 
	 * @param modmailPMs the pms to send out
	 * @return if we did something with reddit
	 */
	protected boolean handleModmailPMs(List<ModmailPMInformation> modmailPMs) {
		boolean didSomething = false;
		for(ModmailPMInformation modPm : modmailPMs) {
			logger.printf(Level.INFO, "Sending some modmail to %s (title=%s)", modPm.subreddit, modPm.title);
			logger.trace("body=" + modPm.body);
			sendModmail(modPm.subreddit.subreddit, modPm.title, modPm.body);
			sleepFor(2000);
			didSomething = true;
		}
		return didSomething;
	}
	
	/**
	 * Handle the list of user pms that need to be done
	 * 
	 * @param userPMs the pms to send out
	 * @return if we did something with reddit
	 */
	protected boolean handleUserPMs(List<UserPMInformation> userPMs) {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		boolean didSomething = false;
		for(UserPMInformation userPm : userPMs) {
			logger.printf(Level.INFO, "Sending some mail to %s (title=%s)", userPm.person.username, userPm.title);
			al.append(String.format("Sending a personal message to {link user %d} (the title is %s)..", userPm.person.id, userPm.title));
			Boolean succ = sendMessage(userPm.person.username, userPm.title, userPm.body);
			
			if(succ != null && succ.booleanValue()) {
				userPm.callbacks.forEach((cb) -> cb.run());
			}
			
			sleepFor(2000);
			didSomething = true;
		}
		return didSomething;
	}
	
	/**
	 * Bans the specified user from the specified subreddit. This is just 
	 * a wrapper around handleBanUser(subreddit, username, message, reason, note)
	 * 
	 * @param banInfo the information regarding the ban
	 */
	protected void handleBanUser(UserBanInformation banInfo) {
		super.handleBanUser(banInfo.subreddit.subreddit, banInfo.person.username, banInfo.banMessage, banInfo.banReason, banInfo.banNote);
	}
	
	/**
	 * Unbans the specified user from the specified subreddit.
	 * This is just a wrapper around handleUnbanUser(suberddit, username)
	 * 
	 * @param unbanInfo the information regarding the unban
	 */
	protected void handleUnbanUser(UserUnbanInformation unbanInfo) {
		super.handleUnbanUser(unbanInfo.subreddit.subreddit, unbanInfo.person.username);
	}
	/**
	 * Sends modmail to the specified subreddit
	 * @param sub subreddit to message
	 * @param title the title of the message
	 * @param body the body of the message
	 */
	protected void sendModmail(String sub, String title, String body) {
		sendMessage("/r/" + sub, title, body);
	}
	
	/**
	 * Wrapper around {@link me.timothy.jreddit.RedditUtils#getModeratorLog(String, String, String, String, me.timothy.jreddit.User)}
	 * Awaiting changes to https://www.reddit.com/r/redditdev/comments/78gj7b/api_can_the_rsubaboutlogjson_endpoint_allow/ this
	 * returns all modactions for the subreddit so that you can parse both banuser and unbanuser actions.
	 * 
	 * @param sub monitored subreddit
	 * @param before before (wont return this id)
	 * @param after after (wont return this id)
	 * @return the listing
	 */
	protected Listing getSubActions(MonitoredSubreddit sub, String before, String after) {
		return new Retryable<Listing>("get subreddit moderator log", maybeLoginAgainRunnable) {
			@Override
			protected Listing runImpl() throws Exception {
				return RedditUtils.getModeratorLog(sub.subreddit, null, null, before, after, bot.getUser());
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
