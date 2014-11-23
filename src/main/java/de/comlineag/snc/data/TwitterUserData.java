package de.comlineag.snc.data;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	data type
 * @version 	0.2b			- 22.07.2014
 * @status		productive
 * 
 * @description Describes a single twitter user with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              user type save.
 * 
 * @param <JSonObject>
 * 			  "domain" String
 * 			  "customer" String
 * 			  "object_status" String
 *            "id" String
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
 * @changelog	0.1 (Magnus)	class created
 * 				0.2 (Chris)		added support for counters
 * 				0.2a 			moved variable initialization into method initialize
 * 				0.2b			changed id from Long to String
 * 
 */

public final class TwitterUserData extends UserData implements ISncDataObject{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * Constructor, based on the JSONObject sent from Twitter the Data Object is prepared
	 * 
	 * @param jsonObject
	 */
	public TwitterUserData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of user (TW-"  + jsonObject.get("id") + ") from twitter user-object");
		
		// set all values to zero
		initialize();
		String s;
		
		try {
			s = Objects.toString(jsonObject.get("id"), null);
			setId((String) s);
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
			logger.error("EXCEPTION :: during parsing of json twitter user-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private void initialize() {
		// first setup the internal json objct
		internalJson = new JSONObject();
		
		// setting everything to 0 or null default value.
		id = "0";
		setObjectStatus("new");
		
		// set the internal fields and embedded json objects for domain, customer and social network
		setSnId(SocialNetworks.getSocialNetworkConfigElement("code", "TWITTER"));
		setDomain(new CrawlerConfiguration<String>().getDomain());
		setCustomer(new CrawlerConfiguration<String>().getCustomer());
		
		username = null;
		screen_name = null;
		lang = null;
		geoLocation = "";
		followers_count = 0;
		friends_count = 0;
		postings_count = 0;
		favorites_count = 0;
		lists_and_groups_count = 0;
		
		
	}
}