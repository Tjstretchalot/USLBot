package me.timothy.bots;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.Response;
import me.timothy.bots.responses.ResponseFormatter;
import me.timothy.bots.responses.ResponseFormatter.ExpectedKey;

/**
 * Contains some utility functions for working with responses.
 * 
 * @author Timothy
 */
public class ResponseUtils {
	/**
	 * Verify that the given response is in the database and that it only uses the specified keys.
	 * 
	 * @param database the database to verify inside of
	 * @param response the name of the response to verify
	 * @param desc the description of the response, used for error messages
	 * @param keys the keys that you are able to substitute, i.e., 'name' (no quotes)
	 */
	public static void verifyFormat(MappingDatabase database, String response, String desc, String... keys) {
		Response responseObj = database.getResponseMapping().fetchByName(response);
		if(responseObj == null) {
			throw new AssertionError("Missing response '" + response + "': " + desc);
		}
		
		ExpectedKey[] expKeys = new ExpectedKey[keys.length / 2];
		for(int keyInd = 0; keyInd < expKeys.length; keyInd++) {
			expKeys[keyInd] = new ExpectedKey(keys[keyInd * 2], keys[keyInd * 2 + 1]);
		}
		
		ResponseFormatter.verifyFormat(responseObj.responseBody, "Failed to validate '" + response + "' - " + desc + ":\n\n", expKeys);
	}
}
