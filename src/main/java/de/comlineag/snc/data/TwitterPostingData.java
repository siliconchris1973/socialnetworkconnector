package de.comlineag.snc.data;

import java.util.Iterator;
import java.util.List;
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
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	data type
 * @version 	0.9d		- 22.10.2014
 * @status		in production (but some fields are still missing)
 * 
 * @description Describes a single twitter posting with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              posting type save.
 * 
 * @param <JSonObject>
 * 			  "domain" List
 * 			  "customer" List
 *            "id" String - sometimes also post_id
 *            "sn_id" String
 *            "user_id" Long
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
 * @changelog	0.1 (Chris)		class created
 * 				0.2 (Magnus)	added all simple fields
 * 				0.3 (Chris)		added constant for social network
 * 				0.4 (Magnus)	work on geo geoLocation
 * 				0.5 			skeleton for symbols, hashtags and mentions - as of v1.1 still not implemented
 * 				0.6 			first productive version without geo geoLocation and hashtag, symbols, mentions
 * 				0.7 (Chris)		minor bugfixings
 * 				0.8 			geo geoLocation services and datatypes are now in their own class TwitterLocationData
 * 				0.9 			changed geo geoLocation to make use of simple class LocationData and added teaser as substring of post
 * 				0.9a			field length on teaser and subject and stripping of html for text
 * 				0.9b			added domain
 * 				0.9c			bug fixing for the id and post_id issue (sometimes, id is post_id as it seems)
 * 				0.9d			changed id (post_id) from Long to String
 * 				
 * 
 * @TODO 1. create code for hashtags
 * @TODO 2. create code for symbols
 * @TODO 3. create code for mentions
 */

public final class TwitterPostingData extends PostingData {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * Constructor, based on the JSONObject sent from Twitter the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Twitter
	 */
	public TwitterPostingData(JSONObject jsonObject) {
		// set all values to zero
		initialize();
		String s; // helper var to cast from long to string
		
		try {
			// posting ID - is either id or post_id
			if (jsonObject.get("id")==null) {
				s = Objects.toString(jsonObject.get("post_id"), null);
			} else {
				s = Objects.toString(jsonObject.get("id"), null);
			}
			setId(s);
			
			// log the startup message
			logger.debug("constructing new subset of data of tweet (TW-"  + id + ") from twitter post-object");
			
			// User ID
			JSONObject user = (JSONObject) jsonObject.get("user");
			s = Objects.toString(user.get("id"), null);
			setUserId(s);
			
			
			// langauge
			setLang((String) jsonObject.get("lang"));
			
			
			// Timestamp as a string and as an object for the oDATA call
			setTime((String) jsonObject.get("created_at"));
			setTimestamp(DateTimeServices.prepareLocalDateTime(getTime(), getSnId()));
			
			
			// Flag truncated - we can use this to indicate more text
			setTruncated((Boolean) jsonObject.get("truncated"));
			
			
			// content of the posting, can either be stored with or without markup elements. 
			// TODO move this in RuntimeConfiguration and source it in from XML 
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
			// TODO move this in RuntimeConfiguration and source it in from XML 
			if (GeneralDataDefinitions.TEASER_WITH_MARKUP){
				setTeaser(getText());
			}else{
				setTeaser((String) StringServices.stripHTML(getText()));
			}
			if (getTeaser().length() > GeneralDataDefinitions.TEASER_MAX_LENGTH)
				setTeaser(getTeaser().substring(0, GeneralDataDefinitions.TEASER_MAX_LENGTH-3)+"...");
			
			
			// a subject is created from the first 20 chars of the post 
			// the persistence layer can also truncate the subject, in case field length is smaller
			// TODO move this in RuntimeConfiguration and source it in from XML 
			if (GeneralDataDefinitions.SUBJECT_WITH_MARKUP){
				setSubject(getText());
			}else{
				setSubject((String) StringServices.stripHTML(getText()));
			}
			if (getSubject().length() > GeneralDataDefinitions.SUBJECT_MAX_LENGTH)
				setSubject(getSubject().substring(0, GeneralDataDefinitions.SUBJECT_MAX_LENGTH-3)+"...");
			
			
			// what client posted the tweet - this is an url to possible clients on twitter
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
				setGeoAroundLatitude(twPlace.getGeoAroundLongitude());
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
			if (jsonObject.containsKey("entities")){
				JSONObject entityObject = new JSONObject((JSONObject) jsonObject.get("entities"));
				logger.trace("the entity-object contains: {}", entityObject.toString());
				
				//setHashtags((List<?>)jsonObject.get("hashtags"));
				//setSymbols((List<?>)jsonObject.get("symbols"));
				//setMentions((List<?>)jsonObject.get("user_mentions"));
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json twitter post-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	
	public void setMentions(List<String> listOfMentions) {
		// TODO Implement proper algorithm to deal with user mentions
		logger.trace("List of mentioned users received, creating something different from it");
		Iterator<?> itr = listOfMentions.iterator();
		while(itr.hasNext()){
			logger.trace("found user " + itr.next());
		}
	}
	
	public void setSymbols(List<String> listOfSymbols) {
		// TODO Implement proper algorithm to deal with symbols
		logger.trace("List of symbols received, creating something different from it");
		Iterator<?> itr = listOfSymbols.iterator();
		while(itr.hasNext()){
			logger.trace("found symbol " + itr.next());
		}
	}
	
	public void setHashtags(List<String> listOfHashtags) {
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
	@SuppressWarnings("unchecked")
	private void initialize() {
		// first setup the internal json objct
		internalJson = new JSONObject();
		
		// setting everything to 0 or null default value.
		id = "0";
		
		objectStatus = "new";
		
		// set the internal fields and embedded json objects for domain, customer and social network
		sn_id = SocialNetworks.getSocialNetworkConfigElement("code", "TWITTER");
		domain = new CrawlerConfiguration<String>().getDomain();
		customer = new CrawlerConfiguration<String>().getCustomer();
		
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