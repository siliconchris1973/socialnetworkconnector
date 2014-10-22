package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

import com.google.common.base.Stopwatch;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.TextExtractor;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.helper.UniqueIdServices;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.1				- 22.10.2014
 * @status		in development
 * 
 * @description WOCommunityWebParser is the implementation of the generic web parser for 
 * 				wallstreet-online community web sites - these sites are NOT the discussion sites,
 * 				but precede these.
 * 				It tries to get the relevant content out of a given wo-community site and returns a 
 * 				list of SimpleWebPosting objects with extracted page content to the crawler
 * 
 * @changelog	0.1 (Chris)		created as copy from WOPostingWebParser version 0.3
 * 				
 * 
 * TODO implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO extract user information from the website
 * 
 */
public final class WOCommunityWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public WOCommunityWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WOCommunityWebParser(String page, URL url, ArrayList<String> tTerms, String sn_id, String curCustomer, String curDomain) {
		parse(page, url, tTerms, sn_id, curCustomer, curDomain);
	}
	
	// multi threading implementation
	public void run() {
		// TODO implement run method for multi threaded parsing
	}
	@Override
	public Object execute(String page, URL url) {
		ExecutorService persistenceExecutor = Executors.newFixedThreadPool(rtc.getPERSISTENCE_THREADING_POOL_SIZE());
		
		// TODO implement execute-method to make parser thread save
		return null;
	}
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens, String sn_id, String curCustomer, String curDomain) {
		String PARSER_NAME="Wallstreet Online Discussion";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START for url " + url.toString());
		
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		String plainText = "";
		String segmentText = "";
		String page_id, user_id, user_name, screen_name;
		long postings_count = 0;
		int pageSize = page.length()/8/1024;
		String page_lang = "DE"; // TODO implement proper language detection
		String user_lang = page_lang;
		boolean truncated = Boolean.parseBoolean("false");
		
		// vars for the token extraction
		int lowBorderMark = 300;
		int highBorderMark = 300;
		ArrayList<Integer> positions = new ArrayList<Integer>();
		
		
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			// first we set some preliminary data for the json object
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			
			List<Element> siteElements = source.getAllElements("id", "page", false);
			for (int i=0;i<siteElements.size();i++) {
				// pass the site element on to a private method (below) that extracts the pure plain text content according to white and blacklist given
				plainText = getGridFsPlainText(siteElements.get(i));
								
				/* uses a combination of returnTokenPosition and trimStringAtPosition
				 *   - works! returns exactly one string if the words are near to each other
				 *     and multiple concatinated strings in case the words are far apart
				 */      
				for (int iii=0; iii < tokens.size(); iii++) {
					String token = tokens.get(iii);
					logger.trace("working on token " + token);
					
					positions = returnTokenPosition(plainText, token, positions, lowBorderMark, highBorderMark);
				}
				
				segmentText = "";
				for (int iii=0;iii<positions.size();iii++) {
					int position = positions.get(iii);
					segmentText += trimStringAtPosition(plainText, position, 
														rtc.getWC_WORD_DISTANCE_CUTOFF_MARGIN(), 
														rtc.getWC_WORD_DISTANCE_CUTOFF_MARGIN());
				}
				logger.trace("TruncatedText: >>> " + segmentText);
				
				text = segmentText;
				user_name = url.getHost().toString();
				screen_name = user_name;
				page_id = UniqueIdServices.createMessageDigest(text);
				user_id = UniqueIdServices.createMessageDigest(user_name);
				
				// add page to list if it contains the track terms or if we want all pages
				if (!rtc.isWC_GET_ONLY_RELEVANT_PAGES() || findNeedleInHaystack(text, tokens)){
					logger.trace("adding extracted page content to posting list");
					
					// add the parsed site to the message list for saving in the DB
					//JSONObject pageJson = createPageJsonObject(sn_id, title, description, plainText, text, url, truncated, page_lang, curCustomer, curDomain);
					JSONObject pageJson = createPageJsonObject(sn_id, title, description, plainText, text, url, truncated, page_lang, page_id, user_id, user_name, screen_name, user_lang, postings_count, curCustomer, curDomain);
					SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(pageJson);
					postings.add(parsedPageSimpleWebPosting);
				}
			} // end for loop over page id elements
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
			e.printStackTrace();
		}
		
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds for " + pageSize +"Kb");
		return postings;
	}
	
	
	/**
	 * takes a site element and returns the plain text as a combination of allowed and disallowed site tags
	 * @param siteElement
	 * @return plainText
	 */
	private String getGridFsPlainText(Element siteElement) {
		return new TextExtractor(siteElement) {
			public boolean includeElement(StartTag startTag) {
				return "grid fs ".equalsIgnoreCase(startTag.getAttributeValue("class"));
			}
			public boolean excludeElement(StartTag startTag){
				return startTag.getName()==HTMLElementName.TITLE
						|| startTag.getName()==HTMLElementName.THEAD
						|| startTag.getName()==HTMLElementName.SCRIPT
						|| startTag.getName()==HTMLElementName.HEAD
						|| startTag.getName()==HTMLElementName.META
						|| "keywords".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "clear".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "modulecontent".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "cs mooMenu".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "toolbar".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "postingHead".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "postingFooter".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "grid ws threadbodybox ".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "grid ns ".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "modulecontent copyright".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "more_link".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "tbutton b".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "voting_stars_display".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "pagination".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "tab_wrapper".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "pagination ".equalsIgnoreCase(startTag.getAttributeValue("class"))
						|| "overflow:hidden".equalsIgnoreCase(startTag.getAttributeValue("style"))
						|| "pagination".equalsIgnoreCase(startTag.getAttributeValue("form"))
						|| "alt".equalsIgnoreCase(startTag.getAttributeValue("img"))
						|| "themes_postings_module".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "afterhead".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "breadcrumb".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "adsd_billboard_div".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "sitehead".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "footer".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "userDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "postingDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "Ads_TFM_BS".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "Ads_TFM_*".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "viewModeDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
						|| "userbar".equalsIgnoreCase(startTag.getAttributeValue("id"))
						;
				}
			}.toString();
	}
	
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
		// 2nd. the url contains community and
		// 3rd. the page source code has class-tags "posting" in it
		// if all conditions are met, this is the right parser for this site
		boolean iAmTheOne = false;
		int hitRatio = 0;
		
		// we know, that discussion contains posting tags, so we give 5 points 
		if (url.getHost().contains("wallstreet-online")) hitRatio += 3;
		if (url.getPath().contains("community")) hitRatio += 3;
		
		Source source=new Source(page);
		source.fullSequentialParse();
		
		List<Element> siteElements = source.getAllElementsByClass("page");
		if (siteElements.size() > 0)
			hitRatio += 1;
		
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
