package me.timothy.bots;

import static me.timothy.bots.ResponseUtils.verifyFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.timothy.bots.functions.IsModeratorFunction;
import me.timothy.bots.memory.HandledModActionJoinHistory;
import me.timothy.bots.memory.UnbanRequestResult;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * Determines if an unban request is valid and responds to the user explaining what it decided
 * and why.
 * 
 * @author Timothy
 */
public class USLUnbanRequestHandler {
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected IsModeratorFunction isModerator;
	
	protected Person currentMod;
	protected Map<String, Boolean> modCache;
	
	public USLUnbanRequestHandler(USLDatabase database, USLFileConfiguration config, IsModeratorFunction isModerator) {
		this.database = database;
		this.config = config;
		this.isModerator = isModerator;
		
		modCache = new HashMap<>();
	}
	
	/**
	 * Checks to make sure that the database is not missing any responses.
	 */
	public void verifyHaveResponses() {
		verifyFormat(database, "unban_request_to_mod_denied_generic_title", 
				"The title of the pm to send to the person who sent us an unban request when we are denying them and we don't know "
				+ "of any reason to trust them.",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");
		
		verifyFormat(database, "unban_request_to_mod_denied_generic_body",
				"The body of the pm to send to the person who sent us an unban request when we are denying them and we don't know "
				+ "of any reason to trust them",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");
		
		verifyFormat(database, "unban_request_to_mod_denied_prevented_title",
				"The title of the pm to send to the person who sent us an unban request when we are denying them but we can trust "
				+ "them. The reason for denial is one or more conflicting bans.",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");

		verifyFormat(database, "unban_request_to_mod_denied_prevented_body",
				"The body of the pm to send to the person who sent us an unban request when we are denying them but we can trust "
				+ "them. The reason for denial is one or more conflicting bans.",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban",
				"history table", "A markup formatted table for the full history of the banned user from the history markup formatter",
				"authorize table", "A markup formatted table for what gave them authorization to make the request",
				"prevent table", "A markup formatted table for what prevented the request");
		
		verifyFormat(database, "unban_request_to_mod_denied_unknown_title",
				"The title of the pm to send to the person who sent us an unban request when we are denying them because we don't "
				+ "know who they are talking about, but they are a moderator of the USL",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");
		
		verifyFormat(database, "unban_request_to_mod_denied_unknown_body",
				"The body of the pm to send to the person who sent us an unban request when we are denying them because we don't "
				+ "know who they are talking about, but they are a moderator of the USL",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");
		
		verifyFormat(database, "unban_request_to_mod_denied_no_tags_title",
				"The title of the pm to send to the person who sent us an unban request when we are denying them because the "
				+ "user they are talking about has no active tags, but they are a moderator of the USL",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban");
		
		verifyFormat(database, "unban_request_to_mod_denied_no_tags_body",
				"The body of the pm to send to the person who sent us an unban request when we are denying them because the "
				+ "user they are talking about has no active tags, but they are a moderator of the USL",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person they tried to unban",
				"history table", "The full history of the banned person from the history markup formatter");
		
		verifyFormat(database, "unban_request_to_mod_approved_title",
				"The title of the pm to send to the person who sent us an unban request when we are approving them.",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person that will be unbanned");
		
		verifyFormat(database, "unban_request_to_mod_approved_body_no_footer",
				"The body of the pm to send to the person who sent us an unban request when we are approving them. This will be appended by"
				+ " other messages before the footer, as appropriate for the request made",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person that will be unbanned",
				"tags", "A comma-separated list of tags that were removed",
				"history table", "The full history of the banned person from the history markup formatter");
		
		verifyFormat(database, "unban_request_to_mod_approved_tradscammer_append",
				"The appended text to the body of the pm to the requester of an approved unban request when the person was removed from "
				+ "the traditional scammer list",
				"banned", "The username of the person that will be removed from the traditional list",
				"reason", "The reason that was originally given",
				"description", "The description that we were using for the traditional ban");
		
		verifyFormat(database, "unban_request_to_mod_approved_override_append",
				"The appended text to the body of the pm to the requester of an approved unban request when the person who is performing "
				+ "the unban is a usl moderator and they are overriding other peoples bans.",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person that will be removed from the traditional list",
				"override table", "A mark-up formatted table of the bans that are being overriden");
		
		verifyFormat(database, "unban_request_to_mod_approved_footer",
				"The footer text for the bottom of the body of the pm to the requester of an approved unban request",
				"mod", "The username of the person who made the request",
				"banned", "The username of the person that will be removed from the traditional list");
		
	}
	
	protected boolean isMod(String sub) {
		if(modCache.containsKey(sub))
			return modCache.get(sub).booleanValue();
		
		boolean res = isModerator.isModerator(sub, currentMod.username);
		modCache.put(sub, res);
		return res;
	}
	
	public UnbanRequestResult handleUnbanRequest(UnbanRequest request) {
		currentMod = database.getPersonMapping().fetchByID(request.modPersonID);
		UnbanRequestResult res = handleUnbanRequestReal(request);
		modCache.clear();
		currentMod = null;
		return res;
	}
	
	protected UnbanRequestResult handleUnbanRequestReal(UnbanRequest request) {
		boolean isModOfPrim = isMod(config.getProperty("user.main_sub"));
		
		if(isModOfPrim) {
			TraditionalScammer trad = database.getTraditionalScammerMapping().fetchByPersonID(request.bannedPersonID);
			if(trad != null) {
				return authorizeRequest(request);
			}
		}
		
		USLAction action = database.getUSLActionMapping().fetchLatest(request.bannedPersonID);
		if(action == null) {
			if(isModOfPrim)
				return sendNoKnownPersonMessage(request);
			return sendGenericNotAuthorized(request);
		}
		
		if(!isModOfPrim) {
			Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
	
			List<USLActionBanHistory> attachedBans = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(action.id);
			List<USLActionHashtag> attachedTags = database.getUSLActionHashtagMapping().fetchByUSLActionID(action.id);
			
			List<HandledModActionJoinHistory> authorizingBans = new ArrayList<>();
			List<HandledModActionJoinHistory> preventingBans = new ArrayList<>();
			for(USLActionBanHistory attachedBan : attachedBans) {
				BanHistory ban = database.getBanHistoryMapping().fetchByID(attachedBan.banHistoryID);
				if(ban.modPersonID != bot.id) {
					HandledModAction hma = database.getHandledModActionMapping().fetchByID(ban.handledModActionID);
					MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
					
					if(!sub.readOnly) {
						List<SubscribedHashtag> subscribed = database.getSubscribedHashtagMapping().fetchForSubreddit(sub.id, false);
						List<Hashtag> tags = database.getHashtagMapping().fetchForSubscribed(subscribed);
						
						String descLower = ban.banDescription.toLowerCase();
						boolean foundTag = false;
						for(Hashtag tag : tags) {
							if(attachedTags.stream().anyMatch((a) -> a.hashtagID == tag.id) && descLower.contains(tag.tag.toLowerCase())) {
								foundTag = true;
								break;
							}
						}
						
						if(foundTag) {
							if(isMod(sub.subreddit)) {
								authorizingBans.add(new HandledModActionJoinHistory(hma, ban, null));
							}else {
								preventingBans.add(new HandledModActionJoinHistory(hma, ban, null));
							}
						}
					}
				}
			}
			
			if(authorizingBans.isEmpty()) 
				return sendGenericNotAuthorized(request);
			
			if(!preventingBans.isEmpty())
				return sendBanPrevented(request, authorizingBans, preventingBans);
			
			return authorizeRequest(request);
		}
		
		List<USLActionHashtag> tags = database.getUSLActionHashtagMapping().fetchByUSLActionID(action.id);
		if(tags == null)
			return sendNoTagsMessage(request);
		
		return authorizeRequest(request);
	}

	private UnbanRequestResult authorizeRequest(UnbanRequest request) {
		String titleFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_approved_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_approved_body_no_footer").responseBody;
		
		Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(request.bannedPersonID);
		String historyTable = USLHistoryMarkupFormatter.format(database, config, banned.id, true);

		USLAction action = database.getUSLActionMapping().fetchLatest(request.bannedPersonID);
		TraditionalScammer traditional = database.getTraditionalScammerMapping().fetchByPersonID(request.bannedPersonID);
		
		List<Hashtag> matching = new ArrayList<>();
		if(action != null) {
			List<USLActionHashtag> attachedTags = database.getUSLActionHashtagMapping().fetchByUSLActionID(action.id);
			
			for(int i = 0; i < attachedTags.size(); i++) {
				Hashtag tag = database.getHashtagMapping().fetchByID(attachedTags.get(i).hashtagID);
				matching.add(tag);
				
			}
		}else if(traditional != null) {
			List<Hashtag> allTags = database.getHashtagMapping().fetchAll();
			String descLower = traditional.description.toLowerCase();
			for(Hashtag tag : allTags) {
				if(descLower.contains(tag.tag.toLowerCase())) {
					matching.add(tag);
				}
			}
		}

		String tagsString = "none found";
		for(int i = 0; i < matching.size(); i++) {
			Hashtag tag = matching.get(i);
			
			if(i == 0) {
				tagsString = tag.tag;
			}else if(i == matching.size() - 1) {
				if(i == 1) {
					tagsString += " and " + tag.tag;
				}else {
					tagsString += ", and " + tag.tag;
				}
			}else {
				tagsString += ", " + tag.tag;
			}
		}
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("mod", mod.username);
		respInfo.addLongtermString("banned", banned.username);
		respInfo.addTemporaryString("history table", historyTable);
		respInfo.addTemporaryString("tags", tagsString);
		
		StringBuilder body = new StringBuilder(new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database));
		respInfo.clearTemporary();
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		if(traditional != null) {
			String appendFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_approved_tradscammer_append").responseBody;
			
			respInfo.addTemporaryString("reason", traditional.reason);
			respInfo.addTemporaryString("description", traditional.description);
			
			String append = new ResponseFormatter(appendFormat, respInfo).getFormattedResponse(config, database);
			body.append(append);
			
			respInfo.clearTemporary();
		}
		
		if(action != null) {
			Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
			
			List<HandledModActionJoinHistory> overridenBans = new ArrayList<>();
			List<USLActionBanHistory> attachedBans = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(action.id);
			
			for(USLActionBanHistory attached : attachedBans) {
				BanHistory ban = database.getBanHistoryMapping().fetchByID(attached.banHistoryID);
				
				if(ban.modPersonID != bot.id) { 
					HandledModAction hma = database.getHandledModActionMapping().fetchByID(ban.handledModActionID);
					
					MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
					if(!isMod(sub.subreddit)) {
						List<SubscribedHashtag> tagsForSub = database.getSubscribedHashtagMapping().fetchForSubreddit(sub.id, false);
						List<Hashtag> tags = database.getHashtagMapping().fetchForSubscribed(tagsForSub);
						
						String descLower = ban.banDescription.toLowerCase();
						for(Hashtag tg : tags) {
							if(descLower.contains(tg.tag.toLowerCase())) {
								overridenBans.add(new HandledModActionJoinHistory(hma, ban, null));
								break;
							}
						}
					}
				}
			}
			
			if(!overridenBans.isEmpty()) {
				String appendFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_approved_override_append").responseBody;
				
				DateFormat formatter = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
				StringBuilder overrideTable = new StringBuilder("Time|Subreddit|Moderator|Ban Note\n:--|:--|:--|:--\n");
				for(HandledModActionJoinHistory overrideBan : overridenBans) {
					sendBanPreventedHelper_FormatBanRow(formatter, overrideTable, overrideBan);
				}
				
				respInfo.addTemporaryString("override table", overrideTable.toString());
				String append = new ResponseFormatter(appendFormat, respInfo).getFormattedResponse(config, database);
				
				body.append(append);
				
				respInfo.clearTemporary();
			}
		}
		
		String footerFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_approved_footer").responseBody;
		
		String footer = new ResponseFormatter(footerFormat, respInfo).getFormattedResponse(config, database);
		body.append(footer);
		
		return new UnbanRequestResult(request, Collections.emptyList(), 
				Collections.singletonList(new UserPMInformation(mod, title, body.toString())),
				traditional, false);
	}

	private void sendBanPreventedHelper_FormatBanRow(DateFormat formatter, StringBuilder table, HandledModActionJoinHistory ban) {
		table.append(formatter.format(ban.handledModAction.occurredAt));
		table.append('|');
		
		MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByID(ban.handledModAction.monitoredSubredditID);
		table.append("/r/");
		table.append(sub.subreddit);
		table.append('|');
		
		Person authMod = database.getPersonMapping().fetchByID(ban.banHistory.modPersonID);
		table.append("/u/");
		table.append(authMod.username);
		table.append('|');
		
		table.append(ban.banHistory.banDescription);
		table.append('\n');
	}
	
	private UnbanRequestResult sendBanPrevented(UnbanRequest request, List<HandledModActionJoinHistory> authorizingBans,
			List<HandledModActionJoinHistory> preventingBans) {
		String titleFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_prevented_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_prevented_body").responseBody;
		
		Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(request.bannedPersonID);
		String historyTable = USLHistoryMarkupFormatter.format(database, config, banned.id, true);
		
		DateFormat formatter = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		StringBuilder authorizeTable = new StringBuilder("Time|Subreddit|Moderator|Ban Note\n:--|:--|:--|:--\n");
		for(HandledModActionJoinHistory authBan : authorizingBans) {
			sendBanPreventedHelper_FormatBanRow(formatter, authorizeTable, authBan);
		}
		
		StringBuilder preventionTable = new StringBuilder("Time|Subreddit|Moderator|Ban Note\n:--|:--|:--|:--\n");
		for(HandledModActionJoinHistory prevBan : preventingBans) {
			sendBanPreventedHelper_FormatBanRow(formatter, preventionTable, prevBan);
		}
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("mod", mod.username);
		respInfo.addLongtermString("banned", banned.username);
		respInfo.addTemporaryString("history table", historyTable);
		respInfo.addTemporaryString("prevent table", preventionTable.toString());
		respInfo.addTemporaryString("authorize table", authorizeTable.toString());
		
		String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
		respInfo.clearTemporary();
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		return new UnbanRequestResult(request, Collections.emptyList(), 
				Collections.singletonList(new UserPMInformation(mod, title, body)),
				null, true);
	}

	private UnbanRequestResult sendGenericNotAuthorized(UnbanRequest request) {
		String titleFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_generic_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_generic_body").responseBody;
		
		Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(request.bannedPersonID);
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("mod", mod.username);
		respInfo.addLongtermString("banned", banned.username);

		String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		return new UnbanRequestResult(request, Collections.emptyList(), 
				Collections.singletonList(new UserPMInformation(mod, title, body)),
				null, true);
	}

	private UnbanRequestResult sendNoTagsMessage(UnbanRequest request) {
		String titleFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_no_tags_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_no_tags_body").responseBody;
		
		Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(request.bannedPersonID);
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("mod", mod.username);
		respInfo.addLongtermString("banned", banned.username);
		respInfo.addTemporaryString("history table", USLHistoryMarkupFormatter.format(database, config, banned.id, true));
		
		String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
		respInfo.clearTemporary();
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		return new UnbanRequestResult(request, Collections.emptyList(), 
				Collections.singletonList(new UserPMInformation(mod, title, body)),
				null, true);
	}

	private UnbanRequestResult sendNoKnownPersonMessage(UnbanRequest request) {
		String titleFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_unknown_title").responseBody;
		String bodyFormat = database.getResponseMapping().fetchByName("unban_request_to_mod_denied_unknown_body").responseBody;
		
		Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
		Person banned = database.getPersonMapping().fetchByID(request.bannedPersonID);
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("mod", mod.username);
		respInfo.addLongtermString("banned", banned.username);

		String body = new ResponseFormatter(bodyFormat, respInfo).getFormattedResponse(config, database);
		String title = new ResponseFormatter(titleFormat, respInfo).getFormattedResponse(config, database);
		
		return new UnbanRequestResult(request, Collections.emptyList(), 
				Collections.singletonList(new UserPMInformation(mod, title, body)),
				null, true);
	}
}
