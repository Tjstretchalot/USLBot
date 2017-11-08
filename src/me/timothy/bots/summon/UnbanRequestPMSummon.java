package me.timothy.bots.summon;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;
import me.timothy.bots.USLDatabase;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;
import me.timothy.bots.summon.SummonResponse.ResponseType;
import me.timothy.jreddit.info.Message;

/**
 * Handles pms from moderators of the form
 * 
 * $unban /u/john
 * 
 * And then it places an unban request on their behalf, which is handled later
 * 
 * @author Timothy
 */
public class UnbanRequestPMSummon implements PMSummon {
	/**
	 * The pattern we check for
	 */
	private static final Pattern UNBAN_PATTERN = Pattern.compile("\\s*\\$unban\\s/u/\\S+");
	/**
	 * The format passed to the response formatter to parse the group from the pattern
	 */
	private static final String UNBAN_FORMAT = "$unpaid <to_unban>";
	
	@Override
	public SummonResponse handlePM(Message message, Database db, FileConfiguration config) {
		USLDatabase database = (USLDatabase)db;
		Matcher matcher = UNBAN_PATTERN.matcher(message.body());
		
		boolean respond = false;
		List<String> usersToUnban = new ArrayList<>();
		
		long now = System.currentTimeMillis();
		Person author = database.getPersonMapping().fetchOrCreateByUsername(message.author());
		while(matcher.find()) {
			respond = true;
			
			String group = matcher.group().trim();
			ResponseInfo responseInfo = ResponseInfoFactory.getResponseInfo(UNBAN_FORMAT, group);
			
			String toUnban = responseInfo.getObject("to_unban").toFormattedString(responseInfo, "to_unban", config, db);
			usersToUnban.add(toUnban);
			
			Person toUnbanPerson = database.getPersonMapping().fetchOrCreateByUsername(toUnban);
			
			UnbanRequest request = new UnbanRequest(-1, author.id, toUnbanPerson.id, new Timestamp(now), null, false);
			database.getUnbanRequestMapping().save(request);
		}
		
		if(!respond)
			return null;
		
		String usersToUnbanString = String.join(", ", usersToUnban);
		
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addLongtermString("users_to_unban", usersToUnbanString);
		
		Response titleResponse = database.getResponseMapping().fetchByName("unban_request_from_pm_response_title");
		Response bodyResponse = database.getResponseMapping().fetchByName("unban_request_from_pm_response_body");
		
		String titleString = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, db);
		String bodyString = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, db);
		
		return new SummonResponse(ResponseType.VALID, null, null, Arrays.asList(new PMResponse(message.author(), titleString, bodyString)));
	}

}
