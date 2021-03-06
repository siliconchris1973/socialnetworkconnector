package de.comlineag.snc.data;

import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.GeneralDataDefinitions;
import de.comlineag.snc.helper.StringServices;
import de.comlineag.snc.helper.DateTimeServices;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.6b		- 22.10.2014
 * @status		productive
 * 
 * @description Describes a single Lithium posting with all relevant informations. 
 * 				The class shall be used to make all methods handling a Lithium posting type save.
 * 
 * 				ATTENTION: 	The messages in the Lithium network differ fundamentally from a Twitter 
 * 							Tweet. On the one side, there are less information (geoLocation and reply 
 * 							information are missing) and on the other side there are many other 
 * 							informations unknown to twitter, like kudos count, views count, board
 * 							subject, teaser and the like. So in the end, a lithium message will
 * 							show up differently in the db.
 * 
 * @param <JSonObject>
 * 			Our internal column name	data type	element in json object
 *            "domain" 					String		domain
 *            "id" 						String		id
 *            "sn_id" 					String		fixed to LT
 *            "created_at" 				String		post_time
 *            "text" 					String		body
 *            "raw_text" 				String		body
 *            "teaser"	 				String		teaser
 *            "subject"	 				String		subject
 *            "source" 					String		href to the board in which the message was posted
 *            
 *            "viewcount"				int			views
 *            "favoritecount"			int			kudos
 *            
 *            "truncated" 				Boolean		fixed to FALSE because not used
 *            "lang" 					String		fixed to de
 *            
 *            "hashtags" 				List		fixed to NULL because not used
 *            "symbols" 				List		fixed to NULL because not used
 *            "mentions" 				List		fixed to NULL because not used
 *            
 *            "in_reply_to_status_id" 	Long		fixed to NULL because not used
 *            "in_reply_to_user_id" 	Long		fixed to NULL because not used
 *            "in_reply_to_screen_name"	String		fixed to NULL because not used
 *            
 *            "coordinates" 			List		fixed to NULL because not used
 *            "geoLocation" 			List		fixed to NULL because not used
 *            
 * 
 * @changelog	0.1 (Chris)		first initial version as copy from TwitterPostingrData
 * 				0.2		 		added parsing of 2nd and 3rd level of json string
 * 				0.3 			jump to first productive version
 * 				0.4 			added support to strip all html for text and created raw text
 * 				0.5				set the truncated flag, if posts are truncated due to length violation on MAX_NVARCHAR_SIZE
 * 				0.6				changed field handling according to new constants from GeneralDataDefinitions
 * 				0.6a			added field domain
 * 				0.6b			changed id from long to String
 * 
 * TODO Add support for labels
 * TODO implement jericho to truncate html
 * TODO check if we need to safe the thread from which the posting originated
 * 
 * 
 * JSON Structure:
 * 
 * 		1. Level			directly accessible through passed JSONObject
 * 			2. Level		one more JSONObject creation
 * 				3. Level	two more JSONObject creation 
 * {}JSON
		{}body										- this is the message text
		{}message_rating
			$ : 0
			type : "float"
		{}read_only
			$ : false
			type : "boolean"
		{}message_status
			{}name
				$ : "Unspecified"
				type : "string"
			type : "message_status"
			href : "/message_statuses/id/1"
			{}key
				$ : "unspecified"
				type : "string"
		{}root										- this is the json object containing the href
			type : "message"
			href : "/messages/id/2961"
		{}subject									- the top subject, as used in the search 
			$ : "Aktie"
			type : "string"
		{}labels
			{}label
				0
				{}id
					$ : 163
					type : "int"
				{}text
					$ : "Aktie"
					type : "string"
				type : "label"
				href : "/labels/id/163"
		{}parent
			$ : null
			type : "message"
			null : true
		{}last_edit_time
			$ : "2014-02-18T10:49:59+00:00"
			type : "date_time"
		{}board										- a json object containing the board
			type : "board"
			href : "/boards/id/Boersenlexikon"		- we use this as the source
		type : "message"
		{}board_id									- the board id
			$ : 1119
			type : "int"
		{}deleted
			$ : false
			type : "boolean"
		{}id										- the message id
			$ : 2961
			type : "int"
		{}author									- this is the user that posted the message
			{}login
				$ : "Cortal_Consors"
				type : "string"
			type : "user"
			href : "/users/id/9"
		{}last_edit_author
			{}login
				$ : "Cortal_Consors"
				type : "string"
			type : "user"
			href : "/users/id/9"
		{}views
			{}count
				$ : 363
				type : "int"
		{}thread
			type : "thread"
			href : "/threads/id/2961"
		{}teaser
			$ : ""
			type : "string"
		href : "/messages/id/2961"
		{}kudos
			{}count
				$ : 0
				type : "int"
		{}post_time
			$ : "2014-01-08T12:21:42+00:00"
			type : "date_time"
 */

