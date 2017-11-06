package me.timothy.bots;

import java.util.ArrayList;
import java.util.List;

import me.timothy.bots.memory.ModmailPMInformation;
import me.timothy.bots.memory.TraditionalScammerHandlerResult;
import me.timothy.bots.memory.UserBanInformation;
import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.Response;
import me.timothy.bots.models.SubscribedHashtag;
import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * This class determines what actions need to take place as a result of 
 * a given entry in the traditional scammer list.
 * 
 * 
 * @author Timothy Moore
 */
public class USLTraditionalScammerHandler {
	/** The database containing all the relational mappings */
	protected USLDatabase database;
	
	/** All the simple configuration that's saved in flatfile formats */
	protected USLFileConfiguration config;
	
	/**
	 * Create a new traditional scammer handler tied to the specified relational
	 * database and flat-file configuration
	 * 
	 * @param database the database
	 * @param config the configuration
	 */
	public USLTraditionalScammerHandler(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	/**
	 * Determines what to do about us knowing that we have the specified entry in traditional scammers
	 * and the specified entry in the suberddit
	 * 
	 * @param scammer the scammer entry
	 * @param subreddit the subreddit entry
	 * @return what to do about it
	 */
	public TraditionalScammerHandlerResult handleTraditionalScammer(TraditionalScammer scammer, MonitoredSubreddit subreddit) {
		List<UserBanInformation> bans = new ArrayList<>();
		List<ModmailPMInformation> modmailPMs = new ArrayList<>();
		List<UserPMInformation> userPMs = new ArrayList<>();
		
		if(!USLUtils.potentiallyNotBannedOnSubreddit(database, scammer.personID, subreddit)) // don't ban if he's definitely already banned
			return new TraditionalScammerHandlerResult(scammer, subreddit, bans, modmailPMs, userPMs);
		
		List<SubscribedHashtag> relevant = USLUtils.getRelevantTags(database, config, subreddit, scammer.description.toLowerCase());
		if(relevant.isEmpty())
			return new TraditionalScammerHandlerResult(scammer, subreddit, bans, modmailPMs, userPMs);
		
		Person scammerPerson = database.getPersonMapping().fetchByID(scammer.personID);
		if(!subreddit.writeOnly) {
			ResponseInfo banMessageInfo = new ResponseInfo(ResponseInfoFactory.base);
			Response banMessageResponse = database.getResponseMapping().fetchByName("traditional_scammer_banned_ban_message");
			String banMessageString = new ResponseFormatter(banMessageResponse.responseBody, banMessageInfo).getFormattedResponse(config, database);
			bans.add(new UserBanInformation(scammerPerson, subreddit, banMessageString, "other", "USL list (" + scammer.description + ")"));

			
			if(!subreddit.silent) {
				String tagsString = USLUtils.combineTagsWithCommas(relevant);
				
				ResponseInfo modmailInfo = new ResponseInfo(ResponseInfoFactory.base);
				modmailInfo.addLongtermString("banned person", scammerPerson.username);
				modmailInfo.addLongtermString("description", scammer.description);
				modmailInfo.addLongtermString("tags", tagsString);
				
				Response titleResponse = database.getResponseMapping().fetchByName("traditional_scammer_banned_title");
				Response bodyResponse = database.getResponseMapping().fetchByName("traditional_scammer_banned_body");
				
				String titleString = new ResponseFormatter(titleResponse.responseBody, modmailInfo).getFormattedResponse(config, database);
				String bodyString = new ResponseFormatter(bodyResponse.responseBody, modmailInfo).getFormattedResponse(config, database);
				
				modmailPMs.add(new ModmailPMInformation(subreddit, titleString, bodyString));
			}
		}
		
		
		return new TraditionalScammerHandlerResult(scammer, subreddit, bans, modmailPMs, userPMs);
	}
}
