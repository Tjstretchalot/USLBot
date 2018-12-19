package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.HashtagMapping;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.SubscribedHashtag;

public class MysqlHashtagMapping extends MysqlObjectWithIDMapping<Hashtag> implements HashtagMapping {
	private static final Logger logger = LogManager.getLogger();

	public MysqlHashtagMapping(USLDatabase database, Connection connection) {
		super(database, connection, "hashtags", 
				new MysqlColumn(Types.INTEGER, "id", true),
				new MysqlColumn(Types.VARCHAR, "tag"),
				new MysqlColumn(Types.LONGVARCHAR, "description"),
				new MysqlColumn(Types.INTEGER, "submitter_person_id"),
				new MysqlColumn(Types.INTEGER, "last_updated_by_person_id"),
				new MysqlColumn(Types.TIMESTAMP, "created_at"),
				new MysqlColumn(Types.TIMESTAMP, "updated_at"));
	}

	@Override
	public void save(Hashtag a) throws IllegalArgumentException {
		if(!a.isValid())
			throw new IllegalArgumentException(a + " is not valid!");
		
		if(a.createdAt != null) { a.createdAt.setNanos(0); }
		if(a.updatedAt != null) { a.updatedAt.setNanos(0); }
		
		try {
			PreparedStatement statement;
			if(a.id <= 0) {
				statement = connection.prepareStatement("INSERT INTO " + table + " (tag, description, submitter_person_id, last_updated_by_person_id, "
						+ "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			}else {
				statement = connection.prepareStatement("UPDATE " + table + " SET tag=?, description=?, submitter_person_id=?, last_updated_by_person_id=?, "
						+ "created_at=?, updated_at=? WHERE id=?");
			}
			
			int counter = 1;
			statement.setString(counter++, a.tag);
			statement.setString(counter++, a.description);
			statement.setInt(counter++, a.submitterPersonId);
			statement.setInt(counter++, a.lastUpdatedByPersonId);
			statement.setTimestamp(counter++, a.createdAt);
			statement.setTimestamp(counter++, a.updatedAt);
			
			if(a.id > 0) {
				statement.setInt(counter++, a.id);
				statement.execute();
			}else {
				statement.execute();
				
				ResultSet keys = statement.getGeneratedKeys();
				if(!keys.next()) {
					keys.close();
					statement.close();
					throw new RuntimeException("Expected generated keys for table " + table);
				}
				a.id = keys.getInt(1);
				keys.close();
			}
			statement.close();
		}catch(SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Hashtag fetchFromSet(ResultSet set) throws SQLException {
		return new Hashtag(set.getInt("id"), set.getString("tag"), set.getString("description"), 
				set.getInt("submitter_person_id"), set.getInt("last_updated_by_person_id"), 
				set.getTimestamp("created_at"), set.getTimestamp("updated_at"));
	}
	
	@Override
	public List<Hashtag> fetchForSubscribed(List<SubscribedHashtag> subTags) {
		List<Hashtag> result = new ArrayList<>();
		for(int i = 0; i < subTags.size(); i++) {
			result.add(fetchByID(subTags.get(i).hashtagID));
		}
		return result;
	}

	@Override
	protected void createTable() throws SQLException {
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE " + table + " ("
				+ "id INT NOT NULL AUTO_INCREMENT, "
				+ "tag VARCHAR(63) NOT NULL,"
				+ "description TEXT NOT NULL, "
				+ "submitter_person_id INT NOT NULL, "
				+ "last_updated_by_person_id INT NOT NULL, "
				+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', "
				+ "PRIMARY KEY(id), "
				+ "UNIQUE KEY unique_ht_tag (tag),"
				+ "INDEX ind_ht_subm_pers_id (submitter_person_id), "
				+ "INDEX ind_ht_upd_by_pers_id (last_updated_by_person_id), "
				+ "FOREIGN KEY (submitter_person_id) REFERENCES persons(id), "
				+ "FOREIGN KEY (last_updated_by_person_id) REFERENCES persons(id)"
				+ ")");
		statement.close();
	}

}
