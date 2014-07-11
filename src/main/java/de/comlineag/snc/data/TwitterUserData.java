package de.comlineag.snc.data;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	data type
 * @version 	0.2		- 10.07.2014
 * @status		productive
 * 
 * @description Describes a single twitter user with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              user type save.
 * 
 * @param <JSonObject>
 *            "id" Long
 *            "sn_id" String
 *            "name" String
 *            "screen_name" String
 *            "geoLocation" List
 *            "followers_count" Long
 *            "friends_count" Long
 *            "statuses_count" Long
 *            "favourites_count" Long
 *            "lists_and_groups_count" Long
 *            "lang" String
 *            
 * @changelog	0.1 (Magnus)	class created
 * 				0.2 (Chris)		added support for counters
 * 
 */

public final class TwitterUserData extends UserData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Twitter the Data Object is prepared
	 * 
	 * @param jsonObject
	 */
	public TwitterUserData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of user (TW-"  + jsonObject.get("id") + ") from twitter user-object");
		logger.trace("  working on " + jsonObject.toString());
		
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;
		sn_id = SocialNetworks.TWITTER.getValue();
		username = null;
		screen_name = null;
		lang = null;
		geoLocation = null;
		followers_count = 0;
		friends_count = 0;
		postings_count = 0;
		favorites_count = 0;
		lists_and_groups_count = 0;
		
		setId((Long) jsonObject.get("id"));
		setUsername((String) jsonObject.get("name"));
		setScreenName((String) jsonObject.get("screen_name"));
		
		setLang((String) jsonObject.get("lang"));
		
		if (jsonObject.get("geoLocation") != null)
			setGeoLocation((String) jsonObject.get("geoLocation"));

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
	}
}