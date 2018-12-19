package me.timothy.bots;

import static me.timothy.bots.ResponseUtils.verifyFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.PropagateResult;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserUnbanInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * The purpose of this class is to propagate our local ban histories
 * to subreddits, according to their configuration.
 * 
 * This class does not manage deciding which banhistory / subreddit 
 * combinations we haven't looked at yet. All it does is, when given
 * a subreddit and a banhistory, ensures the necessary action has 
 * been taken on the subreddit for that BanHistory.
 * 
 * This class does NOT interact with reddit or modify the database!
 * 
 * @author Timothy
 */
public class USLPropagator {
	//private static final Logger logger = LogManager.getLogger();
	
	/**
	 * The file configuration
	 */
	protected USLFileConfiguration config;
	
	/**
	 * The database
	 */
	protected USLDatabase database;
	
	/**
	 * Create a new instance of the USLPropagator using configuration
	 * information from config and uses the database for storage.
	 * 
	 * @param config config
	 * @param database database
	 */
	public USLPropagator(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	/**
	 * Checks the database to ensure we have all the necessary responses. This serves both as an automated
	 * tool to verify no responses are missing, and active documentation on which responses we use.
	 */
	public void verifyHaveResponses() {
		verifyFormat(database, "propagated_ban_message", "This is the message that is sent to the user when they are banned",
				"new subreddit", "The subreddit which they were just banned on",
				"original subreddit", "The oldest subreddit with a non-bot ban for the user");
		
		verifyFormat(database, "propagated_ban_note", "This is the note that only moderators can see for the ban",
				"original subreddit", "The oldest subreddit with a non-bot ban for the user",
				"triggering tags", "Comma separated tags which are followed by the subreddit we are banning the user on");
		
		verifyFormat(database, "propagated_ban_modmail_title", "The title of the PM to the subreddit (not silent) to notify them we banned someone",
				"banned user", "The user that we banned");
		
		verifyFormat(database, "propagated_ban_modmail_body", "The body of the PM to the subreddit (not silent) to notify them we banned someone",
				"banned user", "The user that we banned",
				"user history", "Markup formatted information about the history of the person. See USLUserHistoryMarkupFormatter",
				"triggering tags", "Comma separated tags which are followed by the subreddit we are banning the user on");
		
		verifyFormat(database, "propagate_ban_to_subreddit_override_unban_title", "The title of the PM to the subreddit (silent or not) to notify them that "
				+ "we are banning someone they previously unbanned", 
				"banned user", "The user that we banned");
		
		verifyFormat(database, "propagate_ban_to_subreddit_override_unban_body", "The body of the PM to the subreddit (silent or not) to notify them that "
				+ "we are banning someone they previously unbanned",
				"banned user", "The user that we banned",
				"user history", "Markup formatted information about the history of the person. See USLUserHistoryMarkupFormatter",
				"triggering tags", "Comma separated tags which are followed by the subreddit we are banning the user on",
				"matched ban", "The matching ban for the unban. If we couldn't match the ban, then this is 'none found'");
		
		verifyFormat(database, "propagate_ban_to_subreddit_ban_collision_to_collider_title",
				"The title of the PM to the subreddit which previously permabanned a user which has now been banned again. Specifically, "
				+ "this pm is sent went we have a ban on a subreddit which occurred prior to the action and was not done by the bot.",
				"banned user", "The user that was banned");
		
		verifyFormat(database, "propagate_ban_to_subreddit_ban_collision_to_collider_body",
				"The body of the PM to the subreddit which previously banned a user which has now been banned again",
				"banned user", "The user is banned by the recipient but just had an action for a different reason",
				"user history", "Markup formatted information about the history of the person. See USLUserHistoryMarkupFormatter",
				"triggering tags", "Comma separated tags which are followed by the subreddit we are banning the user on");
		
		verifyFormat(database, "propagate_unban_std_modmail_title", "The title of the PM to the subreddit (not silent) to notify them that "
				+ "we are unbanning someone that was previously banned by the bot.",
				"unbanned user", "The user which we unbanned");
		
		verifyFormat(database, "propagate_unban_std_modmail_body", "The body of the PM to the subreddit (not silent) to notify them that "
				+ "we are unbanning someone that was previously banned by the bot.",
				"unbanned user", "The user which we unbanned",
				"banned at", "When the user was originally banned", 
				"ban note", "The original ban note the user was banned with");
		
		verifyFormat(database, "propagate_unban_primary_modmail_title", "The title of the PM to the subreddit (silent or not) to notify them that "
				+ "we are unbanning someone that was banned on the subreddit but not by the bot. The decision to do this was made by the "
				+ "unban request handler so there's really no logic to be done at this point",
				"unbanned user", "The user which we unbanned");
		
		verifyFormat(database, "propagate_unban_primary_modmail_body", "The body of the PM to the subreddit to notify them we are unbanning someone "
				+ "that they banned, not the bot.",
				"unbanned user", "The user which we unbanned",
				"banned at", "When the user was originally banned",
				"ban note", "The original ban note the user was banned with");
		
		verifyFormat(database, "propagate_unban_failed_modmail_title", "The title of the PM to the subreddit (silent or not) to notify them that "
				+ "they have a ban that doesn't appear to be related to the USL for a user which we wanted to unban. We do not unban "
				+ "the user in this case.",
				"unbanned user", "The user which was not unbanned but would have been");
		
		verifyFormat(database, "propagate_unban_failed_modmail_body", "The body of the PM to the subreddit (silent or not) to notify them that "
				+ "they have a ban that doesn't appear to be related to the USL for a user which we wanted to unban. We do not unban "
				+ "the user in this case.",
				"unbanned user", "The user which was not unbanned but would have been",
				"banned at", "When the user was banned by the subreddit",
				"ban note", "The note that the subreddit has about the user");
	}
	
	/**
	 * Propagate the given action. This should only be invoked once per action, and should be invoked in order
	 * for only the latest action to prevent extra work. This function needs to do extra work on every tick that
	 * can be cached; use the overload to avoid this.
	 *  
	 * @param action the action to propagate
	 * @return what needs to be done.
	 * @see #propagateAction(List, USLAction)
	 */
	public PropagateResult propagateAction(USLAction action) {
		List<MonitoredSubreddit> subs = database.getMonitoredSubredditMapping().fetchAll();
		HashMap<Integer, MonitoredSubreddit> mapSubs = new HashMap<>();
		
		for(MonitoredSubreddit sub : subs) {
			if(!sub.writeOnly) {
				mapSubs.put(sub.id, sub);
			}
		}
		
		return propagateAction(mapSubs, action);
	}
	
	/**
	 * Propagates the given action to the given reading subreddits.
	 * 
	 * @param readingSubreddits the subreddits which are receiving bans. The keys are the ids of the subreddits.
	 * This avoids having to repeatedly query the database for monitored subreddits.
	 * @param action the action which you are interested in propagating.
	 * @return the action by the bot that needs to take place.
	 */
	public PropagateResult propagateAction(HashMap<Integer, MonitoredSubreddit> readingSubreddits, USLAction action) {
		TraditionalScammer tradScammer = database.getTraditionalScammerMapping().fetchByPersonID(action.personID);
		if(tradScammer != null) // not our problem
			return new PropagateResult(action);
		
		Person pers = database.getPersonMapping().fetchByID(action.personID);
		if(pers.username.equals("[deleted]"))
			return new PropagateResult(action); // not a real user
		
		Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		List<Integer> expBanOnIDs = database.getMonitoredSubredditMapping().fetchReadableIDsThatFollowActionsTags(action.id);
		
		// We locate the subreddit which we will classify as the source of this ban. We will only identify it as a single
		// subreddit IF there is EXACTLY one ban on the person attached to the action which was not done by the bot. In
		// all other cases, the original subreddit is just "(ambiguous)"
		String originalSubreddit = null;
		for(int subId : expBanOnIDs) {
			MonitoredSubreddit sub = readingSubreddits.get(subId);
			BanHistory ban = database.getBanHistoryMapping().fetchByActionAndSubreddit(action.id, sub.id);
			
			if(ban != null && ban.modPersonID != bot.id) {
				if(originalSubreddit != null) {
					originalSubreddit = null;
					break;
				}else {
					originalSubreddit = sub.subreddit;
				}
			}
		}
		if(originalSubreddit == null) {
			originalSubreddit = "(ambiguous)";
		}
		
		Set<Integer> subredditsNotYetHandled = new HashSet<Integer>();
		subredditsNotYetHandled.addAll(readingSubreddits.keySet());
		
		PropagateResult result = new PropagateResult(action);
		for(int expBanOn : expBanOnIDs) {
			MonitoredSubreddit sub = readingSubreddits.get(expBanOn);
			subredditsNotYetHandled.remove(expBanOn);
			
			result = propagateWhenExpectBan(action, sub, originalSubreddit).merge(result);
		}
		
		for(int missed : subredditsNotYetHandled) {
			MonitoredSubreddit sub = readingSubreddits.get(missed);
			
			result = propagateWhenExpectUnbanned(action, sub).merge(result);
		}
		
		return result;
	}
	
	private PropagateResult propagateWhenExpectBan(USLAction action, MonitoredSubreddit subreddit, String originalSubreddit) {
		Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		final Person toBan = database.getPersonMapping().fetchByID(action.personID);
		
		BanHistory ban = database.getBanHistoryMapping().fetchByActionAndSubreddit(action.id, subreddit.id);
		if(ban != null) {
			// We definitely don't need to ban, since the person is already banned on the recipient subreddit. However,
			// we might need to send some messages out.
			
			if(ban.modPersonID == bot.id) {
				// The ban is from the bot; nothing to do
				return new PropagateResult(action);
			}
			
			HandledModAction hma = database.getHandledModActionMapping().fetchByID(ban.handledModActionID);
			if(hma.occurredAt.after(action.createdAt)) {
				// The ban is from the future! Nothing to do
				return new PropagateResult(action);
			}
			
			if(hma.occurredAt.equals(action.createdAt)) {
				// This is what triggered the action in the first place. Nothing to do
				return new PropagateResult(action);
			}
			
			// Here's what we know: 
			// The person has an action for some tags that the subreddit subscribes to
			// The person is banned on the subreddit
			// The person was not banned by the bot
			// The person was banned prior to the action
			
			// Scenario A: Banned on sub A (no tag), Banned on sub B (tag), we're trying to propagate from B to A
			// Scenario B: Banned on sub A (tag B doesn't follow), banned on sub B (tag A and B follow), we're trying to propagate from B to A
			
			// Note: Subreddit B is unknown at this point, but we know they must have just *done something*
			// Note: It's possible subreddit B doesn't know about the ban from subreddit A
			
			// We're going to notify subreddit A
			
			String titleFormat = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_ban_collision_to_collider_title").responseBody;
			String bodyFormat = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_ban_collision_to_collider_body").responseBody;
			
			ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
			respInfo.addLongtermString("banned user", toBan.username);
			
			String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
			
			respInfo.addLongtermString("user history", USLHistoryMarkupFormatter.format(database, config, action.personID, false));
			respInfo.addLongtermString("triggering tags", getPrettyTriggeringTags(action, subreddit));
			
			String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
			
			return new PropagateResult(action, Collections.emptyList(), Collections.emptyList(), 
					Collections.singletonList(new ModmailPMInformation(subreddit, title, body)), Collections.emptyList());
		}
		
		UnbanHistory unban = database.getUnbanHistoryMapping().fetchByActionAndSubreddit(action.id, subreddit.id);
		List<ModmailPMInformation> pms = new ArrayList<>();
		if(unban != null) {
			HandledModAction unbanHMA = database.getHandledModActionMapping().fetchByID(unban.handledModActionID);
			if(unbanHMA.occurredAt.after(action.createdAt)) {
				// They were unbanned after this action. Nothing needs to be done.
				return new PropagateResult(action);
			}
			
			// We are overriding an unban, we might need to send a message
			if(unban.modPersonID != bot.id) {
				// We are overriding someone else's unban, we need to inform them about this regardless of their
				// silent status.
				String titleFormat = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_override_unban_title").responseBody;
				String bodyFormat = database.getResponseMapping().fetchByName("propagate_ban_to_subreddit_override_unban_body").responseBody;
				
				ResponseInfo responseInfo = new ResponseInfo(ResponseInfoFactory.base);
				responseInfo.addLongtermString("banned user", toBan.username);
				
				String title = new ResponseFormatter(titleFormat, responseInfo).getFormattedResponse(config, database);
				
				responseInfo.addTemporaryString("user history", USLHistoryMarkupFormatter.format(database, config, action.personID, false));
				responseInfo.addTemporaryString("triggering tags", getPrettyTriggeringTags(action, subreddit));
				
				BanHistory matched = matchUnban(subreddit, unban);
				if(matched == null)
					responseInfo.addTemporaryString("matched ban", "none found");
				else 
					responseInfo.addTemporaryString("matched ban", matched.toPrettyString(database)); // This could be prettier
				
				String body = new ResponseFormatter(bodyFormat, responseInfo).getFormattedResponse(config, database);
				pms.add(new ModmailPMInformation(subreddit, title, body));
			}
		}
		
		// They are not banned. We may have a modmail pm in "pms". We should ban.
		if(!subreddit.silent && pms.isEmpty()) {
			// They are not silent mode and we're about to ban them without a message! We'll just send them a
			// courteous message
			String titleFormat = database.getResponseMapping().fetchByName("propagated_ban_modmail_title").responseBody;
			String bodyFormat = database.getResponseMapping().fetchByName("propagated_ban_modmail_body").responseBody;
			
			ResponseInfo responseInfo = new ResponseInfo(ResponseInfoFactory.base);
			responseInfo.addLongtermString("banned user", toBan.username);
			
			String title = new ResponseFormatter(titleFormat, responseInfo).getFormattedResponse(config, database);
			
			responseInfo.addTemporaryString("user history", USLHistoryMarkupFormatter.format(database, config, action.personID, false));
			responseInfo.addTemporaryString("triggering tags", getPrettyTriggeringTags(action, subreddit));
			
			String body = new ResponseFormatter(bodyFormat, responseInfo).getFormattedResponse(config, database);
			pms.add(new ModmailPMInformation(subreddit, title, body));
		}
		
		// Actually ban them
		String noteFormat = database.getResponseMapping().fetchByName("propagated_ban_note").responseBody;
		String msgFormat = database.getResponseMapping().fetchByName("propagated_ban_message").responseBody;
		
		ResponseInfo responseInfo = new ResponseInfo(ResponseInfoFactory.base);
		responseInfo.addLongtermString("banned user", toBan.username);
		responseInfo.addLongtermString("original subreddit", originalSubreddit);
		responseInfo.addLongtermString("new subreddit", subreddit.subreddit);
		
		String message = new ResponseFormatter(msgFormat, responseInfo).getFormattedResponse(config, database);
		
		responseInfo.addLongtermString("triggering tags", getPrettyTriggeringTags(action, subreddit));
		
		String note = new ResponseFormatter(noteFormat, responseInfo).getFormattedResponse(config, database);
		
		return new PropagateResult(action, 
				Collections.singletonList(new UserBanInformation(toBan, subreddit, message, "other", note)), 
				Collections.emptyList(), 
				pms, 
				Collections.emptyList());
	}
	
	private PropagateResult propagateWhenExpectUnbanned(USLAction action, MonitoredSubreddit subreddit) {
		Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		Person toBan = database.getPersonMapping().fetchByID(action.personID);
		
		UnbanHistory unban = database.getUnbanHistoryMapping().fetchByActionAndSubreddit(action.id, subreddit.id);
		if(unban != null) {
			// The user is already unbanned there! There is nothing to do.
			return new PropagateResult(action);
		}
		
		BanHistory ban = database.getBanHistoryMapping().fetchByActionAndSubreddit(action.id, subreddit.id);

		if(ban == null) {
			// In the earlier implementations we would do an unban here just in case they were banned before,
			// however it is preferable to miss those situations than to unban users who were temp banned by
			// the subreddit. Thus we do nothing.
			return new PropagateResult(action);
		}

		HandledModAction hma = database.getHandledModActionMapping().fetchByID(ban.handledModActionID);
		if(hma.occurredAt.after(action.createdAt)) {
			// We don't override bans from the future!
			return new PropagateResult(action);
		}
		
		List<ModmailPMInformation> pms = new ArrayList<>();
		if(ban.modPersonID != bot.id) {
			// If there's a tag on this ban that is followed by the issuing subreddit then we will override
			// it. Otherwise, we will just send them a message but not override it.
			
			
			List<SubscribedHashtag> subTagsForSubreddit = database.getSubscribedHashtagMapping().fetchForSubreddit(subreddit.id, false);
			List<Hashtag> tagsForSubreddit = database.getHashtagMapping().fetchForSubscribed(subTagsForSubreddit);
			String banNoteLower = ban.banDescription.toLowerCase();
			
			boolean override = false;
			for(Hashtag tag : tagsForSubreddit) {
				if(banNoteLower.contains(tag.tag.toLowerCase())) {
					override = true;
					break;
				}
			}
			
			if(override) {
				// When overriding a ban on a subreddit done by a person, we notify the subreddit regardless of their settings.
				String titleFormat = database.getResponseMapping().fetchByName("propagate_unban_primary_modmail_title").responseBody;
				String bodyFormat = database.getResponseMapping().fetchByName("propagate_unban_primary_modmail_body").responseBody;
				
				ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
				respInfo.addLongtermString("unbanned user", toBan.username);
				
				String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
				
				respInfo.addLongtermString("banned at", SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(hma.occurredAt));
				respInfo.addLongtermString("ban note", ban.banDescription);
				
				String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
				
				pms.add(new ModmailPMInformation(subreddit, title, body));
			}else {
				// We will not override this ban since we can't relate it to a tag relevant to the subreddit. We will notify
				// them about the situation.
				
				String titleFormat = database.getResponseMapping().fetchByName("propagate_unban_failed_modmail_title").responseBody;
				String bodyFormat = database.getResponseMapping().fetchByName("propagate_unban_failed_modmail_body").responseBody;
				
				ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
				respInfo.addLongtermString("unbanned user", toBan.username);

				String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
				
				respInfo.addLongtermString("banned at", SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(hma.occurredAt));
				respInfo.addLongtermString("ban note", ban.banDescription);
				
				String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
				
				return new PropagateResult(action, Collections.emptyList(), Collections.emptyList(), 
						Collections.singletonList(new ModmailPMInformation(subreddit, title, body)), Collections.emptyList());
			}
		}
		
		if(!subreddit.silent && pms.isEmpty()) {
			// When not in silent mode we *must* send a message when we take actions on the subreddit.
			String titleFormat = database.getResponseMapping().fetchByName("propagate_unban_std_modmail_title").responseBody;
			String bodyFormat = database.getResponseMapping().fetchByName("propagate_unban_std_modmail_body").responseBody;

			ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
			respInfo.addLongtermString("unbanned user", toBan.username);
			
			String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
			
			respInfo.addLongtermString("banned at", SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(hma.occurredAt));
			respInfo.addLongtermString("ban note", ban.banDescription);
			
			String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
			
			pms.add(new ModmailPMInformation(subreddit, title, body));
		}
		
		// Unban them!
		
		return new PropagateResult(action, Collections.emptyList(), 
				Collections.singletonList(new UserUnbanInformation(toBan, subreddit)), 
				pms, Collections.emptyList());
	}
	
	private List<Hashtag> getTriggeringTags(USLAction action, MonitoredSubreddit subreddit) {
		List<USLActionHashtag> tagsForAction = database.getUSLActionHashtagMapping().fetchByUSLActionID(action.id);
		List<SubscribedHashtag> subscribed = database.getSubscribedHashtagMapping().fetchForSubreddit(subreddit.id, false);
		
		List<Hashtag> triggering = new ArrayList<>();
		for(SubscribedHashtag sh : subscribed) {
			// linear search faster than hash search at fewer than 3 elements, which is pretty likely here
			if(tagsForAction.stream().anyMatch((ta) -> ta.hashtagID == sh.hashtagID)) {
				triggering.add(database.getHashtagMapping().fetchByID(sh.hashtagID));
			}
		}
		
		return triggering;
	}
	
	private String getPrettyTriggeringTags(USLAction action, MonitoredSubreddit subreddit) {
		return getTriggeringTags(action, subreddit).stream().map((tag) -> tag.tag).collect(Collectors.joining(", "));
	}
	
	private BanHistory matchUnban(MonitoredSubreddit sub, UnbanHistory unban) {
		HandledModAction unbanHma = database.getHandledModActionMapping().fetchByID(unban.handledModActionID);
		
		List<BanHistory> allBans = database.getBanHistoryMapping().fetchBanHistoriesByPersonAndSubreddit(unban.unbannedPersonID, sub.id);
		
		BanHistory best = null;
		long bestTime = -1;
		
		for(BanHistory bh : allBans) {
			HandledModAction hma = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
			if(hma.occurredAt.before(unbanHma.occurredAt)) {
				if(best == null || hma.occurredAt.getTime() < bestTime) {
					best = bh;
					bestTime = hma.occurredAt.getTime();
				}
			}
		}
		
		return best;
	}
}
