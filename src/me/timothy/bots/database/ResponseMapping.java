package me.timothy.bots.database;

import me.timothy.bots.models.Response;

/**
 * Describes a response mapping
 * 
 * @author Timothy
 */
public interface ResponseMapping extends ObjectMapping<Response> {
	/**
	 * Fetches the responses with the specified name
	 * @param name the name of the response 
	 * @return the response with that name or null
	 */
	public Response fetchByName(String name);
}
