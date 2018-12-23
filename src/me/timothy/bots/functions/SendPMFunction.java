package me.timothy.bots.functions;

import me.timothy.bots.memory.UserPMInformation;

/**
 * Describes a function which sends a pm given the pm information
 * 
 * @author Timothy
 */
public interface SendPMFunction {
	/**
	 * Sends the given pm described by the pm information
	 * @param pmInfo the pm to send
	 */
	public void send(UserPMInformation pmInfo);
}
