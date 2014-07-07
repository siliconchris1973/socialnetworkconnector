package de.comlineag.snc.data;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.helper.DataHelper;

/**
 * 
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	data type
 * @version 	1.3
 * @status		productive but some fields are missing
 * 
 * @description Describes a single twitter posting with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              posting type save.
 * 
 * @param <JSonObject>
 *            "id" Long
 *            "sn_id" String
 *            "created_at" String
 *            "text" String
 *            "source" String
 *            "truncated" Boolean
 *            "in_reply_to_status_id" Long
 *            "in_reply_to_user_id" Long
 *            "in_reply_to_screen_name" String
 *            "coordinates" List
 *            "geoLocation" List
 *            "lang" String
 *            "hashtags" List
 *            "symbols" List
 *            "mentions" List
 * 
 * @changelog	0.1 class created
 * 				0.2 added all simple fields
 * 				0.3 added constant for social network
 * 				0.4 - 0.8 work on geo location - as of v1.1 still not implemented
 * 				0.9 skeleton for symbols, hashtags and mentions - as of v1.1 still not implemented
 * 				1.0 first productive version without geo location and hashtag, symbols, mentions
 * 				1.1 minor bugfixings
 * 				1.2 geo location services and datatypes are now in their own class TwitterLocationData
 * 				1.3 changed geo location to make use of simple class LocationData and added teaser as substring of post
 * 
 * TODO 1. implement hashtags, symbols and mentions
 * 
 */

