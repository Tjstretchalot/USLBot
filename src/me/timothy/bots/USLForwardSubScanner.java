package me.timothy.bots;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.ModAction;
import me.timothy.jreddit.info.Thing;

/**
 * <p>One public function that forward-scans a single subreddit.</p>
 * 
 * <p>The goal of this class is to take a subreddit
 * then go to its mod action history page. We then keep clicking "next page" ("next" is where the idea
 * of this being a "forward" search comes from) until reddit says there's no more stuff.</p>
 * 
 * <p>We might have to interrupt this process to do other stuff, since we can't spend too
 * much time doing any one thing.</p>
 * 
 * <p>For every mod action we find, we send it over to the USLModActionProcessor. We must 
 * inform the calling code when we've reached the end.</p>
 * 
 * @author Timothy
 *
 */
public class USLForwardSubScanner {
	/*
	 * To repeat the class comments:
	 * 
	 * Our goal is only to:
	 *   1) Get the pagination mod action id
	 *   2) Get the page from reddit
	 *   3) Send all the mod actions to the mod action processor
	 *   4) 
	 *     a) Update the oldest pagination id
	 *     b) Initialize the newest pagination id, if appropriate
	 *   5)
	 *     a) If there ARE more pages, return CONTINUE
	 *     b) If there are NO MORE pages, return FINISHED
	 *   
	 * 
	 * 
	 * Note, this ABSOLUTELY DOES NOT:
	 *   - Determine if we SHOULD do a forward search
	 *   - Update from forward search to reverse search
	 *   - Check if the subreddit is read/write only
	 */
	
	/**
	 * The result if there's more stuff to do
	 */
	public static final boolean CONTINUE = true;
	
	/**
	 * The result if there's no more stuff to do
	 */
	public static final boolean FINISHED = false;
	
	private static final Logger logger = LogManager.getLogger();
	private static final int MAX_PAGES_PER_SCAN = 3;
	
	
	/**
	 * Starts or continues a forward scan of the given subreddits mod action history.
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
		Listing page = getPageFollowing(bot, subreddit, paginationID);
		sleepFor(USLBotDriver.BRIEF_PAUSE_MS);
		sendPageToProcessor(bot, database, config, page);
		
		if(isPageLastPage(page))
			return FINISHED;
		
		String newPaginationID = getNewPaginationModActionID(page);
		updatePaginationModActionID(database, subreddit, newPaginationID);
		
		if(needToInitializeNewestPaginationID(database, subreddit)) {
			newPaginationID = getReversePaginationModActionID(page);
			initializeReversePaginationModActionID(database, subreddit, newPaginationID);
		}
		
		return CONTINUE;
	}
	
	private static String getPaginationModActionID(USLDatabase database, MonitoredSubreddit sub) {
		SubredditModqueueProgress progress = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
		
		return progress.latestModActionID;
	}
	
	private static Listing getPageFollowing(Bot bot, MonitoredSubreddit subreddit, String modActionID) {
		return new Retryable<Listing>("getPageFollowing", USLBotDriver.sMaybeLoginAgainRunnable) {

			@Override
			protected Listing runImpl() throws Exception {
				return RedditUtils.getModeratorLog(subreddit.subreddit, null, null, null, modActionID, 500, bot.getUser());
			}
			
		}.run();
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
		for(int i = 0; i < listing.numChildren(); i++) {
			if(listing.getChild(i) instanceof ModAction)
				return false;
		}
		
		return true;
	}
	
	private static String getNewPaginationModActionID(Listing listing) {
		ModAction oldest = null;
		
		for(int i = 0; i < listing.numChildren(); i++) {
			Thing child = listing.getChild(i);
			
			if(child instanceof ModAction) {
				ModAction ma = (ModAction) child;
				
				if(oldest == null || ma.createdUTC() < oldest.createdUTC()) {
					oldest = ma;
				}
			}
		}
		
		return oldest.id();
	}
	
	private static boolean needToInitializeNewestPaginationID(USLDatabase database, MonitoredSubreddit subreddit) {
		SubredditModqueueProgress progress = database.getSubredditModqueueProgressMapping().fetchForSubreddit(subreddit.id);
		return progress.newestModActionID == null;
	}
	
	private static String getReversePaginationModActionID(Listing listing) {
		ModAction newest = null;
		
		for(int i = 0; i < listing.numChildren(); i++) {
			Thing child = listing.getChild(i);
			
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
		progress.latestModActionID = newID;
		database.getSubredditModqueueProgressMapping().save(progress);
	}
	
	private static void initializeReversePaginationModActionID(USLDatabase database, MonitoredSubreddit sub, String newID) {
		SubredditModqueueProgress progress = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
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
