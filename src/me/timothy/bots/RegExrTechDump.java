package me.timothy.bots;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.timothy.bots.models.MonitoredSubreddit;
import me.timothy.bots.models.Person;

/**
 * Dumps the database in the format expected by the RegExrTech version of the
 * bot: https://github.com/RegExrTech/UniversalScammerList
 *
 * This corresponds to one relevant file - bans.json
 *
 * <pre>
 * {
 *   "username": {
 *     "tag": {
 *       "banned_by": "username",
 *       "issued_on": unix time integer seconds,
 *       "banned_on": "subreddit",
 *       "description": "text"
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Timothy
 */
public class RegExrTechDump {
	private final Logger logger;
	private final USLDatabase database;
	private final USLFileConfiguration config;

	public RegExrTechDump(USLDatabase database, List<MonitoredSubreddit> subreddits, USLFileConfiguration config) {
		this.logger = LogManager.getLogger();
		this.database = database;
		this.config = config;
	}

	/**
	 * Dumps the bans in the RegExrTech format to bans.json
	 */
	public void dump() {
		try (OutputStream os = new FileOutputStream("bans.json")) {
			dumpBans(os);
		} catch (IOException | SQLException e) {
			logger.throwing(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes the bans to the given file without loading the entire database into
	 * memory.
	 *
	 * The bans file has the following format
	 *
	 * <pre>
	 * {
	 *   "username": {
	 *     "tag": {
	 *       "banned_by": "username",
	 *       "issued_on": unix time integer seconds,
	 *       "banned_on": "subreddit",
	 *       "description": "text"
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * For each banned user, for each tag they are banned for, for the first
	 * subreddit that banned them with that tag, there should be an entry in
	 *
	 * bans[username][tag]
	 *
	 * @param os The stream to write the action queue to
	 * @throws IOException If one occurs while writing
	 */
	private void dumpBans(OutputStream os) throws IOException, SQLException {
		Person botPerson = database.getPersonMapping().fetchByUsername(config.getProperty("user.username"));
		Connection connection = database.getConnection();

		PrintWriter writer = new PrintWriter(os);
		try {
			writer.append('{');

			PreparedStatement statement = connection.prepareStatement(
					"SELECT persons.username, hashtags.tag, mod_persons.username, usl_actions.created_at, monitored_subreddits.subreddit, ban_histories.ban_description FROM usl_actions "
							+ "JOIN usl_action_hashtags ON usl_action_hashtags.usl_action_id = usl_actions.id "
							+ "JOIN hashtags ON hashtags.id = usl_action_hashtags.hashtag_id "
							+ "JOIN persons ON persons.id = usl_actions.person_id "
							+ "JOIN usl_action_ban_history ON usl_action_ban_history.usl_action_id = usl_actions.id "
							+ "JOIN ban_histories ON ban_histories.id = usl_action_ban_history.ban_history_id "
							+ "JOIN persons AS mod_persons ON mod_persons.id = ban_histories.mod_person_id "
							+ "JOIN handled_modactions ON handled_modactions.id = ban_histories.handled_modaction_id "
							+ "JOIN monitored_subreddits ON monitored_subreddits.id = handled_modactions.monitored_subreddit_id "
							+ "WHERE"
							+ "  ban_histories.mod_person_id != ?"
							+ "  AND ban_histories.ban_description LIKE CONCAT('%', hashtags.tag, '%')"
							+ "  AND handled_modactions.occurred_at = usl_actions.created_at "
							+ "ORDER BY persons.username ASC, hashtags.tag ASC "
							);
			statement.setInt(1, botPerson.id);
			ResultSet results = statement.executeQuery();

			String currentPersonUsername = null;
			while (results.next()) {
				String bannedPersonUsername = results.getString(1);
				String tag = results.getString(2);
				String modPersonUsername = results.getString(3);
				Timestamp bannedAt = results.getTimestamp(4);
				String bannedOn = results.getString(5);
				String banDescription = results.getString(6);

				if (!bannedPersonUsername.equals(currentPersonUsername)) {
					if (currentPersonUsername != null) {
						writer.append("},");
					}
					writer.append(quote(bannedPersonUsername));
					writer.append(":{");
					currentPersonUsername = bannedPersonUsername;
				} else {
					writer.append(",");
				}

				writer.append(quote(tag.substring(1)));
				writer.append(":{\"banned_by\":");
				writer.append(quote(modPersonUsername));
				writer.append(",\"issued_on\":");
				writer.append(Long.toString(bannedAt.getTime() / 1000));
				writer.append(",\"banned_on\":");
				writer.append(quote(bannedOn));
				writer.append(",\"description\":");
				writer.append(quote(banDescription));
				writer.append('}');
			}

			results.close();
			statement.close();

			statement = connection.prepareStatement(
					"SELECT persons.username, traditional_scammers.description, traditional_scammers.created_at "
					+ "FROM traditional_scammers "
					+ "JOIN persons ON persons.id = traditional_scammers.person_id "
					+ "ORDER BY persons.username ASC"
			);
			results = statement.executeQuery();

			Pattern tagPattern = Pattern.compile("#\\w+");
			Pattern subredditPattern = Pattern.compile("/r/\\w+");
			while (results.next()) {
				String bannedPersonUsername = results.getString(1);
				String description = results.getString(2);
				Timestamp bannedAt = results.getTimestamp(3);

				if (currentPersonUsername != null) {
					writer.append("},");
					currentPersonUsername = bannedPersonUsername;
				}

				writer.append(quote(bannedPersonUsername));
				writer.append(":{");

				String subreddit = "universalscammerlist";
				if (description != null) {
					Matcher subredditMatcher = subredditPattern.matcher(description);
					if (subredditMatcher.find()) {
						subreddit = subredditMatcher.group().substring(3);
					}
				}

				Matcher tagMatcher = tagPattern.matcher(description == null ? "" : description);
				String tag = "#scammer";
				if (tagMatcher.find()) {
					tag = tagMatcher.group();
				}
				boolean first = true;
				while(true) {
					if (!first) {
						writer.append(",");
					}
					first = false;

					writer.append(quote(tag.substring(1)));
					writer.append(":{\"banned_by\":\"uslbot\",\"issued_on\":");
					writer.append(Long.toString(bannedAt.getTime() / 1000));
					writer.append(",\"banned_on\":");
					writer.append(quote(subreddit));
					writer.append(",\"description\":");
					writer.append(quote("grandfathered; " + description));
					writer.append('}');

					if (!tagMatcher.find())
						break;

					tag = tagMatcher.group();
				}
			}

			results.close();
			statement.close();

			statement = connection.prepareStatement(
					"SELECT"
					+ "    outer_persons.username,"
					+ "    outer_ban_histories.ban_description,"
					+ "    outer_usl_actions.created_at "
					+ "FROM persons AS outer_persons "
					+ "JOIN usl_actions AS outer_usl_actions ON outer_usl_actions.person_id = outer_persons.id "
					+ "JOIN usl_action_ban_history AS outer_usl_action_ban_history ON outer_usl_action_ban_history.usl_action_id = outer_usl_actions.id "
					+ "JOIN ban_histories AS outer_ban_histories ON outer_usl_action_ban_history.ban_history_id = outer_ban_histories.id "
					+ "JOIN handled_modactions AS outer_handled_modactions ON outer_handled_modactions.id = outer_ban_histories.handled_modaction_id "
					+ "WHERE"
					+ "    NOT EXISTS ("
					+ "        SELECT 1 FROM usl_actions"
					+ "        JOIN usl_action_hashtags ON usl_action_hashtags.usl_action_id = usl_actions.id"
					+ "        JOIN hashtags ON hashtags.id = usl_action_hashtags.hashtag_id"
					+ "        JOIN persons ON persons.id = usl_actions.person_id"
					+ "        JOIN usl_action_ban_history ON usl_action_ban_history.usl_action_id = usl_actions.id"
					+ "        JOIN ban_histories ON ban_histories.id = usl_action_ban_history.ban_history_id"
					+ "        JOIN persons AS mod_persons ON mod_persons.id = ban_histories.mod_person_id"
					+ "        JOIN handled_modactions ON handled_modactions.id = ban_histories.handled_modaction_id"
					+ "        JOIN monitored_subreddits ON monitored_subreddits.id = handled_modactions.monitored_subreddit_id"
					+ "        WHERE"
					+ "          ban_histories.mod_person_id != ?"
					+ "          AND persons.id = outer_persons.id"
					+ "          AND ban_histories.ban_description LIKE CONCAT('%', hashtags.tag, '%')"
					+ "          AND handled_modactions.occurred_at = usl_actions.created_at"
					+ "          AND usl_actions.is_ban = 1"
					+ "          AND usl_actions.is_latest = 1"
					+ "    )"
					+ "    AND NOT EXISTS ("
					+ "        SELECT 1 FROM usl_action_ban_history"
					+ "        JOIN ban_histories ON ban_histories.id = usl_action_ban_history.ban_history_id"
					+ "        JOIN handled_modactions ON handled_modactions.id = ban_histories.handled_modaction_id"
					+ "        WHERE usl_action_ban_history.usl_action_id = outer_usl_actions.id"
					+ "          AND ban_histories.ban_details = 'permanent'"
					+ "          AND ("
					+ "            ("
					+ "              handled_modactions.occurred_at < outer_handled_modactions.occurred_at"
					+ "              AND (outer_ban_histories.ban_description IS NULL OR ban_histories.ban_description IS NOT NULL)"
					+ "            )"
					+ "            OR (outer_ban_histories.ban_description IS NULL AND ban_histories.ban_description IS NOT NULL)"
					+ "          )"
					+ "    )"
					+ "    AND NOT EXISTS ("
					+ "        SELECT 1 FROM traditional_scammers"
					+ "        WHERE traditional_scammers.person_id = outer_persons.id"
					+ "    )"
					+ "    AND outer_usl_actions.is_ban = 1"
					+ "    AND outer_usl_actions.is_latest = 1"
					+ "    AND outer_ban_histories.ban_details = 'permanent'"
			);
			statement.setInt(1, botPerson.id);
			results = statement.executeQuery();

			while (results.next()) {
				String bannedPersonUsername = results.getString(1);
				String description = results.getString(2);
				Timestamp bannedAt = results.getTimestamp(3);

				if (currentPersonUsername != null) {
					writer.append("},");
					currentPersonUsername = bannedPersonUsername;
				}

				writer.append(quote(bannedPersonUsername));
				writer.append(":{");

				String subreddit = "universalscammerlist";
				if (description != null) {
					Matcher subredditMatcher = subredditPattern.matcher(description);
					if (subredditMatcher.find()) {
						subreddit = subredditMatcher.group().substring(3);
					}
				}

				Matcher tagMatcher = tagPattern.matcher(description == null ? "" : null);
				String tag = "#scammer";
				if (tagMatcher.find()) {
					tag = tagMatcher.group();
				}
				boolean first = true;
				while(true) {
					if (!first) {
						writer.append(",");
					}
					first = false;

					writer.append(quote(tag.substring(1)));
					writer.append(":{\"banned_by\":\"uslbot\",\"issued_on\":");
					writer.append(Long.toString(bannedAt.getTime() / 1000));
					writer.append(",\"banned_on\":");
					writer.append(quote(subreddit));
					writer.append(",\"description\":");
					writer.append(quote("history lost; " + description));
					writer.append('}');

					if (!tagMatcher.find())
						break;

					tag = tagMatcher.group();
				}
			}

			statement.close();
			results.close();

			if (currentPersonUsername != null) {
				writer.append('}');
			}

			writer.append('}');
		} finally {
			writer.flush();
		}
	}

	private String quote(String raw) {
		StringBuilder sb = new StringBuilder(raw.length() + 2);
		sb.append('"');
		for (int idx = 0; idx < raw.length(); idx++) {
			char ch = raw.charAt(idx);
			switch (ch) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(ch);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (ch < ' ') {
					String t = "000" + Integer.toHexString(ch);
					sb.append("\\u" + t.substring(t.length() - 4));
				} else {
					sb.append(ch);
				}
			}
		}
		sb.append("\"");
		return sb.toString();
	}

}
