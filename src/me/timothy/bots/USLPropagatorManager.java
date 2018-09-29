package me.timothy.bots;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.functions.PropagateResultHandlerFunction;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledAtTimestamp;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.SubredditPropagateStatus;
import me.timothy.bots.models.UnbanHistory;

/**
 * This class decides which and when to propagate bans using
 * the USLPropagator.
 * 
 * @author Timothy
 */
public class USLPropagatorManager {
	/*
	 * Originally, the propagation status to subreddits was deciding by the following
	 * model: (id, monitored_subreddit_id, latest_propagated_action_time, updated_at)
	 * 
	 * In combination with a helper table that would keep track of the action(s) that
	 * have latest_propagated_action_time as their timestamp. 
	 * 
	 * With that method, the USLBot could guarantee it propagated each action exactly
	 * once, which is the idea. However, when a new subreddit was added to the list,
	 * new handled mod actions would be added that were *in the past*, and there was
	 * no way to distinguish which ones the propagater handled already. The only choice,
	 * then, was to repropagate every single action AFTER the new subreddit had been
	 * completely parsed. This process would take longer and longer every time a new
	 * subreddit was added.
	 * 
	 * ---
	 * 
	 * The new method solves this by splitting up each monitored subreddits propagation
	 * status based on the subreddit. The new propagate status model became:
	 * 
	 *   id, 
	 *   major_subreddit_id, 
	 *   minor_subreddit_id,
	 *   latest_propagated_action_time,
	 *   updated_at
	 *   
	 * It is still necessary to avoid repropagating the same action over and over in quick
	 * succession, so the helper table became:
	 * 
	 *   id,
	 *   major_subreddit_id,
	 *   minor_subreddit_id,
	 *   action_id
	 */
	private static final Logger logger = LogManager.getLogger();
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected USLPropagator propagator;
	protected PropagateResultHandlerFunction resultHandler;
	
	/**
	 * Create a new propagator manager
	 * 
	 * @param database database
	 * @param config configuration
	 * @param propagator the thing to manage
	 * @param resultHandler how propagate results are handled
	 */
	public USLPropagatorManager(USLDatabase database, USLFileConfiguration config, USLPropagator propagator,
			PropagateResultHandlerFunction resultHandler) {
		this.database = database;
		this.config = config;
		this.propagator = propagator;
		this.resultHandler = resultHandler;
	}
	
	/**
	 * Determine what actions to propagate to which subreddits to avoid
	 * repetition, and handle their results using the result consumer
	 * specified in the constructor.
	 */
	public void managePropagating(List<MonitoredSubreddit> tracked) {
		ActionLogMapping al = database.getActionLogMapping();
		for(int i = 0; i < tracked.size(); i++) {
			MonitoredSubreddit toSubreddit = tracked.get(i);
			al.append(String.format("Propagating mod actions to {link subreddit %d}..", toSubreddit.id));
			for(int j = 0; j < tracked.size(); j++) {
				if(i == j)
					continue;
				
				MonitoredSubreddit fromSubreddit = tracked.get(j);
				//logger.trace("Managing propagating from " + minor.subreddit + " to " + major.subreddit);
				managePropagatingSubPair(toSubreddit, fromSubreddit);
				//logger.trace("Finished propagating from " + minor.subreddit + " to " + major.subreddit);
			}
		}
	}
	
