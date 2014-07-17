package de.comlineag.snc.job;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
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

import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.TwitterConstants;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.DomainDrivenConfiguration;
import de.comlineag.snc.handler.TwitterParser;

/**
 *
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.9c		- 17.07.2014
 * @status		productive	- occasional error while inserting data
 *
 * @description this is the actual crawler of the twitter network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              twitter api to grab new tweets as they are created on the
 *              network.
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
 *
 * TODO 1. fix crawler bug, that causes the persistence to try to insert a post or user multiple times
 * 			This bug has something to do with the number of threads provided by the Quartz job control
 * 			In case 1 thread is provided, everything is fine, but if 2 threads are provided (as needed if two crawler
 * 			run - Twitter and Lithium) the Twitter crawler tries to insert some posts more then once - an attempt 
 * 			obviously doomed. We log the following error: 
 * 				could not create post (TW-488780024691322880): Expected status 201, found 400
 * 			and the hana db returns this message:
 * 				Service exception: unique constraint violated.
 * 
 * TODO 2. find out and fix the following warning:
 * 			HttpMethodBase - Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
 */
public class TwitterCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

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
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(TwitterConstants.MESSAGE_BLOCKING_QUEUE_SIZE);
		eventQueue = new LinkedBlockingQueue<Event>(TwitterConstants.EVENT_BLOCKING_QUEUE_SIZE);
		
		// instantiate the Twitter-Posting-Manager
		post = new TwitterParser();
	}
	
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		JSONObject configurationScope = DomainDrivenConfiguration.getDomainSetup();
		configurationScope.put((String) "SN_ID", (String) "\""+SocialNetworks.TWITTER+"\"");
		
		// log the startup message
		// set the customer we start the crawler for
		String curCustomer = (String) configurationScope.get(ConfigurationConstants.customerIdentifier);
				
		logger.info("Twitter-Crawler START for " + curCustomer);
		int messageCount = 0;
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		// THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
		logger.info("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_TERM_TEXT, configurationScope);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_LANGUAGE_TEXT, configurationScope);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(ConfigurationConstants.CONSTRAINT_USER_TEXT, configurationScope);
		ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(ConfigurationConstants.CONSTRAINT_LOCATION_TEXT, configurationScope);
		
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
			
			logger.info("new twitter crawler instantiated - restricted to track " + smallLogMessage);
			
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
				logger.info("New Tweet tracked from " + msg.substring(15, 45) + "... / number " + messageCount + " in this job run");
				logger.trace("   content: " + msg );
				
				// Jede einzelne Message wird nun an den Parser TwitterParser uebergeben
				post.process(msg);
			}
		} catch (Exception e) {
			logger.error("Error while processing messages", e);
		}
		client.stop();
		logger.info("Twitter-Crawler END - tracked "+messageCount+" messages\n");
	}
}
