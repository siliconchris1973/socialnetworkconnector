package de.comlineag.snc.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.GeneralDataDefinitions;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.helper.DateTimeServices;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.2				- 10.10.2014
 * @status		productive
 * 
 * @description Describes a single web page with all relevant informations.
 *              The class shall be used to make all methods handling a web page
 *              type save.
 *              The WebUserData handling differs from, say, Twitter User handling
 *              in that the user-object is embedded within the page object.
 *              As a consequence, the parser and the crawler have to operate
 *              a bit different on the posting (aka page) and user-object. One
 * 				consequence is, that the WebPostingData class introduces new
 * 				new methods getUser and setUser - to get and set the embedded 
 * 				user object.
 * 
 * @param <JSonObject>
 * 			  "Domain" String
 * 			  "Customer" String
 *            "id" Long - sometimes also post_id
 *            "sn_id" String
 *            "user_id" Long
 *            "truncated" Boolean
 *            "lang" String
 *            "created_at" String
 *            "text" String
 *            "raw_text" String
 *            "subject" String
 *            "teaser" String
 *            "source" String
 *            "user" JsonObject
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				added json object for embedded user object
 * 
 */

public final class WebPostingData extends PostingData {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	JSONObject user = new JSONObject();
	
	/**
	 * Constructor, based on the JSONObject sent from Web Crawler the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single page or news article
	 */
	public WebPostingData(JSONObject jsonObject) {
		try {
			// set all values to zero
			initialize();
			
			// page ID
			setId((String) jsonObject.get("page_id"));
			// SN_ID
			setSnId((String) jsonObject.get("sn_id"));
			
			setDomain((String) jsonObject.get("Domain"));
			setCustomer((String) jsonObject.get("Customer"));
			
			// log the startup message
			logger.debug("constructing new data-subset from page ("+getSnId()+"-"  + getId() + ")");
			
			
			
			// langauge
			setLang((String) jsonObject.get("lang"));
			
			
			// Timestamp as a string and as an object for the oDATA call
			//setTime((String) jsonObject.get("created_at"));
			setTime((String) jsonObject.get("created_at"));
			setTimestamp(DateTimeServices.prepareLocalDateTime(getTime(), getSnId()));
			
			
			// Flag truncated - we can use this to indicate more text
			setTruncated((Boolean) jsonObject.get("truncated"));
			
			
			// content of the page
			setText((String) jsonObject.get("text"));
			
			// content of the raw text of the posting, can either be stored with or without markup elements. 
			setRawText((String) jsonObject.get("raw_text"));
			
			
			
			// a teaser is created from the first 256 chars of the post 
			if (jsonObject.get("teaser") != null){
				setTeaser((String) jsonObject.get("teaser"));
			} else {
				setTeaser(getText());
			if (getTeaser().length() > GeneralDataDefinitions.TEASER_MAX_LENGTH)
				setTeaser(getTeaser().substring(0, GeneralDataDefinitions.TEASER_MAX_LENGTH-3)+"...");
			}
			
			// a subject is created from the first 20 chars of the post
			if (jsonObject.get("subject") != null){
				setSubject((String) jsonObject.get("subject"));
			} else {
				setSubject(getText());
				if (getSubject().length() > GeneralDataDefinitions.SUBJECT_MAX_LENGTH)
					setSubject(getSubject().substring(0, GeneralDataDefinitions.SUBJECT_MAX_LENGTH));
			}
			
			// the url of the page
			setClient((String) jsonObject.get("source"));
			
			// User ID
			JSONObject user = (JSONObject) jsonObject.get("user");
			setUser((JSONObject) user);
			setUserId((String) user.get("id"));
			
			logger.trace("the page object " + jsonObject.toString());
			logger.trace("the user object " + user.toString());
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json web page-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * setup the Object with NULL
	 */
	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = "0";
		
		objectStatus = "new";
		domain = new CrawlerConfiguration<String>().getDomain();
		customer = new CrawlerConfiguration<String>().getCustomer();
		
		sn_id = SocialNetworks.getSocialNetworkConfigElement("code", "WEBCRAWLER");
		
		text = null;
		raw_text = null;
		subject = null;
		teaser = null;
		
		time = null;
		lang = null;
		
		viewcount = 0;
		favoritecount=0;
		
		userId="0";
		
		user=null;
		
		posted_from_client = null;
		truncated = true;
		
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
	
	// new methods to get and set the user object within the page object
	public void setUser(JSONObject userJson){this.user = userJson;}
	public JSONObject getUser(){return user;}
}