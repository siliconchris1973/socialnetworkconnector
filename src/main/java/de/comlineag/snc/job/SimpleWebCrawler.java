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

import com.porva.html.keycontent.Kce;
import com.porva.html.keycontent.KceSettings;
import com.porva.html.keycontent.LinkFoundListener;
import com.porva.html.keycontent.TitleFoundListener;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.helper.DataHelper;

/**
 * 
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.3			15.09.2014
 * @status		in development
 *  
 * @description A minimal web crawler. takes an URL from job control and
 * 				fetches that page plus all links up to max depth as defined in 
 * 				RuntimeConfiguration section web crawler 
 * 				The SimpleWebCrawler uses KCE (Key Content Extractor to clean downloaded pages from
 * 				all the unnecessary stuff on the sites we do not want, like ads and the like
 * 				see http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/ 
 * 				for more information
 * 
 * @changelog	0.1 (Chris)		class created as copy from http://cs.nyu.edu/courses/fall02/G22.3033-008/WebCrawler.java
 * 				0.2				implemented configuration options from RuntimeConfiguration
 * 				0.3				implemented KCE from http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
 * 
 */
public class SimpleWebCrawler extends GenericCrawler implements Job {
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WEBCRAWLER";
		
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private String urlToParse = null;
	private int maxDepth = 0;
	private String host = "";
	 // whether or not to follow links OFF of the initial domain
	private Boolean stayOnDomain = true;
	private int port = 443;
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
		
		// get the initial server url and extract host and port for the authentication process from it
		if (!arg0.getJobDetail().getJobDataMap().containsKey("server_url")){
			logger.error("ERROR :: not url to parse given - this is fatal: exiting");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
		urlToParse = (String) arg0.getJobDetail().getJobDataMap().get("server_url");
		try {
			URL tempurl = new URL(urlToParse);
			this.host = tempurl.getHost();
			this.port = tempurl.getPort();
		} catch (MalformedURLException e2) {} 
		
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
		logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+urlToParse+" - restricted to track " + smallLogMessage);
		
		if (this.user.length() > 1) {
			logger.debug("trying to authenticate against site " +  this.host);
			HttpBasicAuthentication();
		}
		// initialize the urls that we want to parse
		initialize(urlToParse, maxDepth);
		
		// a temporary file where we store the contents of the website
		String fileName = null;
					
