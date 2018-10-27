package me.timothy.bots.functions;

import me.timothy.bots.memory.TemporaryAuthGranterResult;

/**
 * A function that can handle a TemporaryAuthGranterResult
 * 
 * @author Timothy
 */
public interface TemporaryAuthGrantResultHandlerFunction {
	/**
	 * Handle the given result of processing a TemporaryAuthRequest
	 * 
	 * @param result the result to process
	 */
	public void handleResult(TemporaryAuthGranterResult result);
}
