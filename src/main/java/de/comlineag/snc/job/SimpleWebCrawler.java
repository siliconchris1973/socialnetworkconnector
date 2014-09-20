package de.comlineag.snc.job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.data.HtmlContent;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.ContentExtractionResource;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.helper.BoilerpipeContentExtractionService;

/**
 *
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.6				- 20.09.2014
 * @status		in development
 *
 * @description A minimal web crawler. Takes an URL from job control and fetches 
 * 				that page plus all linked pages up to max number of pages as defined
 * 				in applicationContext or RuntimeConfiguration section web crawler
 * 
 *
 * @changelog	0.1 (Chris)		class created as copy from http://cs.nyu.edu/courses/fall02/G22.3033-008/WebCrawler.java
 * 				0.2				implemented configuration options from RuntimeConfiguration
 * 				0.3				implemented KCE from http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
 * 				0.4				added support for runState configuration, to check if the crawler shall actually run
 * 				0.5	(Maic)		replaced ref-parsing with regular expression in the link-search method
 * 				0.6 (Chris)		implemented boilerpipe to get only the main content from page without any clutter
 *
 */
public class SimpleWebCrawler extends GenericCrawler implements Job {
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WEBCRAWLER";
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	 // whether or not to follow links OFF of the initial domain
	private Boolean stayOnDomain = true;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private final String smallLogMessage = "";
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	
	public SimpleWebCrawler(){}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();

		// first check is to get the information, if the crawler was
		// deactivated from within the crawler configuration, even if
		// it is active in applicationContext.xml
		if (crawlerConfig.getRunState(CRAWLER_NAME)) {
			//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
			JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
			configurationScope.put("SN_ID", SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));

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
			
			String urlToParse;
			String host = null;
			int port = 0;
			int pageCount = 0;
			int maxDepth = 0;
			int maxPages = 0;
			
			String user;
			String passwd;

