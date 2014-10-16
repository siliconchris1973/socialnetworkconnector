package de.comlineag.snc.crawler;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Location;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.FacebookConstants;
import de.comlineag.snc.parser.FacebookParser;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;

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
public class FacebookSpringFbCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private static String CRAWLER_NAME="FACEBOOK";
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// the post object for the facebook parser - probably not needed
	private final FacebookParser post;
	
	private Facebook facebook;
	
	// a single post as a String object
	private String msg = null;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	@Inject
	public FacebookSpringFbCrawler() {
		// instantiate the Facebook-Posting-Manager
		post = new FacebookParser();
		this.facebook = facebook;
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
			
			
			// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
			logger.info("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintTermText(), configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(rtc.getConstraintLanguageText(), configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(rtc.getConstraintUserText(), configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(rtc.getConstraintLocationText(), configurationScope);
			
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
			if (tLocas.size()>0) {
				smallLogMessage += "specific Locations ";
			}
			
			// get OAuth authorization details and acquire facebook instance
			  // (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_APP_ID_KEY))
			  // (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_APP_SECRET_KEY))
			  // (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_ACCESS_TOKEN_KEY))
			  // (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_PERMISSIONSET_KEY));
			
			try {
				logger.info("new facebook crawler instantiated - restricted to track " + smallLogMessage);
				
				// execute a different search type
				String searchUri = "https://graph.facebook.com/search?access_token="+(String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_ACCESS_TOKEN_KEY)+"q=QUERY&type=OBJECT_TYPE";
				
				// Do whatever needs to be done with messages
				for (int msgRead = 0; msgRead < 1000; msgRead++) {
					messageCount++;
					
					logger.info("New message tracked from " + msg.toString().substring(15, 45) + "... / number " + messageCount + " in this job run");
					logger.trace("   content: " + msg );
					
					// pass each tracked message to the parser
					//post.process(msg.toString());
				}
			} catch (Exception e) {
				logger.error("Error while processing messages", e);
			}
			// kill the connection
			//client.stop();
			timer.stop();
			logger.info(CRAWLER_NAME+"-Crawler END - tracked "+messageCount+" messages in "+timer.elapsed(TimeUnit.SECONDS)+" seconds\n");
		}
	}
}
