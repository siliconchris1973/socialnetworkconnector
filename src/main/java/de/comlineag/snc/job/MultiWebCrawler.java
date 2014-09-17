package de.comlineag.snc.job;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.helper.DataHelper;
import de.comlineag.snc.spider.Arachnid;
import de.comlineag.snc.spider.PageInfo;


/**
 * 
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.1				- 16.09.2014
 * @status		skeleton
 *  
 * @description A web crawler that downloads multiple pages in parallel. takes an URL from job 
 * 				control and	fetches that page plus all links up to max depth as defined in 
 * 				RuntimeConfiguration section web crawler. 
 * 
 * @changelog	0.1 (Chris)		class created as copy from SimpleWebCrawler.java
 * 
 */
public class MultiWebCrawler extends Arachnid implements Job {
	
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WEBCRAWLER";
		
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private URL url = null;
	private int maxDepth = 0;
	private String host = "";
	 // whether or not to follow links OFF of the initial domain
	private Boolean stayOnDomain = true;
	private int port = 80;
	private String user = null;
	private String passwd = null;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	// max number of pages to download
	int maxPages; 
	
	public MultiWebCrawler(URL base) throws MalformedURLException {
		super(base);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		
		// first check is to get the information, if the crawler was 
		// deactivated from within the crawler configuration, even if 
		// it is active in applicationContext.xml
		if ((Boolean) crawlerConfig.getRunState(CRAWLER_NAME)) {
			
			JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
			configurationScope.put((String) "SN_ID", (String) SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
			
			// set the customer we start the crawler for and log the startup message
			String curDomain = (String) configurationScope.get(RuntimeConfiguration.getDomainidentifier());
			String curCustomer = (String) configurationScope.get(RuntimeConfiguration.getCustomeridentifier());
			
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
			int pageCount = 0;
			
			/* THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS
			logger.info("retrieving restrictions from configuration db");
			ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintTermText(), configurationScope);
			ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintLanguageText(), configurationScope);
			ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(RuntimeConfiguration.getConstraintUserText(), configurationScope);
			ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(RuntimeConfiguration.getConstraintLocationText(), configurationScope);
			
			// log output
			if (tTerms.size()>0) { smallLogMessage += "specific terms "; }
			if (tUsers.size()>0) { smallLogMessage += "specific users "; }
			if (tLangs.size()>0) { smallLogMessage += "specific languages "; }
			if (tLocas.size()>0) { smallLogMessage += "specific Locations "; }
			*/
			
			// get the iinitial url(s) from CrawlerConfiguration file
			//url = new CrawlerConfiguration<URL>()
			url = (URL) arg0.getJobDetail().getJobDataMap().get("server_url");
			host = url.getHost();
			port = url.getPort();
			logger.trace("url to parse is " + url.toString());
			
			// check if the configuration setting to stay on the initial domain is set in the 
			// job control and if not, get it from the runtime configuration (global setting)
			if (arg0.getJobDetail().getJobDataMap().containsKey("stayOnDomain")) {
				stayOnDomain = (Boolean) arg0.getJobDetail().getJobDataMap().getBooleanFromString("stayOnDomain");
			} else {
				logger.trace("configuration setting stayOnDomain not found in job control, getting from runtime configuration");
				stayOnDomain = RuntimeConfiguration.isSTAY_ON_DOMAIN();
			}
			if (arg0.getJobDetail().getJobDataMap().containsKey("max_depth"))
				maxDepth = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get("max_depth"));
			
			if ((arg0.getJobDetail().getJobDataMap().containsKey("user")) && (arg0.getJobDetail().getJobDataMap().containsKey("passwd"))) {
				try {
					this.user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
					this.passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
				} catch (GenericCryptoException e1) {
					logger.error("Could not decrypt username and password from applicationContext.xml");
				}
			}
			
			
			// all configuration settings sourced in - let's start
			logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+url.toString()+" - restricted to track " + smallLogMessage);
			
			// a temporary file where we store the contents of the website
			String fileName = null;
			
			for (int i = 0; i < maxPages; i++) {
				
				// this is just the temporary file - delete it when finished
				fileName = url.toString().replace("/", "_").replace(":", "");
				
				pageCount++;
				logger.info("Url "+url+" is #" + pageCount + " to crawl");
				if (robotSafe(url)) {
					// this is the place to implement the url code
					
					// currently we just create a new file and store the page content in it
					File f1 = new File("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
					if (!f1.isFile() || f1.getTotalSpace()<1) {
						FileWriter rawFile;
						FileWriter strippedFile;
						try {
							rawFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
							//rawFile.write(dataCryptoProvider.encryptValue(page));
							rawFile.flush();
							rawFile.close();
							
							
						} catch (IOException e) {
							logger.error("ERROR :: could not write content of page "+url.toString()+" to disk");
						}/* catch (GenericCryptoException e) {
							logger.error("ERROR :: could not encrypt data prior writing the file for page " + url.toString());
							e.printStackTrace();
						}*/
					}
				}
			}
			
			logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages");
		}
	} 
	
	
	// Check that the robot exclusion protocol does not disallow downloading from this url.
	public boolean robotSafe(URL url) {
		String strHost = url.getHost();
		
		// form URL of the robots.txt file
		String strRobot = "http://" + strHost + "/robots.txt";
		URL urlRobot;
		try { 
			urlRobot = new URL(strRobot);
		} catch (MalformedURLException e) {
			// something weird is happening, so don't trust it
			logger.warn("something weird is happening, we don't trust this site");
			
			return false;
		}
		
		logger.trace("Checking robot protocol " + urlRobot.toString());
		String strCommands;
		try {
			InputStream urlRobotStream = urlRobot.openStream();
			
			// read in entire file
			byte b[] = new byte[1000];
			int numRead = urlRobotStream.read(b);
			strCommands = new String(b, 0, numRead);
			while (numRead != -1) {
				numRead = urlRobotStream.read(b);
				if (numRead != -1) {
					String newCommands = new String(b, 0, numRead);
					strCommands += newCommands;
				}
			}
			urlRobotStream.close();
		} catch (IOException e) {
		    // if there is no robots.txt file, it is OK to search
			return true;
		}

		//logger.debug("robot commands are " + strCommands);
		
		// assume that this robots.txt refers to us and 
		// search for "Disallow:" commands.
		String strURL = url.getFile();
		int index = 0;
		
		while ((index = strCommands.indexOf(RuntimeConfiguration.getROBOT_DISALLOW_TEXT(), index)) != -1) {
			index += RuntimeConfiguration.getROBOT_DISALLOW_TEXT().length();
			String strPath = strCommands.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);
			
			if (!st.hasMoreTokens()) break;
			
			String strBadPath = st.nextToken();
			
			// if the URL starts with a disallowed path, it is not safe
			if (strURL.indexOf(strBadPath) == 0) return false;
		}
		
		return true;
	}
	

	@Override
	protected void handleBadLink(URL url, URL parent, PageInfo p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleLink(PageInfo p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleNonHTMLlink(URL url, URL parent, PageInfo p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleExternalLink(URL url, URL parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleBadIO(URL url, URL parent) {
		// TODO Auto-generated method stub
		
	}
}
