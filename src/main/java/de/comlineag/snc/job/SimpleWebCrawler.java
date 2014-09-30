package de.comlineag.snc.job;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.SimpleWebParser;


/**
 *
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.8				- 24.09.2014
 * @status		Beta
 *
 * @description A minimal web crawler. Takes a URL from job control and fetches that page
 * 				plus all linked pages up to max number of pages and max depth of links defined
 * 				either in applicationContext or RuntimeConfiguration (section web crawler)
 *
 *
 * @changelog	0.1 (Chris)		class created as copy from http://cs.nyu.edu/courses/fall02/G22.3033-008/WebCrawler.java
 * 				0.2				implemented configuration options from RuntimeConfiguration
 * 				0.3				implemented KCE from http://sourceforge.net/projects/senews/files/KeyContentExtractor/KCE-1.0/
 * 				0.4				added support for runState configuration, to check if the crawler shall actually run
 * 				0.5	(Maic)		replaced ref-parsing with regular expression in the link-search method
 * 				0.6 (Chris)		implemented boilerpipe to get only the main content from page without any clutter
 * 				0.7				removed boilerpipe (does not work) and implemented jericho for html parsing
 * 				0.7a (Maic)		fixed boilerpipe issues
 * 				0.8	(Chris)		implemented combination of boilerpipe and jericho
 *
 */
public class SimpleWebCrawler extends GenericCrawler implements Job {
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static String CRAWLER_NAME="WEBCRAWLER";

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// instantiate a new fixed thread pool (size configured in SNC_Runtime_Configuration.xml) for parsing of the web page
	ExecutorService executor = Executors.newFixedThreadPool(RuntimeConfiguration.getPARSER_THREADING_POOL_SIZE());
	
	
	// whether or not to follow links OFF of the initial domain
	private Boolean stayOnDomain = true;
	// whether or not to parse urls above the initial given path of the url
	private Boolean stayBelowGivenPath = false;
	// whether or not the crawler shall only get pages containing any of the track terms
	private boolean getOnlyRelevantPages = false;
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	
	private final SimpleWebParser pageContent;
	
	public SimpleWebCrawler(){
		// instantiate the Web-Parser
		pageContent = new SimpleWebParser();
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			String smallLogMessage = "";	
			// runtime settings and crawler constraints
			String initialPath = "/";
			String urlToParse;
			String host = null;
			int port = 0;
			URL url = null;
			int maxDepth = 0;
			int maxPages = 0; 
			
			// authentication options
			String user;
			String passwd;
			
			
			@SuppressWarnings("rawtypes")
			CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
			
			// first check is to get the information, if the crawler was
			// deactivated from within the crawler configuration, even if
			// it is active in applicationContext.xml
			if (crawlerConfig.getRunState(CRAWLER_NAME)) {
				//JSONObject configurationScope = new CrawlerConfiguration<JSONObject>().getCrawlerConfigurationScope();
				JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
				configurationScope.put("SN_ID", SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
				
				if (arg0.getJobDetail().getJobDataMap().containsKey("useAllCrawlerConstraints"))
					configurationScope.put("INCLUDE_ALL", arg0.getJobDetail().getJobDataMap().containsKey("useAllCrawlerConstraints"));
				
				
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
				
				/* THESE CONSTRAINTS ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES, USERS AND LOCATIONS */
				logger.info("retrieving restrictions from configuration db");
				ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintTermText(), configurationScope);
				ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintSiteText(), configurationScope);
				ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(RuntimeConfiguration.getConstraintLanguageText(), configurationScope);
				ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(RuntimeConfiguration.getConstraintUserText(), configurationScope);
				//ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(RuntimeConfiguration.getConstraintLocationText(), configurationScope);
	
				// log output
				if (tTerms.size()>0) { smallLogMessage += " specific terms "; }
				if (tSites.size()>0) { smallLogMessage += " specific sites "; }
				if (tUsers.size()>0) { smallLogMessage += " specific users "; }
				if (tLangs.size()>0) { smallLogMessage += " specific languages "; }
				//if (tLocas.size()>0) { smallLogMessage += " specific Locations "; }
				
				
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
					initialPath = tempurl.getPath();
					//if (initialPath == null) initialPath = "/";
				} catch (MalformedURLException e2) {}
	
				
				// check if the configuration setting to stay below the given path is set in the
				// job control and if not, get it from the runtime configuration (global setting)
				if (arg0.getJobDetail().getJobDataMap().containsKey("stayBelowGivenPath")) {
					stayBelowGivenPath = arg0.getJobDetail().getJobDataMap().getBooleanFromString("stayBelowGivenPath");
				} else {
					logger.trace("configuration setting stayBelowGivenPath not found in job control, getting from runtime configuration");
					stayBelowGivenPath = RuntimeConfiguration.isWC_STAY_BELOW_GIVEN_PATH();
				}
				if (stayBelowGivenPath) {
					stayOnDomain = true;
					smallLogMessage += " below given path ";
				} else {
					// check if the configuration setting to stay on the initial domain is set in the
					// job control and if not, get it from the runtime configuration (global setting)
					if (arg0.getJobDetail().getJobDataMap().containsKey("stayOnDomain")) {
						stayOnDomain = arg0.getJobDetail().getJobDataMap().getBooleanFromString("stayOnDomain");
					} else {
						logger.trace("configuration setting stayOnDomain not found in job control, getting from runtime configuration");
						stayOnDomain = RuntimeConfiguration.isWC_STAY_ON_DOMAIN();
					}
					
					if (stayOnDomain)
						smallLogMessage += " on initial domain ";
				}
				
