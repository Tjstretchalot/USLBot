package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a single person which the bot knows about.
 * 
 * @author Timothy
 */
public class Person {
	public int id;
	public String username;
	public String passwordHash;
	public String email;
	public int authLevel;
	public Timestamp createdAt;
	public Timestamp updatedAt;
	
	/**
	 * Create a new user with the given information. Use -1 for id and now for createdAt and updatedAt if this
	 * does not correspond with a database entry.
	 * 
	 * @param id the database unique identifier
	 * @param username login username
	 * @param passwordHash password hash
	 * @param authLevel the authorization level of the user
	 * @param createdAt when this user was created
	 * @param updatedAt when this user was last updated
	 */
	public Person(int id, String username, String passwordHash, String email, int authLevel, Timestamp createdAt, Timestamp updatedAt) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.email = email;
		this.authLevel = authLevel;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
	
	/**
	 * Check if this person passes some sanity checks for entering
	 * the database.
	 * 
	 * @return if this is probably a valid entry
	 */
	public boolean isValid() {
		return username != null && createdAt != null && updatedAt != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + authLevel;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + id;
		result = prime * result + ((passwordHash == null) ? 0 : passwordHash.hashCode());
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
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
		Person other = (Person) obj;
		if (authLevel != other.authLevel)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id != other.id)
			return false;
		if (passwordHash == null) {
			if (other.passwordHash != null)
				return false;
		} else if (!passwordHash.equals(other.passwordHash))
			return false;
		if (updatedAt == null) {
			if (other.updatedAt != null)
				return false;
		} else if (!updatedAt.equals(other.updatedAt))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", passwordHash=" + (passwordHash==null ? "null" : "<omitted not null>") 
				+ ", email=" + email + ", authLevel=" + authLevel + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
	}
}
