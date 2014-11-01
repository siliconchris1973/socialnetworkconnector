package de.comlineag.snc.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.base.Stopwatch;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.TwitterConstants;
import de.comlineag.snc.parser.TwitterParser;

/**
 *
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.9f				- 21.10.2014
 * @status		productive	but with occasional error while inserting data
 *
 * @description this is the actual crawler of the twitter network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              twitter api to grab new tweets as they are created on the
 *              network.
 *
 * @limitation	Currently the class uses annotation @DisallowConcurrentExecution
 *              because we can't let the job run multiple times for the sake of 
 *              data consistency 
 *
 * @changelog	0.1 (Chris)			first skeleton against rest-api
 * 				0.2	(Maic)			static version retrieves posts
 * 				0.3	(Chris)			connection against streaming api
 * 				0.4	(Maic)			bug fixing
 * 				0.5 (Magnus)		keys are taken from ApplicationContext.xml
 * 				0.6 				bug fixing and optimization
 * 				0.7 (Chris)			configuration is made dynamic
 *				0.8					added support for SocialNetwork specific configuration
 *				0.9					added constants for configuration retrieval
 *				0.9a (Maic)			Fixed the "crawler stop and no clean restart" bug
 *				0.9b (Chris)		added parameter for customer specific configuration
 *				0.9c				changed method signature for crawler configuration to match JSON object
 *				0.9d				added support for runState configuration, to check if the crawler shall actually run
 *				0.9e				added time measure and new loop to track unlimited messages
 *				0.9f				deactivated loop to track unlimited messages
 *
 * TODO check if we can use getResponseBodyAsStrema to fix the following warning: Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
 * TODO implement possibility have black-list of combinations not to track: e.g. Depot YES / Home Depot NO 
 */
