package me.timothy.bots.database;

import me.timothy.bots.models.PropagatorSetting;

/**
 * Maps the propagator settings to/from the database
 * 
 * @author Timothy
 */
public interface PropagatorSettingMapping extends ObjectMapping<PropagatorSetting> {
	/**
	 * Associates the given key with the given value, overwriting the existing value
	 * @param key the key
	 * @param value the value
	 */
	public void put(PropagatorSetting.PropagatorSettingKey key, String value);
	
	/**
	 * Fetches the value associated with the given key, if there is one. Otherwise returns null
	 * 
	 * @param key the key for the setting you are interested in
	 * @return the value for that key or null if there is not one
	 */
	public String get(PropagatorSetting.PropagatorSettingKey key);
}
