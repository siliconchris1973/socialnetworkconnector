package de.comlineag.snc.job;

import java.util.ArrayList;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;

/**
 *
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.1		- 01.09.2014
 * @status		in development
 *
 * @description this is a generic web crawler. It get's a URL (from applicationContext.xml)
 *              and starts crawling that website following links 
 *
 * @limitation	- 
 *
 * @changelog	0.1 (Chris)			copy from TwitterCrawler Revision 0.9c
 * 
 */
@DisallowConcurrentExecution 
public class GenericWebCrawler extends GenericCrawler implements Job {
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WEBCRAWLER";
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";

	public GenericWebCrawler() {
		
		// instantiate the WebCrawler-Posting-Manager
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> webcrawlerConfig = new CrawlerConfiguration();
		//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
		JSONObject configurationScope = webcrawlerConfig.getCrawlerConfigurationScope();
		configurationScope.put((String) "SN_ID", (String) SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
		
		// set the customer we start the crawler for and log the startup message
		String curDomain = (String) configurationScope.get(RuntimeConfiguration.getDomainidentifier());
		String curCustomer = (String) configurationScope.get(RuntimeConfiguration.getCustomeridentifier());
		
		if ("undefined".equals(curDomain) && "undefined".equals(curCustomer)) {
			logger.info("Web-Crawler START");
		} else {
			if (!"undefined".equals(curDomain) && !"undefined".equals(curCustomer)) {
				logger.info("Web-Crawler START for " + curCustomer + " in " + curDomain);
			} else {
				if (!"undefined".equals(curDomain))
					logger.info("Web-Crawler START for " + curDomain);
				else
					logger.info("Web-Crawler START for " + curCustomer);
			}
		}
		int messageCount = 0;
		
		// THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
		logger.info("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintTermText(), configurationScope);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintLanguageText(), configurationScope);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(RuntimeConfiguration.getConstraintUserText(), configurationScope);
		//ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(RuntimeConfiguration.getConstraintLocationText(), configurationScope);
		
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
		//if (tLocas.size()>0) {
		//	smallLogMessage += "specific Locations ";
		//}
		
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_ID_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_CLIENT_SECRET_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_ID_KEY),
											(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_TOKEN_SECRET_KEY));
		*/
		
		// Create a new client to connect to web url
		
		try {
			// Establish a connection
			try {
				logger.debug("crawler connecting to endpoint " );
			} catch (Exception e) {
				logger.error("EXCEPTION :: connecting to host failed: " + e.getMessage(), e);
			}
			
			logger.info("new web crawler instantiated - restricted to track " + smallLogMessage);
			// now get all the content... 
			
		} catch (Exception e) {
			logger.error("Error while processing messages", e);
		}
		logger.info("Web-Crawler END - tracked "+messageCount+" messages\n");
	}
}
