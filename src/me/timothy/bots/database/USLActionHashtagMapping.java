package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.USLActionHashtag;

/**
 * This is the mapping for the many-to-many relationship between USLActions and Hashtags.
 * Recall that the hashtags are always the *active bans* for a person, regardless of "is_ban". This
 * means you can always just look for the latest entry for a person and congratulations you know
 * about all the active tags.
 * 
 * Changing this after-the-fact requires repropagation.
 * 
 * @author Timothy
 */
public interface USLActionHashtagMapping extends ObjectMapping<USLActionHashtag> {
	/**
	 * Fetch all the active banned hashtags for the given action id
	 * 
	 * @param actionID the action id you are interested in
	 * @return the active tags for that action
	 */
	public List<USLActionHashtag> fetchByUSLActionID(int actionID); 
}
