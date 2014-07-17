package de.comlineag.snc.job;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.FacebookConstants;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.FacebookParser;

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
 * @changelog	0.1 (Chris)			copied from TwitterCrawler Revision 0.9a
 *
 */
public class FacebookCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private final BlockingQueue<String> msgQueue;
	private final FacebookParser post;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";

	public FacebookCrawler() {
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(FacebookConstants.MESSAGE_BLOCKING_QUEUE_SIZE);
		
		// instantiate the Facebook-Posting-Manager
		post = new FacebookParser();
	}
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// generate the json to pass to the configuration persistence
		JSONObject configurationScope = new JSONObject();
		configurationScope.put(ConfigurationConstants.domainIdentifier, (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.domainIdentifier));
		configurationScope.put(ConfigurationConstants.customerIdentifier, (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.customerIdentifier));
		configurationScope.put("SN_ID", "\""+SocialNetworks.FACEBOOK+"\"");
				
		// set the customer we start the crawler for
		String curCustomer = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.customerIdentifier);		
		
		// log the startup message
		logger.info("Facebook-Crawler START for " + curCustomer);
		int messageCount = 0;
		
		
		// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
		logger.info("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_TERM_TEXT, configurationScope);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_LANGUAGE_TEXT, configurationScope);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(ConfigurationConstants.CONSTRAINT_USER_TEXT, configurationScope);
		//ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(ConfigurationConstants.CONSTRAINT_LOCATION_TEXT, configurationScope);
		
		// log output AND setup of the filter end point
		if (tTerms.size()>0) {
			smallLogMessage += "specific terms ";
		}
		if (tUsers.size()>0) {
			smallLogMessage += "specific users ";
		}
		if (tLangs.size()>0) {
			smallLogMessage += "specific languages ";
		}
		/*
		if (tLocas.size()>0) {
			smallLogMessage += "specific Locations ";
		}
		
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_ID_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_SECRET_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_ID_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_SECRET_KEY));
		
		// Create a new BasicClient. By default gzip is enabled.
		Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
				.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(1000).build();
		*/
		try {
			// Establish a connection
			try {
				logger.debug("crawler connecting to endpoint " );
				//client.connect();
			} catch (Exception e) {
				logger.error("EXCEPTION :: connecting to " +  " failed: " + e.getMessage(), e);
				//client.stop();
			}
			
			logger.info("new facebook crawler instantiated - restricted to track " + smallLogMessage);
			
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
				logger.info("New message tracked from " + msg.substring(15, 45) + "... / number " + messageCount + " in this job run");
				logger.trace("   content: " + msg );
				
				// pass each tracked message to the parser
				post.process(msg);
			}
		} catch (Exception e) {
			logger.error("Error while processing messages", e);
		}
		// kill the connection
		//client.stop();
		logger.info("Facebook-Crawler END - tracked "+messageCount+" messages\n");
	}
}
