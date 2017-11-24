package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.RegisterAccountRequest;
import me.timothy.bots.models.Response;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * Manages sending register account requests
 * 
 * @author Timothy
 */
public class USLRegisterAccountRequestManager {
	protected USLDatabase database;
	protected USLFileConfiguration config;
	
	/**
	 * Initialize the account request manager attached to the specified 
	 * database and config
	 * 
	 * @param db the database
	 * @param cfg the config
	 */
	public USLRegisterAccountRequestManager(USLDatabase db, USLFileConfiguration cfg) 
	{
		this.database = db;
		this.config = cfg;
	}
	
	/**
	 * Determines what account request messages to send out
	 * 
	 * @return the pms to send out
	 */
	public List<UserPMInformation> sendRegisterAccountRequests() {
		List<RegisterAccountRequest> requests = database.getRegisterAccountRequestMapping().fetchUnsent(3);
		
		List<UserPMInformation> pms = new ArrayList<>();
		
		for(RegisterAccountRequest req : requests) {
			Person person = database.getPersonMapping().fetchByID(req.personID);
			
			if(person.passwordHash != null) {
				pms.add(getPMToAlreadyClaimedAccount(req, person));
			}else {
				pms.add(getPMToClaimAccount(req, person));
			}
		}
		
		return pms;
	}

	private UserPMInformation getPMToAlreadyClaimedAccount(RegisterAccountRequest req, Person person) {
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addTemporaryString("username", person.username);
		respInfo.addTemporaryString("id", Integer.toString(person.id));
		
		Response titleResponse = database.getResponseMapping().fetchByName("already_claimed_account_req_acc_reg_title");
		String title = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		Response bodyResponse = database.getResponseMapping().fetchByName("already_claimed_account_req_acc_reg_body");
		String body = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		return new UserPMInformation(person, title, body, createCallback(req));
	}
	
	private UserPMInformation getPMToClaimAccount(RegisterAccountRequest req, Person person) 
	{
		ResponseInfo respInfo = new ResponseInfo();
		respInfo.addTemporaryString("username", person.username);
		respInfo.addTemporaryString("id", Integer.toString(person.id));
		respInfo.addTemporaryString("token", req.token);
		
		Response titleResponse = database.getResponseMapping().fetchByName("claim_account_pm_title");
		String title = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		Response bodyResponse = database.getResponseMapping().fetchByName("claim_account_pm_body");
		String body = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		return new UserPMInformation(person, title, body, createCallback(req));
	}
	
	private Runnable createCallback(RegisterAccountRequest req) {
		return () -> {
			req.sentAt = new Timestamp(System.currentTimeMillis());
			database.getRegisterAccountRequestMapping().save(req);
		};
	}
}
