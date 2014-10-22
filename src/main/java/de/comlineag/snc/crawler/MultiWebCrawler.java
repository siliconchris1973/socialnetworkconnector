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
 * @version		0.1				- 13.10.2014
 * @status		in development
 *
 * @description A web crawler to download multiple URLs in parallel. Takes a list of URLs from 
 * 				the SNC crawler configuration file and fetches all linked pages up to max number 
 * 				of pages and max depth of links defined either in Crawler configuration per start
 * 				URL or RuntimeConfiguration (section web crawler)
 *
 *
 * @changelog	0.1 (Chris)		class created as copy from SimpleWebCrawler Revision 0.9c
 *
 */
public class MultiWebCrawler extends GenericCrawler implements Job {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// it is VERY imoportant to set the crawler name (all in uppercase) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	private static String name="SimpleWebCrawler";

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// instantiate a new fixed thread pool (size configured in SNC_Runtime_Configuration.xml) for parsing of the web page
	//ExecutorService executor = Executors.newFixedThreadPool(rtc.getPARSER_THREADING_POOL_SIZE());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	//private final ParserControl pageContent;
	public MultiWebCrawler(){
		// TODO instantiate the Web-Parser via parserControl 
		//pageContent = new ParserControl();
	}
	
	List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
	
	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext arg0) throws JobExecutionException {}



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
			while ( (numRead != -1) && (content.length() < rtc.getWC_CRAWLER_MAX_DOWNLOAD_SIZE()) ) {
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
	private void getLinksFromPage(URL url, 
									String page, 
									Map<URL, Integer> knownURLs, 
									List<URL> newURLs, 
									Map<URL, Integer> blockedURLs, 
									String initialPath,
									boolean stayBelowGivenPath,
									boolean stayOnDomain) {
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
			addNewUrl(url, newURL, knownURLs, newURLs, blockedURLs, initialPath, stayBelowGivenPath, stayOnDomain);
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
							boolean stayBelowGivenPath,
							boolean stayOnDomain) {
		Boolean proceed = false;
		
		// first of all, we check if the url in question is on the blocking-list
		
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
							if (rtc.isWARN_ON_REJECTED_ACTIONS())
								logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
						}
					} else {
						proceed = false;
						logger.debug("rejecting url " + url.getPath() + " because it not below initial path "+initialPath+" and stayBelowGivenPath is " + stayBelowGivenPath);
					}
				} else {
					proceed = false;
					if (rtc.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayOnDomain is " + stayOnDomain);
				}
			} else {
				// if it is not allowed to leave the domain, let's see if the new
				// url would actually do so, and if not, let it pass
				if (url.getHost().equals(oldURL.getHost())) {
					proceed = true;
				} else {
					proceed = false;
					if (rtc.isWARN_ON_REJECTED_ACTIONS())
						logger.debug("rejecting host " + url.getHost() + " because it is not on the initial domain "+oldURL.getHost()+" and stayonDomain is " + stayOnDomain);
				}
			}
		}
		
		if (blockedURLs.containsKey(url)) {
			if (rtc.isWARN_ON_REJECTED_ACTIONS())
				logger.debug("rejecting url " + url + " because it is in the list of blocked urls");
			proceed = false;
		}
		
		// only process urls that passed the tests above.
		if (proceed){

			if (!knownURLs.containsKey(url) || !blockedURLs.containsKey(url)) {
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

		while ((index = strCommands.indexOf(rtc.getWC_ROBOT_DISALLOW_TEXT(), index)) != -1) {
			index += rtc.getWC_ROBOT_DISALLOW_TEXT().length();
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
	public static void setName(String name) {MultiWebCrawler.name = name;}
}
