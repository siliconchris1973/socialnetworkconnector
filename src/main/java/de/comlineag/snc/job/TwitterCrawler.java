package de.comlineag.snc.job;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
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
import de.comlineag.snc.handler.TwitterParser;

/**
 *
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.9
 * @status		beta
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
 *
 * TODO 1. check why crawler is stopping and not restarting correctly when executed together with LithiumCrawler
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
		// log the startup message
		logger.info("Twitter-Crawler START");

		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

		// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
		logger.trace("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_TERM_TEXT, SocialNetworks.TWITTER);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_LANGUAGE_TEXT, SocialNetworks.TWITTER);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(ConfigurationConstants.CONSTRAINT_USER_TEXT, SocialNetworks.TWITTER);
		ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(ConfigurationConstants.CONSTRAINT_LOCATION_TEXT, SocialNetworks.TWITTER);

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

		logger.info("new twitter crawler instantiated - restricted to track " + smallLogMessage);

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
		} catch (Exception e) {
			logger.error("Error while processing messages", e);
		}
		client.stop();
		logger.info("Twitter-Crawler END");
	}
}