public final class LithiumPostingData extends PostingData implements ISncDataObject{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	UserData userObject = new UserData();
	DomainData domainObject = new DomainData();
	CustomerData customerObject = new CustomerData();
	SocialNetworkData socialNetworkObject = new SocialNetworkData();
	
	ArrayList<String> keywords = new ArrayList<String>();
	
	public LithiumPostingData(){}
	
	/**
	 * Constructor, based on the JSONObject sent from Lithium the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Lithium
	 */
	public LithiumPostingData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of post (LT-"  + jsonObject.get("id") + ") from lithium post-object");
		
		// set all data to initial values and also set the SN_ID to LITHIUM queivalent code
		initialize();
		String s; // helper var to cast from long to string
		
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			
			
			// ID des Posting
			// Structure: 
			// 	{}id
			//		$ : 2961
			// 		type : "int"
			obj = parser.parse(jsonObject.get("id").toString());
			JSONObject jsonObjId = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			s = Objects.toString(jsonObjId.get("$"), null);
			setId(s);
			
			
			// the user
			// Structure: 
			// 	{}author
			//		{}login
			//			$ : "Cortal_Consors"
			//			type : "string"
			//		type : "user"
			//		href : "/users/id/9"
			obj = parser.parse(jsonObject.get("author").toString());
			JSONObject jsonObjAuthor = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			String userHref = jsonObjAuthor.get("href").toString().substring(jsonObjAuthor.get("href").toString().lastIndexOf('/') + 1); 
			setUserId(userHref.trim());
			
			
			// number of kudos (favorites count)
			// Structure: 
			// 	{}kudos								
			//		{}count
			//			$ : "0"
			//			type : "int"
			// first level - get kudos
			obj = parser.parse(jsonObject.get("kudos").toString());
			JSONObject jsonObjKudos = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			// second level - get count
			JSONParser parserKudos = new JSONParser();
			Object objKudos = parserKudos.parse(jsonObjKudos.get("count").toString());
			JSONObject jsonObjKudosTwo = objKudos instanceof JSONObject ?(JSONObject) objKudos : null;
			
			setFavoriteCount((long) jsonObjKudosTwo.get("$"));
			
			
			// number of views
			// Structure: 
			// 	{}views								
			//		{}count
			//			$ : "0"
			//			type : "int"
			// first level - get json-object for views
			obj = parser.parse(jsonObject.get("views").toString());
			JSONObject jsonObjViews = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			// second level - get count
			JSONParser parserViews = new JSONParser();
			Object objViews = parserViews.parse(jsonObjViews.get("count").toString());
			JSONObject jsonObjViewsTwo = objViews instanceof JSONObject ?(JSONObject) objViews : null;
			