@DisallowConcurrentExecution 
public class TwitterCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static final String CRAWLER_NAME="TWITTER";
	private static String name="TwitterCrawler";
		
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	//private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	//private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");

	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	private final int rtcMaxTweetsPerRun = rtc.getIntValue("TwMaxTweetsPerCrawlerRun", "crawler");
	
	
	
	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private final BlockingQueue<String> msgQueue;
	@SuppressWarnings("unused")
	private final BlockingQueue<Event> eventQueue;
	private final TwitterParser post;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";

	public TwitterCrawler() {
		// Define message and event iQueue
		msgQueue = new LinkedBlockingQueue<String>(TwitterConstants.MESSAGE_BLOCKING_QUEUE_SIZE);
		eventQueue = new LinkedBlockingQueue<Event>(TwitterConstants.EVENT_BLOCKING_QUEUE_SIZE);
		
		// instantiate the Twitter-Posting-Manager
		post = new TwitterParser();
	}
	
	
	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Stopwatch timer = new Stopwatch().start();
		
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		
		int messageCount = 0;
		int connectionTimeOut = 1000;
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		// first check is to get the information, if the crawler was 
		// deactivated from within the crawler configuration, even if 
		// it is active in applicationContext.xml
		if ((Boolean) crawlerConfig.getRunState(CRAWLER_NAME)) {
			try {
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
				
				
				if(arg0.getJobDetail().getJobDataMap().containsKey(ConfigurationConstants.TWITTER_API_CLIENT_CONNECTIONTIMEOUT_KEY)){
					try {
						connectionTimeOut = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.TWITTER_API_CLIENT_CONNECTIONTIMEOUT_KEY));
					} catch (Exception e) {
						logger.error("Could not parse "+ConfigurationConstants.TWITTER_API_CLIENT_CONNECTIONTIMEOUT_KEY);
					}
				}
				
				// THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
				logger.debug("retrieving restrictions from configuration db");
				ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
				ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(constraintLangText, configurationScope);
				ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(constraintUserText, configurationScope);
				//ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(constraintLocaText, configurationScope);
				// blocked URLs
				//ArrayList<String> bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
				
				
				
				// log output AND setup of the filter end point
				if (tTerms.size()>0) {
					smallLogMessage += " specific terms ";
					endpoint.trackTerms(tTerms);
				}
				if (tUsers.size()>0) {
					smallLogMessage += " specific users ";
					endpoint.followings(tUsers);
				}
				if (tLangs.size()>0) {
					smallLogMessage += " specific languages ";
					endpoint.languages(tLangs);
				}
				if (tLocas.size()>0) {
					smallLogMessage += " specific Locations ";
					endpoint.locations(tLocas);
				}
	
				logger.info("New "+CRAWLER_NAME+" crawler instantiated - restricted to track " + smallLogMessage);
				
				
				
				Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_ID_KEY),
													(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_SECRET_KEY),
													(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_ID_KEY),
													(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_SECRET_KEY));
				
				
				// Create a new BasicClient. By default gzip is enabled.
				Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
						.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(connectionTimeOut).build();
				
				try {
					// Establish a connection
					try {
						logger.debug("crawler connecting to endpoint " + Constants.STREAM_HOST + client.getEndpoint().getURI());
						client.connect();
					} catch (Exception e) {
						logger.error("EXCEPTION :: connecting to " + Constants.STREAM_HOST + " failed: " + e.getMessage(), e);
						client.stop();
					}
					
					// check if there is a limit on the maximum number of tweets to track per crawler run
/*
					if (rtcMaxTweetsPerRun == -1) {
						// now track all relevant tweets as long as new tweets exist in the queue
						logger.debug("tracking unlimited messages");
						while (!msgQueue.isEmpty()){
							messageCount++;
							setPostsTracked(messageCount);
							
							String msg = "";
							try {
								msg = msgQueue.take();
							} catch (InterruptedException e) {
								logger.error("ERROR :: Message loop interrupted " + e.getMessage());
							} catch (Exception ee) {
								logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
							}
							logger.debug("SocialNetworkPost #"+messageCount+" tracked from " + CRAWLER_NAME);
							//logger.trace("   content: " + msg );
							
							// each tweet is now passed to the parser TwitterParser
							post.process(msg);
							
							// TODO implement SimpleWebCrawler to retrieve a web-page referenced in a tweet by a hyperlink
							
							
						}
					} else {
*/
						// now track all relevant tweets up to maximum number configured
						logger.debug("tracking max "+rtcMaxTweetsPerRun+" messages");
						
						for (int msgRead = 0; msgRead < rtcMaxTweetsPerRun; msgRead++) {
							logger.trace("message " + messageCount + " received");
							messageCount++;
							setPostsTracked(messageCount);
							
							String msg = null;
							try {
								msg = msgQueue.take();
							} catch (InterruptedException e) {
								logger.error("ERROR :: Message loop interrupted " + e.getMessage());
							} catch (Exception ee) {
								logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
							}
							logger.debug("SocialNetworkPost #"+messageCount+" tracked from " + CRAWLER_NAME);
							//logger.trace("   content: " + msg );
	
							// each tweet is now passed to the parser TwitterParser
							post.process(msg);
						}
//					}
				} catch (Exception e) {
					logger.error("Error while processing messages", e);
				}
				client.stop();
				timer.stop();
				logger.info(CRAWLER_NAME+"-Crawler END - tracked "+messageCount+" messages in "+timer.elapsed(TimeUnit.SECONDS)+" seconds\n");
			} catch (Exception e) {
				logger.error("something went wrong ", e);
				e.printStackTrace();
			}
		} 
	}
	
	
	/*
	 * takes messages from the msgQueue  
	 * @param connectionTimeOut
	 * @return
	 
	private String ReadMessage(int connectionTimeOut){
		String msg = null;
		try {
			msg = msgQueue.poll(connectionTimeOut, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("ERROR :: Message polling interrupted " + e.getMessage());
		} catch (Exception ee) {
			logger.error("EXCEPTION :: Exception while polling message " + ee.getMessage());
		}
		return msg;
	}
	*/
	
	
	/**
	 * 
	 * @param msg
	 * @return url
	 */
	private URL getUrlFromTweet(String msg){
		//logger.debug("getLinksFromPage called for " + url.toString());
		String lcPage = msg.toLowerCase(); // tweet in lower case

		Pattern p = Pattern.compile("href=\"(.*?)\"");
		Matcher m = p.matcher(lcPage);
		while(m.find()){
			String link = m.group(1);
			if(link == null) continue;
			URL newURL = null;
			try {
				newURL = new URL(link);
			} catch (MalformedURLException e) {
				//logger.error(String.format("Link %s could not be parsed as a URL.", link), e);
				continue;
			}
			return newURL;
		}
		return null;
	}
	
	/**
	 * 
	 * @param msg
	 * @param bURLs
	 * @param tTerms
	 * @param curCustomer
	 * @param curDomain
	 */
	private void followHyperLinkFromTweet(String msg, ArrayList <String> bURLs, ArrayList<String> tTerms, String curCustomer, String curDomain){
		//logger.debug("getLinksFromPage called for " + url.toString());
		String lcPage = msg.toLowerCase(); // tweet in lower case

		Pattern p = Pattern.compile("href=\"(.*?)\"");
		Matcher m = p.matcher(lcPage);
		while(m.find()){
			String link = m.group(1);
			if(link == null) continue;
			URL newURL = null;
			try {
				newURL = new URL(link);
				SimpleWebCrawler pCrawl = new SimpleWebCrawler(newURL, bURLs, 1, 1, true, true, true, null, null, tTerms, curCustomer, curDomain, "TW");
				
			} catch (MalformedURLException e) {
				//logger.error(String.format("Link %s could not be parsed as a URL.", link), e);
				continue;
			}
		}
	}
	
	
	// these are the getter and setter for the name value - used for JMX support, I think
	public static String getName() {return name;}
	public static void setName(String name) {TwitterCrawler.name = name;}
}
