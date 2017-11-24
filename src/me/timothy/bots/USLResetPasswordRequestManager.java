package me.timothy.bots;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import me.timothy.bots.memory.UserPMInformation;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.ResetPasswordRequest;
import me.timothy.bots.models.Response;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseInfo;
import me.timothy.bots.responses.ResponseInfoFactory;

/**
 * Manages reset password requests
 * 
 * @author Timothy
 */
public class USLResetPasswordRequestManager {
	protected USLDatabase database;
	protected USLFileConfiguration config;
	
	/**
	 * Create a new ResetPasswordRequest manager attached to the specified database
	 * and file configuration.
	 * 
	 * @param database the database
	 * @param config the file configuration
	 */
	public USLResetPasswordRequestManager(USLDatabase database, USLFileConfiguration config) {
		this.database = database;
		this.config = config;
	}
	
	public List<UserPMInformation> sendResetPasswordRequests() {
		List<ResetPasswordRequest> resetPassReqs = database.getResetPasswordRequestMapping().fetchUnsent(3);
		List<UserPMInformation> res = new ArrayList<>();
		
		for(ResetPasswordRequest req : resetPassReqs) {
			Person person = database.getPersonMapping().fetchByID(req.personID);
			
			if(person.passwordHash == null) {
				res.add(getPMForUnclaimed(req, person));
			}else {
				res.add(getPMForClaimed(req, person));
			}
		}
		
		return res;
	}

	private UserPMInformation getPMForUnclaimed(ResetPasswordRequest req, Person person) {
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addTemporaryString("username", person.username);
		respInfo.addTemporaryString("id", Integer.toString(person.id));
		
		Response titleResponse = database.getResponseMapping().fetchByName("unclaimed_account_reset_password_title");
		String title = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		Response bodyResponse = database.getResponseMapping().fetchByName("unclaimed_account_reset_password_body");
		String body = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		return new UserPMInformation(person, title, body, createCallback(req));
	}

	private UserPMInformation getPMForClaimed(ResetPasswordRequest req, Person person) {
		ResponseInfo respInfo = new ResponseInfo(ResponseInfoFactory.base);
		respInfo.addTemporaryString("username", person.username);
		respInfo.addTemporaryString("id", Integer.toString(person.id));
		respInfo.addTemporaryString("token", req.token);
		
		Response titleResponse = database.getResponseMapping().fetchByName("reset_password_title");
		String title = new ResponseFormatter(titleResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		Response bodyResponse = database.getResponseMapping().fetchByName("reset_password_body");
		String body = new ResponseFormatter(bodyResponse.responseBody, respInfo).getFormattedResponse(config, database);
		
		return new UserPMInformation(person, title, body, createCallback(req));
	}
	
	private Runnable createCallback(ResetPasswordRequest request) {
		return () -> {
			request.sentAt = new Timestamp(System.currentTimeMillis());
			database.getResetPasswordRequestMapping().save(request);
		};
	}
}
