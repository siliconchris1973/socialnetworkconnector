package de.comlineag.snc.crawler;

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
import java.util.concurrent.TimeUnit;
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

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.handler.SimpleWebUser;
import de.comlineag.snc.parser.ParserControl;


/**
 *
 * @author 		Christian Guenther
 * @category 	job
 * @version		0.1				- 23.10.2014
 * @status		in development
 *
 * @description A streaming web crawler. On calling it expects  
 * 				one initial url, a list of blocked urls, and some boolean vars (see below) plus the list of 
 * 				track terms. It then starts crawling the initial url fetching all linked sites up to max 
 * 				number of pages and max depth of links defined as well either in applicationContext, 
 * 				RuntimeConfiguration (section web crawler) or passed on to the constructor. Each page is  
 * 				checked against the track terms provided. If a track term is found the crawler hands the page 
 * 				over to ParserControl which determines the correct parser and extracts the interesting part 
 * 				of the site. The parsed site is then given to the persistence layer for storing.
 *
 *
 * @changelog	0.1 (Chris)		class created as copy from SimpleWebCrawler 0.1
 * 								removed old execute method
 * 
 */
public class StreamingWebCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// it is VERY important to set the crawler name (all in upper case) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	private static String name="StramingWebCrawler";
	
	// we use simple org.apache.log4j.Logger for logging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// instantiate a new fixed thread pool (size configured in SNC_Runtime_Configuration.xml) for parsing of the web page
	//ExecutorService executor = Executors.newFixedThreadPool(rtc.getPARSER_THREADING_POOL_SIZE());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	// the list of postings (extracts from web pages crawled) is stored in here 
	// and then handed over, one by one, to the persistence layer
	List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
	
	
	private final boolean rtcWarnOnRejectedActions = rtc.getBooleanValue("WarnOnRejectedActions", "crawler");
	private final int rtcCrawlerMaxDownloadSize = rtc.getIntValue("WcCrawlerMaxDownloadSize", "crawler");
	private final int rtcSearchLimit = rtc.getIntValue("WcSearchLimit", "crawler");
	private final int rtcMaxDepth = rtc.getIntValue("WcMaxDepth", "crawler");
	private final boolean rtcStayOnDomain = rtc.getBooleanValue("WcStayOnDomain", "crawler");
	private final boolean rtcStayBelowGivenPath = rtc.getBooleanValue("WcStayBelowGivenPath", "crawler");
	
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintLangText = rtc.getStringValue("ConstraintLanguageText", "XmlLayout");
	private final String constraintUserText = rtc.getStringValue("ConstraintUserText", "XmlLayout");
	private final String constraintSiteText = rtc.getStringValue("ConstraintSiteText", "XmlLayout");
	//private final String constraintLocaText = rtc.getStringValue("ConstraintLocationText", "XmlLayout");
	private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");
	
	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	private final String rtcRobotDisallowText = rtc.getStringValue("WcRobotDisallowText", "crawler");
	
	
	
	public StreamingWebCrawler(){}
	
	
	
	/**
	 * @description constructor for the SimpleWebCrawler with multiple parameters
	 * 
	 * @param url					- the url to crawl
	 * @param bURLs					- array of blocked urls
	 * @param maxPages				- max number of pages to fetch
	 * @param maxDepth				- max number of links to follow
	 * @param stayOnDomain			- whether or not to follow links that are not on the domain of the initial url
	 * @param stayBelowGivenPath	- whether or not to follow links that are not below the initial given path
	 * @param getOnlyRelevantPages	- whether or not to fetch pages that do NOT contain any of the track terms 
	 * @param user					- a user for simple site authentication
	 * @param passwd				- the corresponding password
	 * @param tTerms				- array of terms to look for in fetched pages
	 * @param curCustomer			- part of the page dataset handed over to the persistence layer
	 * @param curDomain				- curCustomer is the customer and curDomain the domain of interest (eg Banking)
	 * 
	 */
	public StreamingWebCrawler(URL url, ArrayList<String> bURLs, int maxPages, int maxDepth, boolean stayOnDomain, boolean stayBelowGivenPath, boolean getOnlyRelevantPages, String user, String passwd, ArrayList<String> tTerms, String curCustomer, String curDomain, String sn_id){
		String smallLogMessage = "";
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
		//configurationScope.put("SN_ID", SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
		configurationScope.put("SN_ID", sn_id);
		
		if (tTerms.size()>0) smallLogMessage += " specific terms ";
		if (stayBelowGivenPath) {
			stayOnDomain = true;
			smallLogMessage += " below given path ";
		}
		if (stayOnDomain) smallLogMessage += " on initial domain ";
		if (maxPages > rtcSearchLimit || maxPages == 0) maxPages = rtcSearchLimit;
		if (maxDepth > rtcMaxDepth || maxDepth == 0) maxDepth = rtcMaxDepth;
		if (maxPages == -1) smallLogMessage += " on unlimited pages "; else smallLogMessage += " on max "+maxPages+" pages ";
		if (maxDepth == -1) smallLogMessage += " unlimited depth "; else smallLogMessage += " max "+maxDepth+" levels deep ";
		
		logger.debug("New "+CRAWLER_NAME+" started - restricted to track " + smallLogMessage);
		logger.trace("calling doCrawl with url " 
				+ url.toString() 
				+ " / #" +bURLs.size() + " blocked urls / #" 
				+ maxPages + " pages / #" 
				+ maxDepth + " depth / "
				+ "stayOnDomain " +stayOnDomain
				+ " / stayBelowGivenPath " + stayBelowGivenPath
				+ " / getOnlyRelevantPages " + getOnlyRelevantPages
				+ " / user " + user
				+ " / passwd " + passwd
				+ " / # " + tTerms.size() + " track terms"
				+ " / curCustomer " + curCustomer
				+ " / curDomain " + curDomain);
		
		
		// now execute the crawler
		doCrawl(url, bURLs, maxPages, maxDepth, stayOnDomain, stayBelowGivenPath, getOnlyRelevantPages, user, passwd, tTerms, curCustomer, curDomain, sn_id);
		
	}
	
	
	
	
	// this way the crawler is run from jobcontrol. It can also be started for exactly one url as done by twitter parser
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			
		} catch (Exception e) {
			logger.error(CRAWLER_NAME+"-Crawler Exception", e);
		}
	}
	
	
	/**
	 * @description the actual crawling method. is either invoked by quartz job control via the execute method
	 * 				or via the constructor in case a single link, e.g. from within a tweet, is to be fetched  
	 * @param url					- the url to crawl
	 * @param bURLs					- array of blocked urls
	 * @param maxPages				- max number of pages to fetch
	 * @param maxDepth				- max number of links to follow
	 * @param stayOnDomain			- whether or not to follow links that are not on the domain of the initial url
	 * @param stayBelowGivenPath	- whether or not to follow links that are not below the initial given path
	 * @param getOnlyRelevantPages	- whether or not to fetch pages that do NOT contain any of the track terms 
	 * @param user					- a user for simple site authentication
	 * @param passwd				- the corresponding password
	 * @param tTerms				- array of terms to look for in fetched pages
	 * @param curCustomer			- part of the page dataset handed over to the persistence layer
	 * @param curDomain				- curCustomer is the customer and curDomain the domain of interest (eg Banking)
	 * 
	 */
	private void doCrawl(URL url, ArrayList<String> bURLs, int maxPages, int maxDepth, boolean stayOnDomain, boolean stayBelowGivenPath, boolean getOnlyRelevantPages, String user, String passwd, ArrayList<String> tTerms, String curCustomer, String curDomain, String sn_id){
		try {
			Stopwatch timer = new Stopwatch().start();
			
			String urlToParse = url.toString();
			int possibleRelevantPages=0;
			int pageCount=0;
			int realRelevantPages = 0;		// number of pages still containing the searched for terms after cleaning
			int runCounter = 0;				// counter on the number of pages to check 
			
			// URLs to be searched
			List<URL> newURLs = new ArrayList<URL>();
			newURLs.add(url);
			
			String host = url.getHost();
			int port = url.getPort();
			String initialPath = url.getPath();
			
			
			// all configuration settings sourced in - let's start
			logger.info(CRAWLER_NAME+"-Crawler instantiated for site "+urlToParse);
			
			
			// is username/password given for authentication 
			if ((user != null) && (passwd != null)) {
				if (user.length() > 1 && passwd.length() > 1) {
					logger.debug("trying to authenticate against site " +  host);
					HttpBasicAuthentication(host, port, user, passwd);
				}
			}
			
			
			
			
			
			// Known URLs
			Map<URL, Integer> knownURLs = new HashMap<URL, Integer>();
			knownURLs.put(url,new Integer(1));
			
			// blocked URLs
			Map<URL, Integer> blockedURLs = new HashMap<URL, Integer>();
			for (int i=0;i<bURLs.size();i++)
				blockedURLs.put(new URL(bURLs.get(i)),new Integer(1));
			
			
			
			// runtime variable
			if (maxPages == -1) {			// in case maxPages is set to -1 (unlimited) we need to set runCounter 
				runCounter = -2;			// to -2 so as that the while-loop starts at all
				stayOnDomain = true;		// as a last safeguard while configuring the crawler to download and unlimited
			}								// number of pages, we instruct it to only download from the initial domain
			if (maxDepth == -1 ) stayOnDomain = true;
			
			
			while (runCounter < maxPages) {
				//logger.trace("runCounter is " + runCounter + " and maxPages is " + maxPages);
				
				url = newURLs.get(0);
				newURLs.remove(0);
				
				if (robotSafe(url)) {
					boolean relPage = false;
					logger.debug("Url "+url+" is page #" + pageCount + " to crawl (until now " + possibleRelevantPages + " possibly relevant pages found)");
					
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
							possibleRelevantPages++;
							logger.info("Url "+url+" is page #" + possibleRelevantPages + " containing any of the search terms - passing on to parser to check if relevant");
						} else {
							relPage = false;
						}
					} else {
						// proceed in any way
						relPage = true;
						possibleRelevantPages++;
						logger.info("Url "+url+" is page #" + possibleRelevantPages + " tracked ");
					}
					
					
					
					if (relPage) {
						// parsing of the page content is either done to get a list of postings
						// or one cleaned up page (that is without the clutter like ads and the like)
						// or at least a plain text representation of some words around the searched
						// track term. To achieve this, we use different parser and the right parser
						// for each site is chosen by the ParserControl class. Therefore we do not
						// simply call a specific parser here, but route this through parser control.
						postings = ParserControl.submit(page, url, tTerms, sn_id, curCustomer, curDomain);
						
						// invoke the persistence layer - should go to crawler
						for (int ii = 0; ii < postings.size(); ii++) {
							realRelevantPages++;
							logger.debug(realRelevantPages + " pages to store in persistence layer tracked");
							
							setPostsTracked(realRelevantPages);
							
							SimpleWebPosting post = postings.get(ii);
							
							// first get the user-data out of the SimpleWebPosting
							SimpleWebUser userData = new SimpleWebUser(post.getUser()); 
							logger.info("calling persistence layer to save the user " );
							userData.save();
							
							
							// and now pass the web page on to the persistence layer
							logger.info("calling persistence layer to save the page " + url.toString());
							post.save();
						}
						/* 
						// TODO find out how to use executor Service for multi threaded persistence call
						// invoke the persistence layer
						for (int ii = 0; ii < postings.size(); ii++) {
							logger.info("calling persistence layer to save the post from site " + url.toString());
							if (rtc.isPERSISTENCE_THREADING_ENABLED()){
								// execute persistence layer in a new thread, so that it does NOT block the crawler
								logger.debug("execute persistence layer in a new thread...");
								final SimpleWebPosting postT = postings.get(ii);
								//if (!persistenceExecutor.isShutdown()) {
								//	persistenceExecutor.submit(new Thread(new Runnable() {
								//			
								//			@Override
								//			public void run() {
								//					postT.save();
								//			}
								//		}).start());
								//}
								//
								new Thread(new Runnable() {
									
									@Override
									public void run() {
											postT.save();
									}
								}).start();
								
							} else {
								// otherwise just call it sequentially
								SimpleWebPosting post = postings.get(ii);
								post.save();
							}
						}
						 */
					}// end of get only relevant pages
					
					
					
					if ((page.length()) != 0) getLinksFromPage(url, page, knownURLs, newURLs, blockedURLs, initialPath, stayOnDomain, stayBelowGivenPath);
					
					if (newURLs.isEmpty()) {
						logger.info(CRAWLER_NAME+"-Crawler END - scanned " + pageCount + " pages in "+timer.elapsed(TimeUnit.SECONDS)+" seconds and found " + possibleRelevantPages + " matching ones\n");
						break;
					}
					
					pageCount++;
				} // end of robotSafe check
				if (maxPages != -1) runCounter++;
			} // end of for loop over maxPages
		} catch (Exception e){
			logger.error("EXCEPTION :: error crawling " + e.getMessage(), e);
			e.printStackTrace();
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

			// first, read in the entire URL
			byte b[] = new byte[1000];
			int numRead = urlStream.read(b);
			String content = new String(b, 0, numRead);
			while ( (numRead != -1) && (content.length() < rtcCrawlerMaxDownloadSize)) {
				numRead = urlStream.read(b);
				if (numRead != -1) {
					String newContent = new String(b, 0, numRead);
					content += newContent;
				}
			}
			//logger.trace("page Content >>> " + content.substring(0, 20)+"...");
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
	private void getLinksFromPage(URL url, 
									String page, 
									Map<URL, Integer> knownURLs, 
									List<URL> newURLs, 
									Map<URL, Integer> blockedURLs, 
									String initialPath, 
									boolean stayOnDomain, 
									boolean stayBelowGivenPath) {
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
			addNewUrl(url, newURL, knownURLs, newURLs, blockedURLs, initialPath, stayOnDomain, stayBelowGivenPath);
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
	private void addNewUrl(URL oldURL, 
							URL url, 
							Map<URL, Integer> knownURLs, 
							List<URL> newURLs, 
							Map<URL, Integer> blockedURLs, 
							String initialPath,
							boolean stayOnDomain,
							boolean stayBelowGivenPath) {
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
							if (rtcWarnOnRejectedActions)
								logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
						}
					} else {
						proceed = false;
						logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
					}
				} else {
					proceed = false;
					if (rtcWarnOnRejectedActions)
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayOnDomain is " + stayOnDomain);
				}
			} else {
				// if it is not allowed to leave the domain, let's see if the new
				// url would actually do so, and if not, let it pass
				if (url.getHost().equals(oldURL.getHost())) {
					proceed = true;
				} else {
					proceed = false;
					if (rtcWarnOnRejectedActions)
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayonDomain is " + stayOnDomain);
				}
			}
		}
		
		// the last check is, is the url in question on the blocking-list
		if (blockedURLs.containsKey(url)) {
			if (rtcWarnOnRejectedActions)
				logger.debug("rejecting url " + url + " because it is in the list of blocked urls");
			proceed = false;
		}
		
		// only process urls that passed the tests above.
		if (proceed){
			// and only if the url is not already in the list of known urls (the ones we already checked)
			// and is not in the list of blocked urls we proceed and add the url
			if (!knownURLs.containsKey(url) && !blockedURLs.containsKey(url)) {
				/*
				 * if you only want html pages, then set wcContentTypeToDownload in RuntimeConfiguration
				String filename =  url.getFile();
				int iSuffix = filename.lastIndexOf("htm");

				if ((iSuffix == filename.length() - 3) ||
					(iSuffix == filename.length() - 4)) {
							knownURLs.put(url,new Integer(1));
							newURLs.addElement(url);
							logger.info("Adding new URL " + url.toString() + " to crawling list");
				}
				
				// to make debug log less noisy I omit this log-entry
				logger.debug("Adding new URL " + url.toString() + " to crawling list");
				*/
				knownURLs.put(url,new Integer(1));
				newURLs.add(url);
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

		//logger.trace("Checking robot protocol " + urlRobot.toString());
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

		while ((index = strCommands.indexOf(rtcRobotDisallowText, index)) != -1) {
			index += rtcRobotDisallowText.length();
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
		    return true;
		}
		
		return false;
	}
	
	
	// these are the getter and setter for the name value - used for JMX support, I think
	public static String getName() {return name;}
	public static void setName(String name) {StreamingWebCrawler.name = name;}
}
