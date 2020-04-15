package me.timothy.bots.summon;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;
import me.timothy.bots.USLDatabase;
import me.timothy.bots.USLHistoryMarkupFormatter;
import me.timothy.bots.functions.IsModeratorFunction;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;
import me.timothy.bots.summon.SummonResponse.ResponseType;
import me.timothy.jreddit.info.Message;

/**
 * Handles pms from moderators of the form
 * 
 * $check /u/john
 * 
 * And then responds with just some information about that user
 * 
 * @author Timothy
 */
public class CheckPMSummon implements PMSummon, AuthCheckingSummon {
	private static Logger logger = LogManager.getLogger();
	
	/**
	 * The pattern we check for
	 */
	private static final Pattern CHECK_PATTERN = Pattern.compile("\\s*\\$check\\s/u/\\S+");
	/**
	 * The format passed to the response formatter to parse the group from the pattern
	 */
	private static final String CHECK_FORMAT = "$check <user_to_check>";
	/**
	 * A function to determine if the user is a mod on a subreddit
	 */
	private IsModeratorFunction isMod;
	
	@Override
	public SummonResponse handlePM(Message message, Database db, FileConfiguration config) {
		USLDatabase database = (USLDatabase)db;
		Matcher matcher = CHECK_PATTERN.matcher(message.body());
		
		String mainSub = config.getProperty("user.main_sub");
		String author = message.author();
		if(!isMod.isModerator(mainSub, author)) {
			return null;
		}
		
		String userToCheck = null;
		if(matcher.find()) {
			String group = matcher.group().trim();
			ResponseInfo responseInfo = ResponseInfoFactory.getResponseInfo(CHECK_FORMAT, group);
			
			userToCheck = responseInfo.getObject("user_to_check").toString();
		}else {
			return null;
		}

		Person personToCheck = database.getPersonMapping().fetchOrCreateByUsername(userToCheck);
		String historyTable = USLHistoryMarkupFormatter.format(database, config, personToCheck.id, false);
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("user_to_check", userToCheck);
		respInfo.addLongtermString("user_to_check_history", historyTable);
		
		Response titleResponse = database.getResponseMapping().fetchByName("check_from_pm_response_title");
		Response bodyResponse = database.getResponseMapping().fetchByName("check_from_pm_response_body");
		
		String titleString = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, db);
		String bodyString = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, db);
		
		return new SummonResponse(ResponseType.VALID, null, null, Arrays.asList(new PMResponse(message.author(), titleString, bodyString)));
	}

	@Override
	public void setIsModerator(IsModeratorFunction isMod) {
		logger.debug("CheckPMSummon setting isMod! null? " + (isMod == null ? "yes" : "no"));
		this.isMod = isMod;
	}

}