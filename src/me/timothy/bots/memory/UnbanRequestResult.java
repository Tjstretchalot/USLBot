package me.timothy.bots.memory;

import java.util.List;

import me.timothy.bots.models.TraditionalScammer;
import me.timothy.bots.models.UnbanRequest;

/**
 * What to do as a result of an unban request
 * 
 * @author Timothy Moore
 * @see me.timothy.bots.models.UnbanRequest
 */
public class UnbanRequestResult {
	/** The request that generated this result */
	public final UnbanRequest unbanRequest;
	
	/** The unbans that should occur */
	public final List<UserUnbanInformation> unbans;
	
	/** The subreddits that should be pmd */
	public final List<ModmailPMInformation> modmailPMs;
	
	/** The users that should be pmd */
	public final List<UserPMInformation> userPMs;
	
	/** The scammer that should be deleted from the list */
	public final TraditionalScammer scammerToRemove;
	
	/** If this request was invalid */
	public final boolean invalid;

	/**
	 * @param unbanRequest the unban request that this was generated from
	 * @param unbans unbans that should occur
	 * @param modmailPMs modmail pms that should occur
	 * @param userPMs user pms that should occur
	 * @param scammerToRemove the scammer to remove from the list, or null
	 * @param invalid if this request was invalid
	 */
	public UnbanRequestResult(UnbanRequest unbanRequest, List<UserUnbanInformation> unbans, List<ModmailPMInformation> modmailPMs,
			List<UserPMInformation> userPMs, TraditionalScammer scammerToRemove, boolean invalid) {
		this.unbanRequest = unbanRequest;
		this.unbans = unbans;
		this.modmailPMs = modmailPMs;
		this.userPMs = userPMs;
		this.scammerToRemove = scammerToRemove;
		this.invalid = invalid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (invalid ? 1231 : 1237);
		result = prime * result + ((modmailPMs == null) ? 0 : modmailPMs.hashCode());
		result = prime * result + ((scammerToRemove == null) ? 0 : scammerToRemove.hashCode());
		result = prime * result + ((unbanRequest == null) ? 0 : unbanRequest.hashCode());
		result = prime * result + ((unbans == null) ? 0 : unbans.hashCode());
		result = prime * result + ((userPMs == null) ? 0 : userPMs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UnbanRequestResult))
			return false;
		UnbanRequestResult other = (UnbanRequestResult) obj;
		if (invalid != other.invalid)
			return false;
		if (modmailPMs == null) {
			if (other.modmailPMs != null)
				return false;
		} else if (!modmailPMs.equals(other.modmailPMs))
			return false;
		if (scammerToRemove == null) {
			if (other.scammerToRemove != null)
				return false;
		} else if (!scammerToRemove.equals(other.scammerToRemove))
			return false;
		if (unbanRequest == null) {
			if (other.unbanRequest != null)
				return false;
		} else if (!unbanRequest.equals(other.unbanRequest))
			return false;
		if (unbans == null) {
			if (other.unbans != null)
				return false;
		} else if (!unbans.equals(other.unbans))
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
		return "UnbanRequestResult [unbanRequest=" + unbanRequest + ", unbans=" + unbans + ", modmailPMs=" + modmailPMs
				+ ", userPMs=" + userPMs + ", scammerToRemove=" + scammerToRemove + ", invalid=" + invalid + "]";
	}
}
