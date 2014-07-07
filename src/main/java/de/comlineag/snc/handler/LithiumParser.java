package de.comlineag.snc.handler;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.constants.LithiumConstants;
import de.comlineag.snc.constants.LithiumStatusCode;

/**
 * 
 * @author 		Christian Guenther, Maic Rittmeier
 * @category 	Handler				
 * @version		0.7
 * @status		productive
 *  
 * @description LithiumParser implementation of the parser for the Lithium network community
 * 				LithiumParsr is an extension of the default GenericParser but differs in that
 * 				it implements a new parse method (parseMessages) and not the standard one from 
 * 				GenericParser.
 * 				parseMessage expects a json string and decodes it according to fields provided
 * 				from LithiumConstants 
 * 
 * @changelog	0.1 (Chris)		class created as copy from TwitterParser
 * 				0.2 (Maic)		change from json to xml and back to json
 * 				0.3 (Chris)		implemented constants from LithiumConstants
 * 				0.4				first productive version returns array of messages
 * 				0.5 			bugfixing and optimization
 * 				0.6 			implemented proper json error handling
 * 				0.7 			changed constants to be static
 * 
 */
public final class LithiumParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public LithiumParser() {}

	/**
	 * @description	this is the parser implementation specific for the Lithium network
	 * 				it receives a Json string from the crawler and decodes that in single
	 * 				messages. These messages are then added to an array of Json objects
	 * 				and returned to the crawler, which retrieves every single posting and 
	 * 				feds it to the persistence 
	 * 
	 * @param 		jsonString
	 * 
	 * @return 		Array
	 * 					of json objects
	 */
	public  JSONArray parseMessages(String jsonString) {
		logger.trace("Lithium parser instantiated for messages with " + jsonString);
		
		// this is the status code within the json object string
		LithiumStatusCode jsonStatusCode = null;
		
		// the JSON String we received from the LithiumCrawler is now decoded and every 
		// single message in it is added to an array of messages. The array is handed
		// back to the crawler.
		// 															courtesy by Maic Rittmeier
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(jsonString);
		
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new ParseException(0, "returned json object is null");
			JSONObject responseObj = (JSONObject)jsonObj.get(LithiumConstants.JSON_RESPONSE_OBJECT_TEXT);
			
			// first check if the server response is not only OK from an http point of view, but also
			//    from the perspective of the REST API call
			jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(LithiumConstants.JSON_STATUS_CODE_TEXT).toString());
			if(!jsonStatusCode.isOk()){
				//error json: {"response":{"status":"error","error":{"code":501,"message":"Unbekanntes Pfadelement bei Knoten \u201Ecommunity_search_context\u201C"}}}
				JSONParser errorParser = new JSONParser();
				Object errorObj = errorParser.parse(jsonString);
				JSONObject jsonErrorObj = errorObj instanceof JSONObject ?(JSONObject) errorObj : null;
				if(jsonErrorObj == null)
					throw new ParseException(0, "returned json object is null");
				
				logger.error("the server returned error " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT));
				
				throw new LithiumStatusException("the server returned error " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT));
			}
			
			JSONObject messages = (JSONObject) responseObj.get(LithiumConstants.JSON_MESSAGES_OBJECT_IDENTIFIER);
			JSONArray messageArray = (JSONArray) messages.get(LithiumConstants.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
			
			return messageArray;
			
		} catch (ParseException e) {
			logger.error("EXCEPTION :: Parse Error: " + e.toString(), e);
		} catch (LithiumStatusException e) {
			logger.error("EXCEPTION :: Lithium Error: " + e.toString(), e);
		}
		return null;
	}
	
	@Override
	public void parse(String jsonString) {
		// this parse method is NOT used for the Lithium community
	}

	@Override
	protected void parse(InputStream is) {
		// this parse method is NOT used for the Lithium community
	}
}
