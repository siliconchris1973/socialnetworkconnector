package de.comlineag.sbm.job;

import java.util.ArrayList;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.log4j.Logger;

import de.comlineag.sbm.handler.LithiumParser;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              lithium api to grab new posts as they are created on the
 *              network.
 * 
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	private LithiumParser post;
	
	//TODO these need to come from applicationContext.xml configuration file
	private boolean restrictToTrackterms = true;
	private boolean restrictToLanguages = true;
	private boolean restrictToUsers = true;
	private boolean restrictToLocations = false; // locations does not yet work
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String bigLogMessage = "";
	private String smallLogMessage = "";
	
	// some static vars for the lithium crawler
	String _user = "cmral";
	String _passwd = "123Test123";
	
	String REST_API_LOC = "http://community.lithium.com/community-name/restapi/vc";
	String DEVELOPER_URL = "http://community.lithium.com/t5/Developer-Network/ct-p/Developer";
	
	public LithiumCrawler() {
				
		// instantiate the Lithium-Posting-Manager
		post = new LithiumParser();
		
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
		logger.debug("Lithium-Crawler START");
		
		// possible restrictions
		if (restrictToTrackterms) {
			//TODO check how to track specific terms
			//TODO the trackterms need to go in a configuration file or a database
			
			String[] ttTerms = {"SAP", "ERP", "SAP BW", "BO", "CRM", "SCM", "SRM", "IDM", 
								"NetWeaver", "ABAP", "HANA", "Business Objects", 
								"Business Warehouse", "Customer Relationship Management", 
								"Supply Chain Management", "Supplier Relationship Management", 
								"Identity Management", "Social Brand Monitor",
								"Social Activity Analyzer"};
			
			//String[] ttTerms = {"SocialActivityAnalyzer", "SocialNetworkAnalyzer", "SocialBrandMonitor", "SocialNetworkConnector"};
			ArrayList<String> tTerms = new ArrayList<String>();
			for (int i = 0 ; i < ttTerms.length ; i++) {
				tTerms.add(ttTerms[i]);
			}
			
			
			bigLogMessage += "\n                       restricted to terms: " + tTerms.toString() + "\n";
			smallLogMessage += "specific terms ";
		}
		
		// Restrict tracked messages to english and german
		if (restrictToLanguages) {
			ArrayList<String> langs = new ArrayList<String>();
			langs.add("de");
			langs.add("en");
			
			//endpoint.languages(langs);
			bigLogMessage += "                       restricted to languages: " + langs.toString() + "\n";
			smallLogMessage += "specific languages ";
		}
		
		// Restrict the tracked messages to specific users
		if (restrictToUsers) {
			ArrayList<Long> users = new ArrayList<Long>();
			users.add(754994L);			// Christian Guenther
			users.add(2412281046L);		// Magnus Leinemann
			
			//endpoint.followings(users);
			bigLogMessage += "                       restricted on user: " + users.toString() + "\n";
			smallLogMessage += "specific users ";
		}
		
		// Restrict the tracked messages to specific locations
		if (restrictToLocations) {
			
			// TODO check how to work with locations in hbc lithium api 
			ArrayList<String> locs = new ArrayList<String>(); 
			locs.add("Germany");
			locs.add("USA");
			
			//endpoint.locations(locs);
			
			bigLogMessage += "                       restricted on locations: " + locs.toString() + " (NOT IMPLEMENTED)";
			smallLogMessage += "specific locations ";
		}
		
		logger.debug("new lithium parser instantiated - restricted to track " + smallLogMessage);
		logger.trace("call for Endpoint POST: " //+ endpoint.getPostParamString() 
					+ bigLogMessage);
		
		//TODO implement authentication against lithium network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), (String) arg0.getJobDetail()
				.getJobDataMap().get("consumerSecret"), (String) arg0.getJobDetail().getJobDataMap().get("token"), (String) arg0
				.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
		
		// TODO implement lithium connection handler
		// Create a new BasicClient. By default gzip is enabled.
		/*
		Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
				.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(1000).build();

		// Establish a connection
		try {
			client.connect();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		*/
		
		// Do whatever needs to be done with messages
		for (int msgRead = 0; msgRead < 1000; msgRead++) {
			String msg = "";
			try {
				// TODO check hwo to take the messages from client
				//msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.error("ERROR :: Message loop interrupted " + e.getMessage());
			} catch (Exception ee) {
				logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
			}
			logger.info("New Post tracked from " + msg.substring(15, 45) + "...");
			logger.trace("complete post: " + msg );

			// Jede einzelne Message wird nun an den Parser LithiumParser
			// (abgeleitet von GenericParser) uebergeben
			post.process(msg);
		}
		
		logger.debug("Lithium-Crawler END");
		// TODO check if we need to stop the client
		//client.stop();
	}
}
