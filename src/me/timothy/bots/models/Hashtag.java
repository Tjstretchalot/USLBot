package me.timothy.bots.models;

import java.sql.Timestamp;

import me.timothy.bots.database.MappingDatabase;

/**
 * Describes a "hashtag", or just a tag that is put into ban notes that has some shared meaning.
 *  
 * @author Timothy
 */
public class Hashtag {
	/** The id of the row in the database, or -1 if not in the database yet. */
	public int id;
	/** The actual tag. i.e. '#scammer' */
	public String tag;
	/** A description for the tag. This should be valid markdown code */
	public String description;
	/** The id of the person who submitted this tag */
	public int submitterPersonId;
	/** The id of the person who last updated this tag */
	public int lastUpdatedByPersonId;
	/** When this tag was created at */
	public Timestamp createdAt;
	/** When this tag was last updated */
	public Timestamp updatedAt;
	
	/**
	 * Create a new instance of a Hashtag
	 * 
	 * @param id the id for the row in the database, or -1 if not in the database yet
	 * @param tag the actual tag, i.e '#scammer'
	 * @param description the description of the tag in valid markdown
	 * @param submitterPersonId the id of the person who submitted this tag
	 * @param lastUpdatedByPersonId the id o the person who last updated this tag
	 * @param createdAt when this tag was created
	 * @param updatedAt when this tag was last updated
	 */
	public Hashtag(int id, String tag, String description, int submitterPersonId, int lastUpdatedByPersonId,
			Timestamp createdAt, Timestamp updatedAt) {
		super();
		this.id = id;
		this.tag = tag;
		this.description = description;
		this.submitterPersonId = submitterPersonId;
		this.lastUpdatedByPersonId = lastUpdatedByPersonId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Determines if this is potentially a reasonable row to add into the database
	 * @return if this is a valid row
	 */
	public boolean isValid() {
		return (tag != null && description != null && submitterPersonId > 0 && 
				lastUpdatedByPersonId > 0 && createdAt != null && updatedAt != null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result + lastUpdatedByPersonId;
		result = prime * result + submitterPersonId;
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
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
		Hashtag other = (Hashtag) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id != other.id)
			return false;
		if (lastUpdatedByPersonId != other.lastUpdatedByPersonId)
			return false;
		if (submitterPersonId != other.submitterPersonId)
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		if (updatedAt == null) {
			if (other.updatedAt != null)
				return false;
		} else if (!updatedAt.equals(other.updatedAt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Hashtag [id=" + id + ", tag=" + tag + ", description=" + description + ", submitterPersonId="
				+ submitterPersonId + ", lastUpdatedByPersonId=" + lastUpdatedByPersonId + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + "]";
	}
	
	public String toPrettyString(MappingDatabase db) {
		return "[id=" + id + ", tag=" + tag + "]"; // typically we don't care about the other stuff when printing
	}
}
