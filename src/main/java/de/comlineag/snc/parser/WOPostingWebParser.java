package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @version		0.3				- 10.10.2014
 * @status		in development
 * 
 * @description WOPostingWebParser is the implementation of the generic web parser for 
 * 				wallstreet-online discussion web sites.
 * 				It tries to get the relevant content out of a given website and calls the 
 * 				persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		created as extraction fromSimpleWebParser version 0.7
 * 				0.2				implemented canExecute method
 * 				0.3				implemented posting and user handling
 * 
 * TODO 2 implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO 3 extract user information from the website
 * 
 */
public final class WOPostingWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
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
		ExecutorService persistenceExecutor = Executors.newFixedThreadPool(rtc.getPERSISTENCE_THREADING_POOL_SIZE());
		
		// TODO implement execute-method to make parser thread save
		return null;
	}
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens) {
		String PARSER_NAME="Wallstreet Online Discussion";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START for url " + url.toString());
		
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// a single page
		String sn_id = "WC"; // TODO implement proper social network id handling
		long page_id = 0;
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		String plainText = "";
		String page_lang = "DE"; // TODO implement proper language detection
		
		// the embedded user data
		String user_name = "";
		String screen_name = "";
		long user_id = 0;
		String user_lang = page_lang;
		int postings_count = 0;
		
		boolean truncated = Boolean.parseBoolean("false");
		
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			// first we set some preliminary data for the json object
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			List<Element> siteElements = source.getAllElementsByClass("posting");
			for (int i=0;i<siteElements.size();i++) {
				Element siteElement = siteElements.get(i).getFirstElementByClass("postingText");
				//plainText = siteElement.getContent().getTextExtractor().excludeElement(StartTag startTag){return "keywords".equalsIgnoreCase(startTag.getAttributeValue("span"));}.toString();
				
				page_id = (Long.valueOf((String) siteElements.get(i).getAttributeValue("data-id")).longValue());
				
				plainText = new TextExtractor(siteElement) {
					public boolean excludeElement(StartTag startTag){
						return "s8 replyTo".equalsIgnoreCase(startTag.getAttributeValue("class"))
								|| "postingHead".equalsIgnoreCase(startTag.getAttributeValue("class"))
								|| "postingFooter".equalsIgnoreCase(startTag.getAttributeValue("class"));
					}
				}.toString();
				
				
				//logger.trace("the plaintext >>> " + plainText);
				text = plainText;
				
				Element userElement = siteElements.get(i).getFirstElementByClass("meta");
				Element innerUserElement = userElement.getFirstElementByClass("user");
				user_name = innerUserElement.getContent().toString();
				screen_name = user_name;
				user_id = (Long.valueOf((String) innerUserElement.getAttributeValue("data-userid")).longValue());
				
				JSONObject pageJson = createPageJsonObject(sn_id, title, description, plainText, text, url, truncated, page_lang, page_id, user_id, user_name, screen_name, user_lang, postings_count);
				
				SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(pageJson);
				// now check if we really really have the searched word within the text and only if so,
				// write the content to disk. We should probably put this before calling the persistence
				if (findNeedleInHaystack(pageJson.toString(), tokens)){
					// add the parsed site to the message list for saving in the DB
					postings.add(parsedPageSimpleWebPosting);
				}
			}
			
			//logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
			e.printStackTrace();
		}
		
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds");
		return postings;
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
	protected Boolean parse(String page) {logger.warn("method not implemented");return false;}
	@Override
	protected Boolean parse(InputStream is) {logger.warn("method not implemented");return false;}
}

