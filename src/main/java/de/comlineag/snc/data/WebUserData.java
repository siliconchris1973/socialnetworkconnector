package de.comlineag.snc.data;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.1				- 10.10.2014
 * @status		productive
 * 
 * @description Describes a single web user with all relevant informations.
 *              The class shall be used to make all methods handling a web user
 *              posting type save.
 * 
 * @param <JSonObject>
 * 			  "domain" String
 * 			  "customer" String
 * 			  "object_status" String
 *            "id" Long
 *            "sn_id" String
 *            "lang" String
 *            "name" String
 *            "screen_name" String
 *            "geoLocation" List
 *            "followers_count" Long
 *            "friends_count" Long
 *            "statuses_count" Long
 *            "favourites_count" Long
 *            "lists_and_groups_count" Long
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */

public final class WebUserData extends UserData {

	// we use simple org.apache.log4j.Logger for lgging
		private final Logger logger = Logger.getLogger(getClass().getName());
		// in case you want a log-manager use this line and change the import above
		//private final Logger logger = LogManager.getLogger(getClass().getName());
		
		/**
		 * Constructor, based on the JSONObject sent from Web Crawler the Data Object is prepared
		 * 
		 * @param jsonObject
		 */
		public WebUserData(JSONObject jsonObject) {
			logger.debug("constructing new subset of data of user (WC-"  + jsonObject.get("id") + ") from web page user-object");
			
			// set all values to zero
			initialize();
			
			try {
				setId(Long.valueOf((String) jsonObject.get("id")).longValue());
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
			} catch (Exception e) {
				logger.error("EXCEPTION :: during parsing of json web page user-object " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}

		private void initialize() {
			// setting everything to 0 or null default value.
			// so I can check on initialized or not initialized values for the
			// posting
			id = 0;
			//sn_id = SocialNetworks.TWITTER.getValue();
			sn_id = SocialNetworks.getSocialNetworkConfigElement("code", "WEBCRAWLER");
			username = null;
			screen_name = null;
			lang = null;
			geoLocation = "";
			followers_count = 0;
			friends_count = 0;
			postings_count = 0;
			favorites_count = 0;
			lists_and_groups_count = 0;
			
			domain = new CrawlerConfiguration<String>().getDomain();
			customer = new CrawlerConfiguration<String>().getCustomer();
			objectStatus = "new";
		}
}