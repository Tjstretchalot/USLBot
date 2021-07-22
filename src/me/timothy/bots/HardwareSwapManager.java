package me.timothy.bots;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;

import me.timothy.bots.database.ActionLogMapping;
import me.timothy.bots.models.HardwareSwapAction;
import me.timothy.bots.models.HardwareSwapBan;
import me.timothy.bots.models.Person;
import me.timothy.bots.models.UnbanRequest;
import me.timothy.bots.summon.CommentSummon;
import me.timothy.bots.summon.LinkSummon;
import me.timothy.bots.summon.PMSummon;
import me.timothy.jreddit.RedditUtils;
import me.timothy.jreddit.info.Listing;
import me.timothy.jreddit.info.Wikipage;

/**
 * Hardware swap wants their bans to be fetched from the wiki without the need for the USLBot
 * to be a mod on their subreddit. We manage this by a separate account (USLBotHelper) which 
 * communicates with the USLBot in the standard way - it bans them on /r/UniversalScammerList
 * when they are posted to the wiki, and requests they are unbanned from there when they are
 * removed from the wiki.
 * 
 * @author Timothy
 */
public class HardwareSwapManager extends BotDriver {
	protected DeletedPersonManager deletedPersonManager;
	
	/**
	 * Initialize the manager with the USLDatabase, USLFileConfiguration, and our bot (which should use
	 * the hwswapuser. prefix)
	 * 
	 * @param database the database to use
	 * @param config the file configuration
	 * @param bot the bot for bannign
	 * @param dpm the instance which should check for deleted accounts
	 */
	public HardwareSwapManager(Database database, FileConfiguration config, Bot bot, DeletedPersonManager dpm) {
		super(database, config, bot, new CommentSummon[0], new PMSummon[0], new LinkSummon[0]);
		
		userConfigPrefix = "hwswapuser.";
		deletedPersonManager = dpm;
		
		try {
			Integer.parseInt(config.getProperty("hwswap.max_bans_per_loop"));
		}catch(NumberFormatException e) {
			logger.throwing(e);
			logger.error("hwswap.max_bans_per_loop is not a number");
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void doLoop() {
		ActionLogMapping al = ((USLDatabase)database).getActionLogMapping();
		al.clear();
		
		logger.debug("Starting HardwareSwapManager loop...");		
		maybeLoginAgain();
		
		logger.trace("Checking HardwareSwap wiki");
		al.append("Checking HardwareSwap wiki");
		updateWikiBans();
		
		logger.trace("Propagating HardwareSwap actions");
		al.append("Propagating HardwareSwap actions");
		propagate();
		
		logger.debug("Finished HardwareSwapManager loop");
	}
	
	protected String getLastKnownRevisionID() {
		final Path savePath = Paths.get(config.getProperty("hwswap.savepath"));
		if(!Files.exists(savePath)) {
			return null;
		}
		
		try(BufferedReader br = new BufferedReader(new FileReader(savePath.toFile()))) {
			return br.readLine();
		}catch(IOException e) {
			logger.catching(e);
			return null;
		}
	}
	
	protected void setLastKnownRevisionID(String id) {
		final Path savePath = Paths.get(config.getProperty("hwswap.savepath"));
		
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(savePath.toFile()))) {
			bw.write(id);
			bw.write("\n");
		}catch(IOException e) {
			logger.catching(e);
		}
	}
	
	protected String getLatestRevisionID() {
		return new Retryable<String>("fetch hardwareswap banlist latest revision id", maybeLoginAgainRunnable) {
			@Override
			protected String runImpl() throws Exception {
				Listing revisions = RedditUtils.getWikiRevisions("hardwareswap", "banlist", 1, bot.getUser());
				return revisions.getChild(0).getString("id");
			}
		}.run();
	}
	
	protected Wikipage getBanlist() {
		return new Retryable<Wikipage>("fetch hardwareswap banlist", maybeLoginAgainRunnable) {
			@Override
			protected Wikipage runImpl() throws Exception {
				return RedditUtils.getWikipage("hardwareswap", "banlist", bot.getUser());
			} 
		}.run();
	}
	
	private void warnStrangeLine(String line, String reason) {
		logger.printf(Level.WARN, "Strange line in hardware wiki banlist: '%s' (strange because %s)", line, reason);
	}
	
