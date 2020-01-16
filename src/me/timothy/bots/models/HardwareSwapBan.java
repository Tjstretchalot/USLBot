package me.timothy.bots.models;

import java.sql.Timestamp;

import me.timothy.bots.USLDatabase;

/**
 * Describes the ban information fetched from the hardware swap banlist. This does not have any information
 * on what we've done with a ban, just mirrors the wiki
 * 
 * @author Timothy
 */
public class HardwareSwapBan {
	/** Database row identifier */
	public int id;
	/** The id of the person associated with this row */
	public int personID;
	/** The description provided alongside this person */
	public String note;
	/** When we detected this ban */
	public Timestamp detectedAt;
	
	/**
	 * @param id the database identifier or -1 if not in the database
	 * @param personID the id of the person who is banned
	 * @param note the note for why the person is ban
	 * @param detectedAt when we detected this row
	 */
	public HardwareSwapBan(int id, int personID, String note, Timestamp detectedAt) {
		super();
		this.id = id;
		this.personID = personID;
		this.note = note;
		this.detectedAt = detectedAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detectedAt == null) ? 0 : detectedAt.hashCode());
		result = prime * result + id;
		result = prime * result + ((note == null) ? 0 : note.hashCode());
		result = prime * result + personID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HardwareSwapBan))
			return false;
		HardwareSwapBan other = (HardwareSwapBan) obj;
		if (detectedAt == null) {
			if (other.detectedAt != null)
				return false;
		} else if (!detectedAt.equals(other.detectedAt))
			return false;
		if (id != other.id)
			return false;
		if (note == null) {
			if (other.note != null)
				return false;
		} else if (!note.equals(other.note))
			return false;
		if (personID != other.personID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HardwareSwapBan [id=" + id + ", personID=" + personID + ", note=" + note + ", detectedAt=" + detectedAt
				+ "]";
	}
	
	/**
	 * Makes a slightly prettier version of this ban by fetching the persons name along with their id
	 * @param db the database containing this entry
	 * @return a string representation of this entry
	 */
	public String toPrettyString(USLDatabase db) {
		if(id == -1)
			return toString();
		
		Person person = db.getPersonMapping().fetchByID(personID);
		String personName = person != null ? person.username : "[INVALID ROW]";

		return "HardwareSwapBan [id=" + id + ", person=" + "[username=" + personName + ", id=" + personID + "], note=" + note + ", detectedAt=" + detectedAt
				+ "]";
	}
}
