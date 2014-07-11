package de.comlineag.snc.data;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.1		- 10.07.2014
 * @status		in development
 * 
 * @description Describes a single facebook user with all relevant informations.
 *              The class shall be used to make all methods handling a facebook
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
 * @changelog	0.1 (Chris)		class copied from TwitterUserData revision 0.3
 * 
 */

public final class FacebookUserData extends UserData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Facebook the Data Object is prepared
	 * 
	 * @param jsonObject
	 */
	public FacebookUserData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of user (FB-"  + jsonObject.get("id") + ") from facebook user-object");
		
		logger.warn("NOT YET IMPLEMENTED");
		
		// set all values to zero
		initialize();
		
		try {
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
			
			logger.debug("     construction finished");
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json facebook user-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;
		sn_id = SocialNetworks.FACEBOOK.getValue();
		username = null;
		screen_name = null;
		lang = null;
		geoLocation = null;
		followers_count = 0;
		friends_count = 0;
		postings_count = 0;
		favorites_count = 0;
		lists_and_groups_count = 0;
	}
}