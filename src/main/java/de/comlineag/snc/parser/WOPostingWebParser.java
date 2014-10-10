package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

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
 * @version		0.2				- 09.10.2014
 * @status		in development
 * 
 * @description WOPostingWebParser is the implementation of the generic web parser for 
 * 				wallstreet-online discussion web sites.
 * 				It tries to get the relevant content out of a given website and calls the 
 * 				persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		created as extraction fromSimpleWebParser version 0.7
 * 				0.2				implemented canExecute method
 * 
 * TODO 2 implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO 3 extract user information from the website
 * 
 */
public final class WOPostingWebParser extends GenericWebParser implements IWebParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public WOPostingWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WOPostingWebParser(String page, URL url, ArrayList<String> tTerms) {
		parse(page, url, tTerms);
	}
	
	// multi threading implementation
	public void run() {
		// TODO implement run method for multi threaded parsing
	}
	@Override
	public Object execute(String page, URL url) {
		ExecutorService persistenceExecutor = Executors.newFixedThreadPool(RuntimeConfiguration.getPERSISTENCE_THREADING_POOL_SIZE());
		
		// TODO implement execute-method tomake parser thread save
		return null;
	}
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens) {
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// log the startup message
		logger.info("Wallstreet Online Postings parser START for url " + url.toString());
		
		JSONObject parsedPageJson = null;
		try {
			parsedPageJson = extractContent(page, url, tokens);
			SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(parsedPageJson);
			
			//logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			// now check if we really really have the searched word within the text and only if so,
			// write the content to disk. We should probably put this before calling the persistence
			if (findNeedleInHaystack(parsedPageJson.toString(), tokens)){
				// add the parsed site to the message list for saving in the DB
				postings.add(parsedPageSimpleWebPosting);
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		
		logger.info("Wallstreet Online Postings parser END\n");
		return postings;
	}
	
	
	
	
	
	
	// START THE SPECIFIC PARSER
	/**
	 * @description	parses a given html-site and extract any tries to get rid of all the clutter surrounding
	 * 				the interesting main content of it
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
	protected JSONObject extractContent(String page, URL url, List<String> tokens) {
		logger.info("parsing site " + url.toString() + " and extracting postings");
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		String plainText = null;
		boolean truncated = Boolean.parseBoolean("false");
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			/**
			 * This text extractor is specific to discussions on the Wallstreet Online sites. It has a positive 
			 * (aka white list) list of tags and markup elements, we want to get extract, while implicitely 
			 * discarding anything else.
			 */
			// the post itself
			TextExtractor woDiskussionSitePostingExtractor=new TextExtractor(source) {
				public boolean includeElement(StartTag startTag) {
					return "posting".equalsIgnoreCase(startTag.getAttributeValue("class"));
					}
				};
			
			// the writer of the post
			TextExtractor woDiskussionSitePostingUserExtractor=new TextExtractor(source) {
				public boolean includeElement(StartTag startTag) {
					return "avatar".equalsIgnoreCase(startTag.getAttributeValue("class"));
					}
				};
			
			plainText = woDiskussionSitePostingExtractor.setIncludeAttributes(true).toString();
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			// now put the reduced text in the original text variable, so that it gets added to the json below
			text = plainText;
			// and also make sure that the truncated flag is set correctly
			truncated = Boolean.parseBoolean("true");
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parsing of site content ", e );
			e.printStackTrace();
		}
		
		JSONObject pageJson = createPageJsonObject(title, description, plainText, text, url, truncated);
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
	
	@SuppressWarnings("unchecked")
	protected JSONObject createPageJsonObject(String title, String description, String page, String text, URL url, Boolean truncated){
		JSONObject pageJson = new JSONObject();
		truncated = Boolean.parseBoolean("false");
		
		// put some data in the json
		pageJson.put("sn_id", "WC"); // TODO implement proper sn_id handling for websites
		pageJson.put("page_id", UniqueIdServices.createId(url.toString()).toString()); // the url is parsed and converted into a long number (returned as a string)
		
		pageJson.put("subject", title);
		pageJson.put("teaser", description);
		pageJson.put("raw_text", page);
		pageJson.put("text", text);
		pageJson.put("source", url.toString());
		pageJson.put("lang", "DE"); // TODO implement language recognition
		pageJson.put("truncated", truncated);
		String s = Objects.toString(System.currentTimeMillis(), null);
		pageJson.put("created_at", s);
		
		pageJson.put("user_id", "0"); // TODO find a way to extract user information from page
		pageJson.put("user_id", pageJson.get("page_id"));
		
		JSONObject userJson = new JSONObject();
		userJson.put("sn_id", "WC"); // TODO implement proper sn_id handling for users from websites
		userJson.put("id", pageJson.get("page_id"));
		userJson.put("username", url.getHost());
		
		
		pageJson.put("user", userJson);
		
		logger.trace("the json object:: " + pageJson.toJSONString());
		return pageJson;
	}
	
	@Override
	protected Boolean parse(String page) {logger.warn("method not implemented");return false;}
	@Override
	protected Boolean parse(InputStream is) {logger.warn("method not implemented");return false;}
}

