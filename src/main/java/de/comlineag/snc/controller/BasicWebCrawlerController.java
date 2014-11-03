package de.comlineag.snc.controller;


import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.crawler.BasicWebCrawler;
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
 * @version		0.1				- 23.10.2014
 * @status		in development
 *
 * @description This is the controller class of the Basic Web Crawler. The BasicWebCrawler is the
 * 				first implementation of the crawler4j implementation within the snc. It is intended
 * 				as a spot in replacement for the SimpleWebCrawler 
 *
 * @changelog	0.1 (Chris)		class created
 * 
 */
public class BasicWebCrawlerController implements Job {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// we use simple org.apache.log4j.Logger for logging
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
			
	// it is VERY important to set the crawler name (all in upper case) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	private static String name="BasicWebCrawlerController";
	
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		String user = null;
		String passwd = null;
		String host = null;
		int port = 0;
		
		try {
			Stopwatch timer = new Stopwatch().start();
			
		    /*
		     * crawlStorageFolder is a folder where intermediate crawl data is
		     * stored.
		     */
		    String crawlStorageFolder = rtc.getStringValue("StoragePath", "runtime");
	
		    CrawlConfig config1 = new CrawlConfig();
		    //CrawlConfig config2 = new CrawlConfig();
		    
		    /*
		     * The two crawlers should have different storage folders for their
		     * intermediate data
		     */
		    config1.setCrawlStorageFolder(crawlStorageFolder + "/crawler1");
		    //config2.setCrawlStorageFolder(crawlStorageFolder + "/crawler2");
	
		    config1.setPolitenessDelay(1000);
		    //config2.setPolitenessDelay(2000);
	
		    config1.setMaxPagesToFetch(50);
		    //config2.setMaxPagesToFetch(100);
	
		    /*
		     * We will use different PageFetchers for the two crawlers.
		     */
		    PageFetcher pageFetcher1 = new PageFetcher(config1);
		    //PageFetcher pageFetcher2 = new PageFetcher(config2);
	
		    /*
		     * We will use the same RobotstxtServer for both of the crawlers.
		     */
		    RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		    RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);
		    
		    CrawlController controller1 = new CrawlController(config1, pageFetcher1, robotstxtServer);
		    //CrawlController controller2 = new CrawlController(config2, pageFetcher2, robotstxtServer);
	
		    String[] crawler1Domains = new String[] { "http://www.wallstreet-online.de" };
		    //String[] crawler2Domains = new String[] { "http://www.wallstreet-online.de" };
		    
		    controller1.setCustomData(crawler1Domains);
		    //controller2.setCustomData(crawler2Domains);
		    
		    controller1.addSeed("http://www.wallstreet-online.de");
		    controller1.addSeed("http://www.wallstreet-online.de/forum/28-1-50/daytrader");
		    controller1.addSeed("http://www.wallstreet-online.de/community/letzte-neueintraege.html");
		    
		    //controller2.addSeed("http://www.wallstreet-online.de");
		    //controller2.addSeed("http://www.wallstreet-online.de/community/letzte-neueintraege.html");
		    
		    
		    // is username/password given for authentication 
		    if ((arg0.getJobDetail().getJobDataMap().containsKey("user")) && (arg0.getJobDetail().getJobDataMap().containsKey("passwd"))) {
				try {
					user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
					passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
				} catch (GenericCryptoException e1) {
					logger.error("Could not decrypt username and password from applicationContext.xml");
				}
			}
		    
		    // is username/password given for authentication 
		    if ((user != null) && (passwd != null)) {
		    	if (user.length() > 1 && passwd.length() > 1) {
		    		URL url = new URL(crawler1Domains[0]);
		    		host = url.getHost();
					port = url.getPort();
		    		logger.debug("trying to authenticate against site " +  host);
		    		HttpBasicAuthentication(host, port, user, passwd);
		    	}
		    }
		    
		    /*
		     * The first crawler will have 5 concurrent threads and the second
		     * crawler will have 7 threads.
		     */
		    controller1.startNonBlocking(BasicWebCrawler.class, 5);
		    //controller2.startNonBlocking(BasicWebCrawler.class, 7);
	
		    controller1.waitUntilFinish();
		    System.out.println("Crawler 1 is finished.");
	
		    //controller2.waitUntilFinish();
		    //System.out.println("Crawler 2 is finished.");	
			
			logger.info(CRAWLER_NAME+"-Crawler END - took "+timer.elapsed(TimeUnit.SECONDS)+" seconds");
			timer.stop();
		} catch (Exception e) {
			logger.error("error during execution of crawling process " + e.getMessage());
		}
	}
	
	
	/**
	 * @description	authenticates the crawler against a site using plain username/password authentication
	 * @param 		host
	 * @param 		port
	 * @param 		user
	 * @param 		passwd
	 * @return		authenticated httpClient
	 */
	private CloseableHttpClient HttpBasicAuthentication(String host, int port, String user, String passwd) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
					new AuthScope(host, port),
					new UsernamePasswordCredentials(user, passwd));
			CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider)
					.build();

			logger.info("authenticated against site "+host + " as user " + user);
			return httpClient;
	}
}