				// shall the crawler get only pages in which the searched terms are found or any page - if key not set use false
				if (arg0.getJobDetail().getJobDataMap().containsKey("getOnlyRelevantPages")) {
					getOnlyRelevantPages = arg0.getJobDetail().getJobDataMap().getBooleanFromString("getOnlyRelevantPages");
				} else {
					getOnlyRelevantPages = false;
				}
				
				

				// max # of pages and max depth
				if (arg0.getJobDetail().getJobDataMap().containsKey("max_depth"))
					maxDepth = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get("max_depth"));
				if (arg0.getJobDetail().getJobDataMap().containsKey("max_pages"))
					maxPages = Integer.parseInt((String) arg0.getJobDetail().getJobDataMap().get("max_pages"));
				// if maxPages or maxDepth (given by crawler configuration) is higher then maximum value in runtime configuration
				// take the values from runtime configuration. otherwise stick with the values from crawler configuration otherwise,
				// or if non values are given by crawler configuration, take the values from runtime configuration
				if (maxPages > RuntimeConfiguration.getWC_SEARCH_LIMIT() || maxPages == 0) maxPages = RuntimeConfiguration.getWC_SEARCH_LIMIT();
				if (maxDepth > RuntimeConfiguration.getWC_MAX_DEPTH() || maxDepth == 0) maxDepth = RuntimeConfiguration.getWC_MAX_DEPTH();
				if (maxPages == -1) smallLogMessage += " on unlimited pages "; else smallLogMessage += " on max "+maxPages+" pages ";
				if (maxDepth == -1) smallLogMessage += " on unlimited depth "; else smallLogMessage += " on max "+maxDepth+" levels deep ";
				
				
				// is username/password given for authentication 
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
				
				// initialize the url that we want to parse
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
				
				
				
				
				// all configuration settings sourced in - let's start
				logger.info("New "+CRAWLER_NAME+" crawler instantiated for site "+urlToParse+" - restricted to track " + smallLogMessage);
