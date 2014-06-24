package de.comlineag.sbm.handler;

import java.io.InputStream;

import org.apache.log4j.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.sbm.data.LithiumConstants;
import de.comlineag.sbm.data.LithiumStatusCode;
import de.comlineag.sbm.data.LithiumStatusException;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * 
 * @description LithiumParser implementation of the parser for the Lithium network community
 * 				LithiumParsr is an extension of the default GenericParser but differs in that
 * 				it implements a new parse method (parseMessages) and not the standard one from 
 * 				GenericParser.
 * 				
 * @version		1.0
 * 
 */
public final class LithiumParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());
	// defines some constants for the crawler behavior and locations
	LithiumConstants CONSTANTS = new LithiumConstants();
	
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
			JSONObject responseObj = (JSONObject)jsonObj.get(CONSTANTS.JSON_RESPONSE_OBJECT_TEXT);
			
			// first check if the server response is not only OK from an http point of view, but also
			//    from the perspective of the REST API call
			// TODO CHECK WHY THIS RETURNS UNKNOWN!!!
			jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT).toString().toUpperCase());
			logger.trace("json status code is " + responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT) + " translates to " + jsonStatusCode);
			
			if(!"success".equals(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT)))
				throw new LithiumStatusException("return code from server is " + jsonStatusCode);
			 
			//if(!jsonStatusCode.isOk())
			//	throw new LithiumStatusException("return code from server is " + jsonStatusCode);
			
			JSONObject messages = (JSONObject) responseObj.get(CONSTANTS.JSON_MESSAGES_OBJECT_IDENTIFIER);
			JSONArray messageArray = (JSONArray) messages.get(CONSTANTS.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
			
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
