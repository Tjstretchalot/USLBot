package me.timothy.bots;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.json.simple.parser.ParseException;

import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;
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
	}

	@Override
	protected void doLoop() throws IOException, ParseException, java.text.ParseException {
		logger.trace("Considering relogging in..");
		maybeLoginAgain();
		
		logger.trace("Updating tracked subreddits..");
		updateTrackedSubreddits();
		
		logger.trace("Scanning for new ban reports..");
		scanForBans();
		
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
			logger.trace("Scanning " + sub.subreddit + " for any new actions...");
			scanSubForBans(sub);
		}
	}
	
	/**
	 * Scans a particular moderated subreddits moderation log for any bans
	 * 
	 * @param sub the subreddit to scan
	 */
	protected void scanSubForBans(MonitoredSubreddit sub) {
		final int MAX_PAGES = 5;
		
		boolean foundKnown = false;
		int pages = 0;
		String after = null;
		while(pages < MAX_PAGES && !foundKnown) {
			Listing actions = getSubBans(sub, after);
			if(actions == null) {
				break;
			}
			
			for(int i = 0; i < actions.numChildren(); i++) {
				Thing child = actions.getChild(i);
				if(!(child instanceof ModAction)) {
					logger.debug("Weird thing in mod action listing: " + child.toString());
					continue;
				}
				
				ModAction mAction = (ModAction) child;
				if(database.containsFullname(mAction.id())) {
					foundKnown = true;
					break;
				}
				
				database.addFullname(mAction.id());
				
				if(mAction.action().equals("banuser")) {
					logger.info("Handling ban id=" + mAction.id() + " by " + mAction.mod() + " targetting " + mAction.targetAuthor() + " for "+ mAction.details());
					handleSubBan(sub, mAction);
				}
			}
			
			after = actions.after();
			pages++;
		}
	}
	
	/**
	 * Handle a ban from a monitored subreddit.
	 *  
	 * @param sub the subreddit the ban was issued in
	 * @param ban the action performed 
	 */
	protected void handleSubBan(MonitoredSubreddit sub, ModAction ban) {
		if(!ban.details().equals("permanent")) {
			logger.trace("Skipping (not a permanent ban!)");
			return;
		}

		USLDatabase db = (USLDatabase)database;
		Person modPerson = db.getPersonMapping().fetchOrCreateByUsername(ban.mod());
		Person bannedPerson = db.getPersonMapping().fetchOrCreateByUsername(ban.targetAuthor());
		long now = System.currentTimeMillis();
		if(ban.mod().equalsIgnoreCase(bot.getUser().getUsername())) {
			logger.trace("Saving and moving on (its my own ban)");
			BanHistory banHistory = new BanHistory(-1, sub.id, modPerson.id, bannedPerson.id, BanHistory.BanReasonIdentifier.Propagate.id,
					ban.description(), ban.id(), false, new Timestamp(now), new Timestamp((long)(ban.createdUTC() * 1000)), new Timestamp(now));
			db.getBanHistoryMapping().save(banHistory);
			return;
		}
		
		
		logger.trace("Saving ban history..");
		BanHistory banHistory = new BanHistory(-1, sub.id, modPerson.id, bannedPerson.id, BanHistory.BanReasonIdentifier.SubBan.id, 
				ban.description(), ban.id(), false, new Timestamp(now), new Timestamp((long)(ban.createdUTC() * 1000)), new Timestamp(now));
		db.getBanHistoryMapping().save(banHistory);
		
		if(sub.writeOnly) {
			logger.trace("Subreddit is write-only. Flagging as suppressed and returning..");
			banHistory.suppressed = true;
			db.getBanHistoryMapping().save(banHistory);
			return;
		}
		
		for(MonitoredSubreddit otherSub : monitoredSubreddits) {
			if(otherSub.subreddit.equals(sub.subreddit))
				continue;
			if(otherSub.readOnly)
				continue;
			logger.printf(Level.TRACE, "Checking if %s is tracking any relevant hashtags..", otherSub.subreddit);
			List<SubscribedHashtag> relevantTags = getRelevantSubscribedHashtags(otherSub, ban.description());
			if(relevantTags.size() <= 0) {
				logger.printf(Level.INFO, "%s does not subscribe to anything in %s", otherSub.subreddit, ban.description());
				continue;
			}
			
			String tagsStringified = String.join(", ", relevantTags.stream().map(tag -> '"' + tag.hashtag + '"').collect(Collectors.toList()));
			logger.printf(Level.INFO, "The following relevant tags triggered on %s: %s", ban.description(), tagsStringified);
			
			logger.printf(Level.TRACE, "Checking if %s is already banned on %s..", ban.targetAuthor(), sub.subreddit);
			if(checkIfBanned(otherSub, ban.targetAuthor())) {
				logger.printf(Level.INFO, "Not banning %s from %s - already banned!", ban.targetAuthor(), otherSub.subreddit);
				continue;
			}
			
			logger.printf(Level.INFO, "Banning %s from %s as propagating from %s banning him on %s with action %s..", ban.targetAuthor(), otherSub.subreddit, ban.mod(), sub.subreddit, ban.id());
			formatMessagesAndBanUserDueToBan(otherSub, ban, sub, tagsStringified);
			
			if(!otherSub.silent) {
				logger.printf(Level.TRACE, "Sending mail to /r/%s about banning %s", otherSub.subreddit, ban.targetAuthor());
				
				formatMessagesAndSendModmail(otherSub, ban, sub, tagsStringified);
			}
		}
	}
	
	/**
	 * Determines if sub is subscribed to anything contains in banDesc.
	 * 
	 * @param sub the subreddit
	 * @param banDesc the ban description
	 * @return if sub subscribes to something in banDesc
	 */
	protected List<SubscribedHashtag> getRelevantSubscribedHashtags(MonitoredSubreddit sub, String banDesc) {
		final String banDescLower = banDesc.toLowerCase();
		final USLDatabase db = (USLDatabase) database;
		
		List<SubscribedHashtag> hashtags = db.getSubscribedHashtagMapping().fetchForSubreddit(sub.id, false);
		List<SubscribedHashtag> result = new ArrayList<>();
		for(SubscribedHashtag hashtag : hashtags) {
			if(banDescLower.contains(hashtag.hashtag.toLowerCase())) {
				result.add(hashtag);
			}
		}
		return result;
	}
	/**
	 * Bans the target of the modaction on the subToBanOn. Formats both the message, reason, and note
	 * appropriately.
	 * 
	 * @param subToBanOn the sub to ban ban.targetAuthor() on
	 * @param ban the original ban
	 * @param subWhichBanned the MonitoredSubreddit for ban.subreddit()
	 * @param tagsStringified 
	 */
	protected void formatMessagesAndBanUserDueToBan(MonitoredSubreddit subToBanOn, ModAction ban, MonitoredSubreddit subWhichBanned, String tagsStringified) {
		USLDatabase db = (USLDatabase) database;
		ResponseInfo banMessageRespInfo = new ResponseInfo(ResponseInfoFactory.base);
		banMessageRespInfo.addTemporaryString("original mod", ban.mod());
		banMessageRespInfo.addTemporaryString("original description", ban.description());
		banMessageRespInfo.addTemporaryString("original subreddit", ban.subreddit());
		banMessageRespInfo.addTemporaryString("triggering tags", tagsStringified);
		banMessageRespInfo.addTemporaryString("new subreddit", subToBanOn.subreddit);
		ResponseFormatter banMessageFormatter = new ResponseFormatter(db.getResponseMapping().fetchByName("propagated_ban_message").responseBody, banMessageRespInfo);
		String banMessage = banMessageFormatter.getFormattedResponse(config, database);
		ResponseInfo banNoteRespInfo = new ResponseInfo(banMessageRespInfo);
		ResponseFormatter banNoteFormatter = new ResponseFormatter(db.getResponseMapping().fetchByName("propagated_ban_note").responseBody, banNoteRespInfo);
		String banNote = banNoteFormatter.getFormattedResponse(config, database);
		banUser(subWhichBanned, ban.targetAuthor(), banMessage, "other", banNote);
	}
	
	/**
	 * Sends modmail to subToMail describing a ban that resulting from a users ban.
	 * 
	 * @param subToMail The sub to modmail
	 * @param ban the original ban
	 * @param subWhichBanned the MonitoredSubreddit version of ban.subreddit()
	 */
	protected void formatMessagesAndSendModmail(MonitoredSubreddit subToMail, ModAction ban, MonitoredSubreddit subWhichBanned, String tagsStringified) {
		USLDatabase db = (USLDatabase) database;
		ResponseInfo messageRespInfo = new ResponseInfo(ResponseInfoFactory.base);
		messageRespInfo.addTemporaryString("original mod", ban.mod());
		messageRespInfo.addTemporaryString("original description", ban.description());
		messageRespInfo.addTemporaryString("original subreddit", ban.subreddit());
		messageRespInfo.addTemporaryString("original timestamp", timeToPretty(ban.createdUTC()));
		messageRespInfo.addTemporaryString("original id", ban.id());
		messageRespInfo.addTemporaryString("banned user", ban.targetAuthor());
		messageRespInfo.addTemporaryString("triggering tags", tagsStringified);
		ResponseFormatter modMailTitleFormatter = new ResponseFormatter(db.getResponseMapping().fetchByName("propagated_ban_modmail_title").responseBody, messageRespInfo);
		String modMailTitle = modMailTitleFormatter.getFormattedResponse(config, database);
		ResponseFormatter modMailBodyFormatter = new ResponseFormatter(db.getResponseMapping().fetchByName("propagated_ban_modmail_body").responseBody, messageRespInfo);
		String modMailBody = modMailBodyFormatter.getFormattedResponse(config, database);
		sendModmail(subWhichBanned, modMailTitle, modMailBody);
	}
	
	/**
	 * Determine if usernameToCheck is banned on sub
	 * @param sub the subreddit
	 * @param usernameToCheck the username to check
	 * @return if banned on sub
	 */
	protected boolean checkIfBanned(MonitoredSubreddit sub, String usernameToCheck) {
		return new Retryable<Boolean>("check if " + usernameToCheck + " is banned on " + sub.subreddit, maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				Listing listing = RedditUtils.getBannedUsersForSubredditByName(sub.subreddit, usernameToCheck, bot.getUser());
				return listing.numChildren() > 0;
			}
			
		}.run().booleanValue();
	}
	
	/**
	 * Bans the specified user from the specified subreddit
	 * @param sub the subreddit to ban from
	 * @param userToBan the user to ban
	 * @param banMessage the message to pass to the user
	 * @param banReason the string "other"
	 * @param note the note to moderators
	 */
	protected void banUser(MonitoredSubreddit sub, String userToBan, String banMessage, String banReason, String note) {
		new Retryable<Boolean>("ban " + userToBan + " on " + sub.subreddit, maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				RedditUtils.banFromSubreddit(sub.subreddit, userToBan, banMessage, banReason, note, bot.getUser());
				return true;
			}
			
		}.run();
	}
	
	/**
	 * Sends modmail to the specified subreddit
	 * @param sub subreddit to message
	 * @param title the title of the message
	 * @param body the body of the mesage
	 */
	protected void sendModmail(MonitoredSubreddit sub, String title, String body) {
		new Retryable<Boolean>("sending mail to /r/" + sub.subreddit, maybeLoginAgainRunnable) {

			@Override
			protected Boolean runImpl() throws Exception {
				RedditUtils.sendPersonalMessage(bot.getUser(), "/r/" + sub.subreddit, title, body);
				return true;
			}
			
		}.run();
	}
	
	/**
	 * Wrapper around {@link me.timothy.jreddit.RedditUtils#getModeratorLog(String, String, String, String, me.timothy.jreddit.User)}
	 * specific for getting bans for a subreddit inside a retryable
	 * 
	 * @param sub monitored subreddit
	 * @param after after
	 * @return the listing
	 */
	protected Listing getSubBans(MonitoredSubreddit sub, String after) {
		return new Retryable<Listing>("get subreddit ban actions", maybeLoginAgainRunnable) {
			@Override
			protected Listing runImpl() throws Exception {
				return RedditUtils.getModeratorLog(sub.subreddit, null, "banuser", after, bot.getUser());
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
	protected String timeToPretty(long timeMS) {
		return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date(timeMS));
	}
	
	/**
	 * Convert the specified time, in seconds since epoch, to something more legible
	 * @param timeReddit time since epoch in seconds
	 * @return string representation
	 */
	protected String timeToPretty(double timeReddit) {
		return timeToPretty((long)(timeReddit * 1000));
	}
}
