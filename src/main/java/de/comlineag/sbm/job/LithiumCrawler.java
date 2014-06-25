package de.comlineag.sbm.job;


import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpErrorMessages;
import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.data.LithiumConstants;
import de.comlineag.sbm.data.LithiumStatusCode;
import de.comlineag.sbm.data.LithiumStatusException;
import de.comlineag.sbm.handler.CrawlerConfiguration;
import de.comlineag.sbm.handler.LithiumParser;
import de.comlineag.sbm.handler.LithiumPosting;
import de.comlineag.sbm.handler.LithiumUser;
import de.comlineag.sbm.persistence.NoBase64EncryptedValue;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		1.1
 * 
 * @description this is the actual crawler for the Lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              Lithium REST API to search for posts that contain keywords.
 *              The keywords are sourced in by the configuration manager.
 *              The crawler also gets the user for each post and  
 * 
 * @changelog	0.9	first static version retrieves posts			Chris
 * 				1.0 retrieves posts and users						Chris
 * 				1.1	configuration is made dynamic 					Chris
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// defines some constants for the crawler behavior and locations
	LithiumConstants CONSTANTS = new LithiumConstants();
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	public LithiumCrawler() {}
	
	/**
	 * @author		Magbus Leinemann, Christian Guenther
	 * @version 	1.4
	 * 
	 * @description	this is the actual crawler implementation for the lithium network
	 *  
	 */
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.info("Lithium-Crawler START");
		
		// some static vars for the lithium crawler taken from applicationContext.xml
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get("PROTOCOL");
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get("SERVER_URL");
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get("PORT");
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get("REST_API_LOC");
		
		final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + REST_API_LOC;
		
		// authentication to lithium
		String _user = null;
		String _passwd = null;
		try {
			_user = decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
			_passwd = decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
		} catch (NoBase64EncryptedValue e) {
			logger.error("EXCEPTION :: value for user or passwd is NOT base64 encrypted " + e.toString(), e);
		}
		
		logger.trace("setting up the rest endpoint at " + REST_API_URL + " with user " + _user);
		
		
		// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES AND USERS
		logger.debug("now retrieving restrictions from configuration db");
		String searchTerm = null;
		ArrayList<String> tTerms = new CrawlerConfiguration().getTrackTerms(); 
		ArrayList<String> tLangs = new CrawlerConfiguration().getTrackLanguages(); 
		ArrayList<String> tSites = new CrawlerConfiguration().getTrackSites();
		
		// simple log output
		if (tSites.size()>0)
			smallLogMessage += "specific Sites ";
		if (tTerms.size()>0)
			smallLogMessage += "specific terms ";
		if (tLangs.size()>0)
			smallLogMessage += "specific languages ";
		logger.info("new lithium crawler instantiated - restricted to track " + smallLogMessage);
		
		
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), 
											(String) arg0.getJobDetail().getJobDataMap().get("consumerSecret"), 
											(String) arg0.getJobDetail().getJobDataMap().get("token"), 
											(String) arg0.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
				
		// this is the status code for the http connection
		HttpStatusCode httpStatusCode = null;
		
		try {
			logger.info("initiating ssl-connection to " + REST_API_URL);
			HttpClient client = new HttpClient();
			
			//TODO change this simple for-loop to something more sophisticated
			for (int i = 0 ; i < tTerms.size(); i++ ){
				searchTerm = tTerms.get(i).toString();
				logger.info("now searching for " + searchTerm);
			
				PostMethod method = new PostMethod(REST_API_URL+CONSTANTS.REST_MESSAGES_SEARCH_URI);
				method.addParameter(CONSTANTS.HTTP_RESPONSE_FORMAT_COMMAND, CONSTANTS.HTTP_RESPONSE_FORMAT);
				method.addParameter(CONSTANTS.SEARCH_TERM, searchTerm);
				httpStatusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
				String jsonString = method.getResponseBodyAsString();
				logger.trace("our json: " + jsonString);
				
				if (!httpStatusCode.isOk()){
					if (httpStatusCode == HttpStatusCode.FORBIDDEN){
						logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()));
					} else {
						logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()) + " could not connect to " + REST_API_URL);
					}
				} else {
					logger.trace("http connection established (status is " + httpStatusCode + ")");
				}	
				
				// give the json object to the lithium parser for further processing
				LithiumParser litParse = new LithiumParser();
				JSONArray messageArray = litParse.parseMessages(jsonString);
				
				for(Object messageObj : messageArray){
					String messageRef = (String) ((JSONObject)messageObj).get(CONSTANTS.JSON_MESSAGE_REFERENCE);
					
					JSONObject messageResponse = SendObjectRequest(messageRef, REST_API_URL, CONSTANTS.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
					if (messageResponse != null) {
						// first save the message
						new LithiumPosting(messageResponse).save();
						
						// now get the user from REST url and save it also
						try {
							JSONParser parser = new JSONParser();
							Object obj = parser.parse(messageResponse.toString());
							JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
							if(jsonObj == null)
								throw new ParseException(0, "returned json object is null");
							JSONObject authorObj = (JSONObject)jsonObj.get(CONSTANTS.JSON_AUTHOR_OBJECT_IDENTIFIER);
							
							String userRef = (String) ((JSONObject)authorObj).get(CONSTANTS.JSON_AUTHOR_REFERENCE);
							JSONObject userResponse = SendObjectRequest(userRef, REST_API_URL, CONSTANTS.JSON_USER_OBJECT_IDENTIFIER);
							logger.trace("user object: " + userResponse.toString());
							
							new LithiumUser(userResponse).save();
							
						} catch (ParseException e) {
							logger.error("EXCEPTION :: could not retrieve user from message object " + e.getLocalizedMessage());
							e.printStackTrace();
						}
						
						
					}
				}
				
			} // loop over search terms
		} catch (HttpException e) {
			logger.error("EXCEPTION :: HTTP Error: " + e.toString(), e);
		} catch (IOException e) {
			logger.error("EXCEPTION :: IO Error: " + e.toString(), e);
		}
		
		logger.info("Lithium-Crawler END");
	}
	
	
	/**
	 * @author		Christian Guenther
	 * 
	 * @description	expects an object-ref (part of a url) and the REST-Url plus a json identifier
	 * 				and retrieves that specific element (currently user or message) from the community
	 * 
	 * @param 		objectRef
	 * 					the uri part of a specific object - is appended to REST_API_URL
	 * @param 		REST_API_URL
	 * @param		jsonObjectIdentifier
	 * 					an element from CONSTANTS which identifies a json element within the object
	 * @return		JSONObject
	 * 					a json object (as identified from jsonObjectIdentifier) from the web ressource
	 * 
	 */
	private JSONObject SendObjectRequest(String objectRef, String REST_API_URL, String jsonObjectIdentifier) {
		HttpStatusCode httpStatusCode = null;
		LithiumStatusCode jsonStatusCode = null;
		
		try {
			logger.info("Lithium " + jsonObjectIdentifier+" tracked, retrieving from " + REST_API_URL + objectRef);
			HttpClient client = new HttpClient();
			
			PostMethod method = new PostMethod(REST_API_URL+objectRef);
			method.addParameter(CONSTANTS.HTTP_RESPONSE_FORMAT_COMMAND, CONSTANTS.HTTP_RESPONSE_FORMAT);
			httpStatusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
			String jsonString = method.getResponseBodyAsString();
			
			if (!httpStatusCode.isOk()){
				if (httpStatusCode == HttpStatusCode.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode())+" could not connect to " + REST_API_URL);
				}
			} else {
				logger.trace("http connection established (status is " + httpStatusCode + ")");
			}	
			
			logger.trace("our json: " + jsonString);
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
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
			
			return (JSONObject) responseObj.get(jsonObjectIdentifier);
			
		} catch (LithiumStatusException le){
			logger.error("EXCEPTION :: Lithium Error: " + le.toString(), le);
		} catch (Exception e) {
			logger.error("EXCEPTION :: Parse Error: " + e.toString(), e);
		}
		
		return null;
	}
	
	
	// some useful functions
	/**
	 * 
	 * @description	this method shall commit basic authentication against a web resource
	 * 
	 * @param 		user
	 * @param 		pwd
	 * @returns 	new password authentication against web resource
	 * 
	 */
	private void basicAuthentication(String user, String pwd){ 
		Authenticator.setDefault( new Authenticator() {
			@Override protected PasswordAuthentication getPasswordAuthentication() {
				System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n",
	                       getRequestingURL(), getRequestingHost(),
	                       getRequestingSite(), getRequestingPort() );
				
				return new PasswordAuthentication( "user", "pwd???".toCharArray() );
			}
		});
	}
		
	/**
	 * 
	 * @description decrypt given text 
	 * 
	 * @param 		param
	 *          	  encrypted text 
	 * @return 		clear text
	 *
	 */
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// the decode returns a byte-Array which needs to be converted to a string before returning
		byte[] base64Array;

		// validates that an encrypted value was returned
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschluesselt");
		}
		// (re)convert into string and return
		return new String(base64Array);
	}
}
