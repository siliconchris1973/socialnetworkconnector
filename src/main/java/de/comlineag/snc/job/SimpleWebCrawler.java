package de.comlineag.snc.job;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.twitter.hbc.core.endpoint.Location;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;

/**
 * 
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.1
 * @status		in development
 *  
 * @description A minimal web crawler. takes an URL from job control and
 * 				fetches that page plus all links up to max depth as defined in 
 * 				RuntimeConfiguration section web crawler 
 * 
 * @changelog	0.1 (Chris)		class created as copy from http://cs.nyu.edu/courses/fall02/G22.3033-008/WebCrawler.java
 * 
 */
public class SimpleWebCrawler extends GenericCrawler implements Job {
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WALLSTREETONLINE";
		
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// these values are used to restrict the maximum number of links the web crawler shall follow, 
	// to honor robot text and limit the size of the parsed sites
	// TODO move these to RuntimeConfiguration
	private static final int	SEARCH_LIMIT = 20;  // Absolute max pages 
	private static final String	DISALLOW = "Disallow:";
	private static final int	MAXSIZE = 20000; // Max size of file 
	
	private String urlToParse = null;
	private int maxDepth = 10;
	private String user = null;
	private String passwd = null;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	// URLs to be searched
	Vector<URL> newURLs;
	// Known URLs
	Hashtable<URL, Integer> knownURLs;
	// max number of pages to download
	int maxPages; 
	
	public SimpleWebCrawler(){}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> twitterConfig = new CrawlerConfiguration();
		//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
		JSONObject configurationScope = twitterConfig.getCrawlerConfigurationScope();
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
		
		urlToParse = (String) arg0.getJobDetail().getJobDataMap().get("server_url");
		//maxDepth = (int) arg0.getJobDetail().getJobDataMap().get("max_depth");
		try {
			user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
			passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
		} catch (GenericCryptoException e1) {
			logger.error("Could not decrypt username and password from applicationContext.xml");
		}
		
		logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+urlToParse+" - restricted to track " + smallLogMessage);
		
		// initialize the 
		initialize(urlToParse, maxDepth);
		
		// a temporary file where we store the contents of the website
		String fileName = null;
					
		for (int i = 0; i < maxPages; i++) {
			URL url = (URL) newURLs.elementAt(0);
			newURLs.removeElementAt(0);
			
			fileName = url.toString().replace("/", "_").replace(":", "");
			
			logger.debug("Crawling " + url.toString());
			
			if (robotSafe(url)) {
				String page = getpage(url);
				
				// this is the position to implement the actual url parser and store the data in the db
				//logger.trace(page);
				
				// currently we just create a new file and store the page content in it
				File f1 = new File("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
				if (!f1.isFile() || f1.getTotalSpace()<1) {
					FileWriter file;
					try {
						file = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
						file.write(dataCryptoProvider.encryptValue(page));
						file.flush();
						file.close();
					} catch (IOException e) {
						logger.error("ERROR :: could not write content of page "+url.toString()+" to disk");
					} catch (GenericCryptoException e) {
						logger.error("ERROR :: could not encrypt data prior writing the file for page " + url.toString());
						e.printStackTrace();
					}
				}
				if (page.length() != 0) processpage(url,page);
				
				if (newURLs.isEmpty()) break;
			}
		}
		
		logger.info("crawling of "+urlToParse+" complete.");
	} 
	
	
	// initializes data structures
	public void initialize(String urlToParse, int maxDepth) {
		URL url = null;
		knownURLs = new Hashtable<URL, Integer>();
		newURLs = new Vector<URL>();
		
		try { 
			url = new URL(urlToParse); 
		} catch (MalformedURLException e) {
			logger.error("Invalid starting URL " + url + ": " + e.getLocalizedMessage());
			return;
		}
		
		knownURLs.put(url,new Integer(1));
		newURLs.addElement(url);
		
		logger.info("Starting crawl: Initial URL " + url.toString());
		
		maxPages = SEARCH_LIMIT;
		int iPages = maxDepth;
		if (iPages < maxPages) maxPages = iPages;
		
		logger.debug("Maximum number of pages:" + maxPages);
		
		// Behind a firewall set your proxy and port here!
		Properties props= new Properties(System.getProperties());
		props.put("http.proxySet", "true");
		props.put("http.proxyHost", "webcache-cup");
		props.put("http.proxyPort", "8080");
		
		Properties newprops = new Properties(props);
		System.setProperties(newprops);
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
		
		logger.debug("Checking robot protocol " + urlRobot.toString());
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

		logger.debug("robot commands are " + strCommands);
		
		// assume that this robots.txt refers to us and 
		// search for "Disallow:" commands.
		String strURL = url.getFile();
		int index = 0;
		
		while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
			index += DISALLOW.length();
			String strPath = strCommands.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);
			
			if (!st.hasMoreTokens()) break;
			
			String strBadPath = st.nextToken();
			
			// if the URL starts with a disallowed path, it is not safe
			if (strURL.indexOf(strBadPath) == 0) return false;
		}
		
		return true;
	}
	
	
	// adds new URL to the queue. Accept only new URL's that end in
	// htm or html. oldURL is the context, newURLString is the link
	// (either an absolute or a relative URL).
	public void addnewurl(URL oldURL, String newUrlString) { 
		URL url; 
		logger.trace("URL String " + newUrlString);
		try { 
			url = new URL(oldURL,newUrlString);
			
			if (!knownURLs.containsKey(url)) {
				String filename =  url.getFile();
				int iSuffix = filename.lastIndexOf("htm");
				
				if ((iSuffix == filename.length() - 3) ||
					(iSuffix == filename.length() - 4)) {
							knownURLs.put(url,new Integer(1));
							newURLs.addElement(url);
							logger.info("Found new URL " + url.toString());
				} 
			}
			
		} catch (MalformedURLException e) { return; }
	}
	
	
	// Go through page finding links to URLs.  A link is signalled
	// by <a href=" ...   It ends with a close angle bracket, preceded
	// by a close quote, possibly preceded by a hatch mark (marking a
	// fragment, an internal page marker)
	public void processpage(URL url, String page) { 
		String lcPage = page.toLowerCase(); // Page in lower case
		int index = 0; // position in page
		int iEndAngle, ihref, iURL, iCloseQuote, iHatchMark, iEnd;
		
		while ((index = lcPage.indexOf("<a",index)) != -1) {
			iEndAngle = lcPage.indexOf(">",index);
			ihref = lcPage.indexOf("href",index);
			
			if (ihref != -1) {
				iURL = lcPage.indexOf("\"", ihref) + 1;
				
				if ((iURL != -1) && (iEndAngle != -1) && (iURL < iEndAngle)) { 
					iCloseQuote = lcPage.indexOf("\"",iURL);
					iHatchMark = lcPage.indexOf("#", iURL);
					
					if ((iCloseQuote != -1) && (iCloseQuote < iEndAngle)) {
						iEnd = iCloseQuote;
						
						if ((iHatchMark != -1) && (iHatchMark < iCloseQuote))
							iEnd = iHatchMark;
						
						String newUrlString = page.substring(iURL,iEnd);
						addnewurl(url, newUrlString);
					}
				}
			}
			
			index = iEndAngle;
		}
	}
	
	
	// Download the content of the URL
	public String getpage(URL url) { 
		try { 
			// try opening the URL
			URLConnection urlConnection = url.openConnection();
			logger.info("Downloading " + url.toString());
			
			urlConnection.setAllowUserInteraction(false);
			InputStream urlStream = url.openStream();
			
			// search the input stream for links
			// first, read in the entire URL
			byte b[] = new byte[1000];
			int numRead = urlStream.read(b);
			String content = new String(b, 0, numRead);
			
			while ((numRead != -1) && (content.length() < MAXSIZE)) {
				numRead = urlStream.read(b);
				if (numRead != -1) {
					String newContent = new String(b, 0, numRead);
					content += newContent;
				}
			}
			return content;
			
		} catch (IOException e) {
			logger.error("ERROR :: couldn't open URL " + url.toString());
			return "";
		}
	}
}
