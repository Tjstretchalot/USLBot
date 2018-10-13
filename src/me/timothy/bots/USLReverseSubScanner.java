package me.timothy.bots;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.ModAction;
import me.timothy.jreddit.info.Thing;

/**
 * <p>A single public function that reverse-scans a given subreddit.</p>
 * 
 * <p>The goal of this class is to go to a subreddits mod history page, then 
 * ask for all of the stuff newer than the last thing we've seen. In effect,
 * we keep hitting the "back" button, hence the name "reverse" scanner. We 
 * do this until reddit says theres no more stuff.</p>
 * 
 * <p>We might have to interrupt this process to do other stuff, since we can't
 * spend too much time doing one thing.</p>
 * 
 * <p>For every mod action we find, we send it over to the USLModActionProcessor. We must 
 * inform the calling code when we've reached the end.</p>
 * 
 * @author Timothy
 *
 */
public class USLReverseSubScanner {
	/*
	 * To repeat the class comments:
	 * 
	 * Our goal is only to:
	 *   1) Get the pagination mod action id
	 *   2) Get the page from reddit
	 *   3) Send all the mod actions to the mod action processor
	 *   4) Update the newest pagination id
	 *   5)
	 *     a) If there ARE more pages, return CONTINUE
	 *     b) If there are NO MORE pages, return FINISHED
	 * 
	 * 
	 * Note, this ABSOLUTELY DOES NOT:
	 *   - Determine if we SHOULD do reverse search
	 *   - Check if the subreddit is read/write only
	 *   - Initialize or modify the oldest pagination id
	 */

	
	/**
	 * The result if there's more stuff to do
	 */
	public static final boolean CONTINUE = true;
	
	/**
	 * The result if there's no more stuff to do
	 */
	public static final boolean FINISHED = false;
	
	private static final int ACTIONS_PER_PAGE = 500;
	private static final int MAX_PAGES_PER_SCAN = 15;
	private static final Logger logger = LogManager.getLogger();
	
	
	/**
	 * Starts or continues a reverse scan of the given subreddits mod action history.
	 * 
	 * @param bot The bot, used for authenticating with reddit
	 * @param database The database, used for saving state information and the mod actions we find
	 * @param config The configuration, used for fetching user-controlled variables
	 * @param subreddit The subreddit to perform a forward search on.
	 * @return CONTINUE if we have not finished our forward search, FINISHED if we have
	 */
	public static boolean scan(Bot bot, USLDatabase database, USLFileConfiguration config, MonitoredSubreddit subreddit) {
		for(int i = 0; i < MAX_PAGES_PER_SCAN; i++) {
			boolean result = scanPage(bot, database, config, subreddit);
			if(result == FINISHED)
				return FINISHED;
		}
		
		return CONTINUE;
	}

	private static boolean scanPage(Bot bot, USLDatabase database, USLFileConfiguration config, MonitoredSubreddit subreddit) {
		String paginationID = getPaginationModActionID(database, subreddit);
		Listing page = getPagePreceeding(bot, subreddit, paginationID);
		printInformationAboutPage(subreddit, page, paginationID);
		sleepFor(USLBotDriver.BRIEF_PAUSE_MS);
		sendPageToProcessor(bot, database, config, page);
		
		if(isPageLastPage(page))
			return FINISHED;
		
		String newPaginationID = getNewPaginationModActionID(page, paginationID);
		if(newPaginationID.equals(paginationID))
			logger.printf(Level.ERROR, "Pagination id did not change even though not the last page? Still is %s", newPaginationID);
		else
			logger.printf(Level.TRACE, "Determined new pag id for sub %s (id=%d) should be %s", subreddit.subreddit, subreddit.id, newPaginationID);
		
		updatePaginationModActionID(database, subreddit, newPaginationID);
		return CONTINUE;
	}

	
	private static String getPaginationModActionID(USLDatabase database, MonitoredSubreddit sub) {
		SubredditModqueueProgress progress = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		
		return progress.newestModActionID;
	}