	/**
	 * Spends a reasonable amount of time propagating actions from minor to major
	 * if it is possible and reasonable to do so
	 * 
	 * @param toSub the major subreddit
	 * @param fromSub the minor subreddit
	 */
	protected void managePropagatingSubPair(MonitoredSubreddit toSub, MonitoredSubreddit fromSub) {
		SubredditModqueueProgress fromProgres = database.getSubredditModqueueProgressMapping().fetchForSubreddit(fromSub.id);
		if(fromProgres.lastTimeHadFullHistory == null) {
			database.getActionLogMapping().append(fromSub.subreddit + " is still having its history fetched. Not propagating.");
			logger.trace(fromSub.subreddit + " is still having its history fetched. Not propagating.");
			return;
		}
		
		
		SubredditPropagateStatus propStatus = database.getSubredditPropagateStatusMapping()
				.fetchForSubredditPair(toSub.id, fromSub.id);
		if(propStatus == null) {
			propStatus = initMajorMinorStatus(toSub, fromSub);
		}
		
		logger.printf(Level.TRACE, "Propagating actions that occurred AFTER %s on %s to %s", 
				propStatus.latestPropagatedActionTime.toString(), fromSub.toString(), toSub.toString());
		
		int[] hmaCounter = new int[] { 0 };
		// We use an array here to get the equivalent of C-like & 
		int[] actionCounter = new int[] { 0 } ;
		
		final int MAX_HMAS = 250;
		int hmasPerFetch = 50;
		final int MAX_ACTIONS = 3;
		
		while(hmaCounter[0] < MAX_HMAS && actionCounter[0] < MAX_ACTIONS) {
			List<HandledAtTimestamp> hats = fetchHandledAtTimestamp(toSub, fromSub);
			List<HandledModAction> hmas = fetchHandledModActions(toSub, fromSub, propStatus.latestPropagatedActionTime, hmasPerFetch);
			
			if(hmas.isEmpty()) {
				//logger.trace("Nothing more from " + minor.subreddit + " to propagate to " + major.subreddit);
				return;
			}
			
			boolean foundNonHat = false;
			
			for(HandledModAction hma : hmas) {
				if(hmaCounter[0] >= MAX_HMAS)
					break;
				if(actionCounter[0] >= MAX_ACTIONS)
					break;
				if(hats.stream().anyMatch((hat) -> hat.handledModActionID == hma.id))
					continue;
				
				foundNonHat = true;
				
				
				handleHandledModAction(hma, toSub, fromSub, hats, actionCounter, hmaCounter, propStatus);
			}
			
			if(hmaCounter[0] >= MAX_HMAS)
				break;
			if(actionCounter[0] >= MAX_ACTIONS)
				break;
			
			if(!foundNonHat) {
				if(hmas.size() < hmasPerFetch)
					break; // didn't get as many as we asked and found nothing new -> done
				
				logger.printf(Level.WARN, "Had to increase hmas per fetch because we didnt get any new things (from %d to %d)", hmasPerFetch, hmasPerFetch + 50);
				hmasPerFetch += 50; // we need to view more to get the full picture
			}
		}
		
		//logger.printf(Level.TRACE, "Propagated %d hmas which required %d actions (config: %d max hmas, %d max actions)", hmaCounter[0], actionCounter[0], MAX_HMAS, MAX_ACTIONS);
	}
	
	
	/**
	 * Handles a single mod action from minor and propagates it to major,
	 * if appropriate, and updates the action counter (if appropriate) and
	 * the major/minor status (if appropriate).
	 * 
	 * @param hma the handled mod action
	 * @param toSub the subreddit being propagated to
	 * @param fromSub the subreddit being propagated from
	 * @param hats the list of HATs
	 * @param actionCounter incremented at index 0 if a reddit action is required
	 * @param majorMinorStatus the status of propagating minor to major
	 */
	protected void handleHandledModAction(HandledModAction hma, MonitoredSubreddit toSub, MonitoredSubreddit fromSub,
			List<HandledAtTimestamp> hats, int[] actionCounter, int[] hmaCounter, SubredditPropagateStatus majorMinorStatus) {
		hmaCounter[0]++;
		BanHistory bh = database.getBanHistoryMapping().fetchByHandledModActionID(hma.id);
		if(bh != null) {
			logger.printf(Level.TRACE, "Propagating bh id=%d, modPerson=%s, bannedPerson=%s, hma=[id = %d, mod action=%s @ %s] with desc=%s and details=%s from %s to %s",
					bh.id, 
					database.getPersonMapping().fetchByID(bh.modPersonID).username,
					database.getPersonMapping().fetchByID(bh.bannedPersonID).username,
					bh.handledModActionID,
					database.getHandledModActionMapping().fetchByID(bh.handledModActionID).modActionID,
					database.getHandledModActionMapping().fetchByID(bh.handledModActionID).occurredAt.toString(),
					bh.banDescription,
					bh.banDetails,
					fromSub.subreddit, toSub.subreddit);
			PropagateResult result = propagator.propagateBan(toSub, hma, bh);
			if(resultHandler.handleResult(result)) {
				actionCounter[0]++;
			}
		}else {
			UnbanHistory ubh = database.getUnbanHistoryMapping().fetchByHandledModActionID(hma.id);
			if(ubh != null) {
				logger.printf(Level.TRACE, "Propagating ubh=%s from %s to %s", ubh.toString(), fromSub.subreddit, toSub.subreddit);
				PropagateResult result = propagator.propagateUnban(toSub, hma, ubh);
				if(resultHandler.handleResult(result)) {
					actionCounter[0]++;
				}
			}
		}
		
		handleHats(hma, toSub, fromSub, hats);
		
		majorMinorStatus.latestPropagatedActionTime = new Timestamp(hma.occurredAt.getTime());
		majorMinorStatus.updatedAt = new Timestamp(System.currentTimeMillis());
		database.getSubredditPropagateStatusMapping().save(majorMinorStatus);
	}