	protected void updateWikiBans() {
		USLDatabase db = (USLDatabase)database;
		
		final String lastKnownRevisionID = getLastKnownRevisionID();
		final String currentRevisionID = getLatestRevisionID();
		if(lastKnownRevisionID != null) {
			sleepFor(BRIEF_PAUSE_MS);
			if(currentRevisionID.equals(lastKnownRevisionID))
				return;
			logger.trace("Determined hardwareswap updated banlist");
			db.getActionLogMapping().append("Determined hardwareswap updated banlist");
		}else {
			logger.trace("Fetching hardwareswap banlist for the first time");
			db.getActionLogMapping().append("Fetching hardwareswap banlist for the first time");
		}
		
		Wikipage wiki = getBanlist();
		sleepFor(BRIEF_PAUSE_MS);
		Set<Integer> persons = new HashSet<>();
		
		try(BufferedReader br = new BufferedReader(new StringReader(wiki.contentMarkdown()))) {
			String line;
			while((line = br.readLine()) != null) {
				if(!line.substring(0, 5).equals("* /u/")) {
					warnStrangeLine(line, "should start with '* /u/'");
					continue;
				}
				
				int firstSpace = line.indexOf(' ', 5);
				if(firstSpace < 0) {
					warnStrangeLine(line, "missing reason");
					continue;
				}
				
				boolean bad = false;
				for(int i = 5; i < firstSpace; i++) {
					char ch = line.charAt(i);
					if(!Character.isAlphabetic(ch) && !Character.isDigit(ch) && ch != '-' && ch != '_') {
						warnStrangeLine(line, "invalid username (has invalid character '" + ch + "' at index " + (i+5) + " in username)");
						bad = true;
						break;
					}
				}
				if(bad)
					continue;
				
				String username = line.substring(5, firstSpace);
				String reason = line.substring(firstSpace + 1);
				
				Person pers = db.getPersonMapping().fetchOrCreateByUsername(username);
				persons.add(pers.id);
				
				if(db.getHardwareSwapBanMapping().fetchByPersonID(pers.id) == null) {
					logger.printf(Level.INFO, "Detected new ban from hardwareswap: /u/%s (reason: %s)", username, reason);
					db.getActionLogMapping().append("Detected new ban from hardwareswap: /u/" + username + " (reason: " + reason + ")");
					db.getHardwareSwapBanMapping().save(new HardwareSwapBan(-1, pers.id, reason, new Timestamp(System.currentTimeMillis())));
				}
			}
		}catch(IOException e) {
			logger.error("Should never get an io error reading from a StringReader!");
			logger.throwing(e);
			throw new RuntimeException(e);
		}
		
		List<HardwareSwapBan> unbanned = db.getHardwareSwapBanMapping().fetchWherePersonNotIn(persons);
		for(HardwareSwapBan unban : unbanned) { 
			Person pers = db.getPersonMapping().fetchByID(unban.personID);
			logger.printf(Level.INFO, "Detected hardwareswap unbanned /u/%s (old reason: %s)", pers.username, unban.note);
			db.getActionLogMapping().append("Detected hardwareswap unbanned /u/" + pers.username + " (old reason: " +  unban.note + ")");
			db.getHardwareSwapBanMapping().deleteByID(unban.id);
		}
		
		setLastKnownRevisionID(currentRevisionID);
	}
	
	protected void propagate() {
		// We will perform unbans without delay, but we will stagger bans
		propagateUnbans();
		propagateBans(Integer.valueOf(config.getProperty("hwswap.max_bans_per_loop")));
	}
	
	protected void propagateUnbans() {
		USLDatabase db = (USLDatabase) database;
		Person me = db.getPersonMapping().fetchByUsername(config.getProperty(userConfigPrefix + "username"));
		
		List<HardwareSwapAction> toUnban = db.getHardwareSwapActionMapping().fetchActionsOnUnbanned();
		for(HardwareSwapAction act : toUnban) {
			Person pers = db.getPersonMapping().fetchByID(act.personID);
			if(deletedPersonManager.isDeleted(pers.username)) {
				db.getActionLogMapping().append("hardwareswap unbanned /u/" + pers.username + " but he deleted his account so no need to do anything");
				logger.printf(Level.TRACE, "hardwareswap unbanned /u/%s - a deleted account (no action taken)");
				
				db.getHardwareSwapActionMapping().deleteByID(act.id);
				continue;
			}
			
			db.getActionLogMapping().append("Queuing unban for /u/" + pers.username);
			logger.printf(Level.DEBUG, "Queueing unban for /u/%s (id=%d) (from hardwareswap)", pers.username, pers.id);
			db.getUnbanRequestMapping().save(new UnbanRequest(-1, me.id, pers.id, new Timestamp(System.currentTimeMillis()), null, false));
			db.getHardwareSwapActionMapping().deleteByID(act.id);
		}
	}
	
	protected void propagateBans(int max) {
		final String banSub = config.getProperty("hwswap.bansub");
		if(banSub == null)
			throw new RuntimeException("bansub cannot be null");
		
		USLDatabase db = (USLDatabase) database;
		List<HardwareSwapBan> toBan = db.getHardwareSwapBanMapping().fetchWithoutAction(max);
		
		if(toBan.size() > 0) {
			db.getActionLogMapping().append("Handling " + toBan.size() + " bans from hardwareswap (requested " + max + ")");
			logger.printf(Level.DEBUG, "Handling %d bans from hardwareswap (max per loop: %d)", toBan.size(), max);
		}
		
		for(HardwareSwapBan ban : toBan) {
			Person pers = db.getPersonMapping().fetchByID(ban.personID);
			if(deletedPersonManager.isDeleted(pers.username)) {
				db.getActionLogMapping().append("hardwareswap banned /u/" + pers.username + " but he deleted his account so no need to do anything");
				logger.printf(Level.TRACE, "hardwareswap banned /u/%s - a deleted account (no action taken)", pers.username);
				db.getHardwareSwapActionMapping().save(new HardwareSwapAction(-1, pers.id, HardwareSwapAction.BAN_ACTION, new Timestamp(System.currentTimeMillis())));
				continue;
			}
			
			db.getActionLogMapping().append("hardwareswap banned /u/" + pers.username + " - banning him on " + banSub);
			logger.printf(Level.INFO, "Banning /u/%s on /r/%s - reason: %s", pers.username, banSub, "hardwareswap " + ban.note);
			super.handleBanUser(banSub, pers.username, "Replication of /r/hardwareswap ban", "other", "hardwareswap " + ban.note);
			db.getHardwareSwapActionMapping().save(new HardwareSwapAction(-1, pers.id, HardwareSwapAction.BAN_ACTION, new Timestamp(System.currentTimeMillis())));
		}
	}
}
