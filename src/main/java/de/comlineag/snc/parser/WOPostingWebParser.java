package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;

import com.google.common.base.Stopwatch;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.TextExtractor;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.WebPosting;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.3				- 10.10.2014
 * @status		in development
 * 
 * @description WOPostingWebParser is the implementation of the generic web parser for 
 * 				wallstreet-online discussion web sites.
 * 				It tries to get the relevant content out of a given wo-discussion site and returns a 
 * 				list of WebPosting objects with extracted page content to the crawler
 * 
 * @changelog	0.1 (Chris)		created as extraction fromSimpleWebParser version 0.7
 * 				0.2				implemented canExecute method
 * 				0.3				implemented posting and user handling
 * 
 * TODO implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO extract user information from the website
 * 
 */
public final class WOPostingWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private final boolean rtcGetOnlyRelevantPages = rtc.getBooleanValue("WcGetOnlyRelevantPages", "crawler");
	//private final int rtcWordDistanceCutoffMargin = rtc.getIntValue("WcWordDistanceCutoffMargin", "crawler");
	//private final boolean rtcPersistenceThreading = rtc.getBooleanValue("PersistenceThreadingEnabled", "runtime");
	private final int rtcPersistenceThreadingPoolSize = rtc.getIntValue("PersistenceThreadingPoolSize", "runtime");
	
	
	public WOPostingWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WOPostingWebParser(String page, URL url, ArrayList<String> tTerms, String sn_id, String curCustomer, String curDomain) {
		parse(page, url, tTerms, sn_id, curCustomer, curDomain);
	}
	
	// multi threading implementation
	public void run() {
		// TODO implement run method for multi threaded parsing
	}
	@Override
	public Object execute(String page, URL url) {
		ExecutorService persistenceExecutor = Executors.newFixedThreadPool(rtcPersistenceThreadingPoolSize);
		
		// TODO implement execute-method to make parser thread save
		return null;
	}
	
	@Override
	public List<WebPosting> parse(String page, URL url, List<String> tokens, String sn_id, String curCustomer, String curDomain) {
		String PARSER_NAME="Wallstreet Online Discussion";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START for url " + url.toString());
		
		List<WebPosting> postings = new ArrayList<WebPosting>();
		
		// a single page
		String master_page_id = null;
		String referer_page_id = null;
		String page_id = null;
		String title = null;
		String description = null;
		String keywords = null;
		String created_at = null;
		String text = null;
		String plainText = null;
		int pageSize = page.length()/8/1024;
		String page_lang = "DE"; // TODO implement proper language detection
		boolean truncated = Boolean.parseBoolean("false");
		
		// the embedded user data
		String user_name = null;
		String screen_name = null;
		String user_id = null;
		String user_lang = page_lang;
		int postings_count = 0;
		
		
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			// first we set some preliminary data for the json object
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			List<Element> siteElements = source.getAllElementsByClass("posting");
			logger.debug("received {} site elements", siteElements.size());
			for (int i=0;i<siteElements.size();i++) {
				Element siteElement = siteElements.get(i).getFirstElementByClass("postingText");
				logger.trace("working on site element {} as {}...", i, siteElement.toString().substring(0, 20));
				
				page_id = ((String) siteElements.get(i).getAttributeValue("data-id"));
				plainText = getReplyToPlainText(siteElement);
				text = plainText;
				
				logger.trace("TruncatedText: >>> " + text);
				
				Element postingHeadElement = siteElements.get(i).getFirstElementByClass("postingHead");
				Element metaElement = postingHeadElement.getFirstElementByClass("meta");
				Element userElement = metaElement.getFirstElementByClass("user");
				//<div class="user" data-dropdown="#userDD" data-userid="319107" data-nick="Nemesi5">Nemesi5</div>
				user_name = ((String) userElement.getAttributeValue("data-nick"));
				user_id = ((String) userElement.getAttributeValue("data-userid"));
				screen_name = user_name;
				
				Element timeElement = metaElement.getFirstElementByClass("timestamp");
				//<div class="timestamp"> schrieb am 06.11.14 17:28:37</div>
				created_at = ((String) timeElement.getContent().toString().substring(12));
				
				logger.debug("using {} as page_id ", page_id);
				logger.debug("user found with user_id {} and name {}", user_id, user_name);
				logger.debug("using time {} on site for created_at value", created_at);
				
				// add page to list if it contains the track terms or if we want all pages
				if (!rtcGetOnlyRelevantPages || findNeedleInHaystack(text, tokens)){
					logger.debug("adding extracted page content-json to posting list");
					
					// add the parsed site to the message list for saving in the DB
					JSONObject pageJson = createPageJsonObject(sn_id, 
																title, 
																description, 
																plainText, 
																text, 
																created_at, 
																url, 
																truncated, 
																page_lang, 
																page_id, 
																user_id, 
																user_name, 
																screen_name, 
																user_lang, 
																postings_count, 
																curCustomer, 
																curDomain);
					
					//logger.trace("created json is {}", pageJson.toString());
					WebPosting parsedPageSimpleWebPosting = new WebPosting(pageJson);
					postings.add(parsedPageSimpleWebPosting);
				}
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
			e.printStackTrace();
		}
		
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds for " + pageSize +"Kb");
		return postings;
	}
	
	
	private String getReplyToPlainText(Element siteElement) {
		return new TextExtractor(siteElement) {
			public boolean excludeElement(StartTag startTag){
				return "s8 replyTo".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "postingHead".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "postingFooter".equalsIgnoreCase(startTag.getAttributeValue("class"));
			}
		}.toString();
	}
	// START OF JERICHO SPECIFIC PARSER STUFF
	private static String getTitle(Source source) {
		Element titleElement=source.getFirstElement(HTMLElementName.TITLE);
		if (titleElement==null) return null;
		// TITLE element never contains other tags so just decode it collapsing whitespace:
		return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
	}
	private static String getMetaValue(Source source, String key) {
		for (int pos=0; pos<source.length();) {
			StartTag startTag=source.getNextStartTag(pos,"name",key,false);
			if (startTag==null) return null;
			if (startTag.getName()==HTMLElementName.META)
				return startTag.getAttributeValue("content"); // Attribute values are automatically decoded
			pos=startTag.getEnd();
		}
		return null;
	}
	// END OF JERICHO PARSER SPECIFIC STUFF
	
	
	@Override
	public boolean canExecute(String page, URL url) {
		// there are 3 indications whether the parser can work with this page:
		// 1st. the host is wallstreet-online
		// 2nd. the url contains diskussion and
		// 3rd. the page source code has class-tags "posting" in it
		// if all conditions are met, this is the right parser for this site
		boolean iAmTheOne = false;
		int hitRatio = 0;
		int elementCount = 0;
		
		// we know, that discussion contains posting tags, so we give 5 points 
		if (url.getHost().contains("wallstreet-online")) hitRatio += 3;
		if (url.getPath().contains("diskussion")) hitRatio += 3;
		
		Source source=new Source(page);
		source.fullSequentialParse();
		
		List<Element> postingsElement = source.getAllElementsByClass("posting");
		for (int i=0;i<postingsElement.size();i++) {
			List<Element> subElements = postingsElement.get(i).getAllElementsByClass("postingText");
			for (int ii=0;ii<subElements.size();ii++) {
				elementCount += 1;
				logger.trace("#" +elementCount+ " postingText element(s) found");
			}
		}
		if (elementCount == 1) hitRatio += 1; else hitRatio += elementCount/2;
		logger.trace("hit ratio is "+hitRatio+" for url " + url.toString());
		// if the above two tests get a score at least 7 (that is 2/3 of 10 possible) points,
		// we assume the parser to be right for the site.
		if (hitRatio >= 7) iAmTheOne = true;
			
		return iAmTheOne;
	}
	
	@Override
	protected boolean parse(String page) {logger.warn("method not implemented");return false;}
	@Override
	protected boolean parse(InputStream is) {logger.warn("method not implemented");return false;}
}

