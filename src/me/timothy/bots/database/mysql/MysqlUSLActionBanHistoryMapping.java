package me.timothy.bots.database.mysql;

import java.sql.Connection;
import java.util.List;

import me.timothy.bots.USLDatabase;
import me.timothy.bots.database.USLActionBanHistoryMapping;
import me.timothy.bots.models.USLActionBanHistory;

public class MysqlUSLActionBanHistoryMapping extends MysqlManyToManyMapping<USLActionBanHistory> implements USLActionBanHistoryMapping {

	public MysqlUSLActionBanHistoryMapping(USLDatabase database, Connection connection) {
		super(database, connection, USLActionBanHistory.class, "usl_action_ban_history", 
				"usl_action_id", "usl_actions", "actionID", 
				"ban_history_id", "ban_histories", "banHistoryID");
	}

	@Override
	public List<USLActionBanHistory> fetchByUSLActionID(int uslActionId) {
		return fetchByCol1(uslActionId);
	}

	@Override
	public List<USLActionBanHistory> fetchByBanHistoryID(int banHistoryId) {
		return fetchByCol2(banHistoryId);
	}

	@Override
	public boolean contains(int uslActionId, int banHistoryId) {
		return !fetchByBoth(uslActionId, banHistoryId).isEmpty();
	}

}
