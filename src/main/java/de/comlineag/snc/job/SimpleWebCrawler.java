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

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;


/*
import com.porva.html.keycontent.Kce;
import com.porva.html.keycontent.KceSettings;
import com.porva.html.keycontent.LinkFoundListener;
import com.porva.html.keycontent.TitleFoundListener;
*/

/**
 * 
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.4				- 16.09.2014
 * @status		in development
 *  
 * @description A minimal web crawler. takes an URL from job control and
 * 				fetches that page plus all links up to max depth as defined in 
 * 				RuntimeConfiguration section web crawler 
 * 				The SimpleWebCrawler uses KCE (Key HtmlContent Extractor to clean downloaded pages from
 * 				all the unnecessary stuff on the sites we do not want, like ads and the like
 * 				see http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/ 
 * 				for more information
 * 
 * @changelog	0.1 (Chris)		class created as copy from http://cs.nyu.edu/courses/fall02/G22.3033-008/WebCrawler.java
 * 				0.2				implemented configuration options from RuntimeConfiguration
 * 				0.3				implemented KCE from http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
 * 				0.4				added support for runState configuration, to check if the crawler shall actually run
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
	private int port = 80;
	private String user = null;
	private String passwd = null;
	
	private String page = null;
	private String rawArticle = null;
	
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
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		
		// first check is to get the information, if the crawler was 
		// deactivated from within the crawler configuration, even if 
		// it is active in applicationContext.xml
		if ((Boolean) crawlerConfig.getRunState(CRAWLER_NAME)) {
			//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
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
			
			// get the initial server url and extract host and port for the authentication process from it
			if (!arg0.getJobDetail().getJobDataMap().containsKey("server_url")){
				logger.error("ERROR :: not url to parse given - this is fatal: exiting");
				System.exit(SNCStatusCodes.FATAL.getErrorCode());
			}
			urlToParse = (String) arg0.getJobDetail().getJobDataMap().get("server_url");
			try {
				URL tempurl = new URL(urlToParse);
				host = tempurl.getHost();
				port = tempurl.getPort();
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
					user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
					passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
					if (user.length() > 1 && passwd.length() > 1) {
						logger.debug("trying to authenticate against site " +  host);
						HttpBasicAuthentication();
					}
				} catch (GenericCryptoException e1) {
					logger.error("Could not decrypt username and password from applicationContext.xml");
				}
			}
			
			// all configuration settings sourced in - let's start
			logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+urlToParse+" - restricted to track " + smallLogMessage);
			
			
			FileWriter ff = null;
			try {
				ff = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+"sitelist.txt");
			} catch (IOException e1) {}
			
			
			// initialize the urls that we want to parse
			initUrls(urlToParse, maxDepth);
			
			for (int i = 0; i < maxPages; i++) {
				URL url = (URL) newURLs.elementAt(0);
				newURLs.removeElementAt(0);
				
				page = null;
				rawArticle = null;
				
				
				if (robotSafe(url)) {
					pageCount++;
					logger.info("Url "+url+" is #" + pageCount + " to crawl");
					
					page = getPage(url);
					
					// currently we just create a new file and store the page content in it
					String fileName = url.toString().substring(4).replaceAll(":", "").replaceAll("//", "").replaceAll("/", "_");
					File f1 = new File("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
					if (!f1.isFile() || f1.getTotalSpace()<1) {
						FileWriter rawFile;
						
						try {
							// put url to sitelist
							ff.append(url.toString() + "\n");
							
							// and content in it's own file
							rawFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
							rawFile.write(dataCryptoProvider.encryptValue(page));
							rawFile.flush();
							rawFile.close();
							
							ff.flush();
							
						} catch (IOException e) {
							logger.error("ERROR :: could not write content of page "+url.toString()+" to disk");
						} catch (GenericCryptoException e) {
							logger.error("ERROR :: could not encrypt data prior writing the file for page " + url.toString());
							e.printStackTrace();
						}
					}
					
					if (page.length() != 0) getLinksFromPage(url,page);
					
					if (newURLs.isEmpty()) break;
				}
			}
			
			try { ff.close(); } catch (IOException e) {}
			logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages");
		}
	} 
	
	
	// initialize the data structures for new and known urls and set proxy settings
	public void initUrls(String urlToParse, int maxDepth) {
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
		//Properties props= new Properties(System.getProperties());
		//props.put("http.proxySet", "true");
		//props.put("http.proxyHost", "webcache-cup");
		//props.put("http.proxyPort", "8080");
		
		//Properties newprops = new Properties(props);
		//System.setProperties(newprops);
		
		logger.debug("All set! Initializing scan - starting with site " + url + " and limited to download " + maxPages + " pages");
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
	
	
	// adds new URL to the iQueue. Accept only new URL's that end in
	// htm or html. oldURL is the context, newURLString is the link
	// (either an absolute or a relative URL).
	public void addNewUrl(URL oldURL, String newUrlString) { 
		Boolean proceed = false;
		URL url; 
		
		//logger.trace("URL String " + newUrlString);
		try { 
			url = new URL(oldURL,newUrlString);
			logger.trace("addNewUrl processing url " + url.toString());
			
			// we only parse sites on the same domain as the starting domain
			// except in case the configuration settigs stayOnDomain is set to false
			// so first thing is to check if configuration setting allows to leave a domain
			if (!stayOnDomain){
				logger.debug("stayOnDomain is false - no need to check for allowed parsing");
				proceed = true;
			} else {
				// second, if it is not allowed to leave the domain, let's see if the new
				// url would actually do so, and if not, let it pass
				
				String oldDom = oldURL.getHost();	// InternetDomainName.from(url.toString()).topPrivateDomain().name();
				String newDom = new URL(newUrlString).getHost();		// InternetDomainName.from(newUrlString).topPrivateDomain().name();
				
				//logger.trace("old domain: " + oldDom + " new domain: " + newDom);
				
				if (newDom.equals(oldDom)) {
					logger.trace("new domain " + newDom + " equals old domain");
					proceed = true;
				} else if (!newDom.equals(oldDom)) {
					proceed = false;
					if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + newDom + " due to configuration setting stayOnDomain " + stayOnDomain);
				} else {
					logger.warn("WARNING :: undefined state found comparing old and new domain");
				}
			}
			
			logger.trace("proceed is "+proceed+" during run for url " + url.toString());
			// only process urls that passed the leave the domain check above.
			if (proceed){
				
				if (!knownURLs.containsKey(url)) {
					logger.info("Adding new URL " + url.toString() + " to crawling list");
					knownURLs.put(url,new Integer(1));
					newURLs.addElement(url);
					/*
					String filename =  url.getFile();
					int iSuffix = filename.lastIndexOf("htm");
					
					if ((iSuffix == filename.length() - 3) ||
						(iSuffix == filename.length() - 4)) {
								knownURLs.put(url,new Integer(1));
								newURLs.addElement(url);
								logger.info("Adding new URL " + url.toString() + " to crawling list");
					} 
					*/
					
				} else {
					logger.trace("the url " + url.toString() + " is already in the list");
				}
				
			} else {
				//logger.debug("rejecting url " + url.toString() + " because proceed is " + proceed);
			}
		} catch (MalformedURLException e) { return; }
	}
	
	
	// Go through page finding links to URLs.  A link is signalled
	// by <a href=" ...   It ends with a close angle bracket, preceded
	// by a close quote, possibly preceded by a hatch mark (marking a
	// fragment, an internal page marker)
	public void getLinksFromPage(URL url, String page) {
		logger.debug("getLinksFromPage called for " + url.toString());
		String lcPage = page.toLowerCase(); // Page in lower case
		int lengthOfUrl = lcPage.length();
		
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
						
						addNewUrl(url, newUrlString);
					}
				}
			}
			
			index = iEndAngle;
		}
	}
	
	/* get the cleaned page content
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
		
	*/
	public String getCleanPageContent(String file){
		logger.debug("cleaning page "+file+" from all clutter...");
		/*
		
		// settings for the KCE Key HtmlContent Extractor from 
		// http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
		// allocate a new extractor with default settings
		// TODO find out why KCE brings the webcrawler to a hold, when this code is reached
		KceSettings settings = new KceSettings();
		logger.debug("1");
		// load settings from "conf/keycontent.properties" file
		settings.loadSettings("WEB-INF/keycontent.properties");
		logger.debug("2");
		// construct a new extractor
		Kce extractor = new Kce(settings);
		logger.debug("3");
		*/
		/*  TODO find out why KCE brings the webcrawler to a hold, when this is activated
		// register additional listeners
		LinkFoundListener linkFoundListener = new LinkFoundListener();
		TitleFoundListener titleNodeFoundListener = new TitleFoundListener();
		
		extractor.registerNodeFoundListener(linkFoundListener);
		extractor.registerNodeFoundListener(titleNodeFoundListener);
		*/
		/*
		try {
			// perform extraction of key content from a html file "file.html" encoded as ISO-8859-1 
			//Document document = extractor.extractKeyContent(new FileInputStream(new File("file.html")), "ISO-8859-1", null);
			Document document = extractor.extractKeyContent(new FileInputStream(new File(file)), "ISO-8859-1", null);
			logger.debug("4");
			if (document != null) { // cleaning was successful
				logger.trace("...success");
				StringWriter stringWriter = new StringWriter();
				// present cleaned document as String
				Kce.prettyPrint(document, "utf-8", stringWriter);
				System.out.println(stringWriter);
				//return stringWriter.toString();
			}
		} catch (FileNotFoundException e) {
			logger.error("WARN :: could not get the file " + file.toString() + " - " + SNCStatusCodes.WARN.getErrorCode());
		}
		*/
		return null;
	}
	
	
	// Download the content of the URL
	public String getPage(URL url) { 
		logger.trace("getPage called for url " + url.toString());
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
			while ( (numRead != -1) && (content.length() < RuntimeConfiguration.getCRAWLER_MAX_DOWNLOAD_SIZE()) ) {
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
