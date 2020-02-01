package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final Logger logger = LogManager.getLogger();
	
	protected USLDatabase database;
	protected USLFileConfiguration config;
	protected DeletedPersonManager deletedPersons;
	
	/**
	 * Initialize the account request manager attached to the specified 
	 * database and config
	 * 
	 * @param db the database
	 * @param cfg the config
	 */
	public USLRegisterAccountRequestManager(USLDatabase db, USLFileConfiguration cfg, DeletedPersonManager deletedPersons) 
	{
		this.database = db;
		this.config = cfg;
		this.deletedPersons = deletedPersons;
	}
	
	/**
	 * Determines what account request messages to send out
	 * 
	 * @param limit The maximum number of pms to return
	 * @return the pms to send out
	 */
	public List<UserPMInformation> sendRegisterAccountRequests() {
		int limit = 3;
		String prop = config.getProperty("register_account_requests.limit_per_loop");
		if (prop != null) {
			try {
				limit = Integer.valueOf(prop);
			}catch(NumberFormatException e) {
				logger.catching(e);
			}
		}
		
		
		List<UserPMInformation> pms = new ArrayList<>();
		while(true) {
			int skips = 0;
			List<RegisterAccountRequest> requests = database.getRegisterAccountRequestMapping().fetchUnsent(limit);
			
			Timestamp oldestAllowed = new Timestamp(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7);
			for(RegisterAccountRequest req : requests) {
				Person person = database.getPersonMapping().fetchByID(req.personID);
				
				if(req.createdAt.before(oldestAllowed)) {
					logger.printf(Level.INFO, "Skipping register account request from /u/%s - did not get to it in time", person.username);
					req.sentAt = new Timestamp(System.currentTimeMillis());
					database.getRegisterAccountRequestMapping().save(req);
					skips++;
					continue;
				}
				if(deletedPersons.isDeleted(person.username)) {
					logger.printf(Level.INFO, "Skipping register account request from deleted user /u/%s", person.username);
					req.sentAt = new Timestamp(System.currentTimeMillis());
					database.getRegisterAccountRequestMapping().save(req);
					skips++;
					continue;
				}
				
				if(person.passwordHash != null) {
					pms.add(getPMToAlreadyClaimedAccount(req, person));
				}else {
					pms.add(getPMToClaimAccount(req, person));
				}
			}
			
			if(skips < limit)
				break;
			logger.printf(Level.DEBUG, "Register account request manager skipped all requests we fetched - retrying (limit=%d)", limit);
		}
		logger.printf(Level.DEBUG, "Register account manager found %d requests (limit=%d)", pms.size(), limit);
		
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
