package de.comlineag.snc.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.appstate.CrawlerConfiguration;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.handler.SimpleWebUser;
import de.comlineag.snc.parser.ParserControl;
import de.comlineag.snc.webcrawler.crawler.Page;
import de.comlineag.snc.webcrawler.crawler.WebCrawler;
import de.comlineag.snc.webcrawler.parser.HtmlParseData;
import de.comlineag.snc.webcrawler.url.WebURL;


/**
*
* @author 		Christian Guenther
* @category 	controller / job
* @version		0.3				- 05.11.2014
* @status		in development
*
* @description 	This is the crawler class of the Basic Web Crawler. The THEWebCrawler is the
* 				first implementation of the crawler4j implementation within the snc. It is intended
* 				as a spot in replacement for the SimpleWebCrawler 
*
* @changelog	0.1 (Chris)		class created
* 				0.2				alpha release
* 				0.3				beta release with limitations
* 
* @limitations	SN_ID is taken from hardcoded crawler name WALLSTREETONLINE
* 				blocked sites not working
* 				no implementation of knownUrls so far - an url might be crawled more than once
* 
*/
public class THEWebCrawler extends WebCrawler {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	// it is VERY important to set the crawler name (all in upper case) here
	// FIXME the CRAWLER_NAME must be WEBCRAWLER and SN_ID must be passed on from controller or taken dynamically 
	private static final String CRAWLER_NAME="WALLSTREETONLINE";
	private static String name="THEWebCrawler";
	
	
	// negative list of files (targets of links) we don't retrieve because they are of no interest
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
		      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	
	// the list of postings (extracted from web pages crawled) is stored in here 
	// and then handed over, one by one, to the persistence layer
	List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
	
	// global settings from SNC_Runtime_Configuration-1.0.xml
	private final boolean rtcWarnOnRejectedActions = rtc.getBooleanValue("WarnOnRejectedActions", "crawler");
	private final boolean rtcStayOnDomain = rtc.getBooleanValue("WcStayOnDomain", "crawler");
	private final boolean rtcStayBelowGivenPath = rtc.getBooleanValue("WcStayBelowGivenPath", "crawler");
	private final boolean rtcStopOnConfigurationFailure = rtc.getBooleanValue("StopOnConfigurationFailure", "crawler");
	private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");
	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	
	private String userName = null;
	private String password = null;
	private String host = null;
	private int port = 0;
	private URL url = null;
	private String sn_id = null;
	private ArrayList<String> tTerms = null;
	private ArrayList<String> bURLs = null;
	private String curDomain = null;
	private String curCustomer = null;
	private Map<URL, Integer> blockedURLs = new HashMap<URL, Integer>();
	
	// the array holds the given domains to crawl (or stay on) passed on by THEWebCrawlerController
	private String[] theCrawlDomains;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onStart() {
		// setup the crawler configuration settings
		@SuppressWarnings("rawtypes")
		CrawlerConfiguration<?> crawlerConfig = new CrawlerConfiguration();
		JSONObject configurationScope = crawlerConfig.getCrawlerConfigurationScope();
		configurationScope.put("SN_ID", SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME));
		sn_id=SocialNetworks.getSocialNetworkConfigElement("code", CRAWLER_NAME);
		
		// retrieve the track terms and the blocked urls
		bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
		tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
		// set the customer we start the crawler for
		curDomain = (String) configurationScope.get(rtcDomainKey);
		curCustomer = (String) configurationScope.get(rtcCustomerKey);
		
