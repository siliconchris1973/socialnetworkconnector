package de.comlineag.snc.data;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @version 	0.2			- 22.10.2014
 * @status		in development
 * 
 * @description Describes a single Facebook posting with all relevant informations.
 *              The class shall be used to make all methods handling a Facebook
 *              posting type save.
 * 
 * @param <JSonObject>
 * 			  "domain" String
 *            "id" String
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
 * 				0.2				changed id from long to String
 * 
 * TODO create implementation for facebook posting data
 * 
 */

public final class FacebookPostingData extends PostingData {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
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
		String s;
		
		try {
			// posting ID 
			s = Objects.toString(jsonObject.get("id"), null);
			setId(s);
			
			
			// User ID
			JSONObject user = (JSONObject) jsonObject.get("USER");
			setUserId((String) user.get("id"));
			
			
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
			//setHashtags((List<?>)jsonObject.get("hashtags"));
			//setSymbols((List<?>)jsonObject.get("symbols"));
			//setMentions((List<?>)jsonObject.get("user_mentions"));
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json facebook post-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * setup the Object with NULL
	 */
	@SuppressWarnings("unchecked")
	private void initialize() {
		// first setup the internal json objct
		internalJson = new JSONObject();
		
		// setting everything to 0 or null default value.
		id = "0";
		setObjectStatus("new");
		
		setSnId(SocialNetworks.getSocialNetworkConfigElement("code", "FACEBOOK"));
		setDomain(new CrawlerConfiguration<String>().getDomain());
		setCustomer(new CrawlerConfiguration<String>().getCustomer());
		
		
		// create the embedded social network json
		JSONObject tJson = new JSONObject();
		tJson.put("sn_id", sn_id);
		tJson.put("name", SocialNetworks.getSocialNetworkConfigElementByCode("name", sn_id).toString());
		tJson.put("domain", SocialNetworks.getSocialNetworkConfigElementByCode("domain", sn_id).toString());
		tJson.put("description", SocialNetworks.getSocialNetworkConfigElementByCode("description", sn_id).toString());
		SocialNetworkData socData = new SocialNetworkData(tJson);
		logger.trace("storing created social network object {} as embedded object", socData.toString());
		setSocialNetworkData(socData);
		
		// create the embedded domain json
		tJson = new JSONObject();
		tJson.put("name", domain);
		DomainData domData = new DomainData(tJson);
		setDomainData(domData);
		
		// create the embedded customer json
		tJson = new JSONObject();
		tJson.put("name", customer);
		CustomerData cusData = new CustomerData(tJson);
		setCustomerData(cusData);
		
		
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