package de.comlineag.sbm.job;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpErrorMessages;
import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.data.LithiumStatusException;
import de.comlineag.sbm.handler.LithiumParser;
import de.comlineag.sbm.handler.LithiumPosting;
import de.comlineag.sbm.handler.TwitterPosting;
import de.comlineag.sbm.handler.TwitterUser;
import de.comlineag.sbm.persistence.NoBase64EncryptedValue;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              lithium rest api to fetch posts and users
 * 
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	//private String smallLogMessage = "";
	
	
	public LithiumCrawler() {}

	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Lithium-Crawler START");
		
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
		} catch (NoBase64EncryptedValue e4) {
			logger.error("EXCEPTION :: value for user or passwd is NOT base64 encrypted " + e4.toString());
		}
		
		logger.trace("setting up the rest endpoint at " + REST_API_URL + " with user " + _user);
		
		
		/*
		// setup restrictions on what to get from lithium - also says where to look
		String[] tSites = {"/Girokonto-Zahlungsverkehr/bd-p/Girokonto-Zahlungsverkehr",
							"/Sparen-Anlegen/bd-p/Sparen-und-Anlegen",
							"/Wertpapierhandel/bd-p/Wertpapierhandel",
							"/Finanzieren/bd-p/Finanzieren",
							"/Sonstige-Themen/bd-p/Sonstige-Themen"};
		smallLogMessage += "specific Sites ";
		
		String[] tTerms = {"Tagesgeld", "Trading", "Depot", "Girokonto", "Wertpapier", "Kreditkarte", "HBCI"};
		smallLogMessage += "specific terms ";
		
		String[] tLangs = {"de", "en"};
		smallLogMessage += "specific languages ";
		
		logger.debug("new lithium parser instantiated - restricted to track " + smallLogMessage);
		*/
		
		//TODO implement authentication against lithium network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), 
											(String) arg0.getJobDetail().getJobDataMap().get("consumerSecret"), 
											(String) arg0.getJobDetail().getJobDataMap().get("token"), 
											(String) arg0.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
		
		// this is the status code for the http connection
		HttpStatusCode statusCode = null;
		
		try {
			logger.trace("initiating ssl-connection to " + REST_API_URL);
			HttpClient client = new HttpClient();
			
			PostMethod method = new PostMethod(REST_API_URL+"/search/messages");
			method.addParameter("restapi.response_format", "json");
			method.addParameter("phrase", "Aktien");
			statusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
			String jsonString = method.getResponseBodyAsString();
			logger.trace("our json: " + jsonString);
			
			if (!statusCode.isOk()){
				if (statusCode == HttpStatusCode.FORBIDDEN){
					// TODO implement proper authorization handling
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode())+" could not connect to " + REST_API_URL);
				}
			} else {
				logger.debug("connection established (status is " + statusCode + ")");
			}	
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new Exception();
			JSONObject responseObj = (JSONObject)jsonObj.get("response");
			String status = (String) responseObj.get("status");
			if(!"success".equals(status))
				throw new LithiumStatusException("statusText");
			JSONObject messages = (JSONObject) responseObj.get("messages");
			JSONArray messageArray = (JSONArray) messages.get("message");
		
			for(Object messageObj : messageArray){
				
				String messageRef = (String) ((JSONObject)messageObj).get("href");
				
				JSONObject messageResponse = SendMessageRequest(messageRef, REST_API_URL);
				if (messageResponse != null)
					new LithiumPosting(messageResponse).save();
				
			}
			
			//List<LithiumPosting> postings = new ArrayList<LithiumPosting>();
			//List<LithiumUser> users = new ArrayList<LithiumUser>();
			
			// TODO implement counter and loop to get all messages for given keyword 
			
		}
		catch (LithiumStatusException le){
			
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("EXCEPTION :: " + e.toString());
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
		HttpStatusCode statusCode = null;
		
		try {
			logger.trace("initiating ssl-connection to " + REST_API_URL);
			HttpClient client = new HttpClient();
			
			PostMethod method = new PostMethod(REST_API_URL+messageRef);
			method.addParameter("restapi.response_format", "json");
			statusCode = HttpStatusCode.getHttpStatusCode(client.executeMethod(method));
			String jsonString = method.getResponseBodyAsString();
			logger.trace("our json: " + jsonString);
			
			if (!statusCode.isOk()){
				if (statusCode == HttpStatusCode.FORBIDDEN){
					// TODO implement proper authorization handling
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode())+" could not connect to " + REST_API_URL);
				}
			} else {
				logger.debug("connection established (status is " + statusCode + ")");
			}	
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new Exception();
			JSONObject responseObj = (JSONObject)jsonObj.get("response");
			String status = (String) responseObj.get("status");
			if(!"success".equals(status))
				throw new LithiumStatusException("statusText");
			return (JSONObject) responseObj.get("message");
		}
		catch (LithiumStatusException le){
			
		}
		catch (Exception e) {
			logger.error("EXCEPTION :: " + e.toString());
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
	 * @description Entschluesselt Werte aus der Konfig fuer die Connection
	 *
	 * @param param
	 *            der Wert der entschluesselt werden soll
	 * @return Klartext
	 *
	 */
	@SuppressWarnings("unused")
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// byte-Array kommt vom Decoder zurueck und kann dann in String uebernommen und zurueckgegeben werden
		byte[] base64Array;

		// Validierung das auch ein verschluesselter Wert da angekommen ist
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschluesselt");
		}
		// konvertiere in String
		return new String(base64Array);
	}
}
