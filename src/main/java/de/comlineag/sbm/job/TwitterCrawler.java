package de.comlineag.sbm.job;

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

import de.comlineag.sbm.handler.TwitterParser;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the twitter network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              twitter api to grab new tweets as they are created on the
 *              network.
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
	private String bigLogMessage = "";
	private String smallLogMessage = "";
	
	public TwitterCrawler() {
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
		eventQueue = new LinkedBlockingQueue<Event>(1000);
				
		// instantiate the Twitter-Posting-Manager
		post = new TwitterParser();
		
		// TODO check what about multithreading and executor services
		/*
		// Set up the executor service to distribute the actual tasks
		final int numProcessingThreads = 4;
		// Create an executor service which will spawn threads to do the actual work
		// of parsing the incoming messages and calling the listeners on each message
		ExecutorService service = Executors.newFixedThreadPool(numProcessingThreads);
		 */
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Twitter-Crawler START");
		
		
		// setup restrictions on what to track
		// TODO check why these won't be fetched from applicationContext.xml
		final boolean restrictToTrackterms = true;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToTrackterms");
		final boolean restrictToLanguages = true;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToLanguages");
		final boolean restrictToUsers = false;		//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToUsers");
		final boolean restrictToLocations = false;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToLocations"); // locations does not yet work
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		// possible restrictions
		if (restrictToTrackterms) {
			//TODO the trackterms need to go in a configuration file or a database
			/*
			String[] ttTerms = {"SAP", "ERP", "SAP BW", "BO", "CRM", "SCM", "SRM", "IDM", 
								"NetWeaver", "ABAP", "HANA", "Business Objects", 
								"Business Warehouse", "Customer Relationship Management", 
								"Supply Chain Management", "Supplier Relationship Management", 
								"Identity Management", "Social Brand Monitor",
								"Social Activity Analyzer"};
			*/
			String[] ttTerms = {"Tagesgeld", "Trading", "Depot", "Girokonto", "Wertpapier", "Kreditkarte", "HBCI"};
			
			//String[] ttTerms = {"SocialActivityAnalyzer", "SocialNetworkAnalyzer", "SocialBrandMonitor", "SocialNetworkConnector"};
			ArrayList<String> tTerms = new ArrayList<String>();
			for (int i = 0 ; i < ttTerms.length ; i++) {
				tTerms.add(ttTerms[i]);
			}
			endpoint.trackTerms(tTerms);
			
			bigLogMessage += "\n                       restricted to terms: " + tTerms.toString() + "\n";
			smallLogMessage += "specific terms ";
		}
		
		// Restrict tracked messages to english and german
		if (restrictToLanguages) {
			ArrayList<String> langs = new ArrayList<String>();
			langs.add("de");
			langs.add("en");
			
			endpoint.languages(langs);
			bigLogMessage += "                       restricted to languages: " + langs.toString() + "\n";
			smallLogMessage += "specific languages ";
		}
		
		// Restrict the tracked messages to specific users
		if (restrictToUsers) {
			ArrayList<Long> users = new ArrayList<Long>();
			users.add(754994L);			// Christian Guenther
			users.add(2412281046L);		// Magnus Leinemann
			
			endpoint.followings(users);
			bigLogMessage += "                       restricted on user: " + users.toString() + "\n";
			smallLogMessage += "specific users ";
		}
		
		// Restrict the tracked messages to specific locations
		if (restrictToLocations) {
			
			// TODO check how to work with locations in hbc twitter api 
			ArrayList<String> locs = new ArrayList<String>(); 
			locs.add("Germany");
			locs.add("USA");
			
			//endpoint.locations(locs);
			
			bigLogMessage += "                       restricted on locations: " + locs.toString() + " (NOT IMPLEMENTED)";
			smallLogMessage += "specific locations ";
		}
		
		logger.debug("new twitter parser instantiated - restricted to track " + smallLogMessage);
		logger.trace("call for Endpoint POST: " + endpoint.getPostParamString() 
					+ bigLogMessage);

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
		
		logger.debug("Twitter-Crawler END");
		client.stop();
	}
}
