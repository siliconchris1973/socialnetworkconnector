package de.comlineag.snc.crawler;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
 * @version		0.9d				- 16.09.2014
 * @status		productive	but with occasional error while inserting data
 *
 * @description this is the actual crawler of the twitter network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              twitter api to grab new tweets as they are created on the
 *              network.
 *
 * @limitation	Currently the class uses annotation @DisallowConcurrentExecution
 *              because we can't let the job run multiple times for the sake of 
 *              data consistency - this will be resolved in version 1.1 
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
 *
 * TODO 1. fix crawler bug, that causes the persistence to try to insert a post or user multiple times
 * 			This bug has something to do with the number of threads provided by the Quartz job control
 * 			In case 1 thread is provided, everything is fine, but if 2 or possibly more threads are provided 
 * 			(as needed when multiple crawler are started) the Twitter crawler tries to insert some posts more 
 * 			then once - an attempt obviously doomed to fail. We log the following error: 
 * 				could not create post (TW-488780024691322880): Expected status 201, found 400
 * 			and the hana db returns this message:
 * 				Service exception: unique constraint violated.
 * 
 * TODO 2. find out and fix the following warning:
 * 			HttpMethodBase - Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
 */
@DisallowConcurrentExecution 
public class TwitterCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime cinfiguration
	private RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="TWITTER";
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
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
			String curDomain = (String) configurationScope.get(rtc.getDomainidentifier());
			String curCustomer = (String) configurationScope.get(rtc.getCustomeridentifier());
	
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
			int messageCount = 0;
			
			StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
			
			// THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
			logger.info("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintTermText(), configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintLanguageText(), configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(rtc.getConstraintUserText(), configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(rtc.getConstraintLocationText(), configurationScope);
			
			/*
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintTermText(), configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintLanguageText(), configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(rtc.getConstraintUserText(), configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(rtc.getConstraintLocationText(), configurationScope);
			*/
			
			// log output AND setup of the filter end point
			if (tTerms.size()>0) {
				smallLogMessage += "specific terms ";
				endpoint.trackTerms(tTerms);
			}
			if (tUsers.size()>0) {
				smallLogMessage += "specific users ";
				endpoint.followings(tUsers);
			}
			if (tLangs.size()>0) {
				smallLogMessage += "specific languages ";
				endpoint.languages(tLangs);
			}
			if (tLocas.size()>0) {
				smallLogMessage += "specific Locations ";
				endpoint.locations(tLocas);
			}
			
			Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_ID_KEY),
												(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_SECRET_KEY),
												(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_ID_KEY),
												(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_SECRET_KEY));
			
			// Create a new BasicClient. By default gzip is enabled.
			Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
					.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(1000).build();
			try {
				// Establish a connection
				try {
					logger.debug("crawler connecting to endpoint " + Constants.STREAM_HOST + client.getEndpoint().getURI());
					client.connect();
				} catch (Exception e) {
					logger.error("EXCEPTION :: connecting to " + Constants.STREAM_HOST + " failed: " + e.getMessage(), e);
					client.stop();
				}
				
				logger.info("New "+CRAWLER_NAME+" crawler instantiated - restricted to track " + smallLogMessage);
				
				// Do whatever needs to be done with messages
				for (int msgRead = 0; msgRead < 1000; msgRead++) {
					messageCount++;
					String msg = "";
					try {
						msg = msgQueue.take();
					} catch (InterruptedException e) {
						logger.error("ERROR :: Message loop interrupted " + e.getMessage());
					} catch (Exception ee) {
						logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
					}
					logger.info("SocialNetworkPost #"+messageCount+" tracked from " + CRAWLER_NAME);
					logger.trace("   content: " + msg );
					
					// Jede einzelne Message wird nun an den Parser TwitterParser uebergeben
					post.process(msg);
				}
			} catch (Exception e) {
				logger.error("Error while processing messages", e);
			}
			client.stop();
			logger.info(CRAWLER_NAME+"-Crawler END - tracked "+messageCount+" messages\n");
		}
	}
}