		// create the map of blocked urls - we need to convert this to a map to make use of
		// the contains method when checking, whether or not a link should be visited.
		logger.trace("setting up list of blocked urls");
		for (int i=0;i<bURLs.size();i++) {
			try {
				blockedURLs.put(new URL(bURLs.get(i)),new Integer(1));
			} catch (MalformedURLException e) {
				logger.warn("could not add url {} to list of blocked urls", bURLs.get(i));
			}
		}
		
			
		// initiate the first given domain as use this to authenticate
		theCrawlDomains = (String[]) myController.getCustomData();
		if (theCrawlDomains.length > 0) {
			logger.trace("the first domain to crawl is " + theCrawlDomains[0].toString().toLowerCase());
			// in case the use did not add the http to the first domain, we add it, so that the authentication works
			String temp = theCrawlDomains[0].toString().toLowerCase();
			if (!temp.startsWith("http://"))
				temp = "http://" + temp;
			
			
			// if username/password is given, then decrypt it
			// TODO retrieve username/password from crawler configuration
			try {
				userName = configurationCryptoProvider.decryptValue((String) "Y29taW5lMjAxNA==");
				password = configurationCryptoProvider.decryptValue((String) "TUd1LTZ0Yy1BUjUtdTdS");
			} catch (GenericCryptoException e1) {
				logger.warn("Could not decrypt username and password - authentication likely to fail");
			}
			// now authenticate to site with username/password
			if ((userName != null) && (password != null)) {
				if (userName.length() > 1 && password.length() > 1) {
					try {
						url = new URL(temp);
					} catch (MalformedURLException e) {
						logger.error("EXCEPTION :: malformed url ({}) received from THEWebCrawlerController ", temp );
						if (rtcStopOnConfigurationFailure)
							System.exit(SNCStatusCodes.ERROR.getErrorCode());
						return;
					}
					
					host = url.getHost();
					port = url.getPort();
					logger.debug("trying to authenticate against site " +  host);
					HttpBasicAuthentication(host, port, userName, password);
				}
			}
		}
	}
	
	
	
	
	@Override
	public boolean shouldVisit(Page page, WebURL url) {
		String href = url.getURL().toLowerCase();
		
		logger.trace("checking if url {} should be visited", href);
		
		if (FILTERS.matcher(href).matches()) {
			if (rtcWarnOnRejectedActions)
				logger.trace("rejecting url " + url.getPath() + " because it leads to a file on the black list");
			return false;
		}
		
		
		if (blockedURLs.containsKey(url)) {
			if (rtcWarnOnRejectedActions)
				logger.trace("rejecting url " + url + " because it is in the list of blocked urls");
			return false;
		}
		
		// only check if the link is on the same domain, if the configuration says to stay on the same domain
		logger.trace("stayOnDomain is set to {} and stayBelowGivenPath is set to {}", rtcStayOnDomain, rtcStayBelowGivenPath);
		if (rtcStayOnDomain) {
			String domPart = url.getDomain();
			for (String crawlDomain : theCrawlDomains) {
				logger.trace("checking {} against {} as crawlDomain ", domPart, crawlDomain);
				if (href.startsWith(crawlDomain) ) {
					logger.trace("the domain {} has same starting domain as {} - fetching", domPart, crawlDomain);
					return true;
				} else if (domPart.contains(crawlDomain) && !rtcStayBelowGivenPath) {
					logger.trace("the domain {} is contained within the domain {} - fetching", domPart, crawlDomain);
					return true;
				}
			}
		} else {
			// otherwise allow the link
			logger.debug("configuration does not enforce to stay on the initial domain - fetching");
			return true;
		}
		
		if (rtcWarnOnRejectedActions)
			logger.trace("rejecting url " + url.getDomain() + " because it is not on the initial given domains");
		return false;
	}
	
	
	
	@Override
	public void visit(Page page) {
		try {
			int docid = page.getWebURL().getDocid();
			String url = page.getWebURL().getURL();
			URL realUrl = new URL(url);
			int parentDocid = page.getWebURL().getParentDocid();
			
			logger.debug("Docid: " + docid);
			logger.debug("URL: " + url);
			logger.debug("Docid of parent page: " + parentDocid);
			
			if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				String text = htmlParseData.getText();
				String html = htmlParseData.getHtml();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();
				
				logger.debug("Text length: " + text.length());
				logger.debug("Html length: " + html.length());
				logger.debug("Number of outgoing links: " + links.size());
				
				if (containsWord(html, tTerms)) {
					logger.debug("now passing the page to the parser");
					
					postings = ParserControl.submit(html, realUrl, tTerms, sn_id, curCustomer, curDomain);
					
					// invoke the persistence layer - should go to crawler
					for (int ii = 0; ii < postings.size(); ii++) {
						SimpleWebPosting postData = postings.get(ii);
						
						// first get the user-data out of the SimpleWebPosting
						SimpleWebUser userData = new SimpleWebUser(postData.getUser()); 
						logger.info("calling persistence layer to save the user ");
						userData.save();
						
						
						// and now pass the web page on to the persistence layer
						logger.info("calling persistence layer to save the page ");
						postData.save();
					}
				}
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: error while visiting the page " + e.getMessage());
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
	
	/**
	 * @description checks if any word of a given set of tokens is found in the given text
	 * 				this check is done a second time in the parser, after all irrelevant content
	 * 				like advertisement and the like has been stripped off the page.
	 * @param 		text
	 * @param 		tokens
	 * @return 		true if any of the tokens was found, otherwise false
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
	public static void setName(String name) {THEWebCrawler.name = name;}
}
