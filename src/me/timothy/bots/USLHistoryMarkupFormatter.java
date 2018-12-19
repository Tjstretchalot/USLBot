package me.timothy.bots;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.PriorityQueue;

import me.timothy.bots.database.MappingDatabase;
import me.timothy.bots.models.BanHistory;
import me.timothy.bots.models.HandledModAction;
import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.USLAction;
import me.timothy.bots.models.USLActionHashtag;
import me.timothy.bots.models.UnbanHistory;
import me.timothy.bots.models.UnbanRequest;

/**
 * This class produces the history of a person on the USL in a reddit-markup way.
 * 
 * @author Timothy
 */
public class USLHistoryMarkupFormatter {
	private static class HistoryItem implements Comparable<HistoryItem> {
		public HandledModAction hma;
		public BanHistory ban;
		public UnbanHistory unban;
		public UnbanRequest request;
		
		public USLAction action;
		
		public HistoryItem(HandledModAction hma, BanHistory ban) {
			this.ban = ban;
			this.hma = hma;
		}
		
		public HistoryItem(HandledModAction hma, UnbanHistory unban) {
			this.hma = hma;
			this.unban = unban;
		}
		
		public HistoryItem(USLAction action) {
			this.action = action;
		}
		
		public HistoryItem(UnbanRequest request) {
			this.request = request;
		}
		
		public Timestamp getTime() {
			if(hma != null)
				return hma.occurredAt;
			
			if(action != null)
				return action.createdAt;
			
			if(request != null)
				return request.handledAt;
			
			throw new AssertionError();
		}

		@Override
		public int compareTo(HistoryItem other) {
			Timestamp mine = getTime();
			Timestamp his = other.getTime();
			
			if(mine.equals(his)) {
				if(request != null) {
					if(other.request == null)
						return -1;
					return 0;
				}else if(other.request != null) {
					return 1;
				}
				
				if(unban != null) {
					if(other.unban == null)
						return -1;
					return 0;
				}else if(other.unban != null) {
					return 1;
				}
				
				return 0;
			}
			
			return mine.compareTo(his);
		}
		
		public void appendToTable(StringBuilder report, MappingDatabase database) {
			Timestamp time = getTime();
			
			DateFormat formatter = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			report.append(formatter.format(time));
			report.append('|');
			
			if(ban != null) {
				report.append("BAN|");
				
				MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
				Person mod = database.getPersonMapping().fetchByID(ban.modPersonID);
				report.append("/u/").append(mod.username).append(" banned with note '").append(ban.banDescription).append("' on /r/");
				report.append(sub.subreddit);
			}else if(unban != null) {
				report.append("UNBAN|");

				MonitoredSubreddit sub = database.getMonitoredSubredditMapping().fetchByID(hma.monitoredSubredditID);
				Person mod = database.getPersonMapping().fetchByID(unban.modPersonID);
				report.append("/u/").append(mod.username).append(" unbanned ").append(" on /r/").append(sub.subreddit);
			}else if(request != null) {
				report.append("UNBAN REQUEST|");
				Person mod = database.getPersonMapping().fetchByID(request.modPersonID);
				if(request.invalid) {
					report.append("/u/").append(mod.username).append(" requested that I unban but was denied");
				}else {
					report.append("/u/").append(mod.username).append(" requested that I unban and was approved");
				}
			}else {
				report.append("LIST UPDATE|");

				report.append("New list tags: ");
				List<USLActionHashtag> actTags = database.getUSLActionHashtagMapping().fetchByUSLActionID(action.id);
				for(int i = 0; i < actTags.size(); i++) {
					Hashtag tag = database.getHashtagMapping().fetchByID(actTags.get(i).hashtagID);
					
					if(i != 0) {
						report.append(", ");
					}
					report.append('\'').append(tag.tag).append('\'');
				}
			}
		}
	}
	
	/**
	 * This function produces a string representation about the history of the given person on the
	 * universal scammer list. The exact wording and format are subject to change, but broadly it
	 * covers the following information:
	 * 
	 * <ul>
	 * <li>Any bans by actual people</li>
	 * <li>The state of the person now</li>
	 * <li>The USLAction's generated against the person</li>
	 * </ul>
	 * 
	 * 
	 * @param database the database
	 * @param config the file configuration
	 * @param personId the id of the person to generate a report on
	 * @param includeBotActivity true if we suspect the bot's activity is unexpected, false otherwise
	 * @return the report.
	 */
	public static String format(MappingDatabase database, FileConfiguration config, int personId, boolean includeBotActivity) {
		Person bot = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		Person person = database.getPersonMapping().fetchByID(personId);
		
		List<BanHistory> bansOnPerson = database.getBanHistoryMapping().fetchBanHistoriesByPerson(personId);
		List<UnbanHistory> unbansOnPerson = database.getUnbanHistoryMapping().fetchByPerson(personId);
		List<USLAction> actionsOnPerson = database.getUSLActionMapping().fetchByPerson(personId);
		List<UnbanRequest> requestsOnPerson = database.getUnbanRequestMapping().fetchHandledByBannedPerson(personId);
		
		PriorityQueue<HistoryItem> sortedHistory = new PriorityQueue<HistoryItem>();
		for(BanHistory bh : bansOnPerson) {
			if(includeBotActivity || bh.modPersonID != bot.id) {
				HandledModAction hma = database.getHandledModActionMapping().fetchByID(bh.handledModActionID);
				sortedHistory.offer(new HistoryItem(hma, bh));
			}
		}
		
		for(UnbanHistory ubh : unbansOnPerson) {
			if(includeBotActivity || ubh.modPersonID != bot.id) { 
				HandledModAction hma = database.getHandledModActionMapping().fetchByID(ubh.handledModActionID);
				sortedHistory.offer(new HistoryItem(hma, ubh));
			}
		}
		
		for(USLAction action : actionsOnPerson) {
			sortedHistory.offer(new HistoryItem(action));
		}
		
		for(UnbanRequest req : requestsOnPerson) {
			sortedHistory.offer(new HistoryItem(req));
		}

		StringBuilder result = new StringBuilder();
		result.append("# USL Generated Report: /u/").append(person.username);
		result.append(" (Bot Activity: ").append(includeBotActivity ? "Included" : "Not included").append(")\n\n");
		
		result.append("Time|Action|Description\n");
		result.append(":--|:--|:--\n");
		
		while(!sortedHistory.isEmpty()) {
			HistoryItem item = sortedHistory.poll();
			item.appendToTable(result, database);
			result.append("\n");
		}
		
		return result.toString();
	}
}