	/**
	 * Manage the list of handled at timestamps and update it for us handled
	 * the specified hma
	 * 
	 * @param hmaWeJustHandled the hma we are handling
	 * @param toSub major subreddit
	 * @param fromSub minor subreddit
	 * @param hats list of handled at timestamps
	 */
	protected void handleHats(HandledModAction hmaWeJustHandled, MonitoredSubreddit toSub, MonitoredSubreddit fromSub,
			List<HandledAtTimestamp> hats) {
		boolean clearHats = true;
		if(hats.size() > 0) {
			HandledModAction hatHma = database.getHandledModActionMapping().fetchByID(hats.get(0).handledModActionID);
			Timestamp timestampOfOldHats = hatHma.occurredAt;
			if(timestampOfOldHats.getTime() == hmaWeJustHandled.occurredAt.getTime()) {
				clearHats = false;
			}
		}
		
		if(clearHats) {
			hats.clear();
			database.getHandledAtTimestampMapping().deleteBySubIDs(toSub.id, fromSub.id);
		}
		
		HandledAtTimestamp newHat = new HandledAtTimestamp(toSub.id, fromSub.id, hmaWeJustHandled.id);
		database.getHandledAtTimestampMapping().save(newHat);
		hats.add(newHat);
	}

	/**
	 * Fetch a reasonable number of handled mod actions that probably haven't been propagated
	 * yet and aren't later than the latest full history
	 * 
	 * @param toSubreddit subreddit propagating to
	 * @param fromSubreddit subreddit being propagating
	 * @param fetchAfter the timestamp after which we should get
	 * @param hmasPerFetch how many to fetch
	 * @return a list of handled mod actions in chronological order
	 */
	protected List<HandledModAction> fetchHandledModActions(MonitoredSubreddit toSubreddit, MonitoredSubreddit fromSubreddit,
			Timestamp after, int hmasPerFetch) {
		if(after == null) {
			after = new Timestamp(1000 * 60 * 60 * 24 * 10); // 10 days after epoch
			after.setNanos(0);
		}
		
		return database.getHandledModActionMapping().fetchLatestForSubreddit(fromSubreddit.id, after, null, hmasPerFetch);
	}

	/**
	 * Fetch the list of HMAs that have been handled by major from minor at the latest
	 * timestamp
	 * @param major the subreddit being propagated to
	 * @param minor the subreddit being propagated from
	 * @return the list of HATs
	 */
	protected List<HandledAtTimestamp> fetchHandledAtTimestamp(MonitoredSubreddit major, MonitoredSubreddit minor) {
		return database.getHandledAtTimestampMapping().fetchBySubIDs(major.id, minor.id);
	}

	/**
	 * Sets up the default values for the subreddit propagate status from minor to major,
	 * saves it in the database, and returns it
	 * 
	 * @param major the subreddit being propagated to
	 * @param minor the subreddit being propagated from
	 * @return the saved default status
	 */
	protected SubredditPropagateStatus initMajorMinorStatus(MonitoredSubreddit major, MonitoredSubreddit minor) {
		logger.debug("Found no sub prop status for propagating " + minor.subreddit + " to " + major.subreddit + ", autogenerating");
		SubredditPropagateStatus status = new SubredditPropagateStatus(-1, major.id, minor.id, null, new Timestamp(System.currentTimeMillis()));
		database.getSubredditPropagateStatusMapping().save(status);
		return status;
	}
}
