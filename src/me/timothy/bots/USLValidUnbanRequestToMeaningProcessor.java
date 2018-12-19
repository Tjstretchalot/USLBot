package me.timothy.bots;

import java.sql.Timestamp;
import java.util.List;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionBanHistory;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.USLActionUnbanHistory;
import me.timothy.bots.models.UnbanRequest;

/**
 * It is the job of this class to determine what the effect of an unban request is on the actual
 * list. It changes the USLAction of the person appropriately for the request made.
 * 
 * @author Timothy
 */
public class USLValidUnbanRequestToMeaningProcessor {
	protected MappingDatabase database;
	protected USLFileConfiguration config;
	
	public USLValidUnbanRequestToMeaningProcessor(MappingDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	/**
	 * Process the given unban request. This will update the USLAction for the person that was unbanned,
	 * requiring that they be sent to the propagator to decide what to do.
	 * 
	 * @param unbanRequest the request to process
	 */
	public void processUnbanRequest(UnbanRequest unbanRequest) {
		USLAction latest = database.getUSLActionMapping().fetchLatest(unbanRequest.bannedPersonID);
		
		if(latest == null)
			return;
		
		List<USLActionHashtag> tags = database.getUSLActionHashtagMapping().fetchByUSLActionID(latest.id);
		if(tags.isEmpty())
			return;
		
		USLAction newAct = database.getUSLActionMapping().create(false, unbanRequest.bannedPersonID, new Timestamp(System.currentTimeMillis()));
		
		List<USLActionBanHistory> bans = database.getUSLActionBanHistoryMapping().fetchByUSLActionID(latest.id);
		for(USLActionBanHistory bh : bans) {
			database.getUSLActionBanHistoryMapping().save(new USLActionBanHistory(newAct.id, bh.banHistoryID));
		}
		
		List<USLActionUnbanHistory> unbans = database.getUSLActionUnbanHistoryMapping().fetchByUSLActionID(latest.id);
		for(USLActionUnbanHistory ubh : unbans) {
			database.getUSLActionUnbanHistoryMapping().save(new USLActionUnbanHistory(newAct.id, ubh.unbanHistoryID));
		}
	}
}
