package de.comlineag.snc.crawler;

import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
* @description 	This is the crawler class of the Basic Web Crawler. The BasicWebCrawler is the
* 				first implementation of the crawler4j implementation within the snc. It is intended
* 				as a spot in replacement for the SimpleWebCrawler 
*
* @changelog	0.1 (Chris)		class created
* 
*/
public class BasicWebCrawler extends WebCrawler {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	
	//private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
	//	      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(htm|html|php|asp)$");
	
	private String[] myCrawlDomains;

	@Override
	public void onStart() {
		myCrawlDomains = (String[]) myController.getCustomData();
	}

	@Override
	public boolean shouldVisit(Page page, WebURL url) {
		String href = url.getURL().toLowerCase();
		if (FILTERS.matcher(href).matches()) {
			return false;
		}
		
		for (String crawlDomain : myCrawlDomains) {
			if (href.startsWith(crawlDomain)) {
				return true;
			}
		}
		
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
}
