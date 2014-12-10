package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import de.comlineag.snc.helper.UniqueIdServices;



/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.9a			- 09.10.2014
 * @status		beta
 * 
 * @description SimpleWebParser is the simplest implementation of the generic web parser for web sites.
 * 				It retrieves a number of words (currently 30) before and after the given track term from
 * 				the site and calls the persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		first skeleton
 * 				0.2				try and error with jericho
 * 				0.3				implemented boilerpipe
 * 				0.4				removed boilerpipe
 * 				0.5				rewritten jericho implementation
 * 				0.6				implemented my own parser based on jericho
 * 				0.7				added boolean return value for method parse 
 * 				0.8				removed Wallstreet Online specific implementation 
 *				0.9				implemented productive code to get substring of a page
 *								around the searched track terms 
 *				0.9a			moved helkper methods returnTokenPosition and trimStringAtPosition
 *								into GenericWebParser as it is also neede for other web parser
 * 
 * TODO implement correct threaded parser to aid in multithreading
 * TODO implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * 
 */
public final class SimpleWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	
	private final boolean rtcGetOnlyRelevantPages = rtc.getBooleanValue("WcGetOnlyRelevantPages", "crawler");
	private final int rtcWordDistanceCutoffMargin = rtc.getIntValue("WcWordDistanceCutoffMargin", "crawler");
	
	
	public SimpleWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public SimpleWebParser(String page, URL url, ArrayList<String> tTerms, String sn_id, String curCustomer, String curDomain) {
		parse(page, url, tTerms, sn_id, curCustomer, curDomain);
	}
	
	
	@Override
	public List<WebPosting> parse(String page, URL url, List<String> tokens, String sn_id, String curCustomer, String curDomain) {
		String PARSER_NAME="SimpleWebParser";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START for url " + url.toString());
		
		JSONObject parsedPageJson = null;
		List<WebPosting> postings = new ArrayList<WebPosting>();
		
		try {
			parsedPageJson = extractContent(page, url, tokens, sn_id, curCustomer, curDomain);
			WebPosting parsedPageSimpleWebPosting = new WebPosting(parsedPageJson);
			
			logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			// now check if we really really have the searched word within the text and only if so,
			// write the content to disk. We should probably put this before calling the persistence
			if (!rtcGetOnlyRelevantPages || findNeedleInHaystack(parsedPageJson.toString(), tokens)){
				// add the parsed site to the message list for saving in the DB
				postings.add(parsedPageSimpleWebPosting);
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds");
		return postings;
	}
	
	
	
	
	
	
	// START THE SPECIFIC PARSER
	/**
	 * @description	parses a given html-site, removes all header information and extracts 30 words before and after 
	 * 				the track term.
	 * 
	 * @param		page 	- the page to parse as a string containing the html sourcecode
	 * @param		url		- the url to the site
	 * @param		tokens	- a list of tokens we searched for when finding this page
	 * @return		json	- a json object containing the following fields:
	 * 						  text = the plain text of the main content of the site
	 * 						  raw_text = the raw html markup sourcecode
	 * 						  title = the page title
	 * 						  description = the meta tag description of the page
	 * 						  truncated = a boolean field indicating whether the page was truncated or not - usually true
	 * 						  source = the url to the site
	 * 						  created_at = a time value of the millisecond the page was parsed
	 * 						  page_id = a long value created from the url by substituting every character to a number
	 * 						  user_id = 0 
	 */
	protected JSONObject extractContent(String page, URL url, List<String> tokens, String sn_id, String curCustomer, String curDomain) {
		logger.trace("parsing site " + url.toString() + " and removing clutter");
		String title = null;
		String description = null;
		String keywords = null;
		String created_at = null;
		String text = null;
		String plainText = null;
		String pageLang = "DE";
		String user_name = null;
		String screen_name = null;
		String page_id = null;
		String master_page_id = null;
		String referer_page_id = null;
		String user_id = null;
		String userLang = pageLang;
		long postings_count = 0;
		boolean truncated = Boolean.parseBoolean("false");
		
		// vars for the token extraction
		int lowBorderMark = 300;
		int highBorderMark = 300;
		ArrayList<Integer> positions = new ArrayList<Integer>();
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			TextExtractor genericSiteTextExtractor = new TextExtractor(source) {
				public boolean excludeElement(StartTag startTag) {
					return startTag.getName()==HTMLElementName.TITLE
							|| startTag.getName()==HTMLElementName.THEAD
							|| startTag.getName()==HTMLElementName.SCRIPT
							|| startTag.getName()==HTMLElementName.HEAD
							|| startTag.getName()==HTMLElementName.META;
				}
			};
			
			
			plainText = genericSiteTextExtractor.setIncludeAttributes(true).toString();
			//String plainText = aBigPlainText();
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			
			/* uses a combination of returnTokenPosition and trimStringAtPosition
			 *   - works! returns exactly one string if the words are near to each other
			 *     and multiple concatinated strings in case the words are far apart
			 */      
			for (int i=0; i < tokens.size(); i++) {
				String token = tokens.get(i);
				logger.trace("working on token " + token);
				
				positions = returnTokenPosition(plainText, token, positions, lowBorderMark, highBorderMark);
			}
			
			String segmentText = "";
			for (int i=0;i<positions.size();i++) {
				int position = positions.get(i);
				segmentText += trimStringAtPosition(plainText, position, 
													rtcWordDistanceCutoffMargin, 
													rtcWordDistanceCutoffMargin);
			}
			
			// now put the reduced text in the original text variable, so that it gets added to the json below
			text = segmentText;
			
			logger.trace("Truncated text: >>> " + text);
			
			// and also make sure that the truncated flag is set correctly
			logger.trace("plainText was " + plainText.length() + " and extracted is " + text.length());
			if (plainText.length() > text.length()) {
				truncated = Boolean.parseBoolean("true");
			} else {
				truncated = Boolean.parseBoolean("false");
			} 
			
			user_name = url.getHost().toString();
			screen_name = user_name;
			page_id = UniqueIdServices.createMessageDigest(text);
			user_id = UniqueIdServices.createMessageDigest(user_name);
			long s = System.currentTimeMillis();
			// converting to 06.11.14 17:28:37
			Date date = new Date(s);
			DateFormat formatter = new SimpleDateFormat("dd.MM.YY HH:mm:ss");
			created_at = formatter.format(date);
						
			logger.debug("no explicit post information on site, using {} as page_id", page_id);
			logger.debug("no explicit user information on site, using {} as user_id and {} as name", user_id, user_name);
			logger.debug("no explicit time information on site, using {} for created_at value", created_at);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parsing of site content ", e );
			e.printStackTrace();
		}
		
		JSONObject pageJson = createPageJsonObject(sn_id, title, description, plainText, text, created_at, url, truncated, pageLang, page_id, user_id, user_name, screen_name, userLang, postings_count, curCustomer, curDomain);
		
		return pageJson;
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
	public Object execute(String page, URL url) {
		// TODO implement execute method in SompleWebParser to make it thread save
		return null;
	}
	
	@Override
	public boolean canExecute(String page, URL url) {
		// the simple parser will work on any page - thus returning true
		return true;
	}
	
	@Override
	protected boolean parse(String page) {logger.warn("method not impleented");return false;}
	@Override
	protected boolean parse(InputStream is) {logger.warn("method not impleented");return false;}
}

