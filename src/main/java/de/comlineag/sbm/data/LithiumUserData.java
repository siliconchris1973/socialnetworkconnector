package de.comlineag.sbm.data;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * 
 * @author Christian Guenther
 * @category data type
 * 
 * @description Describes a single lithium user with all relevant informations.
 *              The class shall be used to make all methods handling a lithium
 *              user type save.
 * 
 * @param <JSonObject>
 *            "id" Long
 *            "sn_id" String
 *            "name" String
 *            "screen_name" String
 *            "location" List
 *            "followers_count" Long
 *            "friends_count" Long
 *            "statuses_count" Long
 *            "favourites_count" Long
 *            "lists_and_groups_count" Long
 *            "lang" String
 */

public final class LithiumUserData extends UserData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public LithiumUserData() {}
	
	/**
	 * Constructor, based on the JSONObject sent from Lithium the Data Object is prepared
	 * 
	 * @param jsonObject
	 */
	//TODO Check if lithium sends a JSON Object or an aml 
	public LithiumUserData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of user (ID: " + jsonObject.get("id") + ") from lithium user-object");
		logger.trace("  working on " + jsonObject.toString());
		
		id = 0;
		sn_id = SocialNetworks.LITHIUM.getValue();
		username = null;
		screen_name = null;
		location = null;
		followers_count = 0;
		friends_count = 0;
		postings_count = 0;
		favorites_count = 0;
		lists_and_groups_count = 0;
		lang = null;
		
		setId((Long) jsonObject.get("id"));
		setUsername((String) jsonObject.get("name"));
		setScreenName((String) jsonObject.get("screen_name"));
		setLocation((String) jsonObject.get("location"));
		if (jsonObject.get("followers_count") != null)
			setFollowersCount((Long) jsonObject.get("followers_count"));
		if (jsonObject.get("friends_count") != null)
			setFriendsCount((Long) jsonObject.get("friends_count"));
		if (jsonObject.get("statuses_count") != null)
			setPostingsCount((Long) jsonObject.get("statuses_count"));
		if (jsonObject.get("favorites_count") != null)
			setFavoritesCount((Long) jsonObject.get("favorites_count"));
		if (jsonObject.get("lists_and_groups_count") != null)
			setListsAndGroupsCount((Long) jsonObject.get("lists_and_groups_count"));
		setLang((String) jsonObject.get("lang"));
	}
}