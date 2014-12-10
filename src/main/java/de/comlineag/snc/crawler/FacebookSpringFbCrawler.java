package de.comlineag.snc.crawler;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import de.comlineag.snc.parser.FacebookParser;

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
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
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
	
	
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	//private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");

	// convenience variables to make the code easier to read and reduce number of calls to RuntimeConfiguration
	private final String domainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String customerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	
	
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
			String curDomain = (String) configurationScope.get(domainKey);
			String curCustomer = (String) configurationScope.get(customerKey);
			
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
			
			
			// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS, GEO-LOCATIONS and PAGES (aka sites)
			logger.info("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(constraintLangText, configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(constraintUserText, configurationScope);
			ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(constraintLocaText, configurationScope);
			// blocked URLs
			//ArrayList<String> bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
			
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
					setPostsTracked(messageCount);
					
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
			
			long seconds = timer.elapsed(TimeUnit.SECONDS);
		    long calcSeconds = seconds;
		    long calcMinutes = 0;
		    long calcHours = 0;
		    long calcDays = 0;
		    if (calcSeconds > 60) {
		    	calcMinutes = calcSeconds / 60;
		    	calcSeconds = calcSeconds - (calcMinutes * 60);
		    }
		    if (calcMinutes > 60) {
		    	calcHours = calcMinutes / 60;
		    	calcMinutes = calcMinutes - (calcHours * 60);
		    }
		    if (calcHours > 24) {
		    	calcDays = calcHours / 24;
		    	calcHours = calcHours - (calcHours * 24);
		    }
			logger.info(CRAWLER_NAME+"-Crawler END - tracked {} messages in {} days {} hours {} minutes {} seconds\n", messageCount, calcDays, calcHours, calcMinutes, calcSeconds);
		}
	}
	
	
	/**
	 * @description 	seeks to find the needle (only one at a time) in the haystack
	 * @param 			haystack
	 * @param 			needle
	 * @return 			true if the needle was found in the haystack, otherwise false
	 */
	private boolean findTheNeedle(String haystack, String needle){
		String patternString = ".*"+needle+".*";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(haystack);

		while (matcher.find())
		    return true;
		return false;
	}
}
