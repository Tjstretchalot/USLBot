package me.timothy.bots.summon;

import me.timothy.bots.functions.IsModeratorFunction;;

/**
 * Describes a summon which requires checking if users are moderators of
 * particular subreddits in order to respond.
 * 
 * @author Timothy
 */
public interface AuthCheckingSummon {
	/**
	 * Inject the given function for checking if a given user is a moderator of
	 * a particular subreddit.
	 * @param isMod The function that checks if a user is a mod on a sub.
	 */
	public void setIsModerator(IsModeratorFunction isMod);
}
