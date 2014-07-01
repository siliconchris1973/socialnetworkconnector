package de.comlineag.snc.job;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.log4j.Logger;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import de.comlineag.snc.data.SocialNetworks;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.TwitterParser;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		1.3
 * 
 * @description this is the actual crawler of the twitter network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              twitter api to grab new tweets as they are created on the
 *              network.
 * 
 * @changelog	0.1 - 0.4 first static version retrieves posts				Chris and Maic
 * 				0.5 keys are taken from ApplicationContext.xml				Magnus
 * 				0.6 - 0.9 bugfi2ing and optimization
 * 				1.0	first productive version
 * 				1.1	configuration is made dynamic 							Chris
 *				1.2	added support for SocialNetwork specific configuration	
 *
 */
public class TwitterCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private BlockingQueue<String> msgQueue;
	@SuppressWarnings("unused")
	private BlockingQueue<Event> eventQueue;
	private TwitterParser post;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	public TwitterCrawler() {
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
		eventQueue = new LinkedBlockingQueue<Event>(1000);
				
		// instantiate the Twitter-Posting-Manager
		post = new TwitterParser();
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.info("Twitter-Crawler START");
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, LOCATIONS AND USERS
		logger.info("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint("term", SocialNetworks.TWITTER);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint("language", SocialNetworks.TWITTER);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint("user", SocialNetworks.TWITTER);
		// TODO implement data types for location
		//ArrayList<TwitterLocationData> tLocas = config.getConstraint("location", SocialNetworks.TWITTER);
				
		// log output AND setup of the filter endpoint
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
		/*
		if (tLocas.size()>0) {
			smallLogMessage += "specific Locations ";
			endpoint.locations(tLocas);
		}
		*/
		logger.info("new twitter crawler instantiated - restricted to track " + smallLogMessage);
		
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), 
											(String) arg0.getJobDetail().getJobDataMap().get("consumerSecret"), 
											(String) arg0.getJobDetail().getJobDataMap().get("token"), 
											(String) arg0.getJobDetail().getJobDataMap().get("tokenSecret"));

		// Create a new BasicClient. By default gzip is enabled.
		Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
				.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(1000).build();
		
		// Establish a connection
		try {
			client.connect();
		} catch (Exception e) {
			logger.error("EXCEPTION :: connecting to " + Constants.STREAM_HOST + " failed: " + e.getMessage(), e);
		}
		
		logger.debug("crawler connection endpoint is " + Constants.STREAM_HOST + client.getEndpoint().getURI());
		
		// Do whatever needs to be done with messages
		for (int msgRead = 0; msgRead < 1000; msgRead++) {
			String msg = "";
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.error("ERROR :: Message loop interrupted " + e.getMessage());
			} catch (Exception ee) {
				logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
			}
			logger.info("New Tweet tracked from " + msg.substring(15, 45) + "...");
			logger.trace("complete tweet: " + msg );

			// Jede einzelne Message wird nun an den Parser TwitterParser
			// (abgeleitet von GenericParser) uebergeben
			post.process(msg);
		}
		
		logger.info("Twitter-Crawler END");
		client.stop();
	}
}
