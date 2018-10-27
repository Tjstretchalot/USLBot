package me.timothy.bots.memory;

import java.util.List;

import me.timothy.bots.models.Person;

/**
 * The result of the temporary auth granter
 * 
 * @author Timothy
 */
public class TemporaryAuthGranterResult {
	/** The person who requested authorization */
	public final Person requester;
	
	/** If the person was verified as a moderator */
	public final boolean verified;
	
	/** The user pms that should be sent */
	public final List<UserPMInformation> userPMs;
	
	/** The subreddit pms that should be sent */
	public final List<ModmailPMInformation> subredditPMs;

	/**
	 * Creates a new temporary auth granter with the given information
	 * 
	 * @param requester the person who requested
	 * @param verified if they were verified
	 * @param userPMs the pms to send out
	 * @param subredditPMs the sub pms to send out
	 */
	public TemporaryAuthGranterResult(Person requester, boolean verified, List<UserPMInformation> userPMs, List<ModmailPMInformation> subredditPMs) {
		this.requester = requester;
		this.verified = verified;
		this.userPMs = userPMs;
		this.subredditPMs = subredditPMs;
	}
}
