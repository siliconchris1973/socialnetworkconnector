package de.comlineag.snc.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.webcrawler.crawler.Page;
import de.comlineag.snc.webcrawler.crawler.WebCrawler;
import de.comlineag.snc.webcrawler.parser.HtmlParseData;
import de.comlineag.snc.webcrawler.url.WebURL;


/**
*
* @author 		Christian Guenther
* @category 	controller / job
* @version		0.1				- 23.10.2014
* @status		in development
*
* @description 	This is the crawler class of the Basic Web Crawler. The THEWebCrawler is the
* 				first implementation of the crawler4j implementation within the snc. It is intended
* 				as a spot in replacement for the SimpleWebCrawler 
*
* @changelog	0.1 (Chris)		class created
* 
*/
public class THEWebCrawler extends WebCrawler {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	// it is VERY important to set the crawler name (all in upper case) here
	private static final String CRAWLER_NAME="WEBCRAWLER";
	private static final String name="THEWebCrawler";
	
	
	// negative list of links (aka targets) we don't retrieve
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
		      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	
	// the list of postings (extracts from web pages crawled) is stored in here 
	// and then handed over, one by one, to the persistence layer
	List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
	
	
	private final boolean rtcWarnOnRejectedActions = rtc.getBooleanValue("WarnOnRejectedActions", "crawler");
	private final boolean rtcStayOnDomain = rtc.getBooleanValue("WcStayOnDomain", "crawler");
	private final boolean rtcStayBelowGivenPath = rtc.getBooleanValue("WcStayBelowGivenPath", "crawler");
	
	// we need the list of terms to track
	//private final String constraintTermText = rtc.getStringValue("ConstraintTermText", "XmlLayout");
	// we need the list of blocked sites, because of the shouldVisit method
	//private final String constraintBSiteText = rtc.getStringValue("ConstraintBlockedSiteText", "XmlLayout");
	
	//ArrayList<String> bURLs = new CrawlerConfiguration<String>().getConstraint(constraintBSiteText, configurationScope);
	//ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(constraintTermText, configurationScope);
	
	private final String rtcDomainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String rtcCustomerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	
	
	private String userName = null;
	private String password = null;
	private String host = null;
	private int port = 0;
	private URL url = null;
	
	private String[] theCrawlDomains;
	
	
	@Override
	public void onStart() {
		// is username/password given for authentication 
		try {
			userName = configurationCryptoProvider.decryptValue((String) "Y29taW5lMjAxNA==");
			password = configurationCryptoProvider.decryptValue((String) "TUd1LTZ0Yy1BUjUtdTdS");
		} catch (GenericCryptoException e1) {
			logger.error("Could not decrypt username and password from applicationContext.xml");
		}
	
		
		// finally initiate the crawler
		theCrawlDomains = (String[]) myController.getCustomData();
		
		// is username/password given for authentication 
		if ((userName != null) && (password != null)) {
			if (userName.length() > 1 && password.length() > 1) {
				try {
					url = new URL(theCrawlDomains[0]);
				} catch (MalformedURLException e) {
					logger.error("EXCEPTION :: malformed url ({}) received from THEWebCrawlerController ", url );
				}
				host = url.getHost();
				port = url.getPort();
				logger.debug("trying to authenticate against site " +  host);
				HttpBasicAuthentication(host, port, userName, password);
			}
		}
	}
	
	
	@Override
	public boolean shouldVisit(Page page, WebURL url) {
		String href = url.getURL().toLowerCase();
		logger.trace("checking if url {} should be visited", href);
		
		if (FILTERS.matcher(href).matches()) {
			if (rtcWarnOnRejectedActions)
				logger.debug("rejecting url " + url.getPath() + " because it leads to a file on the black list");
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
			logger.trace("configuration does not enforce to stay on the initial domain - fetching");
			return true;
		}
		
		if (rtcWarnOnRejectedActions)
			logger.debug("rejecting url " + url.getDomain() + " because it is not on anz of the the initial given domains");
		return false;
	}
	
	
	
	@Override
	public void visit(Page page) {
		int docid = page.getWebURL().getDocid();
		String url = page.getWebURL().getURL();
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
		}
		
		logger.debug("=============");
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
