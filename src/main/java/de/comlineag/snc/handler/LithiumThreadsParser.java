package de.comlineag.snc.handler;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.comlineag.snc.constants.LithiumConstants;

/**
 * 
 * @author 		Christian Guenther, Maic Rittmeier
 * @category 	Handler				
 * @version		0.7
 * @status		productive
 *  
 * @description LithiumThreadsParser is the implementation of the parser for the Lithium network community
 * 				LithiumThreadsParser is called by LithiumParser (an extension of the default GenericParser)
 * 				It's single method parse expects a json object and decodes it according to fields provided 
 * 				from LithiumConstants 
 * 
 * @changelog	0.1 (Chris)		class created as copy from LithiumParser Revision 0.7
 * 
 */
public final class LithiumThreadsParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private final static String outerObjectIdentifier = LithiumConstants.JSON_THREADS_OBJECT_IDENTIFIER;
	private final static String innerObjectIdentifier = LithiumConstants.JSON_SINGLE_THREAD_OBJECT_IDENTIFIER;
	
	public LithiumThreadsParser() {}

	/**
	 * @description	this is the parser implementation specific for the Lithium network
	 * 				it receives a Json object and decodes that in single messages.
	 * 				These messages are then added to an array of Json objects
	 * 				and returned to the crawler, which retrieves every single posting and 
	 * 				feds it to the persistence 
	 * 
	 * @param 		json object
	 * 
	 * @return 		Array
	 * 					of json objects
	 */
	public JSONArray parse(JSONObject jsonObject) {
		logger.debug("Lithium parser instantiated for threads");
		//logger.trace("   message content is: " + jsonString);
		
		// the JSON object we received from LithiumParser is now decoded and every 
		// single message in it is added to an array of messages. The array is handed
		// back to the crawler.
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			
			JSONObject messages = (JSONObject) jsonObj.get(outerObjectIdentifier);
			JSONArray messageArray = (JSONArray) messages.get(innerObjectIdentifier);
			
			return messageArray;
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: Parse Error: " + e.toString(), e);
		}
		return null;
	}
}
