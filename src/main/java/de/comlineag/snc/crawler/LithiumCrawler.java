package de.comlineag.snc.crawler;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.CryptoProvider;
import de.comlineag.snc.constants.HttpErrorMessages;
import de.comlineag.snc.constants.HttpStatusCodes;
import de.comlineag.snc.constants.LithiumConstants;
import de.comlineag.snc.constants.LithiumStatusCode;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.GenericCrawlerException;
import de.comlineag.snc.handler.LithiumPosting;
import de.comlineag.snc.handler.LithiumStatusException;
import de.comlineag.snc.handler.LithiumUser;
import de.comlineag.snc.parser.LithiumParser;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		1.3				- 16.09.2014
 * @status		productive
 * 
 * @description this is the actual crawler for the Lithium network. It is implemenetd as a job and,
 *              upon execution, will connect to the Lithium REST API to search for posts that contain keywords.
 *              The keywords are sourced in by the configuration manager.  
 * 
 * @limitation	Currently the class uses annotation @DisallowConcurrentExecution
 *              because we can't let the job run multiple times for the sake of 
 *              data consistency - this will be resolved in version 1.1 
 * 
 * @changelog	0.1 (Chris)		copy of TwitterCrawler
 * 				0.2 			try and error with xml rest api						
 * 				0.3 (Maic)		changed payload to json and retrieves posts	
 * 				0.4 (Chris)		bugfixing and optimization
 * 				0.5 			added support to retrieve users								
 * 				0.6 			added search prior inserting new data						
 * 				0.7				first productive version retrieves posts and users			
 * 				0.8				configuration is made dynamic 
 *				0.9				added support for SocialNetwork specific configuration
 *				1.0 			implemented proper json error handling
 *				1.1 			added configuration constants
 *				1.1a			moved Base64CryptoProvider in its own class
 *				1.1b			added support for different encryption provider, the actual one is set in applicationContext.xml 
 *				1.2				changed search against rest api url to use method parameter instead of for-loop 
 *				1.3				added support for runState configuration, to check if the crawler shall actually run
 *
 * TODO change data retrieval to use getResponseBodyAsStream - fixes warning: Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
 * TODO make parser work when using threads instead of messages
 */
