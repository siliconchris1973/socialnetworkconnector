package de.comlineag.snc.controller;


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crawler.THEWebCrawler;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.webcrawler.crawler.CrawlConfig;
import de.comlineag.snc.webcrawler.crawler.CrawlController;
import de.comlineag.snc.webcrawler.fetcher.PageFetcher;
import de.comlineag.snc.webcrawler.robotstxt.RobotstxtConfig;
import de.comlineag.snc.webcrawler.robotstxt.RobotstxtServer;


/**
 *
 * @author 		Christian Guenther
 * @category 	controller / job
 * @version		0.3				- 05.11.2014
 * @status		in development
 *
 * @description This is the controller class of the Basic Web Crawler. The THEWebCrawler is the
 * 				first implementation of the crawler4j implementation within the snc. It is intended
 * 				as a spot in replacement for the SimpleWebCrawler.
 *
 * @changelog	0.1 (Chris)		class created
 * 				0.2				alpha release with everything static
 * 				0.3				beta release - fetch infos from applicationContext.xml
 * 
 * @limitations	all 3 crawlers work on the same domain and customer - therefore only one active
 * 
 */
public class THEWebCrawlerController implements Job {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	// we use simple org.apache.log4j.Logger for logging
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
			
	// it is VERY important to set the crawler name (all in upper case) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	
	private final int rtcPolitenessDelay = rtc.getIntValue("WcPolitenessDelay","crawler");
	private final int rtcMaxPagesToFetch = rtc.getIntValue("WcSearchLimit","crawler");
	private final int rtcMaxLinkDepth = rtc.getIntValue("WcMaxDepth","crawler");
	private final int rtcCrawlerThreadingPoolSize = rtc.getIntValue("CrawlerThreadingPoolSize", "thrading");
	
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	private final String constraintDnsDomainText = rtc.getStringValue("ConstraintDnsDomainText", "XmlLayout");
	//private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");
	
	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		String sn_id = null;
		
		// crawlStorageFolder is a folder where intermediate crawl data is stored.
		String crawlStorageFolder = rtc.getStringValue("StoragePath", "runtime");
		
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		
		
