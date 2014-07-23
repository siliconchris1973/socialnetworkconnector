package de.comlineag.snc.data;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.GeneralDataDefinitions;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.GeneralConfiguration;
import de.comlineag.snc.helper.DataHelper;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.6a		- 20.07.2014
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
 *            "id" 						Long		id
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
 * 
 * TODO 1. Add support for labels
 * TODO 3. find a new/better method to truncate html
 * TODO 4. check if we need to safe the thread from which the posting originated
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
		logger.debug("constructing new subset of data of post (LT-"  + jsonObject.get("id") + ") from lithium post-object");
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();
		
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
			
			setId((Long) jsonObjId.get("$"));
			
			
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
			setUserId((Long.parseLong(userHref.trim())));
			
			
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
			setTimestamp(DataHelper.prepareLocalDateTime(getTime(), getSnId()));
			
			
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
				setText((String) DataHelper.stripHTML(jsonObjText.get("$").toString()));
			}
			if (GeneralDataDefinitions.RAW_TEXT_WITH_MARKUP) {
				setRawText((jsonObjText.get("$").toString()));
			} else {
				setRawText((String) DataHelper.stripHTML(jsonObjText.get("$").toString()));
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
					setTeaser((String) DataHelper.stripHTML(getText()));
				}
			} else {
				if (GeneralDataDefinitions.TEASER_WITH_MARKUP){
					setTeaser((String) jsonObjTeaser.get("$"));
				} else {
					setTeaser((String) DataHelper.stripHTML(jsonObjTeaser.get("$")));
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
					setSubject((String) DataHelper.stripHTML(getText()));
				}
			} else {
				if (GeneralDataDefinitions.SUBJECT_WITH_MARKUP){
					setSubject((String) jsonObjSubject.get("$"));
				} else {
					setSubject((String) DataHelper.stripHTML(jsonObjSubject.get("$")));
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
			// TODO : when implementing this, be aware of multiple occurrences for Label within Labels 
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
			
			logger.debug("     construction finished");
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json Lithium post-object " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	

	/**
	 * @description	setup a posting data with NULL-values
	 */
	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;
		
		domain = new CrawlerConfiguration<String>().getDomain();
		customer = new CrawlerConfiguration<String>().getCustomer();
		// set social network identifier
		sn_id = SocialNetworks.LITHIUM.getValue();

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
}