			// get the initial server url and extract host and port for the authentication process from it
			if (!arg0.getJobDetail().getJobDataMap().containsKey("server_url")){
				logger.error("ERROR :: no url to parse given - this is fatal: exiting");
				//System.exit(SNCStatusCodes.FATAL.getErrorCode());
				return;
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
				stayOnDomain = arg0.getJobDetail().getJobDataMap().getBooleanFromString("stayOnDomain");
			} else {
				logger.trace("configuration setting stayOnDomain not found in job control, getting from runtime configuration");
				stayOnDomain = RuntimeConfiguration.isWC_STAY_ON_DOMAIN();
			}
			if (arg0.getJobDetail().getJobDataMap().containsKey("max_depth"))
				maxDepth = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get("max_depth"));
			if (arg0.getJobDetail().getJobDataMap().containsKey("max_pages"))
				maxPages = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get("max_pages"));
			
			if ((arg0.getJobDetail().getJobDataMap().containsKey("user")) && (arg0.getJobDetail().getJobDataMap().containsKey("passwd"))) {
				try {
					user = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
					passwd = configurationCryptoProvider.decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
					if (user.length() > 1 && passwd.length() > 1) {
						logger.debug("trying to authenticate against site " +  host);
						HttpBasicAuthentication(host, port, user, passwd);
					}
				} catch (GenericCryptoException e1) {
					logger.error("Could not decrypt username and password from applicationContext.xml");
				}
			}
			
			// all configuration settings sourced in - let's start
			logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+urlToParse+" - restricted to track " + smallLogMessage);
			
			
			// initialize the url that we want to parse
			URL url = null;
			try {
				url = new URL(urlToParse);
			} catch (MalformedURLException e) {
				logger.error("Invalid starting URL " + url + ": " + e.getLocalizedMessage());
				return;
			}

			// Known URLs
			Map<URL, Integer> knownURLs = new HashMap<URL, Integer>();
			knownURLs.put(url,new Integer(1));

			// URLs to be searched
			List<URL> newURLs = new ArrayList<URL>();
			newURLs.add(url);
			
			// if maxPages or maxDepth (given by crawler configuration) is higher then maximum value in runtime configuration
			//take the values from runtime configuration. otherwise stick with the values from crawler configuration otherwise,
			// or if non values are given by crawler configuration, take the values from runtime configuration
			if (maxPages > RuntimeConfiguration.getWC_SEARCH_LIMIT() || maxPages == 0) maxPages = RuntimeConfiguration.getWC_SEARCH_LIMIT();
			if (maxDepth > RuntimeConfiguration.getWC_MAX_DEPTH() || maxDepth == 0) maxDepth = RuntimeConfiguration.getWC_MAX_DEPTH();
			
			// Behind a firewall set your proxy and port here!
			//Properties props= new Properties(System.getProperties());
			//props.put("http.proxySet", "true");
			//props.put("http.proxyHost", "webcache-cup");
			//props.put("http.proxyPort", "8080");
			
			//Properties newprops = new Properties(props);
			//System.setProperties(newprops);
			
			
			FileWriter ff = null;
			try {
				ff = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+"sitelist.txt");
			} catch (IOException e1) {}
			
			
			logger.debug("All set! Initializing scan - starting with site " + url + " and limited to download " + maxPages + " pages and " + maxDepth + " maximum deep dive");

			for (int i = 0; i < maxPages; i++) {
				url = newURLs.get(0);
				newURLs.remove(0);
				
				
				if (robotSafe(url)) {
					pageCount++;
					logger.info("Url "+url+" is #" + pageCount + " to crawl");
					
					// now get the page - mainly only because we need th lunks
					String page = getPage(url);
					// and then get the relevant content from the page
					// Boilerpipe is a library to remove unnecessary clutter from a webpage
					final BoilerpipeContentExtractionService bpes = new BoilerpipeContentExtractionService(); 
					HtmlContent cleanedPage = bpes.getPageContent(url.toString());
					
					String cleanedContent = cleanedPage.getText().toString();
					String pageTitle = cleanedPage.getTitle().toString();
					
					logger.trace("the page " + pageTitle + " contains: " + cleanedContent.substring(0, 200));
					
					// currently we just create a new file and store the page content in it
					String fileName = url.toString().substring(4).replaceAll(":", "").replaceAll("//", "").replaceAll("/", "_");
					File f1 = new File("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
					if (!f1.isFile() || f1.getTotalSpace()<1) {
						//FileWriter rawFile;
						FileWriter cleanFile;
						
						try {
							// put url to sitelist
							ff.append(url.toString() + "\n");
							
							// and content in it's own file
							/*
							rawFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
							rawFile.write(dataCryptoProvider.encryptValue(page));
							rawFile.flush();
							rawFile.close();
							*/
							
							// and leaned content in it's own file
							cleanFile = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName+"-cleaned");
							cleanFile.write(dataCryptoProvider.encryptValue(cleanedContent));
							cleanFile.flush();
							cleanFile.close();
							
							ff.flush();
							
						} catch (IOException e) {
							logger.error("ERROR :: could not write content of page "+url.toString()+" to disk");
						} catch (GenericCryptoException e) {
							logger.error("ERROR :: could not encrypt data prior writing the file for page " + url.toString());
							e.printStackTrace();
						}
					}
					
					
					if (page.length() != 0) getLinksFromPage(url,page, knownURLs, newURLs);
					
					if (newURLs.isEmpty()) break;
				}
			}
			
			try { ff.close(); } catch (IOException e) {}
			logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages");
		}
	}
	
	
	/**
	 * @description		Check that the robot exclusion protocol does not disallow downloading from this url.
	 * @param 			url to download (if ok)
	 * @return			true or false - true = download is ok, false download is NOT ok 
	 */
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

		while ((index = strCommands.indexOf(RuntimeConfiguration.getWC_ROBOT_DISALLOW_TEXT(), index)) != -1) {
			index += RuntimeConfiguration.getWC_ROBOT_DISALLOW_TEXT().length();
			String strPath = strCommands.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);

			if (!st.hasMoreTokens()) break;

			String strBadPath = st.nextToken();

			// if the URL starts with a disallowed path, it is not safe
			if (strURL.indexOf(strBadPath) == 0) return false;
		}

		return true;
	}


	/**
	 * @description		Adds new URL to the Queue. If the configuration setting DOWNLOAD_HTML_ONLY 
	 * 					is set, then only new URL's that end in htm or html are accepted.
	 * @param oldURL 	the context
	 * @param url	 	the new url
	 * @param knownURLs	a map containing urls alreadyknwon to the crawler
	 * @param newURLs	a list of new urls
	 */
	// (either an absolute or a relative URL).
	public void addNewUrl(URL oldURL, URL url, Map<URL, Integer> knownURLs, List<URL> newURLs) {
		Boolean proceed = false;
		// we only parse sites on the same domain as the starting domain
		// except in case the configuration settigs stayOnDomain is set to false
		// so first thing is to check if configuration setting allows to leave a domain
		if (!stayOnDomain){
			proceed = true;
		} else {
			// second, if it is not allowed to leave the domain, let's see if the new
			// url would actually do so, and if not, let it pass
			
			if (url.getHost().equals(oldURL.getHost())) {
				proceed = true;
			} else {
				proceed = false;
				if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
					logger.debug("rejecting host " + url.getHost() + " due to configuration setting stayOnDomain " + stayOnDomain);
			}
		}

		// only process urls that passed the leave the domain check above.
		if (proceed){

			if (!knownURLs.containsKey(url)) {
				logger.info("Adding new URL " + url.toString() + " to crawling list");
				knownURLs.put(url,new Integer(1));
				newURLs.add(url);
				/*
				 * if you only want html pages, then set wcContentTypeToDownload RuntimeConfiguration
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

		}
	}
	
	/**
	 * @description		The pattern matcher finds links within the given page and 
	 * 					calls addNewUrl with the original url, the newly found link and 
	 * 					list of known urls. 
	 * 
	 * @param 			url	is the url the page was downladed from
	 * @param 			page is the html content of the page
	 * @param 			knownURLs a map of already known urls
	 * @param 			newURLs a list of newly found urls 
	 */
	public void getLinksFromPage(URL url, String page, Map<URL, Integer> knownURLs, List<URL> newURLs) {
		//logger.debug("getLinksFromPage called for " + url.toString());
		String lcPage = page.toLowerCase(); // Page in lower case

		Pattern p = Pattern.compile("href=\"(.*?)\"");
		Matcher m = p.matcher(lcPage);
		while(m.find()){
			String link = m.group(1);
			if(link == null) continue;
			URL newURL = null;
			try {
				newURL = new URL(url, link);
			} catch (MalformedURLException e) {
				//logger.error(String.format("Link %s could not be parsed as a URL.", link), e);
				continue;
			}
			addNewUrl(url, newURL, knownURLs, newURLs);
		}
	}
	
	/**
	 * @description	Download and return the content of the given URL
	 * @param 		url
	 * @return		page content (in html probably)
	 */
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
			while ( (numRead != -1) && (content.length() < RuntimeConfiguration.getWC_CRAWLER_MAX_DOWNLOAD_SIZE()) ) {
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
