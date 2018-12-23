package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.database.DirtyPersonMapping;
import me.timothy.bots.database.HandledAtTimestampMapping;
import me.timothy.bots.database.PersonMapping;
import me.timothy.bots.database.RedditToMeaningProgressMapping;
import me.timothy.bots.database.USLActionMapping;
import me.timothy.bots.functions.PropagateResultHandlerFunction;
import me.timothy.bots.memory.BufferedHandledModActionJoinHistoryIter;
import me.timothy.bots.memory.BufferedUnbanRequestIter;
import me.timothy.bots.memory.HandledModActionJoinHistory;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.models.DirtyPerson;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubredditModqueueProgress;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.models.PropagatorSetting.PropagatorSettingKey;

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
	 * The next method solves this by splitting up each monitored subreddits propagation
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
	 *   
	 * ---
	 * 
	 * However with *that* method it was still too slow. This is partly because of how much
	 * work is being repeated by the bot during the propagation step using the previous
	 * technique - looking at every ban would trigger a full propagation step, which required
	 * many database lookups. So the new plan is a two-phase processing step;
	 * the first phase converts the handled mod actions, ban histories, and unban histories
	 * in time-order into USLAction's. This step only requires the expensive ban/unban matching
	 * step one time per user, and only if they are actually "interesting". This table is 
	 * reconstructed when we need to refresh our logic, and then reswept by the propagator.
	 * 
	 * From a propagation step we do the following:
	 * 
	 * Process the mod actions in time order. As before, we have a 1-second "repeat" step that is
	 * deduplicated manually. Once they have been sorted, the mod actions can be sent to the 
	 * reddit-to-meaning processor for conversion into actions. Once they are setup as an action
	 * and we have the full history of all the relevant subreddits, there is no risk in sending 
	 * these actions to the propagator multiple times.
	 * 
	 * So to recap, this new technique is 3-stage processing:
	 * 
	 * Stage 1: From Reddit to Local
	 * 	Go through each subreddit and fetch their history in any order and save it as HandledModActions,
	 * 	BanHistory's, and UnbanHistory's.
	 * 
	 * (Do not proceed until we have a timestamp T that we KNOW we have all the history before, and T 
	 * is "fairly recent")
	 * 
	 * Stage 2: From Reddit Actions to USLActions:
	 * 	Go through every mod action in order up to time T, and send it to the RedditToMeaningProcessor
	 *  to produce USLAction's. This ultimately produces a list of tags for every user which forms the
	 *  "list", along with any exceptions to the users staus on the subreddits. Whenever we change the
	 *  action for a persion, we flag that person as "dirty" so we know to send them to the propagator.
	 *  
	 * (Do not proceed until we have processed all of the mod actions up to time T into usl actions)
	 * 
	 * Stage 3:	From USLActions back to Reddit Actions:
	 * 	Go through every "dirty" user and clean them by sending them through the propagator. This tells
	 * 	us what actions, if any, we should take. Once we've taken those actions we mark the user clean.
	 */
	private static final Logger logger = LogManager.getLogger();
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected USLPropagator propagator;
	protected USLRedditToMeaningProcessor meaning;
	protected USLValidUnbanRequestToMeaningProcessor unbanRequestMeaning;
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
			USLRedditToMeaningProcessor meaning, USLValidUnbanRequestToMeaningProcessor unbanRequestMeaning,
			PropagateResultHandlerFunction resultHandler) {
		this.database = database;
		this.config = config;
		this.propagator = propagator;
		this.meaning = meaning;
		this.unbanRequestMeaning = unbanRequestMeaning;
		this.resultHandler = resultHandler;
	}
	/**
	 * Determine what actions to propagate to which subreddits to avoid
	 * repetition, and handle their results using the result consumer
	 * specified in the constructor.
	 */
	public void managePropagating(List<MonitoredSubreddit> tracked) {
		Timestamp latestHistory = findLatestHistory(tracked);
		if(latestHistory == null) {
			return;
		}
		
		redditEventsToActions(tracked, latestHistory);
		dirtyPeopleToPropagator(tracked);
	}
	
	private Timestamp findLatestHistory(List<MonitoredSubreddit> tracked) {
		ActionLogMapping al = database.getActionLogMapping();
		Timestamp latestHistory = null;
		for(MonitoredSubreddit sub : tracked) {
			SubredditModqueueProgress modqueueProg = database.getSubredditModqueueProgressMapping().fetchForSubreddit(sub.id);
			if(modqueueProg == null || modqueueProg.lastTimeHadFullHistory == null) {
				al.append("Cannot propagate - waiting on history for /r/" + sub.subreddit);
				logger.printf(Level.DEBUG, "Cannot propagate - waiting on history for /r/" + sub.subreddit);
				return null;
			}
			
			if(latestHistory == null || modqueueProg.lastTimeHadFullHistory.after(latestHistory)) {
				latestHistory = modqueueProg.lastTimeHadFullHistory;
			}
		}
		return latestHistory;
	}
	
	
	private void redditEventsToActions(List<MonitoredSubreddit> tracked, Timestamp latestHistory) {
		List<Hashtag> tags = database.getHashtagMapping().fetchAll();
		
		RedditToMeaningProgressMapping rtmpMap = database.getRedditToMeaningProgressMapping();
		HandledAtTimestampMapping hatMap = database.getHandledAtTimestampMapping();
		DirtyPersonMapping dirtMap = database.getDirtyPersonMapping();
		
		Timestamp timeBeforeWhichEverythingDone = rtmpMap.fetch();
		if(timeBeforeWhichEverythingDone == null) {
			timeBeforeWhichEverythingDone = new Timestamp(1000);
			hatMap.clear(timeBeforeWhichEverythingDone);
			rtmpMap.set(timeBeforeWhichEverythingDone);
		}
		
		BufferedHandledModActionJoinHistoryIter histIter = new BufferedHandledModActionJoinHistoryIter(database, timeBeforeWhichEverythingDone, latestHistory);
		BufferedUnbanRequestIter urIter = new BufferedUnbanRequestIter(database, timeBeforeWhichEverythingDone, latestHistory);
		
		HandledModActionJoinHistory hist = histIter.next();
		UnbanRequest ur = urIter.next();
		
		while(hist != null || ur != null) {
			if(hist == null || (ur != null && hist.handledModAction.occurredAt.after(ur.handledAt))) {
				unbanRequestMeaning.processUnbanRequest(ur);
				dirtMap.save(new DirtyPerson(ur.bannedPersonID));
				
				Timestamp time = ur.handledAt;
				
				if(time.after(timeBeforeWhichEverythingDone)) {
					timeBeforeWhichEverythingDone = new Timestamp(time.getTime());
					hatMap.clear(timeBeforeWhichEverythingDone);
					rtmpMap.set(timeBeforeWhichEverythingDone);
				}
				
				hatMap.addUnbanRequest(ur.id);
				
				ur = urIter.next();
			}else {
				Set<Integer> dirtied;
				if(hist.isBan()) {
					dirtied = meaning.processBan(tags, hist.handledModAction, hist.banHistory);
				}else {
					dirtied = meaning.processUnban(tags, hist.handledModAction, hist.unbanHistory);
				}
				
				for(int id : dirtied) {
					dirtMap.save(new DirtyPerson(id));
				}
				
				Timestamp time = hist.handledModAction.occurredAt;

				if(time.after(timeBeforeWhichEverythingDone)) {
					timeBeforeWhichEverythingDone = new Timestamp(time.getTime());
					hatMap.clear(timeBeforeWhichEverythingDone);
					rtmpMap.set(timeBeforeWhichEverythingDone);
				}
				
				if(hist.isBan())
					 hatMap.addBanHistory(hist.banHistory.id);
				else
					hatMap.addUnbanHistory(hist.unbanHistory.id);
				
				
				hist = histIter.next();
			}
		}
	}
	
	private void dirtyPeopleToPropagator(List<MonitoredSubreddit> tracked) {
		Map<Integer, MonitoredSubreddit> readingSubs = new HashMap<>();
		for(MonitoredSubreddit sub : tracked) {
			if(!sub.writeOnly)
				readingSubs.put(sub.id, sub);
		}
		
		DirtyPersonMapping dirtMap = database.getDirtyPersonMapping();
		PersonMapping persMap = database.getPersonMapping();
		USLActionMapping actMap = database.getUSLActionMapping();
		
		long start = System.currentTimeMillis();
		long finishTime = start + (1000 * 60 * 60 * 3);
		
		List<DirtyPerson> dirtyList = new ArrayList<>();
		while(!(dirtyList = dirtMap.fetch(10)).isEmpty()) {
			for(DirtyPerson dirty : dirtyList) {
				Person pers = persMap.fetchByID(dirty.personID);
				logger.printf(Level.DEBUG, "Sending /u/%s to the propagator", pers.username);
				
				USLAction act = actMap.fetchLatest(dirty.personID);
				if(act != null) {
					PropagateResult result = propagator.propagateAction(act);
					resultHandler.handleResult(result);
				}
				
				dirtMap.delete(dirty.personID);
				
				if(System.currentTimeMillis() >= finishTime) {
					logger.printf(Level.INFO, "Propagator reached maximum actions per loop restriction. There are %d more persons to be sent", dirtMap.count());
					return;
				}
			}
		}
		
		String suppressNoOpMessVal = database.getPropagatorSettingMapping().get(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES);
		if(suppressNoOpMessVal == null || !suppressNoOpMessVal.equals("false")) {
			logger.printf(Level.INFO, "Propagator finished loop with no more work to do, unsuppressing no-op messages");
			database.getPropagatorSettingMapping().put(PropagatorSettingKey.SUPPRESS_NO_OP_MESSAGES, "false");
		}
	}
}
