package me.timothy.bots.models;

/**
 * Just a basic fullname that can be mapped to the database
 * in order to prevent the bot from reusing the same one
 * 
 * @author Timothy
 *
 */
public class Fullname {
	public int id;
	public String fullname;

	/**
	 * Creates the fullname using the specified fullname
	 * @param id the id of the fullname
	 * @param fullname the fullname to use
	 */
	public Fullname(int id, String fullname) {
		this.id = id;
		this.fullname = fullname;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((fullname == null) ? 0 : fullname.hashCode());
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
		Fullname other = (Fullname) obj;
		if (fullname == null) {
			if (other.fullname != null)
				return false;
		} else if (!fullname.equals(other.fullname))
			return false;
		else if(other.id != id) 
			return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "Fullname [id=" + id + ", fullname=" + fullname + "]";
	}
}