		for (int i = 0; i < maxPages; i++) {
			URL url = (URL) newURLs.elementAt(0);
			newURLs.removeElementAt(0);
			
			
			// this is just the temporary file - delete it when finished
			fileName = url.toString().replace("/", "_").replace(":", "");
			
			pageCount++;
			logger.info("Url "+url+" is #" + pageCount + " to crawl");
			if (robotSafe(url)) {
				// only need this, if we want to write our own file, otherwise KCE will get the inputstream directly
				String page = getpage(url);
				String cleanedPage = getCleanPageContent(url);
				
				// this is the position to implement the actual url parser and store the data in the db
				logger.trace(cleanedPage);
				
				
				
				// currently we just create a new file and store the page content in it
				File f1 = new File("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
				if (!f1.isFile() || f1.getTotalSpace()<1) {
					FileWriter rawFile;
					FileWriter strippedFile;
					try {
						strippedFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+"stripped_"+fileName);
						strippedFile.write(dataCryptoProvider.encryptValue(DataHelper.stripHTML(cleanedPage)));
						
						rawFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
						rawFile.write(dataCryptoProvider.encryptValue(page));
						
						strippedFile.flush();
						strippedFile.close();
						rawFile.flush();
						rawFile.close();
						
						
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
		
		logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages");
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
		
		maxPages = RuntimeConfiguration.getSEARCH_LIMIT();
		int iPages = maxDepth;
		if (iPages < maxPages) maxPages = iPages;
		
		// Behind a firewall set your proxy and port here!
		Properties props= new Properties(System.getProperties());
		props.put("http.proxySet", "true");
		props.put("http.proxyHost", "webcache-cup");
		props.put("http.proxyPort", "8080");
		
		Properties newprops = new Properties(props);
		System.setProperties(newprops);
		
		logger.debug("All set! Initializing scan - starting with site " + url + " and limited to download " + maxPages);
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
	
	
	// adds new URL to the queue. Accept only new URL's that end in
	// htm or html. oldURL is the context, newURLString is the link
	// (either an absolute or a relative URL).
	public void addnewurl(URL oldURL, String newUrlString) { 
		Boolean proceed = false;
		URL url; 
		
		//logger.trace("URL String " + newUrlString);
		try { 
			url = new URL(oldURL,newUrlString);
			String newHost = url.getHost();
			
			// we only parse sites on the same domain as the starting domain
			// except in case the configuration settigs stayOnDomain is set to false
			// so first thing is to check if configuration setting allows to leave a domain
			if (!this.stayOnDomain){
				proceed = true;
			} else {
				// second, if it is not allowed to leave the domain, let's see if the new
				// url would actually do so, and if not, let it pass
				if (newHost.equals(host)) {
					proceed = true;
				} else {
					if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + newHost + " due to configuration setting stayOnDomain" + stayOnDomain);
				}
			}
			
			// only process urls that passed the leave the domain check above.
			if (proceed){
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
	
	// get the cleaned page content
	public String getCleanPageContent(URL url){
		logger.debug("cleaning page from all clutter...");
		// now get the contet of the page and feed it to KCE
		InputStream urlStream = null;
		try {
			// try opening the URL
			URLConnection urlConnection = url.openConnection();
			
			urlConnection.setAllowUserInteraction(false);
			urlStream = url.openStream();
		} catch (Exception e) {
			logger.error("WARN :: could not get the url " + url.toString() + " - " + SNCStatusCodes.WARN.getErrorCode());
		}
		
		// settings for the KCE Key Content Extractor from 
		// http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
		// allocate a new extractor with default settings
		KceSettings settings = new KceSettings();
		// load settings from "conf/keycontent.properties" file
		settings.loadSettings("WEB-INF/keycontent.properties");
		// construct a new extractor
		Kce extractor = new Kce(settings);
		// register additional listeners
		LinkFoundListener linkFoundListener = new LinkFoundListener();
		TitleFoundListener titleNodeFoundListener = new TitleFoundListener();
		extractor.registerNodeFoundListener(linkFoundListener);
		extractor.registerNodeFoundListener(titleNodeFoundListener);
		// perform extraction of key content from a html file "file.html" encoded as ISO-8859-1 
		//Document document = extractor.extractKeyContent(new FileInputStream(new File("file.html")), "ISO-8859-1", null);
		Document document = extractor.extractKeyContent(urlStream, "ISO-8859-1", null);
		if (document != null) { // cleaning was successful 
			StringWriter stringWriter = new StringWriter();
			// present cleaned document as String
			Kce.prettyPrint(document, "utf-8", stringWriter);
			return stringWriter.toString();
		} else {
			return null;
		}
		
	}
	
	// Download the content of the URL
	public String getpage(URL url) { 
		try { 
			// try opening the URL
			URLConnection urlConnection = url.openConnection();
			
			urlConnection.setAllowUserInteraction(false);
			InputStream urlStream = url.openStream();
			
			// search the input stream for links
			// first, read in the entire URL
			byte b[] = new byte[1000];
			int numRead = urlStream.read(b);
			String content = new String(b, 0, numRead);
			
			while ((numRead != -1) && (content.length() < RuntimeConfiguration.getCRAWLER_MAX_DOWNLOAD_SIZE())) {
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
	
	
	public CloseableHttpClient HttpBasicAuthentication() {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
					new AuthScope(this.host, this.port),
					new UsernamePasswordCredentials(this.user, this.passwd));
			CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider)
					.build();
			
			logger.info("authenticated against site "+this.host + " as user " + this.user);
			return httpClient;
	}
}
