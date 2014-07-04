package de.comlineag.snc.job;

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

import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.HttpErrorMessages;
import de.comlineag.snc.constants.HttpStatusCode;
import de.comlineag.snc.constants.LithiumConstants;
import de.comlineag.snc.constants.LithiumStatusCode;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.GenericCrawlerException;
import de.comlineag.snc.handler.LithiumParser;
import de.comlineag.snc.handler.LithiumPosting;
import de.comlineag.snc.handler.LithiumStatusException;
import de.comlineag.snc.handler.LithiumUser;
import de.comlineag.snc.persistence.NoBase64EncryptedValue;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		1.3
 * 
 * @description this is the actual crawler for the Lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              Lithium REST API to search for posts that contain keywords.
 *              The keywords are sourced in by the configuration manager.
 *              The crawler also gets the user for each post and  
 * 
 * @changelog	0.1 copy of TwitterCrawler										Chris
 * 				0.2 - 0.4 try and error with xml rest api						
 * 				0.5 changed payload to json and retrieves posts					Maic
 * 				0.6 - 0.7 bugfixing and optimization							Chris
 * 				0.8 added support to retrieve users								
 * 				0.9 added search prio inserting new data						
 * 				1.0	first productive version retrieves posts and users			
 * 				1.1	configuration is made dynamic 
 *				1.2	added support for SocialNetwork specific configuration
 *				1.3 implemented proper json error handling
 *				1.4 added configuration constants
 *
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// defines some constants for the crawler behavior and locations
	LithiumConstants LITHIUM_CONSTANTS = new LithiumConstants();
	// defines some configuration details
	ConfigurationConstants CONFIG_CONSTANTS = new ConfigurationConstants();
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	public LithiumCrawler() {}
	
	/**
	 * @author		Christian Guenther
	 * @version 	1.4
	 * @throws		GenericCrawlerException 
	 * @description	this is the actual crawler implementation for the lithium network
	 *  
	 */
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.info("Lithium-Crawler START");
		
		// some static vars for the lithium crawler taken from applicationContext.xml
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.HTTP_ENDPOINT_PROTOCOL_KEY);
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.HTTP_ENDPOINT_SERVER_URL_KEY);
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.HTTP_ENDPOINT_PORT_KEY);
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.HTTP_ENDPOINT_REST_API_LOC_KEY);
		
		final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + REST_API_LOC;
		
		// this is the status code for the http connection
		HttpStatusCode httpStatusCode = null;
		// and this the status code as coded within the json response
		LithiumStatusCode jsonStatusCode = null;
		
		
		// authentication to lithium
		String _user = null;
		@SuppressWarnings("unused")
		String _passwd = null;
		try {
			_user = decryptValue((String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.AUTHENTICATION_USER_KEY));
			_passwd = decryptValue((String) arg0.getJobDetail().getJobDataMap().get(CONFIG_CONSTANTS.AUTHENTICATION_PASSWORD_KEY));
		} catch (NoBase64EncryptedValue e) {
			logger.error("EXCEPTION :: value for user or passwd is NOT base64 encrypted " + e.toString(), e);
			System.exit(-1);
		}
		
		// THESE VALUES ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND SITES (aka boards)
		logger.info("retrieving restrictions from configuration db");
		String searchTerm = null;
		
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(CONFIG_CONSTANTS.CONSTRAINT_TERM_TEXT, SocialNetworks.LITHIUM); 
		ArrayList<String> tUsers = new CrawlerConfiguration<String>().getConstraint(CONFIG_CONSTANTS.CONSTRAINT_USER_TEXT, SocialNetworks.LITHIUM);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(CONFIG_CONSTANTS.CONSTRAINT_LANGUAGE_TEXT, SocialNetworks.LITHIUM); 
		ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(CONFIG_CONSTANTS.CONSTRAINT_SITE_TEXT, SocialNetworks.LITHIUM);
		
		// simple log output
		if (tTerms.size()>0)
			smallLogMessage += "specific terms ";
		if (tUsers.size()>0)
			smallLogMessage += "specific Sites ";
		if (tLangs.size()>0)
			smallLogMessage += "specific languages ";
		if (tSites.size()>0)
			smallLogMessage += "specific Sites ";
		logger.info("new lithium crawler instantiated - restricted to track " + smallLogMessage);
		
		
		// now from this point everything is just one big mess and in it, we retrieve messages and users
		try {
			String postEndpoint = null;
			
			// if no specific sites are configured, we use the standard REST_API_URL and message search endpoint
			if (tSites.size()==0){
				tSites.add(REST_API_URL+LITHIUM_CONSTANTS.REST_MESSAGES_SEARCH_URI);
			} else {
				// otherwise each board from the sites section of the configuration file is surrounded by 
				// the REST_API_URL and the message search uri
				String t = null;
				for (int i = 0; i < tSites.size() ; i++) {
					t = tSites.get(i);
					tSites.set(i, REST_API_URL + t + LITHIUM_CONSTANTS.REST_MESSAGES_SEARCH_URI);
				}
			}
			
			//TODO change this simple double for-loop to something more sophisticated
			// maybe we can use the GenericExecutorService class for this???
			for (int i = 0 ; i < tSites.size(); i++ ){
				// because the search endpoint can either be standard REST_API_URL or any of the configured sites, 
				// we need a temp-variable to store it 
				postEndpoint = tSites.get(i);
				logger.trace("setting up the rest endpoint at " + postEndpoint + " with user " + _user);
				HttpClient client = new HttpClient();
				
				for (int ii = 0 ; ii < tTerms.size(); ii++ ){
					searchTerm = tTerms.get(ii).toString();
					logger.info("now searching for " + searchTerm + " in " + postEndpoint);
					
					// http connection stuff to get messages per search term
					PostMethod method = new PostMethod(postEndpoint );
					method.addParameter(LITHIUM_CONSTANTS.HTTP_RESPONSE_FORMAT_COMMAND, LITHIUM_CONSTANTS.HTTP_RESPONSE_FORMAT);
					method.addParameter(LITHIUM_CONSTANTS.SEARCH_TERM, searchTerm);
					httpStatusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
					if (!httpStatusCode.isOk()){
						if (httpStatusCode == HttpStatusCode.FORBIDDEN){
							logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()));
						} else {
							logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()) + " could not connect to " + postEndpoint);
						}
					} else {
						// ONLY and ONLY if the http connection was successfull, we should even try to decode a response json
						logger.debug("http connection established (status is " + httpStatusCode + ")");
						
						// now get the json and check on that
						String jsonString = method.getResponseBodyAsString();
						logger.trace("our json in the execute loop: " + jsonString);
						
						// now do the check on json error details within  the returned JSON object
						JSONParser errParser = new JSONParser();
						Object errObj = errParser.parse(jsonString);
						JSONObject jsonErrObj = errObj instanceof JSONObject ?(JSONObject) errObj : null;
						if(jsonErrObj == null)
							throw new ParseException(0, "returned json object is null");
						JSONObject responseObj = (JSONObject)jsonErrObj.get(LITHIUM_CONSTANTS.JSON_RESPONSE_OBJECT_TEXT);
						jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(LITHIUM_CONSTANTS.JSON_STATUS_CODE_TEXT).toString());
						
						if(!jsonStatusCode.isOk()){
							/*
							 *  error json structure: 
							 *  {"response":{
							 *  	"status":"error",
							 *  	"error":{
							 *  		"code":501,
							 *  		"message":"Unbekanntes Pfadelement bei Knoten \u201Ecommunity_search_context\u201C"
							 *  		}
							 *  	}
							 *  }
							 */
							JSONObject errorReference = (JSONObject)responseObj.get(LITHIUM_CONSTANTS.JSON_ERROR_OBJECT_TEXT);
							logger.error("the server returned error " + errorReference.get(LITHIUM_CONSTANTS.JSON_ERROR_CODE_TEXT) + " - " + errorReference.get(LITHIUM_CONSTANTS.JSON_ERROR_MESSAGE_TEXT));
							
							throw new GenericCrawlerException("the server returned error " + errorReference.get(LITHIUM_CONSTANTS.JSON_ERROR_CODE_TEXT) + " - " + errorReference.get(LITHIUM_CONSTANTS.JSON_ERROR_MESSAGE_TEXT));
						} else {
							
							// give the json object to the lithium parser for further processing
							LithiumParser litParse = new LithiumParser();
							JSONArray messageArray = litParse.parseMessages(jsonString);
							
							for(Object messageObj : messageArray){
								String messageRef = (String) ((JSONObject)messageObj).get(LITHIUM_CONSTANTS.JSON_MESSAGE_REFERENCE);
								
								JSONObject messageResponse = SendObjectRequest(messageRef, REST_API_URL, LITHIUM_CONSTANTS.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
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
										JSONObject authorObj = (JSONObject)jsonObj.get(LITHIUM_CONSTANTS.JSON_AUTHOR_OBJECT_IDENTIFIER);
										
										String userRef = (String) ((JSONObject)authorObj).get(LITHIUM_CONSTANTS.JSON_AUTHOR_REFERENCE);
										JSONObject userResponse = SendObjectRequest(userRef, REST_API_URL, LITHIUM_CONSTANTS.JSON_USER_OBJECT_IDENTIFIER);
										logger.trace("user object: " + userResponse.toString());
										
										new LithiumUser(userResponse).save();
										
									} catch (ParseException e) {
										logger.error("EXCEPTION :: could not retrieve user from message object " + e.getLocalizedMessage());
										e.printStackTrace();
									} // try catch
								} // if message response is != null
							}// for loop over message array
						}// is json ok
					}// is http ok
				}// loop over search terms.exe
			}// loop over sites
		} catch (HttpException e) {
			logger.error("EXCEPTION :: HTTP Error: " + e.toString(), e);
		} catch (IOException e) {
			logger.error("EXCEPTION :: IO Error: " + e.toString(), e);
		} catch (ParseException e) {
			logger.error("EXCEPTION :: Parse Error: " + e.toString(), e);
		} catch (GenericCrawlerException e) {
			logger.error("EXCEPTION :: Crawler Error: " + e.toString(), e);
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
	 * 					an element from LITHIUM_CONSTANTS which identifies a json element within the object
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
			method.addParameter(LITHIUM_CONSTANTS.HTTP_RESPONSE_FORMAT_COMMAND, LITHIUM_CONSTANTS.HTTP_RESPONSE_FORMAT);
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
			
			logger.trace("our json within SendObjectRequest: " + jsonString);
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new ParseException(0, "returned json object is null");
			JSONObject responseObj = (JSONObject)jsonObj.get(LITHIUM_CONSTANTS.JSON_RESPONSE_OBJECT_TEXT);
			
			// first check if the server response is not only OK from an http point of view, but also
			//    from the perspective of the REST API call
			jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(LITHIUM_CONSTANTS.JSON_STATUS_CODE_TEXT).toString());
			logger.trace("json status code is " + responseObj.get(LITHIUM_CONSTANTS.JSON_STATUS_CODE_TEXT) + " translates to " + jsonStatusCode);
			
			if(!jsonStatusCode.isOk()){
				/*
				 *  error json structure: 
				 *  {"response":{
				 *  	"status":"error",
				 *  	"error":{
				 *  		"code":501,
				 *  		"message":"Unbekanntes Pfadelement bei Knoten \u201Ecommunity_search_context\u201C"
				 *  		}
				 *  	}
				 *  }
				 */
				JSONParser errorParser = new JSONParser();
				Object errorObj = errorParser.parse(jsonString);
				JSONObject jsonErrorObj = errorObj instanceof JSONObject ?(JSONObject) errorObj : null;
				if(jsonErrorObj == null)
					throw new ParseException(0, "returned json object is null");
				
				logger.error("the server returned error " + jsonErrorObj.get(LITHIUM_CONSTANTS.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LITHIUM_CONSTANTS.JSON_ERROR_MESSAGE_TEXT));
				throw new LithiumStatusException("the server returned error " + jsonErrorObj.get(LITHIUM_CONSTANTS.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LITHIUM_CONSTANTS.JSON_ERROR_MESSAGE_TEXT));
			} else  {
				return (JSONObject) responseObj.get(jsonObjectIdentifier);
			}
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
	@SuppressWarnings("unused")
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
