package me.timothy.bots.memory;

import java.util.List;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.TraditionalScammer;

/**
 * Describes what actions need to be taken place due to an entry in the 
 * traditional scammer list and a subreddit being a monitored subreddit.
 * 
 * @author Timothy
 */
public class TraditionalScammerHandlerResult {
	/** The entry that was considered */
	public final TraditionalScammer scammer;
	
	/** The subreddit that was considered */
	public final MonitoredSubreddit subreddit;
	
	/** The bans due to entry / subreddit combo */
	public final List<UserBanInformation> bans;
	
	/** The subreddits that are messaged due to the entry / subreddit combo */
	public final List<ModmailPMInformation> modmailPMs;
	
	/** The users that are messaged due to the entry / subreddit combo */
	public final List<UserPMInformation> userPMs;

	/**
	 * @param scammer the entry that was considered
	 * @param subreddit the subreddit that was considered
	 * @param bans the bans resulting from the scammer / subreddit combo
	 * @param modmailPMs the modmail pms resulting from the scammer / subreddit combo
	 * @param userPMs the user pms resulting from the scammer / subreddit combo
	 */
	public TraditionalScammerHandlerResult(TraditionalScammer scammer, MonitoredSubreddit subreddit,
			List<UserBanInformation> bans, List<ModmailPMInformation> modmailPMs, List<UserPMInformation> userPMs) {
		super();
		this.scammer = scammer;
		this.subreddit = subreddit;
		this.bans = bans;
		this.modmailPMs = modmailPMs;
		this.userPMs = userPMs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bans == null) ? 0 : bans.hashCode());
		result = prime * result + ((modmailPMs == null) ? 0 : modmailPMs.hashCode());
		result = prime * result + ((scammer == null) ? 0 : scammer.hashCode());
		result = prime * result + ((subreddit == null) ? 0 : subreddit.hashCode());
		result = prime * result + ((userPMs == null) ? 0 : userPMs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TraditionalScammerHandlerResult))
			return false;
		TraditionalScammerHandlerResult other = (TraditionalScammerHandlerResult) obj;
		if (bans == null) {
			if (other.bans != null)
				return false;
		} else if (!bans.equals(other.bans))
			return false;
		if (modmailPMs == null) {
			if (other.modmailPMs != null)
				return false;
		} else if (!modmailPMs.equals(other.modmailPMs))
			return false;
		if (scammer == null) {
			if (other.scammer != null)
				return false;
		} else if (!scammer.equals(other.scammer))
			return false;
		if (subreddit == null) {
			if (other.subreddit != null)
				return false;
		} else if (!subreddit.equals(other.subreddit))
			return false;
		if (userPMs == null) {
			if (other.userPMs != null)
				return false;
		} else if (!userPMs.equals(other.userPMs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TraditionalScammerHandlerResult [scammer=" + scammer + ", subreddit=" + subreddit + ", bans=" + bans
				+ ", modmailPMs=" + modmailPMs + ", userPMs=" + userPMs + "]";
	}
}