@DisallowConcurrentExecution 
public class LithiumCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="LITHIUM";
	private static String name="LithiumCrawler";
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	// shall the system grab messages directly or through threads
	String threadsOrMessages = "messages";
	
	

	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	//private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");

	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	private final boolean rtcPersistenceThreading = rtc.getBooleanValue("PersistenceThreadingEnabled", "runtime");
	
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	private int messageCount = 0;
	
	public LithiumCrawler() {}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Stopwatch timer = new Stopwatch().start();
		
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		
		// first check is to get the information, if the crawler was 
		// deactivated from within the crawler configuration, even if 
		// it is active in applicationContext.xml
		if ((Boolean) crawlerConfig.getRunState(CRAWLER_NAME)) {
			
			//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
			JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
	
			configurationScope.put((String) "SN_ID", (String) SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
			
			// set the customer we start the crawler for and log the startup message
			String curDomain = (String) configurationScope.get(rtcDomainKey);
			String curCustomer = (String) configurationScope.get(rtcCustomerKey);
			
			if ("undefined".equals(curDomain) && "undefined".equals(curCustomer)) {
				logger.info(CRAWLER_NAME+"-Crawler START");
			} else {
				if (!"undefined".equals(curDomain) && !"undefined".equals(curCustomer)) {
					logger.info(CRAWLER_NAME+"-Crawler START for " + curCustomer + " in " + curDomain);
				} else {
					if (!"undefined".equals(curDomain))
						logger.info(CRAWLER_NAME+"-Crawler START for " + curDomain);
					else
						logger.info(CRAWLER_NAME+"-Crawler START for " + curCustomer);
				}
			}
			
			// authentication to lithium
			String _user = null;
			@SuppressWarnings("unused")
			String _passwd = null;
			
			// this is just example code to show, how to interact with the CryptoProvider enum
			String desiredStrength = "low";
			CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
			logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
			
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getClass().getSimpleName());
				_user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_USER_KEY));
				_passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_PASSWORD_KEY));
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getClass().getSimpleName() + ": " + e.toString(), e);
			}
			
			// some static vars for the lithium crawler taken from applicationContext.xml
			logger.trace("retrieving configuration details for server endpoint from job control " );
			final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_PROTOCOL_KEY);
			final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_SERVER_URL_KEY);
			final String PORT = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_PORT_KEY);
			final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_REST_API_LOC_KEY);
			
			final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + REST_API_LOC;
			
			// this is the status code for the http connection
			HttpStatusCodes httpStatusCodes = null;
			// and this the status code as coded within the json response
			LithiumStatusCode jsonStatusCode = null;
			
			
			// THESE VALUES ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND SITES (aka boards)
			logger.debug("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(constraintLangText, configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(constraintUserText, configurationScope);
			ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
			//ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(constraintLocaText, configurationScope);
			// blocked URLs
			ArrayList<String> bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
			
			
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
					logger.trace("no site restrictions given");
					// now either call threads or messages and return that			
					if ("messages".equals(threadsOrMessages)) {
						logger.debug("MESSAGES chosen");
						tSites.add(REST_API_URL+LithiumConstants.REST_MESSAGES_SEARCH_URI);
					} else if ("threads".equals(threadsOrMessages)) {
						logger.debug("THREADS chosen");
						tSites.add(REST_API_URL+LithiumConstants.REST_THREADS_URI+"/recent");
					}
					
					logger.trace("no restriction to specific sites, setting endpoint to " + tSites.get(0).toString());
				} else {
					logger.trace("converting given site restrictions to valid rest endpoints");
					// otherwise each board from the sites section of the configuration file is surrounded by 
					// the REST_API_URL and the message search uri
					String t = null;
					for (int i = 0; i < tSites.size() ; i++) {
						t = tSites.get(i);
						// EXAMPLE https://wissen.cortalconsors.de:443/restapi/vc/boards/id/Girokonto-Zahlungsverkehr/search/messages
						if ("messages".equals(threadsOrMessages)) {
							logger.debug("MESSAGES chosen");
							tSites.set(i, REST_API_URL + t + LithiumConstants.REST_MESSAGES_SEARCH_URI);
						} else if ("threads".equals(threadsOrMessages)) {
							logger.debug("THREADS chosen");
							tSites.set(i, REST_API_URL + t + LithiumConstants.REST_THREADS_URI);
						}
						logger.trace("     " + tSites.get(i));
					}
				}
				
				// maybe we can use the GenericExecutorService class for this???
				for (int i = 0 ; i < tSites.size(); i++ ){
					// because the search endpoint can either be standard REST_API_URL or any of the configured sites, 
					// we need a temp-variable to store it 
					postEndpoint = tSites.get(i);
					logger.debug("setting up the rest endpoint at " + postEndpoint + " with user " + _user);
					HttpClient client = new HttpClient();
					String searchTerm = null;
					
					// To perform a community-wide search for a query,  you can use the following REST API call:
					//		http://YOURCOMMUNITYURL/<community-id>/restapi/PVC/search/messages?Q=<query>
					//	For example:
					//		http://community.lithium.com/lithosphere/restapi/vc/search/messages?q=testing
					//	for Cortal Consors this would be
					//		https://wissen.cortalconsors.de:443/restapi/vc/search/messages
					PostMethod method = new PostMethod(postEndpoint);
					method.addParameter(LithiumConstants.HTTP_RESPONSE_FORMAT_COMMAND, LithiumConstants.HTTP_RESPONSE_FORMAT);
					for (int ii = 0 ; ii < tTerms.size(); ii++ ){
						searchTerm = tTerms.get(ii).toString();
						logger.trace("now adding searchterm " + searchTerm + " to method parameter");
						method.addParameter(LithiumConstants.SEARCH_TERM, searchTerm);
					}
					// add some more parameter to search term 
					//method.addParameter("collapse_discussion", "false");
					//method.addParameter("restapi.format_detail","full_list_element");
					//method.addParameter("thread_ascending", "thread_descending");
					
					
					httpStatusCodes = HttpStatusCodes.getHttpStatusCode(client.executeMethod(method));
					if (!httpStatusCodes.isOk()){
						if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
							logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
						} else {
							logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()) + " could not connect to " + postEndpoint);
						}
					} else {
						// ONLY and ONLY if the http connection was successfull, we should even try to decode a response json
						logger.debug("http connection established (status is " + httpStatusCodes + ")");
						
						// now get the json and check on that
						String jsonString = method.getResponseBodyAsString();
						logger.trace("our posting json in the execute loop: " + jsonString);
						
						// now do the check on json error details within  the returned JSON object
						JSONParser errParser = new JSONParser();
						Object errObj = errParser.parse(jsonString);
						JSONObject jsonErrObj = errObj instanceof JSONObject ?(JSONObject) errObj : null;
						if(jsonErrObj == null)
							throw new ParseException(0, "returned json object is null");
						JSONObject responseObj = (JSONObject)jsonErrObj.get(LithiumConstants.JSON_RESPONSE_OBJECT_TEXT);
						jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(LithiumConstants.JSON_STATUS_CODE_TEXT).toString());
						
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
							JSONObject errorReference = (JSONObject)responseObj.get(LithiumConstants.JSON_ERROR_OBJECT_TEXT);
							logger.error("the server returned an error " + errorReference.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + errorReference.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT) + " while trying to retrieve " + postEndpoint);
							
							throw new GenericCrawlerException("the server returned an error " + errorReference.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + errorReference.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT) + " while trying to retrieve " + postEndpoint);
						} else {
							logger.debug("json response was ok, now extracting the messages");
							// give the json object to the lithium parser for further processing
							LithiumParser litParse = new LithiumParser();
							JSONArray messageArray = litParse.parseMessages(jsonString);
							
							for(Object messageObj : messageArray){
								messageCount++;
								setPostsTracked(messageCount);
								
								String messageRef = (String) ((JSONObject)messageObj).get(LithiumConstants.JSON_MESSAGE_REFERENCE);
								
								logger.info("SocialNetworkPost #"+messageCount+" tracked from " + CRAWLER_NAME);
								
													
								JSONObject messageResponse = SendObjectRequest(messageRef, REST_API_URL, LithiumConstants.JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER);
								if (messageResponse != null) {
									// create a lithium posting object from the message response from Lithium
									LithiumPosting litPost = new LithiumPosting(messageResponse);
									
									// TODO check if this is the right spot to add the track terms to the posting
									ArrayList<String> keywords = new ArrayList<String>();
									for (String keyword : tTerms){
										if (findTheNeedle(litPost.getJson().get("text").toString(), keyword)) {
											logger.trace("adding trackterm {} to list of tracked keywords", keyword);
											keywords.add(keyword);
										}
									}
									// now we should have an array list of the found trackterms in the post
									litPost.setTrackTerms(keywords);
									
									try {
										// retrieve the user from REST url and save it
										JSONParser parser = new JSONParser();
										Object obj = parser.parse(messageResponse.toString());
										JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
										if(jsonObj == null)
											throw new ParseException(0, "returned json object is null");
										JSONObject authorObj = (JSONObject)jsonObj.get(LithiumConstants.JSON_AUTHOR_OBJECT_IDENTIFIER);
										
										String userRef = (String) ((JSONObject)authorObj).get(LithiumConstants.JSON_AUTHOR_REFERENCE);
										JSONObject userResponse = SendObjectRequest(userRef, REST_API_URL, LithiumConstants.JSON_USER_OBJECT_IDENTIFIER);
										
										LithiumUser litUser = new LithiumUser(userResponse);
										litUser.save();
										
										// now add the extracted user-data object back in the posting data object
										// so that later, in the call to the graph persistence manager, we can get 
										// post and user-objects from one combined json structure
										logger.trace("about to add the user object to the post object \n    {}", litUser.getJson());
										litPost.setUserObject(litUser.getUserData());
										
										// next save the message
										if (rtcPersistenceThreading){
											final LithiumPosting litTPost = new LithiumPosting(messageResponse);
											
											// execute persistence layer in a new thread, so that it does NOT block the crawler
											logger.trace("execute persistence layer in a new thread...");
											new Thread(new Runnable() {
												@Override
												public void run() {
													litTPost.save();
													
													// next call the graph engine and store data also in the external graph
													// please note that we do not need to do this for the user as well, as 
													// the graph persistence layer uses the embedded user object within the
													// post object
													if (rtc.getBooleanValue("ActivateGraphDatabase", "runtime")) {
														litTPost.saveInGraph();
													}
												}
											}).start();
										} else {
											// otherwise just call it sequentially
											litPost.save();
											
											// next call the graph engine and store data also in the external graph
											// please note that we do not need to do this for the user as well, as 
											// the graph persistence layer uses the embedded user object within the
											// post object
											if (rtc.getBooleanValue("ActivateGraphDatabase", "runtime")) {
												litPost.saveInGraph();
											}
										}
									} catch (ParseException e) {
										logger.error("EXCEPTION :: error " + e.getLocalizedMessage());
										e.printStackTrace();
									} // try catch
								} // if message response is != null
							}// for loop over message array
						}// is json ok
					}// is http ok
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
			
			timer.stop();
			
			long seconds = timer.elapsed(TimeUnit.SECONDS);
		    long calcSeconds = seconds;
		    long calcMinutes = 0;
		    long calcHours = 0;
		    long calcDays = 0;
		    if (calcSeconds > 60) {
		    	calcMinutes = calcSeconds / 60;
		    	calcSeconds = calcSeconds - (calcMinutes * 60);
		    }
		    if (calcMinutes > 60) {
		    	calcHours = calcMinutes / 60;
		    	calcMinutes = calcMinutes - (calcHours * 60);
		    }
		    if (calcHours > 24) {
		    	calcDays = calcHours / 24;
		    	calcHours = calcHours - (calcHours * 24);
		    }
			logger.info(CRAWLER_NAME+"-Crawler END - tracked {} messages in {} days {} hours {} minutes {} seconds\n", messageCount, calcDays, calcHours, calcMinutes, calcSeconds);
		}
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
		HttpStatusCodes httpStatusCodes = null;
		LithiumStatusCode jsonStatusCode = null;
		
		try {
			logger.debug("Lithium " + jsonObjectIdentifier + " tracked, retrieving from " + REST_API_URL + objectRef);
			HttpClient client = new HttpClient();
			
			PostMethod method = new PostMethod(REST_API_URL+objectRef);
			method.addParameter(LithiumConstants.HTTP_RESPONSE_FORMAT_COMMAND, LithiumConstants.HTTP_RESPONSE_FORMAT);
			httpStatusCodes = HttpStatusCodes.getHttpStatusCode(client.executeMethod(method));
			String jsonString = method.getResponseBodyAsString();
			
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode())+" could not connect to " + REST_API_URL);
				}
			} else {
				logger.debug("http connection established (status is " + httpStatusCodes + ")");
			}	
			
			logger.trace("our " + jsonObjectIdentifier + " json within SendObjectRequest: " + jsonString);
			
			// the JSON String we received from the http connection is now decoded and passed on to the 
			// specific parser for posts and user
			// 															courtesy by Maic Rittmeier
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(jsonString);
			JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
			if(jsonObj == null)
				throw new ParseException(0, "returned json object is null");
			JSONObject responseObj = (JSONObject)jsonObj.get(LithiumConstants.JSON_RESPONSE_OBJECT_TEXT);
			
			// first check if the server response is not only OK from an http point of view, but also
			//    from the perspective of the REST API call
			jsonStatusCode = LithiumStatusCode.getLithiumStatusCode(responseObj.get(LithiumConstants.JSON_STATUS_CODE_TEXT).toString());
			
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
				
				logger.error("the server returned error " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT));
				throw new LithiumStatusException("the server returned error " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_CODE_TEXT) + " - " + jsonErrorObj.get(LithiumConstants.JSON_ERROR_MESSAGE_TEXT));
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
				logger.debug("url={}, host={}, ip={}, port={}",
	                       getRequestingURL(), getRequestingHost(),
	                       getRequestingSite(), getRequestingPort() );
				
				return new PasswordAuthentication( "user", "pwd???".toCharArray() );
			}
		});
	}
	
	
	/**
	 * @description 	seeks to find the needle (only one at a time) in the haystack
	 * @param 			haystack
	 * @param 			needle
	 * @return 			true if the needle was found in the haystack, otherwise false
	 */
	private boolean findTheNeedle(String haystack, String needle){
		String patternString = ".*"+needle+".*";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(haystack);

		while (matcher.find())
		    return true;
		return false;
	}
	
	
	// these are the getter and setter for the name value - used for JMX support, I think
	public static String getName() {return name;}
	public static void setName(String name) {LithiumCrawler.name = name;}
}
