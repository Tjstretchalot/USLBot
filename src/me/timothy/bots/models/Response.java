package me.timothy.bots.models;

import java.sql.Timestamp;

/**
 * Describes a response in the database
 * 
 * @author Timothy
 *
 */
public class Response {
	public int id;
	public String name;
	public String responseBody;
	public Timestamp createdAt;
	public Timestamp updatedAt;
	
	public Response(int id, String name, String responseBody, Timestamp createdAt, Timestamp updatedAt) {
		this.id = id;
		this.name = name;
		this.responseBody = responseBody;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
	
	public Response() {
		this(-1, null, null, null, null);
	}

	/**
	 * Checks to make sure the response can be put
	 * in the database without breaking things
	 * 
	 * @return if this response is valid
	 */
	public boolean isValid() {
		return name != null && name.length() <= 255 && responseBody != null && createdAt != null && updatedAt != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((responseBody == null) ? 0 : responseBody.hashCode());
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
		Response other = (Response) obj;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (responseBody == null) {
			if (other.responseBody != null)
				return false;
		} else if (!responseBody.equals(other.responseBody))
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
		return "Response [id=" + id + ", name=" + name + ", responseBody=" + responseBody + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + "]";
	}
	
	
}
