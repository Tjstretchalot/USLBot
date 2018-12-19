package me.timothy.bots.models;

import me.timothy.bots.database.MappingDatabase;

/**
 * Many-to-many mapping between usl actions and the ACTIVE hashtags. This mapping should contain all the
 * active tags that are on the person. It stops being updated when a new action is creaetd (i.e., new tags
 * should only be applied to the latest usl action). Note that when the tags change it requires either a 
 * new usl action or repropagation.
 * 
 * @author Timothy
 */
public class USLActionHashtag {
	/** The id of the USLAction that is being mapping to a hashtag */
	public int actionID;
	/** The hashtag that the person is banned with */
	public int hashtagID;
	
	/**
	 * Create a new mapping between these two. Only context can let you know if this is already
	 * in the database.
	 * @param actionID the id of the action
	 * @param hashtagID the tag that the actions person is banned with.
	 */
	public USLActionHashtag(int actionID, int hashtagID) {
		super();
		this.actionID = actionID;
		this.hashtagID = hashtagID;
	}
	
	/**
	 * If this is a valid entry in the database
	 * @return True if this passes the sniff test, false otherwise
	 */
	public boolean isValid() {
		return actionID > 0 && hashtagID > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actionID;
		result = prime * result + hashtagID;
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
		USLActionHashtag other = (USLActionHashtag) obj;
		if (actionID != other.actionID)
			return false;
		if (hashtagID != other.hashtagID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "USLActionHashtag [actionID=" + actionID + ", hashtagID=" + hashtagID + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[action=" + db.getUSLActionMapping().fetchByID(actionID) + ", hashtag=" + db.getHashtagMapping().fetchByID(hashtagID) + "]";
	}
}
