package me.timothy.bots.models;

/**
 * This acts like a queue of all the persons which have actions that need to be sent to the 
 * propagator.
 * 
 * Dirty here is in the same sense as a dirty bit https://en.wikipedia.org/wiki/Dirty_bit - it
 * lets us know if the action has been modified since we have last looked at it. It doesn't mean
 * that the person is unkempt.
 * 
 * @author Timothy
 */
public class DirtyPerson {
	/** The id of the person which needs to be rescanned */
	public int personID;

	/**
	 * @param personId the person which needs to be sent to the propagator
	 */
	public DirtyPerson(int personId) {
		super();
		this.personID = personId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		DirtyPerson other = (DirtyPerson) obj;
		if (personID != other.personID) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DirtyPerson [personId=" + personID + "]";
	}
}
