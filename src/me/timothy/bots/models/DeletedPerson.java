package me.timothy.bots.models;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import me.timothy.bots.database.MappingDatabase;

/**
 * This is a simple mapping for persons which have been deleted.
 * 
 * @author Timothy
 */
public class DeletedPerson {
	/** The id of the person which has been deleted */
	public int personID;
	/** When we detected that this person was deleted */
	public Timestamp detectedDeletedAt;
	
	/**
	 * @param personID the id of the person who deleted their account
	 * @param detectedDeletedAt when we detected the account was deleted
	 */
	public DeletedPerson(int personID, Timestamp detectedDeletedAt) {
		super();
		this.personID = personID;
		this.detectedDeletedAt = detectedDeletedAt;
	}
	
	/**
	 * Determines if this is a potentialy viable row
	 * 
	 * @return if this row is probably good
	 */
	public boolean isValid() {
		return personID > 0 && detectedDeletedAt == null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detectedDeletedAt == null) ? 0 : detectedDeletedAt.hashCode());
		result = prime * result + personID;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DeletedPerson other = (DeletedPerson) obj;
		if (detectedDeletedAt == null) {
			if (other.detectedDeletedAt != null) {
				return false;
			}
		} else if (!detectedDeletedAt.equals(other.detectedDeletedAt)) {
			return false;
		}
		if (personID != other.personID) {
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return "DeletedPerson [personID=" + personID + ", detectedDeletedAt=" + detectedDeletedAt + "]";
	}
	
	public String toPrettyString(MappingDatabase database) {
		return "[person=" + database.getPersonMapping().fetchByID(personID).username + ", detectedDeletedAt=" + 
				SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(detectedDeletedAt);
	}
}
