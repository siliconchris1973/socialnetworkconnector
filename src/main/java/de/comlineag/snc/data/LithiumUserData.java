package de.comlineag.snc.data;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.4a
 * @status		productive
 * 
 * @description Describes a single lithium user with all relevant informations.
 *              The class shall be used to make all methods handling a lithium
 *              user type save.
 * 
 * @param <JSonObject>
 *            "id" String
 *            "sn_id" String
 *            "name" String
 *            "screen_name" String
 *            "geoLocation" List
 *            "followers_count" Long
 *            "friends_count" Long
 *            "statuses_count" Long
 *            "favourites_count" Long
 *            "lists_and_groups_count" Long
 *            "lang" String
 *
 * @changelog	0.1 (Chris)		first initial version as copy from TwitterUserData
 * 				0.2 			added parsing of 2nd and 3rd level of json string
 * 				0.3 			bugfixing
 * 				0.4 			first productive version - without geo geoLocation
 * 				0.4a			changed id from Long to String
 * 
 * TODO implement geo geoLocation support for users
 * TODO check if user profile can and shall be used
 * TODO check if we need to support for the anonymous and deleted flag
 * TODO add support for average_rating_value, average_posting_rating_value and average_posting_ratio
 * 
 * JSON Structure:
 * 		1. Level			directly accessible through passed JSONObject
 * 			2. Level		one more JSONObject creation
 * 				3. Level	two more JSONObject creation 
 *
		{}user
			type : "user"
			href : "/users/id/9"
		{}profiles
			{}profile
				{}0
					name : "url_icon"
					type : "string"
					$ : "/t5/image/serverpage/image-id/563i1FE178F1680E09BF/image-size/avatar?v=mpbl-1&px=64"
		{}average_message_rating
			type : "float"
			$ : 0
		{}average_rating
			type : "float"
			$ : 0
		{}registration_time
			type : "date_time"
			$ : "2013-12-12T10:03:14+00:00"
		{}login
			type : "string"
			$ : "Cortal_Consors"
		{}anonymous
			type : "boolean"
			$ : false
		{}registered
			type : "boolean"
			$ : true
		{}id
			type : "int"
			$ : 9
		{}deleted
			type : "boolean"
			$ : false
 *
 */
public final class LithiumUserData extends UserData implements ISncDataObject{

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public LithiumUserData() {}
	
	
	
	/**
	 * Constructor, based on the JSONObject sent from Lithium the Data Object is prepared
	 * 
	 * @param jsonObject
	 */
	public LithiumUserData(JSONObject jsonObject) {
		logger.debug("constructing new subset of data of user (LT-"  + jsonObject.get("id") + ") from lithium user-object");
		//logger.trace("  working on " + jsonObject.toString());
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();
		String s; // helper var to cast from long to string
		
		try {
			JSONParser parser = new JSONParser();
			Object obj;
			
			// ID of the user
			// Structure: 
			// 	{}id
			//		$ : 2961
			// 		type : "int"
			obj = parser.parse(jsonObject.get("id").toString());
			JSONObject jsonObjId = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			s = Objects.toString(jsonObjId.get("$"), null);
			setId(s);
			
			// user_name / login and nickname, all the same at lithium
			// Structure
			//	{}login
			//		type : "string"
			//		$ : "Cortal_Consors"
			obj = parser.parse(jsonObject.get("login").toString());
			JSONObject jsonObjLogin = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			setUserName((String) jsonObjLogin.get("$"));
			setScreenName((String) jsonObjLogin.get("$"));
			
			// we are using the geoLocation field for the user profile icon
			// Structure
			//	{}profiles
			//		{}profile
			//			{}0
			//				name : "url_icon"
			//				type : "string"
			//				$ : "/t5/image/serverpage/image-id/563i1FE178F1680E09BF/image-size/avatar?v=mpbl-1&px=64"
			obj = parser.parse(jsonObject.get("profiles").toString());
			JSONObject jsonObjProfiles = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			if (jsonObjProfiles != null){
				// second level - get profile
				JSONParser parserProfile = new JSONParser();
				Object objProfile = parserProfile.parse(jsonObjProfiles.get("profile").toString());
				JSONObject jsonObjProfile0 = objProfile instanceof JSONObject ?(JSONObject) objProfile : null;
				
				if (jsonObjProfile0 != null){
					// third level - get element
					JSONParser parserElement = new JSONParser();
					Object objElement = parserElement.parse(jsonObjProfile0.get("0").toString());
					JSONObject jsonObjElement = objElement instanceof JSONObject ?(JSONObject) objElement : null;
					setGeoLocation((String) jsonObjElement.get("$"));
				}
			}
			
			if (jsonObject.containsKey("lang"))
				setLang((String) jsonObject.get("lang"));
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: during parsing of json Lithium user-object " + e.getLocalizedMessage());
		}
	}
	
	private void initialize(){
		// first setup the internal json objct
		internalJson = new JSONObject();
		
		// setting everything to 0 or null default value.
		id = "0";
		setObjectStatus("new");
		
		// set the internal fields and embedded json objects for domain, customer and social network
		setSnId(SocialNetworks.getSocialNetworkConfigElement("code", "LITHIUM"));
		setDomain(new CrawlerConfiguration<String>().getDomain());
		setCustomer(new CrawlerConfiguration<String>().getCustomer());
		
		user_name 				= null;
		screen_name 			= null;
		geoLocation 			= "";
		followers_count 		= 0;
		friends_count 			= 0;
		postings_count 			= 0;
		favorites_count 		= 0;
		lists_and_groups_count	= 0;
		lang 					= "DE";
	}
}