	private static Listing getPagePreceeding(Bot bot, MonitoredSubreddit subreddit, String modActionID) {
		return new Retryable<Listing>("getPagePreceeding", USLBotDriver.sMaybeLoginAgainRunnable) {

			@Override
			protected Listing runImpl() throws Exception {
				return RedditUtils.getModeratorLog(subreddit.subreddit, null, null, modActionID, null, ACTIONS_PER_PAGE, bot.getUser());
			}
			
		}.run();
	}
	
	private static void printInformationAboutPage(MonitoredSubreddit subreddit, Listing page, String paginationID) {
		if(page.numChildren() == 0)
		{
			logger.printf(Level.TRACE, "%s: Page before %s has 0 children.", subreddit.subreddit, paginationID);
			return;
		}
		
		int numModActions = 0;
		for(int i = 0; i < page.numChildren(); i++) {
			if(page.getChild(i) instanceof ModAction) {
				numModActions++;
			}
		}
		
		if(numModActions == 0) {
			logger.printf(Level.WARN, "%s: Page before %s has %d children yet no modactions on it. This is not good.", subreddit.subreddit, paginationID, page.numChildren());
			return;
		}
		
		if(numModActions != page.numChildren()) {
			logger.printf(Level.WARN, "%s: Page before %s has %d children yet only %d modactions on it.", subreddit.subreddit, paginationID, page.numChildren(), numModActions);
		}
		
		
		ModAction newest = null, oldest = null;
		for(int i = 0; i < page.numChildren(); i++) {
			Thing child = page.getChild(i);
			
			if(child instanceof ModAction) {
				ModAction ma = (ModAction)child;
				
				if(newest == null || ma.createdUTC() > newest.createdUTC()) {
					newest = ma;
				}
				
				if(oldest == null || ma.createdUTC() < oldest.createdUTC()) {
					oldest = ma;
				}
			}
		}
		
		String prettyNewestTime = SimpleDateFormat.getInstance().format(new Date((long)(newest.createdUTC() * 1000)));
		String prettyOldestTime = SimpleDateFormat.getInstance().format(new Date((long)(oldest.createdUTC() * 1000)));
		
		logger.printf(Level.TRACE, "%s: Page before %s has %d actions between %s and %s", subreddit.subreddit, paginationID, numModActions, prettyOldestTime, prettyNewestTime);
	}

	private static void sendPageToProcessor(Bot bot, USLDatabase database, USLFileConfiguration config, Listing listing) {
		for(int i = 0; i < listing.numChildren(); i++) {
			Thing child = listing.getChild(i);
			
			if(child instanceof ModAction) {
				USLModActionProcessor.processModAction(bot, database, config, (ModAction)child);
			}
		}
	}
	
	private static boolean isPageLastPage(Listing listing) {
		// reddit api doesn't actually do before very well so we have to use a crude method
		// if we want any sort of consistency
		if(listing.before() == null && listing.numChildren() < ACTIONS_PER_PAGE)
			return true;
		
		for(int i = 0; i < listing.numChildren(); i++) {
			if(listing.getChild(i) instanceof ModAction)
				return false;
		}
		
		return true;
	}

	private static String getNewPaginationModActionID(Listing page, String oldPaginationID) {
		if(page.before() != null)
			return page.before();
		
		ModAction newest = null;
		
		for(int i = 0; i < page.numChildren(); i++) {
			Thing child = page.getChild(i);
			
			if(child instanceof ModAction) {
				ModAction ma = (ModAction) child;
				
				if(newest == null || ma.createdUTC() > newest.createdUTC()) {
					newest = ma;
				}
			}
		}
		
		return newest.id();
	}

	private static void updatePaginationModActionID(USLDatabase database, MonitoredSubreddit sub, String newID) {
		SubredditModqueueProgress progress = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		if(progress.monitoredSubredditID != sub.id)
			throw new RuntimeException("SubredditModqueueProgressMapping : progress.monitoredSubredditID = " + progress.monitoredSubredditID + "; sub.id = " + sub.id);
		
		progress.newestModActionID = newID;
		database.getSubredditModqueueProgressMapping().save(progress);
	}
	
	private static void sleepFor(long ms) {
		try {
			logger.trace("Sleeping for " + ms + " milliseconds");
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			logger.error(ex);
			throw new RuntimeException(ex);
		}
	}
}