			setViewCount((long) jsonObjViewsTwo.get("$"));
			
			
			// Timestamp as a string and as an objekt for the oDATA call
			// Structure: 
			// 	{}post_time
			//		$ : "2014-01-08T12:21:42+00:00"
			//		type : "date_time"
			obj = parser.parse(jsonObject.get("post_time").toString());
			JSONObject jsonObjTime = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setTime((String) jsonObjTime.get("$"));
			setTimestamp(DateTimeServices.prepareLocalDateTime(getTime(), getSnId()));
			
			
			// The posting itself
			// Structure: 
			//	{}body
			//		$ : ""
			//		type : "string"
			obj = parser.parse(jsonObject.get("body").toString());
			JSONObject jsonObjText = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			if (GeneralDataDefinitions.TEXT_WITH_MARKUP) {
				setText(jsonObjText.get("$").toString());
			} else {
				setText((String) StringServices.stripHTML(jsonObjText.get("$").toString()));
			}
			if (GeneralDataDefinitions.RAW_TEXT_WITH_MARKUP) {
				setRawText((jsonObjText.get("$").toString()));
			} else {
				setRawText((String) StringServices.stripHTML(jsonObjText.get("$").toString()));
			}
			
			
			// a teaser can either be inserted by platform or it is created from the first 20 chars of the post
			// Structure: 
			// 	{}teaser
			//		$ : ""
			//		type : "string"
			obj = parser.parse(jsonObject.get("teaser").toString());
			JSONObject jsonObjTeaser = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			if (jsonObjTeaser.get("$").toString().length() <= GeneralDataDefinitions.TEASER_MIN_LENGTH) {
				if (GeneralDataDefinitions.TEASER_WITH_MARKUP){
					setTeaser(getText());
				}else{
					setTeaser((String) StringServices.stripHTML(getText()));
				}
			} else {
				if (GeneralDataDefinitions.TEASER_WITH_MARKUP){
					setTeaser((String) jsonObjTeaser.get("$"));
				} else {
					setTeaser((String) StringServices.stripHTML(jsonObjTeaser.get("$")));
				}
			}
			if (getTeaser().length() > GeneralDataDefinitions.TEASER_MAX_LENGTH)
				setTeaser(getTeaser().substring(0, GeneralDataDefinitions.TEASER_MAX_LENGTH-3)+"...");
			
			
			// a subject can either be inserted by platform or it is created from the first 20 chars of the post
			// Structure: 
			// 	{}subject
			//		$ : ""
			//		type : "string"
			obj = parser.parse(jsonObject.get("subject").toString());
			JSONObject jsonObjSubject = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			if (jsonObjSubject.get("$").toString().length() <= GeneralDataDefinitions.SUBJECT_MIN_LENGTH) {
				if (GeneralDataDefinitions.SUBJECT_WITH_MARKUP){
					setSubject(getText());
				}else{
					setSubject((String) StringServices.stripHTML(getText()));
				}
			} else {
				if (GeneralDataDefinitions.SUBJECT_WITH_MARKUP){
					setSubject((String) jsonObjSubject.get("$"));
				} else {
					setSubject((String) StringServices.stripHTML(jsonObjSubject.get("$")));
				}
			}
			if (getSubject().length() > GeneralDataDefinitions.SUBJECT_MAX_LENGTH)
				setSubject(getSubject().substring(0, GeneralDataDefinitions.SUBJECT_MAX_LENGTH-3)+"...");
			
			
			// in which board was the message posted - we use the client field for this value
			// Structure:
			//	{}board	
			//		type : "board"
			//		href : "/boards/id/Boersenlexikon"	
			obj = parser.parse(jsonObject.get("board").toString());
			JSONObject jsonObjBoard = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setClient((String) jsonObjBoard.get("href"));
			
			
			// TODO check if we can make use of threads and if threads may be the proper field instead of board
			// the thread in which the posting was posted
			// Structure
			//  {}thread
			//		type : "thread"
			//		href : "/threads/id/2961"
			/*
			obj = parser.parse(jsonObject.get("thread").toString());
			JSONObject jsonObjThread = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setClient((String) jsonObjThread.get("href"));
			*/
			