public final class TwitterPostingData extends PostData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Twitter the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Twitter
	 */
	public TwitterPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("constructing new subset of data of tweet (ID: " + jsonObject.get("id") + ") from twitter post-object");
		logger.trace("  working on " + jsonObject.toString());
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();

		// ID des Posting
		setId((Long) jsonObject.get("id"));

		// User ID
		JSONObject user = (JSONObject) jsonObject.get("user");
		setUserId((Long) user.get("id"));
		
		// Sprache
		setLang((String) jsonObject.get("lang"));

		// Timestamp als String und dann als Objekt fuer den oDATA Call
		setTime((String) jsonObject.get("created_at"));
		setTimestamp(DataHelper.prepareLocalDateTime(getTime(), getSnId()));

		// Text des Post
		setText((String) jsonObject.get("text"));
		
		// a teaser is created from the first 20 chars of the post
		setTeaser(getText().substring(0,20)+"...");
		
		// Metadaten zum Post:
		// von wo erzeugt:
		setClient((String) jsonObject.get("source"));
		
		// Flag gekuerzt....was auch immer damit dann passieren wird...
		setTruncated((Boolean) jsonObject.get("truncated"));

		// Information zu Reply
		if (jsonObject.get("in_reply_to_status_id") != null)
			setInReplyTo((Long) jsonObject.get("in_reply_to_status_id"));
		if (jsonObject.get("in_reply_to_user_id") != null)
			setInReplyToUser((Long) jsonObject.get("in_reply_to_user_id"));
		if (jsonObject.get("in_reply_to_screen_name") != null)
			setInReplyToUserScreenName((String) jsonObject.get("in_reply_to_screen_name"));
		
		
		/*
		 * simple point location as given by e.g. a mobile device
		 *
		 * Structure
		 *		Coordinates {
		 * 			"type":"Point",
		 * 			"coordinates":[-84.497553,33.944551]
		 * 		}
	 	 */
		if (jsonObject.get("coordinates") != null) {
			logger.debug("Found Coordinates " + jsonObject.get("coordinates").toString());
			
			JSONObject place = (JSONObject) jsonObject.get("coordinates");
			LocationData twPlace = new LocationData(place);
			
			setGeoLongitude(twPlace.getGeoLongitude());
			setGeoLatitude(twPlace.getGeoLatitude());
			
			/*
			try {
				JSONParser parser = new JSONParser();
				Object simpleGeoLocationObj = parser.parse(jsonObject.get("coordinates").toString());	
				JSONObject jsonObj = simpleGeoLocationObj instanceof JSONObject ?(JSONObject) simpleGeoLocationObj : null;
				String t = new String(jsonObj.get("coordinates").toString());
				logger.trace("retrieved coordinates: " + t + " / lat: " + t.substring(1, t.indexOf(",")) + " / long: " +t.substring(t.indexOf(",")+1,t.length()-1));
				setGeoLatitude(t.substring(1, t.indexOf(","))); 
				setGeoLongitude(t.substring(t.indexOf(",")+1,t.length()-1));
			} catch (ParseException e) {
				logger.error("error parsing json coordinates object: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			*/
		}
		
		/* 
		 * geoLocation is filled from the users profile - a complex structure
		 *  
		 * Structure
		 * 		geoLocation {
		 * 			"id":"e229de11a7eb6823",
		 * 			"bounding_box":{
		 * 				"type":"Polygon",
		 * 				"coordinates":[
		 * 						[[-84.616812,33.895088],[-84.616812,34.0011594],[-84.46746,34.0011594],[-84.46746,33.895088]]
		 * 				]
		 * 			},
		 * 			"place_type":"city",
		 * 			"name":"Marietta",
		 * 			"attributes":{},
		 * 			"country_code":"US",
		 * 			"url":"https:\/\/api.twitter.com\/1.1\/geo\/id\/e229de11a7eb6823.json",
		 * 			"country":"United States",
		 * 			"full_name":"Marietta, GA"
		 * 		}
		 */
		if (jsonObject.get("geoLocation") != null) {
			logger.trace("Found geoLocation " + jsonObject.get("geoLocation"));
			JSONObject place = (JSONObject) jsonObject.get("geoLocation");
			LocationData twPlace = new LocationData(place);
			
			setGeoLongitude(twPlace.getGeoLongitude());
			setGeoLatitude(twPlace.getGeoLatitude());
			setGeoPlaceId(twPlace.getGeoPlaceId());
			setGeoPlaceName(twPlace.getGeoPlaceName());
			setGeoPlaceCountry(twPlace.getGeoPlaceCountry());
			setGeoAroundLongitude(twPlace.getGeoAroundLongitude());
			setGeoAroundLatitude(getGeoAroundLongitude());
		}
		
		
		/*
		 * 
		 * @description	Structure of Hashtag and Symbols - not yet implemented
		 * 
		 * "entities": {
		 * 		"trends":[],
		 * 		"symbols":[],
		 * 		"urls":[],
		 * 		"hashtags":[{
		 * 			"text":"SocialBrandMonitor",
		 * 			"indices":[20,39]}],
		 * 		"user_mentions":[]
		 * 	}
		 * 
		 */
		// TODO implement proper handling of hashtags, symbols and mentions - this currently kills the parser
		//setHashtags((List<?>)jsonObject.get("hashtags"));
		//setSymbols((List<?>)jsonObject.get("symbols"));
		//setMentions((List<?>)jsonObject.get("user_mentions"));
	}
	
	
	public void setMentions(List<?> listOfMentions) {
		// TODO Implement proper algorithm to deal with user mentions
		logger.trace("List of mentioned users received, creating something different from it");
		Iterator<?> itr = listOfMentions.iterator();
		while(itr.hasNext()){
			logger.trace("found user " + itr.next());
		}
	}
	
	public void setSymbols(List<?> listOfSymbols) {
		// TODO Implement proper algorithm to deal with symbols
		logger.trace("List of symbols received, creating something different from it");
		Iterator<?> itr = listOfSymbols.iterator();
		while(itr.hasNext()){
			logger.trace("found symbol " + itr.next());
		}
	}
	
	public void setHashtags(List<?> listOfHashtags) {
		// TODO Implement proper algorithm to deal with hashtags
		logger.trace("List of Hashtags received, creating something different from it");
		Iterator<?> itr = listOfHashtags.iterator();
		while(itr.hasNext()){
			logger.trace("found hashtag " + itr.next());
		}
	}
	
	/**
	 * setup the Object with NULL
	 */
	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;

		// ACHTUNG, wenn die Klasse fuer Facebook u.a. kopiert wird,
		// daa muss dieses Value natuerlich umgesetzt werden
		sn_id = SocialNetworks.TWITTER.getValue();

		text = null;
		raw_text = null;
		time = null;
		lang = null;
		
		posted_from_client = null;
		truncated = null;
		
		in_reply_to_post = 0;
		in_reply_to_user = 0;
		in_reply_to_user_screen_name = null;
		
		place = null;
		geoLatitude = null;
		geoLongitude = null;
		geoAroundLatitude = null;
		geoAroundLongitude = null;
		geoPlaceId = null;
		geoPlaceName = null;
		geoPlaceCountry = null;
		
		hashtags = null;
		symbols = null;
		mentions = null;
	}
}