package de.comlineag.snc.controller;


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import de.comlineag.snc.crypto.GenericCryptoException;
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
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// we use simple org.apache.log4j.Logger for logging
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
			
	// it is VERY important to set the crawler name (all in upper case) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	
	private final int rtcPolitenessDelay = rtc.getIntValue("WcPolitenessDelay","crawler");
	private final int rtcMaxPagesToFetch = rtc.getIntValue("WcSearchLimit","crawler");
	private final int rtcCrawlerThreadingPoolSize = rtc.getIntValue("CrawlerThreadingPoolSize", "thrading");
	
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	private final String constraintDnsDomainText = rtc.getStringValue("ConstraintDnsDomainText", "XmlLayout");
	
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
				
				// entry for the crawler configuration file
				if (arg0.getJobDetail().getJobDataMap().containsKey("configDbHandler")) {
					logger.debug("configDbHandöer for THEWebCrawler is " +(String) arg0.getJobDetail().getJobDataMap().get("configDbHandler") );
					configurationScope.put("configDbHandler", (String) arg0.getJobDetail().getJobDataMap().get("configDbHandler"));
				}
				
				logger.debug("retrieving restrictions from configuration db");
				ArrayList<String> tDnsDomain1 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites1 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<String> tDnsDomain2 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites2 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				ArrayList<String> tDnsDomain3 = new CrawlerConfiguration<String>().getConstraint(constraintDnsDomainText, configurationScope);
				ArrayList<String> tSites3 = new CrawlerConfiguration<String>().getConstraint(constraintSiteText, configurationScope);
				
				
				try {
					Stopwatch timer = new Stopwatch().start();
					
					CrawlConfig config1 = new CrawlConfig();
					CrawlConfig config2 = new CrawlConfig();
					CrawlConfig config3 = new CrawlConfig();
					
					// The two crawlers should have different storage folders for their intermediate data
					config1.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler1");
					config2.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler2");
					config3.setCrawlStorageFolder(crawlStorageFolder + File.separatorChar + "crawler3");
					
					config1.setPolitenessDelay(rtcPolitenessDelay);
					config2.setPolitenessDelay(rtcPolitenessDelay);
					config3.setPolitenessDelay(rtcPolitenessDelay);
					
					config1.setMaxPagesToFetch(rtcMaxPagesToFetch);
					config2.setMaxPagesToFetch(rtcMaxPagesToFetch);
					config3.setMaxPagesToFetch(rtcMaxPagesToFetch);
					
					// We will use different PageFetchers for the two crawlers.
					PageFetcher pageFetcher1 = new PageFetcher(config1);
					PageFetcher pageFetcher2 = new PageFetcher(config2);
					PageFetcher pageFetcher3 = new PageFetcher(config3);
					
					// We will use the same RobotstxtServer for both of the crawlers.
					RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
					RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);
					
					CrawlController controller1 = new CrawlController(config1, pageFetcher1, robotstxtServer);
					CrawlController controller2 = new CrawlController(config2, pageFetcher2, robotstxtServer);
					CrawlController controller3 = new CrawlController(config3, pageFetcher3, robotstxtServer);
					
					String[] crawler1Domains = new String[tDnsDomain1.size()];
					for (int i=0;i<tDnsDomain1.size();i++) {
						logger.trace("crawler1: adding domain {}", tDnsDomain1.get(i));
						crawler1Domains[i] = tDnsDomain1.get(i);
					}
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
					
					controller1.setCustomData(crawler1Domains);
					controller2.setCustomData(crawler2Domains);
					controller3.setCustomData(crawler3Domains);
				    
					for (int i=0;i<tSites1.size();i++){
						logger.trace("crawler1: adding site {} ax seed", tSites1.get(i));
						controller1.addSeed(tSites1.get(i));
					}
					for (int i=0;i<tSites2.size();i++){
						logger.trace("crawler2: adding site {} ax seed", tSites2.get(i));
						controller2.addSeed(tSites2.get(i));
					}
					for (int i=0;i<tSites3.size();i++){
						logger.trace("crawler3: adding site {} ax seed", tSites3.get(i));
						controller3.addSeed(tSites3.get(i));
					}
					
					// start the crawler and give them the number of threads
				    controller1.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
				    controller2.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
				    controller3.startNonBlocking(THEWebCrawler.class, rtcCrawlerThreadingPoolSize);
			
				    controller1.waitUntilFinish();
				    controller2.waitUntilFinish();
				    controller3.waitUntilFinish();
				    
				    long seconds = timer.elapsed(TimeUnit.SECONDS);
				    long minutes = timer.elapsed(TimeUnit.MINUTES);
				    long hours = timer.elapsed(TimeUnit.HOURS);
				    long days = timer.elapsed(TimeUnit.DAYS);
				    
					logger.info(CRAWLER_NAME+" END - took {} days {} hours {} minutes {} seconds", days, hours, minutes, seconds);
					timer.stop();
				} catch (Exception e) {
					logger.error("error during execution of crawling process " + e.getMessage());
				}
		} // end o check, whether or not the crawler shall actually run
	}
}
