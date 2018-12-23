package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a request from an authorized user to have the bot redecipher the reddit actions
 * into the meaning for the USL. This needs to happen when reddit actions may have gained new
 * meaning they didn't have previously (for example, theres a new tag) or a new subreddit joins
 * (need to process their history), or previous actions lose their meaning (a tag is removed).
 * 
 * Repropagating has the most predictable and battle-tested behavior for modifying the usl logic
 * in general. It requires about 2 hours at the time of writing (12/22/2018) to complete.
 * 
 * @author Timothy
 */
public class RepropagationRequest {
	/** The id of this row in the database */
	public int id;
	/** The id of the person who made this request */
	public int requestingPersonID;
	/** The reason to repropagate. This does not effect anything except the messages sent out / posts made */
	public String reason;
	/** True if this request was approved, false otherwise */
	public boolean approved;
	/** When this row was added to the database / when the user placed the request */
	public Timestamp receivedAt;
	/** When this row was processed by the request handler. */
	public Timestamp handledAt;
	
	/**
	 * @param id the id for the database row or -1 if not in the database yet
	 * @param requestingPersonID the id of the person making the request
	 * @param reason the reason for the request
	 * @param approved if handled, then true if approved and false if rejected. otherwise false
	 * @param receivedAt the time when the request was placed
	 * @param handledAt the time when the request was handled
	 */
	public RepropagationRequest(int id, int requestingPersonID, String reason, boolean approved, Timestamp receivedAt,
			Timestamp handledAt) {
		super();
		this.id = id;
		this.requestingPersonID = requestingPersonID;
		this.reason = reason;
		this.approved = approved;
		this.receivedAt = receivedAt;
		this.handledAt = handledAt;
	}
	
	/**
	 * Determines if this is a potentially valid row in the database
	 * 
	 * @return if this is a potentially valid row in the database
	 */
	public boolean isValid() {
		return requestingPersonID > 0 && reason != null && receivedAt != null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (approved ? 1231 : 1237);
		result = prime * result + ((handledAt == null) ? 0 : handledAt.hashCode());
		result = prime * result + id;
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
		result = prime * result + ((receivedAt == null) ? 0 : receivedAt.hashCode());
		result = prime * result + requestingPersonID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepropagationRequest other = (RepropagationRequest) obj;
		if (approved != other.approved)
			return false;
		if (handledAt == null) {
			if (other.handledAt != null)
				return false;
		} else if (!handledAt.equals(other.handledAt))
			return false;
		if (id != other.id)
			return false;
		if (reason == null) {
			if (other.reason != null)
				return false;
		} else if (!reason.equals(other.reason))
			return false;
		if (receivedAt == null) {
			if (other.receivedAt != null)
				return false;
		} else if (!receivedAt.equals(other.receivedAt))
			return false;
		if (requestingPersonID != other.requestingPersonID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RepropagationRequest [id=" + id + ", requestingPersonID=" + requestingPersonID + ", reason=" + reason
				+ ", approved=" + approved + ", receivedAt=" + receivedAt + ", handledAt=" + handledAt + "]";
	}
}