/*				
				// multithreaded implementation
				final int maxPagesf = maxPages;
	
				new Thread(new Runnable() {
					@Override
					public void run() {
						
						URL url = null;
*/				
						// runtime variable
						int pageCount = 1;			// number of pages crawled
						int relevantPages = 0;		// number of pages containing the searched for terms
						int runCounter = 0;			// counter on the number of pages to check 
						if (maxPages == -1) {		// in case maxPages is set to -1 (unlimited) we need to set runCounter 
							runCounter = -2;		// to -2 so as that the while-loop starts at all
							stayOnDomain = true;	// as a last safeguard while configuring the crawler to download and unlimited
						}							// number of pages, we instruct it to only download from the initial domain
						if (maxDepth == -1 ) stayOnDomain = true;
						
						
						while (runCounter < maxPages) {
							url = newURLs.get(0);
							newURLs.remove(0);
							
							if (robotSafe(url)) {
								boolean relPage = false;
								logger.debug("Url "+url+" is page #" + pageCount + " to crawl (until now " + relevantPages + " relevant pages found)");
								
								String page = null;
								try {
									page = getPage(url);
								} catch (StringIndexOutOfBoundsException e) {
									logger.error("ERROR :: the given url " + url + " did not return any data ");
									break;
								}
								//logger.trace("the page content: " +  page);
								
								if (getOnlyRelevantPages) {
									logger.debug("checking if page from url " + url.toString() + " contains any of the relevant track terms");
									if (containsWord(page, tTerms)){
										// proceed only if at least one track term was found
										relPage = true;
										relevantPages++;
										logger.info("Url "+url+" is page #" + relevantPages + " containing the search terms");
									} else {
										relPage = false;
									}
								} else {
									// proceed in any way
									relPage = true;
									relevantPages++;
									logger.info("Url "+url+" is page #" + relevantPages + " tracked");
								}
								
								if (relPage) {
									if (!executor.isShutdown())
										executor.submit(new SimpleWebParser(page, url, tTerms));
									//pageContent.parse(page, url, tTerms);
								}
								// end of parser specific
								
								
								if (page.length() != 0) getLinksFromPage(url, page, knownURLs, newURLs, initialPath);
								if (newURLs.isEmpty()) {
									logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages and found " + relevantPages + " matching ones");
									break;
								}
								pageCount++;
							} // end of robotSafe check
							if (maxPages != -1) runCounter++;
						} // end of for loop over maxPages
						
						logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages and found " + relevantPages + " matching ones");
		//			}
		//		}).start();
			} // end of crawler deactivation

		} catch (Exception e) {
			logger.error(CRAWLER_NAME+"-Crawler Exception", e);
		}
	}



	/**
	 * @description	Download and return the content of the given URL
	 * @param 		url
	 * @return		page content (in html probably)
	 */
	private String getPage(URL url) throws java.lang.StringIndexOutOfBoundsException {
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
	 * @description		The pattern matcher finds links within the given page and
	 * 					calls addNewUrl with the original url, the newly found link and
	 * 					list of known urls.
	 *
	 * @param 			url	is the url the page was downladed from
	 * @param 			page is the html content of the page
	 * @param 			knownURLs a map of already known urls
	 * @param 			newURLs a list of newly found urls
	 */
	private void getLinksFromPage(URL url, String page, Map<URL, Integer> knownURLs, List<URL> newURLs, String initialPath) {
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
			addNewUrl(url, newURL, knownURLs, newURLs, initialPath);
		}
	}

	/**
	 * @description		Adds new URL to the queue if they pass the leave_the_domain and/or
	 * 					the stay_below_given_path check.  
	 * 					If the configuration setting DOWNLOAD_HTML_ONLY is set, then only 
	 * 					new URL's that end in htm or html are accepted.
	 * @param oldURL 	the context
	 * @param url	 	the new url
	 * @param knownURLs	a map containing urls already known to the crawler
	 * @param newURLs	a list of new urls
	 */
	// (either an absolute or a relative URL).
	private void addNewUrl(URL oldURL, URL url, Map<URL, Integer> knownURLs, List<URL> newURLs, String initialPath) {
		Boolean proceed = false;
		// there are two possibilities to restrict the urls to add to the parsing list
		// one is based on the path and the other is based on the domain.
		// that means we either restrict parsing to urls below the initially given path
		// or we only parse sites on the same domain as the starting domain
		if (!stayBelowGivenPath) {
			proceed = true;

			if (stayOnDomain) proceed = false;
		}

		if (!proceed) {
			// if proceed is false at this point, we either are not allowed to
			// grab urls that aren't below the given path, or we are not allowed to
			// leave the initial domain. Because stayBelowGivenPath supersedes,
			// if set at all, we start with the check on that and only if that is
			// false, we check for the domain
			if (stayBelowGivenPath) {
				// if we are not allowed to grab above the initial path, then
				// we are of course also not allowed to grab from a different domain
				if (url.getHost().equals(oldURL.getHost())) {
					// if it is not allowed to leave the domain, let's see if the new
					// url would actually do so, and if not, let it pass
					if (url.getPath().length() > initialPath.length()) {
						if (url.getPath().substring(0, initialPath.length()).equals(initialPath)) {
							proceed = true;
						} else {
							proceed = false;
							if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
								logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
						}
					} else {
						proceed = false;
						logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
					}
				} else {
					proceed = false;
					if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayOnDomain is " + stayOnDomain);
				}
			} else {
				// if it is not allowed to leave the domain, let's see if the new
				// url would actually do so, and if not, let it pass
				if (url.getHost().equals(oldURL.getHost())) {
					proceed = true;
				} else {
					proceed = false;
					if (RuntimeConfiguration.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayonDomain is " + stayOnDomain);
				}
			}
		}

		// only process urls that passed the leave the domain check above.
		if (proceed){

			if (!knownURLs.containsKey(url)) {
				logger.debug("Adding new URL " + url.toString() + " to crawling list");
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
				//logger.trace("the url " + url.toString() + " is already in the list");
			}

		}
	}

	/**
	 * @description	Check that the robot exclusion protocol does not disallow downloading from this url.
	 * @param 		url to download (if ok)
	 * @return		true or false - true = download is ok, false download is NOT ok
	 */
	private boolean robotSafe(URL url) {
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

	/**
	 * @description 	checks if any word of a given set of tokens is found in the given text
	 * 					this check is done a second time in the parser, after all irrelevant content
	 * 					like advertisement and the like has been stripped off the page.
	 * @param 			text
	 * @param 			tokens
	 * @return 			true if any of the tokens was found, otherwise false
	 */
	private boolean containsWord(String text, List<String> tokens){
		String patternString = "\\b(" + StringUtils.join(tokens, "|") + ")\\b";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(text);

		while (matcher.find()) {
		    logger.trace("found the token " + matcher.group(1));
		    return true;
		}
		
		return false;
	}
}
