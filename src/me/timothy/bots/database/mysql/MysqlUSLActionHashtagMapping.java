package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.util.List;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.USLActionHashtagMapping;
import me.timothy.bots.models.USLActionHashtag;

public class MysqlUSLActionHashtagMapping extends MysqlManyToManyMapping<USLActionHashtag> implements USLActionHashtagMapping {

	public MysqlUSLActionHashtagMapping(USLDatabase database, Connection connection) {
		super(database, connection, USLActionHashtag.class, 
				"usl_action_hashtags", 
				"usl_action_id", "usl_actions", "actionID", 
				"hashtag_id", "hashtags", "hashtagID");
	}

	@Override
	public List<USLActionHashtag> fetchByUSLActionID(int actionID) {
		return fetchByCol1(actionID);
	}

}
