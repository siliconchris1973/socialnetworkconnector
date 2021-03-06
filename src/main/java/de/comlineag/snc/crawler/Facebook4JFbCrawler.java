package de.comlineag.snc.crawler;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.HttpStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.parser.FacebookParser;
import facebook4j.Event;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Group;
import facebook4j.Location;
import facebook4j.Page;
import facebook4j.Place;
import facebook4j.Post;
import facebook4j.ResponseList;
import facebook4j.User;
import facebook4j.conf.ConfigurationBuilder;

/**
 *
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.1		
 * @status		in development
 *
 * @description this is the actual crawler of the Facebook network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              Facebook api to grab new messages as they are created on the
 *              network.
 *              
 * @limitation	Currently the class uses annotation @DisallowConcurrentExecution
 *              because we can't let the job run multiple times for the sake of 
 *              data consistency - this will be resolved in version 1.1 
 *
 * @changelog	0.1 (Chris)			copied from TwitterCrawler Revision 0.9a
 *
 */
@DisallowConcurrentExecution 
public class Facebook4JFbCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private static String CRAWLER_NAME="FACEBOOK";
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// the post object for the facebook parser - probably not needed
	private final FacebookParser post;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";

	// data types provided by facebook4j API for different search options within facebook
	private ResponseList<Post> posts = null;
	private ResponseList<User> users = null;
	private ResponseList<Event> events = null;
	private ResponseList<Group> groups = null;
	private ResponseList<Place> places = null;
	private ResponseList<Location> locas = null;
	private ResponseList<Page> pages = null;
	
	private ArrayList<String> feed = null;
	
	// sourced in from applicationContext.xml during job execution
	private String protocol = null;		// the access protocol 
	private String host = null;			// the actual host
	private String port = null; 		// the port to use
	//private String location = null;		// a path after host and port added to ALL queries
	private String searchURL = null;	// created from above 4 values
	private String queryString = "q=QUERY&type=OBJECT_TYPE"; // what query shall we execute against the graph api
	private String searchUri = null;	// created by concatenating searchURL with access_token and query string
	
	
	// how many messages where tracked during job run
	private int messageCount = 0;
	
	
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	//private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");
	
	
	// convenience variables to make the code easier to read and reduce number of calls to RuntimeConfiguration
	private final String domainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String customerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	
	
	public Facebook4JFbCrawler() {
		// instantiate the Facebook-Posting-Manager - probably wrong
		post = new FacebookParser();
	}
	
	
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
			String curDomain = (String) configurationScope.get(domainKey);
			String curCustomer = (String) configurationScope.get(customerKey);
			
			// this is the status code for the http connection
			HttpStatusCodes httpStatusCodes = null;
			
			// build the search URI
			protocol = (String) arg0.getJobDetail().getJobDataMap().get("protocol");
			host = (String) arg0.getJobDetail().getJobDataMap().get("server_url");
			port = (String) arg0.getJobDetail().getJobDataMap().get("port");
			//location = (String) arg0.getJobDetail().getJobDataMap().get("location");
			searchURL = protocol + "://" + host + ":" + port;
			
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
			
			// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS, GEO-LOCATIONS and PAGES (aka sites)
			logger.info("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(constraintLangText, configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(constraintUserText, configurationScope);
			ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(constraintLocaText, configurationScope);
			// blocked URLs
			//ArrayList<String> bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
			
			
			if (tTerms.size()>0) {
				smallLogMessage += "specific terms ";
			}
			if (tUsers.size()>0) {
				smallLogMessage += "specific users ";
			}
			if (tLangs.size()>0) {
				smallLogMessage += "specific languages ";
			}
			if (tLocas.size()>0) {
				smallLogMessage += "specific Locations ";
			}
			if (tSites.size()>0) {
				smallLogMessage += "specific Sites ";
				
			}
			
			// get OAuth authorization details and acquire facebook instance
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
			  .setOAuthAppId((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_APP_ID_KEY))
			  .setOAuthAppSecret((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_APP_SECRET_KEY))
			  .setOAuthAccessToken((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_ACCESS_TOKEN_KEY))
			  .setOAuthPermissions((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_PERMISSIONSET_KEY));
			FacebookFactory ff = new FacebookFactory(cb.build());
			Facebook facebook = ff.getInstance();
			
			logger.info("new facebook crawler instantiated - restricted to track " + smallLogMessage);
			
			try {
				// you can also execute a different search type by means of a rest url:
				searchUri = searchURL + "?access_token="+(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_ACCESS_TOKEN_KEY)+queryString;
				
				HttpClient client = new HttpClient();
				PostMethod method = new PostMethod(searchUri);
				httpStatusCodes = HttpStatusCodes.getHttpStatusCode(client.executeMethod(method));
				
				logger.trace("httpStatusCode = " + httpStatusCodes);
				
				// get the message feed from a page - works
				//ResponseList<Post> posts = facebook.getFeed("cortal-consors.com");
				
				// does not work either
				/*
				posts = facebook.searchPosts(Joiner.on(",").join(tTerms));
				logger.trace("number of posts: " + posts.getCount() + " size " + posts.size());
				users = facebook.searchUsers(Joiner.on(",").join(tUsers));
				logger.trace("number of users: " + users.getCount() + " size " + users.size());
				locas = facebook.searchLocations(Joiner.on(",").join(tLocas));
				logger.trace("number of locations: " + locas.getCount() + " size " + locas.size());
				pages = facebook.searchPages(Joiner.on(",").join(tSites));
				logger.trace("number of sites: " + pages.getCount() + " size " + pages.size());
				*/ 
				
				// next try with own special method
				//String searchPost = "Hipp";
				//String results = getFacebookPosts(facebook, searchPost);
				//logger.trace("result :: " + results);
				// returns: 404 - (#803) Some of the aliases you requested do not exist: Hipp
				
				ResponseList<Post> feed = facebook.getFeed("Hipp");
				
				// Do whatever needs to be done with messages
				for (int msgRead = 0; msgRead < feed.size(); msgRead++) {
					messageCount++;
					setPostsTracked(messageCount);
					
					Post msg = posts.get(msgRead);
					
					logger.info("New message tracked from " + msg.getName() + "... / number " + messageCount + " in this job run");
					logger.trace("   content: " + msg );
					
					// pass each tracked message to the parser
					//post.process(msg.toString());
				}
			} catch (FacebookException e1) {
				// in case we try to request a non existing object, like page group or the like, just issue a warning.
				if (e1.getStatusCode() == 404){
					logger.warn(e1.getStatusCode() + " - " + e1.getErrorMessage());
				} else if ("OAuthException".equals(e1.getErrorType())) {
					logger.error("OAuth Error "+e1.getErrorCode()+" :: " + e1.getErrorMessage() + ""
							+ " The status code is " + e1.getStatusCode());
					e1.printStackTrace();
				} else if ("GraphMethodException".equals(e1.getErrorType())) {
					logger.error("Graph Error "+e1.getErrorCode()+" :: " + e1.getErrorMessage() + ""
							+ " The status code is " + e1.getStatusCode());
					e1.printStackTrace();
				} else {
					logger.error("some weird facebook error number "+e1.getErrorCode()+" with status code "+e1.getStatusCode()+" occured. Says it belongs to this type of error: "+e1.getErrorType()+". Also states: "+e1.getErrorMessage()
						+ "\nmaybe it's clear to you humans what happened here, but me, being a stupid machine, me have no idea. So here's the bloody rest: " + e1.getMessage());
					e1.printStackTrace();
				}
			} catch (Exception e) {
				logger.error("Error while processing messages", e);
			}
			// kill the connection
			//client.stop();
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
	
	// static This method is used to get Facebook posts based on the search string set above
	@SuppressWarnings("unused")
	private String getFacebookPosts(Facebook Facebook, String searchPost)
			throws FacebookException {
		String searchResult = "Item : " + searchPost + "\n";
		StringBuffer searchMessage = new StringBuffer();
		ResponseList<Post> results = Facebook.getPosts(searchPost);
		for (Post post : results) {
			logger.trace("---> post: " + post.getMessage());
			searchMessage.append(post.getMessage() + "\n");
			for (int j = 0; j < post.getComments().size(); j++) {
				searchMessage.append(post.getComments().get(j).getFrom()
						.getName()
						+ ", ");
				searchMessage.append(post.getComments().get(j).getMessage()
						+ ", ");
				searchMessage.append(post.getComments().get(j).getCreatedTime()
						+ ", ");
				searchMessage.append(post.getComments().get(j).getLikeCount()
						+ "\n");
			}
		}
		String feedString = getFacebookFeed(Facebook, searchPost);
		searchResult = searchResult + searchMessage.toString();
		searchResult = searchResult + feedString;
		return searchResult;
	}

	// static This method is used to get Facebook feeds based on the search string set
	// above
	private String getFacebookFeed(Facebook Facebook, String searchPost)
			throws FacebookException {
		String searchResult = "";
		StringBuffer searchMessage = new StringBuffer();
		ResponseList<Post> results = Facebook.getFeed(searchPost);
		for (Post post : results) {
			logger.trace("---> feed: " + post.getMessage());
			searchMessage.append(post.getFrom().getName() + ", ");
			searchMessage.append(post.getMessage() + ", ");
			searchMessage.append(post.getCreatedTime() + "\n");
		}
		searchResult = searchResult + searchMessage.toString();
		return searchResult;
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
}