		// check if the crawler is deactivated from within the crawler configuration
		if (crawlerConfig.getRunState(CRAWLER_NAME)) {
			
				JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
				if (arg0.getJobDetail().getJobDataMap().containsKey("SN_ID_CODE")){
					configurationScope.put("SN_ID", (String) arg0.getJobDetail().getJobDataMap().get("SN_ID_CODE"));
					sn_id=(String) arg0.getJobDetail().getJobDataMap().get("SN_ID_CODE");
				} else {
					configurationScope.put("SN_ID", SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
					sn_id=SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME);
				}
				// set the customer we start the crawler for and log the startup message
				String curDomain = (String) configurationScope.get(rtcDomainKey);
				String curCustomer = (String) configurationScope.get(rtcCustomerKey);
				
				logger.debug("SN_ID = {} curDomain = {} curCustomer = {}", sn_id, curDomain, curCustomer);
				
				// entry for the crawler configuration file
				if (arg0.getJobDetail().getJobDataMap().containsKey("configDbHandler")) {
					logger.debug("configDbHandöer for THEWebCrawler is " +(String) arg0.getJobDetail().getJobDataMap().get("configDbHandler") );
					configurationScope.put("configDbHandler", (String) arg0.getJobDetail().getJobDataMap().get("configDbHandler"));
				}
				
				logger.debug("retrieving restrictions from configuration db");
				ArrayList<String> tDnsDomain1 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites1 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<String> tTerms1 = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
				ArrayList<URL> bURLs1 = new CrawlerConfiguration<URL>().getConstraint(constraintBSiteText, configurationScope);
				
				/*
				ArrayList<String> tDnsDomain2 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites2 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<String> tTerms2 = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
				ArrayList<URL> bURLs2 = new CrawlerConfiguration<URL>().getConstraint(constraintBSiteText, configurationScope);
				
				ArrayList<String> tDnsDomain3 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites3 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<String> tTerms3 = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
				ArrayList<URL> bURLs3 = new CrawlerConfiguration<URL>().getConstraint(constraintBSiteText, configurationScope);
				*/
				try {
					Stopwatch timer = new Stopwatch().start();
					
					CrawlConfig config1 = new CrawlConfig();
					config1.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler1");
					config1.setPolitenessDelay(rtcPolitenessDelay);
					config1.setMaxPagesToFetch(rtcMaxPagesToFetch);
					config1.setMaxDepthOfCrawling(rtcMaxLinkDepth);
					config1.setResumableCrawling(true);
					PageFetcher pageFetcher1 = new PageFetcher(config1);
					// We will use the same RobotstxtServer for all of the crawlers.
					RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
					RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);
					CrawlController controller1 = new CrawlController(config1, pageFetcher1, robotstxtServer);
					String[] crawler1Domains = new String[tDnsDomain1.size()];
					for (int i=0;i<tDnsDomain1.size();i++) {
						logger.trace("crawler1: adding domain {}", tDnsDomain1.get(i));
						crawler1Domains[i] = tDnsDomain1.get(i);
					}
					
					// we can't simply set the crawlerDomains here, but need to pass an JSONObject, 
					// because we also need to pass the SN_ID, the current Domain of Interest and the 
					// current Customer along with track terms and blocked sites as array lists
					JSONObject configurationScopeToPassToCrawler = new JSONObject();
					configurationScopeToPassToCrawler.put("SN_ID", configurationScope.get("SN_ID"));
					// retrieve the track terms and the blocked urls
					configurationScopeToPassToCrawler.put(constraintTermText, tTerms1);
					configurationScopeToPassToCrawler.put(constraintBSiteText, bURLs1);
					// set the customer we start the crawler for
					configurationScopeToPassToCrawler.put(rtcDomainKey, (String) configurationScope.get(rtcDomainKey));
					configurationScopeToPassToCrawler.put(rtcCustomerKey, (String) configurationScope.get(rtcCustomerKey));
					// set the initial crawl domains
					configurationScopeToPassToCrawler.put("theCrawlDomains", crawler1Domains);
					// set username and password
					configurationScopeToPassToCrawler.put("userName", (String) arg0.getJobDetail().getJobDataMap().get("user"));
					configurationScopeToPassToCrawler.put("password", (String) arg0.getJobDetail().getJobDataMap().get("passwd"));
					
					// put the json object in the custom data field of the controller, so that the crawler can fetch it from there
					controller1.setCustomData(configurationScopeToPassToCrawler);
					
					
					for (int i=0;i<tSites1.size();i++){
						logger.trace("crawler1: adding site {} ax seed", tSites1.get(i));
						controller1.addSeed(tSites1.get(i));
					}
					// start the crawler and give it the number of threads	
					controller1.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
					controller1.waitUntilFinish();
					List<Object> crawler1Data = controller1.getCrawlersLocalData();
					logger.debug("we got {} data back", crawler1Data.size());
					
					
					
					
					/* two more crawlers which can run in parallel to the initial one 
					 * could become handy in case we want to crawl multiple websites or
					 * multiple domain of interests
					CrawlConfig config2 = new CrawlConfig();
					CrawlConfig config3 = new CrawlConfig();
					
					// The two crawlers should have different storage folders for their intermediate data
					config2.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler2");
					config3.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler3");
					
					config2.setPolitenessDelay(rtcPolitenessDelay);
					config3.setPolitenessDelay(rtcPolitenessDelay);
					
					config2.setMaxPagesToFetch(rtcMaxPagesToFetch);
					config3.setMaxPagesToFetch(rtcMaxPagesToFetch);
					
					config2.setMaxDepthOfCrawling(rtcMaxLinkDepth);
					config3.setMaxDepthOfCrawling(rtcMaxLinkDepth);
					
					config2.setResumableCrawling(true);
					config3.setResumableCrawling(true);
					
			        					
					// We will use different PageFetchers for the  crawlers.
					PageFetcher pageFetcher2 = new PageFetcher(config2);
					PageFetcher pageFetcher3 = new PageFetcher(config3);
					
					CrawlController controller2 = new CrawlController(config2, pageFetcher2, robotstxtServer);
					CrawlController controller3 = new CrawlController(config3, pageFetcher3, robotstxtServer);
					
					String[] crawler2Domains = new String[tDnsDomain2.size()];
					for (int i=0;i<tDnsDomain2.size();i++) {
						logger.trace("crawler2: adding domain {}", tDnsDomain2.get(i));
						crawler2Domains[i] = tDnsDomain2.get(i);
					}
					String[] crawler3Domains = new String[tDnsDomain3.size()];
					for (int i=0;i<tDnsDomain3.size();i++) {
						logger.trace("crawler3: adding domain {}", tDnsDomain3.get(i));
						crawler3Domains[i] = tDnsDomain3.get(i);
					}
					
					controller2.setCustomData(configurationScopeToPassToCrawler);
					controller3.setCustomData(configurationScopeToPassToCrawler);
				    
					
					for (int i=0;i<tSites2.size();i++){
						logger.trace("crawler2: adding site {} ax seed", tSites2.get(i));
						controller2.addSeed(tSites2.get(i));
					}
					for (int i=0;i<tSites3.size();i++){
						logger.trace("crawler3: adding site {} ax seed", tSites3.get(i));
						controller3.addSeed(tSites3.get(i));
					}
					
					
					controller2.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
					controller3.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
					
					
					controller2.waitUntilFinish();
					controller3.waitUntilFinish();
				    
				    
				    // after the crawler foinished, get there local data - if any.
				    
				    List<Object> crawler2Data = controller1.getCrawlersLocalData();
				    List<Object> crawler3Data = controller1.getCrawlersLocalData();
				    */
				    
				    
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
					logger.info(CRAWLER_NAME+" END - took {} days {} hours {} minutes {} seconds", calcDays, calcHours, calcMinutes, calcSeconds);
					timer.stop();
				} catch (Exception e) {
					logger.error("error during execution of crawling process " + e.getMessage());
				}
		} // end o check, whether or not the crawler shall actually run
	}
}