package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.util.List;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.USLActionUnbanHistoryMapping;
import me.timothy.bots.models.USLActionUnbanHistory;

public class MysqlUSLActionUnbanHistoryMapping extends MysqlManyToManyMapping<USLActionUnbanHistory> implements USLActionUnbanHistoryMapping {

	public MysqlUSLActionUnbanHistoryMapping(USLDatabase database, Connection connection) {
		super(database, connection, USLActionUnbanHistory.class, "usl_action_unban_history", 
				"usl_action_id", "usl_actions", "actionID", 
				"unban_history_id", "unban_histories", "unbanHistoryID");
	}

	@Override
	public List<USLActionUnbanHistory> fetchByUSLActionID(int uslActionId) {
		return fetchByCol1(uslActionId);
	}

	@Override
	public List<USLActionUnbanHistory> fetchByUnbanHistoryID(int unbanHistoryId) {
		return fetchByCol2(unbanHistoryId);
	}

}
