package de.comlineag.sbm.data;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * 
 * @description Describes a single Lithium posting with all relevant informations. 
 * 				The class shall be used to make all methods handling a Lithium posting type save.
 * 
 * 				ATTENTION: 	The messages in the Lithium network differ fundamentally from a Twitter 
 * 							Tweet. On the one side, there are less information (location and reply 
 * 							information are missing) and on the other side there are man y other 
 * 							informations unknown to twitter, like kudos count, views count, board
 * 							subject, teaser and the like.
 * 
 * @param <JSonObject>
 * 			Our internal column name	data type	element in json object
 *            "id" 						Long		id
 *            "sn_id" 					String		fixed to LT
 *            "created_at" 				String		post_time
 *            "text" 					String		body
 *            "raw_text" 					String		body
 *            "source" 					String		href auf das board
 *            "truncated" 				Boolean		fixed to FALSE because not used
 *            "in_reply_to_status_id" 	Long		fixed to NULL because not used
 *            "in_reply_to_user_id" 	Long		fixed to NULL because not used
 *            "in_reply_to_screen_name"	String		fixed to NULL because not used
 *            "coordinates" 			List		fixed to NULL because not used
 *            "place" 					List		fixed to NULL because not used
 *            "lang" 					String		fixed to NULL because not used
 *            "hashtags" 				List		fixed to NULL because not used
 *            "symbols" 				List		fixed to NULL because not used
 *            "mentions" 				List		fixed to NULL because not used
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
 * 
 * 
 */

public final class LithiumPostingData extends PostData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public LithiumPostingData(){}
	
	/**
	 * Constructor, based on the JSONObject sent from Lithium the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Lithium
	 */
	public LithiumPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("constructing new subset of data of post from lithium post-object");
		logger.trace("  working on " + jsonObject.toString());
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();
		
		
		// ID des Posting
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonObject.get("id").toString());
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setId((Long) jsonObj.get("$"));
			logger.trace("the post id is " + getId());
		} catch (ParseException e) {
			logger.error("EXCEPTION :: parsing json failed " + e.getStackTrace().toString());
		}
		
		
		
		// the user
		// {}author									- this is the user that
		//		{}login
		//			$ : "Cortal_Consors"
		//			type : "string"
		//		type : "user"
		//		href : "/users/id/9"
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonObject.get("author").toString());
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			String idAsString = jsonObj.get("href").toString().substring(jsonObj.get("href").toString().lastIndexOf('/') + 1); 
			setUserId((Long.parseLong(idAsString.trim())));
			logger.trace("the user id is " + getUserId());
		} catch (ParseException e) {
			logger.error("EXCEPTION :: parsing json failed " + e.getStackTrace().toString());
		}
		
		
		// Sprache - fix on de at the moment
		setLang("de");
		
		
		// Timestamp as a string and as an objekt for the oDATA call
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonObject.get("post_time").toString());
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setTime((String) jsonObj.get("$"));
			setTimestamp(DataHelper.prepareLocalDateTime(getTime(), getSnId()));
			logger.trace("the post_time is " + getTime().toString());
		} catch (ParseException e) {
			logger.error("EXCEPTION :: parsing json failed " + e.getStackTrace().toString());
		}
		
		
		// Text des Post
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonObject.get("body").toString());
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			// strip all HTML tags from the post
			setText((String) stripHTML(jsonObj.get("$")));
			setRawText((String) jsonObj.get("$"));
			logger.trace("the text is " +  getText());
		} catch (ParseException e) {
			logger.error("EXCEPTION :: parsing json failed " + e.getStackTrace().toString());
		}
		
		
		// in which board was the message posted - we use the client field for this value
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(jsonObject.get("board").toString());
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setClient((String) jsonObj.get("href"));
			logger.trace("the board: " + getClient());
		} catch (ParseException e) {
			logger.error("EXCEPTION :: parsing json failed " + e.getStackTrace().toString());
		}
		
		// Flag to indicate if the post was truncated - this is NEVER used by the Lithium network
		setTruncated((Boolean) false);
	}
	
	
	/**
	 * 
	 * @description	convert an html text to plain text
	 * @param 		object
	 * @return		plan text
	 */
	private String stripHTML(Object object) {
		String html = object.toString();
		return Jsoup.parse(html).text();
	}

	/**
	 * setup the Object with NULL
	 */
	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;

		// set social network identifier
		sn_id = SocialNetworks.LITHIUM.getValue();

		text = null;
		raw_text = null;
		time = null;
		posted_from_client = null;
		truncated = false;
		lang = null;
		
		
		// these values are never used by Lithium
		in_reply_to_post = 0;
		in_reply_to_user = 0;
		in_reply_to_user_screen_name = null;
		coordinates = null;
		geoLatitude = null;
		geoLongitude = null;
		place = null;
		hashtags = null;
		symbols = null;
		mentions = null;
	}
}