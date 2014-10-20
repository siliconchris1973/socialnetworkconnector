package de.comlineag.snc.data;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.GeneralDataDefinitions;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.helper.StringServices;
import de.comlineag.snc.helper.DateTimeServices;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.1		- 10.07.2014
 * @status		in development
 * 
 * @description Describes a single Facebook posting with all relevant informations.
 *              The class shall be used to make all methods handling a Facebook
 *              posting type save.
 * 
 * @param <JSonObject>
 * 			  "domain" String
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
 * @changelog	0.1 (Chris)		class copied from TwitterPostingData revision 0.9b
 * 
 * TODO 1. create actual implementation
 * 
 */

public final class FacebookPostingData extends PostData {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	/**
	 * Constructor, based on the JSONObject sent from Facebook the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Facebook
	 */
	public FacebookPostingData(JSONObject jsonObject) {
		// log the startup message
		logger.debug("constructing new subset of data of tweet (FB-"  + jsonObject.get("id") + ") from facebook post-object");
		
		logger.warn("NOT YET IMPLEMENTED");
		
		// set all values to zero
		initialize();
		
		try {
			// posting ID 
			setId((Long) jsonObject.get("id"));
			
			
			// User ID
			JSONObject user = (JSONObject) jsonObject.get("user");
			setUserId((Long) user.get("id"));
			
			
			// langauge
			setLang((String) jsonObject.get("lang"));
			
			
			// Timestamp as a string and as an object for the oDATA call
			setTime((String) jsonObject.get("created_at"));
			setTimestamp(DateTimeServices.prepareLocalDateTime(getTime(), getSnId()));
			
			
			// Flag truncated - we can use this to indicate more text
			setTruncated((Boolean) jsonObject.get("truncated"));
			
			
			// content of the posting, can either be stored with or without markup elements. 
			if (GeneralDataDefinitions.TEXT_WITH_MARKUP) {
				setText((String) jsonObject.get("text"));
			} else {
				setText((String) StringServices.stripHTML(jsonObject.get("text")));
			}
			// content of the raw text of the posting, can either be stored with or without markup elements. 
			if (GeneralDataDefinitions.RAW_TEXT_WITH_MARKUP) {
				setRawText((String) jsonObject.get("text"));
			} else {
				setRawText((String) StringServices.stripHTML(jsonObject.get("text")));
			}
			
			
			// a teaser is created from the first 256 chars of the post 
			// the persistence layer can also truncate the teaser, in case field length is smaller
			if (GeneralDataDefinitions.TEASER_WITH_MARKUP){
				setTeaser(getText());
			}else{
				setTeaser((String) StringServices.stripHTML(getText()));
			}
			if (getTeaser().length() > GeneralDataDefinitions.TEASER_MAX_LENGTH)
				setTeaser(getTeaser().substring(0, GeneralDataDefinitions.TEASER_MAX_LENGTH-3)+"...");
			
			
			// a subject is created from the first 20 chars of the post 
			// the persistence layer can also truncate the subject, in case field length is smaller
			if (GeneralDataDefinitions.SUBJECT_WITH_MARKUP){
				setSubject(getText());
			}else{
				setSubject((String) StringServices.stripHTML(getText()));
			}
			if (getSubject().length() > GeneralDataDefinitions.SUBJECT_MAX_LENGTH)
				setSubject(getSubject().substring(0, GeneralDataDefinitions.SUBJECT_MAX_LENGTH-3)+"...");
			
			
			// what client posted the tweet - this is an url to possible clients on facebook
			setClient((String) jsonObject.get("source"));
			
			
			// reply information
			if (jsonObject.get("in_reply_to_status_id") != null)
				setInReplyTo((Long) jsonObject.get("in_reply_to_status_id"));
			if (jsonObject.get("in_reply_to_user_id") != null)
				setInReplyToUser((Long) jsonObject.get("in_reply_to_user_id"));
			if (jsonObject.get("in_reply_to_screen_name") != null)
				setInReplyToUserScreenName((String) jsonObject.get("in_reply_to_screen_name"));
			
			
			/*
			 * simple point geoLocation as given by e.g. a mobile device
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
			 * 			"url":"https:\/\/api.facebook.com\/1.1\/geo\/id\/e229de11a7eb6823.json",
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
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json facebook post-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
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
		
		domain = new CrawlerConfiguration<String>().getDomain();
		customer = new CrawlerConfiguration<String>().getCustomer();
		//sn_id = SocialNetworks.FACEBOOK.getValue();
		sn_id = SocialNetworks.getSocialNetworkConfigElement("code", "FACEBOOK");
		
		text = null;
		raw_text = null;
		subject = null;
		teaser = null;
		
		time = null;
		lang = null;
		
		posted_from_client = null;
		truncated = false;
		
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