			// labels may contain some more valuable information, but we do not use them at the moment
			// when implementing this, be aware of multiple occurrences for Label within Labels 
			// Structure:
			//	{}labels
			//		{}label
			//			0
			//			{}id
			//				$ : 163
			//				type : "int"
			//			{}text
			//				$ : "Aktie"
			//				type : "string"
			//		type : "label"
			//		href : "/labels/id/163"
			/*
			obj = parser.parse(jsonObject.get("labels").toString());
			JSONObject jsonObjLabels = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			// second level - get count
			JSONParser parserLabels = new JSONParser();
			Object objLabels = parserLabels.parse(jsonObjLabels.get("label").toString());
			JSONObject jsonObjLabelsTwo = objLabels instanceof JSONObject ?(JSONObject) objLabels : null;
			
			JSONParser parserLabelsTwo = new JSONParser();
			Object objLabelsTwo = parserLabelsTwo.parse(jsonObjLabelsTwo.get("text").toString());
			JSONObject jsonObjLabelsThree = objLabelsTwo instanceof JSONObject ?(JSONObject) objLabelsTwo : null;
			
			//setLabel((String) jsonObjLabelsThree.get("$"));
			logger.trace("the label is " + jsonObjLabelsThree.get("$"));
			*/
			
			// language
			setLang(lang);
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json Lithium post-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	

	/**
	 * @description	setup a posting data with NULL-values
	 */
	@SuppressWarnings("unchecked")
	private void initialize() {
		// first setup the internal json objct
		internalJson = new JSONObject();
		
		// setting everything to 0 or null default value.
		id = "0";
		setObjectStatus("new");
		
		// set the internal fields and embedded json objects for domain, customer and social network
		setSnId(SocialNetworks.getSocialNetworkConfigElement("code", "LITHIUM"));
		setDomain(new CrawlerConfiguration<String>().getDomain());
		setCustomer(new CrawlerConfiguration<String>().getCustomer());
		
		// create the embedded social network json
		JSONObject tJson = new JSONObject();
		tJson.put("sn_id", sn_id);
		tJson.put("name", SocialNetworks.getSocialNetworkConfigElementByCode("name", sn_id).toString());
		tJson.put("domain", SocialNetworks.getSocialNetworkConfigElementByCode("domain", sn_id).toString());
		tJson.put("description", SocialNetworks.getSocialNetworkConfigElementByCode("description", sn_id).toString());
		SocialNetworkData socData = new SocialNetworkData(tJson.toJSONString());
		logger.trace("storing created social network object {} as embedded object {}", socData.getName(), socData.toJsonString());
		setSocialNetworkData(socData.getJson());
		
		// create the embedded domain json
		tJson = new JSONObject();
		tJson.put("name", domain);
		DomainData domData = new DomainData(tJson.toJSONString());
		logger.trace("storing created domain object {} as embedded object {}", domData.getName(), domData.toJsonString());
		setDomainData(domData.getJson());
		
		// create the embedded customer json
		tJson = new JSONObject();
		tJson.put("name", customer);
		CustomerData subData = new CustomerData(tJson.toJSONString());
		logger.trace("storing created customer object {} as embedded object {}", subData.getName(), subData.toJsonString());
		setCustomerData(subData.getJson());
		
		
		text = null;
		raw_text = null;
		subject = "";
		teaser = "";
		
		viewcount = 0;
		favoritecount = 0;
		
		time = null;
		posted_from_client = null;
		truncated = false;
		lang = "de";
		
		// these values are never used by Lithium
		in_reply_to_post = 0;
		in_reply_to_user = 0;
		in_reply_to_user_screen_name = null;
		
		place = null;
		geoLatitude = null;
		geoLongitude = null;
		geoAroundLongitude = null;
		geoAroundLatitude = null;
		geoPlaceId = null;
		geoPlaceName = null;
		geoPlaceCountry = null;
		
		hashtags = null;
		symbols = null;
		mentions = null;
	}
	
	// new methods to get and set the user, domain, customer and social network object within the page object
	public void setUserObject(UserData userJson){this.userObject = userJson;}
	public UserData getUserObject(){return userObject;}
	
	public void setDomainObject(DomainData domJson){this.domainObject = domJson;}
	public DomainData getDomainObject(){return domainObject;}
	
	public void setCustomerObject(CustomerData subJson){this.customerObject = subJson;}
	public CustomerData getCustomerObject(){return customerObject;}
	
	public void setSocialNetworkObject(SocialNetworkData socJson){this.socialNetworkObject = socJson;}
	public SocialNetworkData getSocialNetworkObject(){return socialNetworkObject;}
}