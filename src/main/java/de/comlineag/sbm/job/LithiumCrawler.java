package de.comlineag.sbm.job;


import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpErrorMessages;
import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.data.LithiumStatusCode;
import de.comlineag.sbm.data.LithiumStatusException;
import de.comlineag.sbm.data.LithiumConstants;
import de.comlineag.sbm.handler.LithiumPosting;
import de.comlineag.sbm.persistence.NoBase64EncryptedValue;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * 
 * @description this is the actual crawler of the Lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              Lithium REST API to fetch posts and users
 * 
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// defines some constants for the crawler behavior and locations
	LithiumConstants CONSTANTS = new LithiumConstants();
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	// restrict the crawler to specific areas and other parameter
	private static boolean restrictToSites = false;
	private static boolean restrictToTerms = true;
	private static boolean restrictToLangs = false;
	private static boolean restrictToUsers = false;
	
	public LithiumCrawler() {}

	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Lithium-Crawler START");
		
		/* TODO: check with Maic, where these values should come from
		 * 
		 * some static vars for the lithium crawler taken from applicationContext.xml
		 * THESE ARE NOW IN LithiumConstants
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get("PROTOCOL");
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get("SERVER_URL");
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get("PORT");
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get("REST_API_LOC");
		*/
		final String REST_API_URL = CONSTANTS.PROTOCOL + "://" + CONSTANTS.SERVER_URL + ":" + CONSTANTS.PORT + CONSTANTS.REST_API_LOC;
		
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
		
		// THIS IS USED TO RESTRICT RESULTS TO SPECIFIC TERMS 
		String searchTerm = null;
		
		// setup restrictions on what to get from lithium - also says where to look
		if (restrictToSites) {
			String[] tSites = {"/Girokonto-Zahlungsverkehr/bd-p/Girokonto-Zahlungsverkehr",
								"/Sparen-Anlegen/bd-p/Sparen-und-Anlegen",
								"/Wertpapierhandel/bd-p/Wertpapierhandel",
								"/Finanzieren/bd-p/Finanzieren",
								"/Sonstige-Themen/bd-p/Sonstige-Themen"};
			smallLogMessage += "specific Sites ";
		}
		if (restrictToTerms) {
			// TODO: make this search term a parameter
			String[] tTerms = {"Aktien", "Tagesgeld", "Trading", "Depot", "Girokonto", "Wertpapier", "Kreditkarte", "HBCI"};
			smallLogMessage += "specific terms ";
		}
		if (restrictToLangs) {
			String[] tLangs = {"de", "en"};
			smallLogMessage += "specific languages ";
		}
		if (restrictToUsers) {
			String[] tUsers = {};
			smallLogMessage += "specific users ";
		}
		logger.debug("new lithium crawler instantiated - restricted to track " + smallLogMessage);
		
		
		//TODO implement authentication against lithium network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), 
											(String) arg0.getJobDetail().getJobDataMap().get("consumerSecret"), 
											(String) arg0.getJobDetail().getJobDataMap().get("token"), 
											(String) arg0.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
				
		// this is the status code for the http connection
		HttpStatusCode httpStatusCode = null;
		LithiumStatusCode jsonStatusCode = null;
		
		try {
			logger.trace("initiating ssl-connection to " + REST_API_URL);
			HttpClient client = new HttpClient();
			
			//TODO implement loop over different search terms
			//for (int i = 0 ; i < 100 ; i++ ){
			
				searchTerm = "HBCI";
			
				PostMethod method = new PostMethod(REST_API_URL+CONSTANTS.REST_MESSAGES_SEARCH_URI);
				method.addParameter(CONSTANTS.HTTP_RESPONSE_FORMAT_COMMAND, CONSTANTS.HTTP_RESPONSE_FORMAT);
				method.addParameter(CONSTANTS.SEARCH_TERM, searchTerm);
				httpStatusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
				String jsonString = method.getResponseBodyAsString();
				logger.trace("our json: " + jsonString);
				
				if (!httpStatusCode.isOk()){
					if (httpStatusCode == HttpStatusCode.FORBIDDEN){
						// TODO implement proper authorization handling
						logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()));
					} else {
						logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()) + " could not connect to " + REST_API_URL);
					}
				} else {
					logger.trace("http connection established (status is " + httpStatusCode + ")");
				}	
				
				// the JSON String we received from the http connection is now decoded and passed on to the 
				// specific parser for posts and user
				// 															courtesy by Maic Rittmeier
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(jsonString);
				JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
				if(jsonObj == null)
					throw new Exception();
				JSONObject responseObj = (JSONObject)jsonObj.get(CONSTANTS.JSON_RESPONSE_OBJECT_TEXT);
				
				// first check if the server response is not only OK from an http point of view, but also
				//    from the perspective of the REST API 
				jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT).toString().toUpperCase());
				
				if(!"success".equals(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT)))
					throw new LithiumStatusException("return code from server is " + jsonStatusCode);
				
				/* TODO check why this does not work
				if(!jsonStatusCode.isOk())
					throw new LithiumStatusException("return code from server is " + jsonStatusCode);
				*/
				JSONObject messages = (JSONObject) responseObj.get(CONSTANTS.JSON_MESSAGES_OBJECT_IDENTIFIER);
				JSONArray messageArray = (JSONArray) messages.get(CONSTANTS.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
			
				for(Object messageObj : messageArray){
					
					String messageRef = (String) ((JSONObject)messageObj).get(CONSTANTS.JSON_MESSAGE_REFERENCE);
					
					JSONObject messageResponse = SendMessageRequest(messageRef, REST_API_URL);
					if (messageResponse != null)
						new LithiumPosting(messageResponse).save();
					
				}
				
				//List<LithiumPosting> postings = new ArrayList<LithiumPosting>();
				//List<LithiumUser> users = new ArrayList<LithiumUser>();
			
			//} // loop over search terms
		}
		catch (LithiumStatusException le) {
			logger.error("EXCEPTION :: " + le.toString(), le);
		}
		catch (Exception e) {
			logger.error("EXCEPTION :: " + e.toString(), e);
		}
		
		logger.debug("Lithium-Crawler END");
	}
	
	/**
	 * @author		Maic Rittmeier
	 * @param 		messageRef
	 * @param 		REST_API_URL
	 * @return		JSONObject
	 * 							representing one message		
	 * 
	 * @description	retrieves a message-ref (part of a url) and the REST-Url
	 * 				and retrieves that specific message from the community 
	 */
	private JSONObject SendMessageRequest(String messageRef, String REST_API_URL) {
		HttpStatusCode httpStatusCode = null;
		LithiumStatusCode jsonStatusCode = null;
		
		try {
			logger.trace("initiating ssl-connection to " + REST_API_URL);
			HttpClient client = new HttpClient();
			
			PostMethod method = new PostMethod(REST_API_URL+messageRef);
			method.addParameter("restapi.response_format", "json");
			httpStatusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
			String jsonString = method.getResponseBodyAsString();
			logger.trace("our json: " + jsonString);
			
			if (!httpStatusCode.isOk()){
				if (httpStatusCode == HttpStatusCode.FORBIDDEN){
					// TODO implement proper authorization handling
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCode.getErrorCode())+" could not connect to " + REST_API_URL);
				}
			} else {
				logger.trace("http connection established (status is " + httpStatusCode + ")");
			}	
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new Exception();
			JSONObject responseObj = (JSONObject)jsonObj.get(CONSTANTS.JSON_RESPONSE_OBJECT_TEXT);
			
			// first check if the server response is not only OK from an http point of view, but also
			//    from the perspective of the REST API
			jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT).toString().toUpperCase());
			
			if(!"success".equals(responseObj.get(CONSTANTS.JSON_STATUS_CODE_TEXT)))
				throw new LithiumStatusException("return code from server is " + jsonStatusCode);
			
			/* TODO check why this does not work
			if(!jsonStatusCode.isOk())
				throw new LithiumStatusException("return code from server is " + jsonStatusCode);
			*/
			
			return (JSONObject) responseObj.get(CONSTANTS.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
		} catch (LithiumStatusException le){
			logger.error("EXCEPTION :: " + le.toString(), le);
		}
		catch (Exception e) {
			logger.error("EXCEPTION :: " + e.toString(), e);
		}
		
		return null;
	}


	// some useful functions
	/**
	 * @param 		user
	 * @param 		pwd
	 * @returns 	new password authentication against web resource
	 * 
	 * @description	this method shall commit basic authentication against a web resource
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
	 * @description decrypt given text 
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
