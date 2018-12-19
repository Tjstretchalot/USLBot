package me.timothy.bots.database;

import java.util.List;

import me.timothy.bots.models.Hashtag;
import me.timothy.bots.models.SubscribedHashtag;

/**
 * Maps Hashtags to/from the database.
 * 
 * @author Timothy
 */
public interface HashtagMapping extends ObjectMapping<Hashtag> {
	/**
	 * Fetch the hashtag by the given id
	 * @param id the id of the tag
	 * @return the hashtag with the corresponding id, or null if there is no such tag
	 */
	public Hashtag fetchByID(int id);
	
	/**
	 * Fetch all the hashtags that correspond with the list of subscribed hashtags.
	 * @param subscribed
	 * @return
	 */
	public List<Hashtag> fetchForSubscribed(List<SubscribedHashtag> subscribed);
}
