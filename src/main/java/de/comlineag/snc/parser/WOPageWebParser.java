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
 * @version		0.1				- 06.10.2014
 * @status		beta
 * 
 * @description SimpleWebParser is the implementation of the generic web parser for web sites.
 * 				It tries to get the relevant content out of a given website and calls the 
 * 				persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		created as extraction fromSimpleWebParser version 0.7
 * 
 * TODO 1 implement correct threaded parser to aid in multithreading
 * TODO 3 implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * 
 */
public final class WOPageWebParser extends GenericWebParser implements IWebParser, Runnable {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// TODO can I put initialization of executor service someplace else? Maybe in the execute method?
	ExecutorService persistenceExecutor = Executors.newFixedThreadPool(RuntimeConfiguration.getPERSISTENCE_THREADING_POOL_SIZE());
	
	public WOPageWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WOPageWebParser(String page, URL url, ArrayList<String> tTerms) {
		parse(page, url, tTerms);
	}
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens) {
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// log the startup message
		logger.info("Wallstreet Online Page parser START for url " + url.toString());
		
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
		
		
		
		
		/* invoke the persistence layer - should go in crawler
		for (int ii = 0; ii < postings.size(); ii++) {
			logger.info("calling persistence layer to save the post from site " + url.toString());
				SimpleWebPosting post = postings.get(ii);
				post.save();
			
		}
		*/
		logger.info("Wallstreet Online Page Web parser END\n");
		return postings;
	}
	
	
	
	
	
	
	// START THE SPECIFIC PARSER
	/**
	 * @description	parses a given html-site and tries to get rid of all the clutter surrounding
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
	@Override
	protected JSONObject extractContent(String page, URL url, List<String> tokens) {
		logger.info("parsing site " + url.toString() + " and removing clutter");
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		boolean truncated = Boolean.parseBoolean("false");
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			/**
			 * This text extractor is specific to generic Wallstreet Online sites. It has a negative
			 * list of tags and markup elements, we want to get rid of.
			 */
			TextExtractor woGenericSiteTextExtractor=new TextExtractor(source) {
				public boolean excludeElement(StartTag startTag) {
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
							|| "fs grid".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "grid fs ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "grid fs static ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "grid fs poll ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "grid ws threadbodybox ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "grid ns ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "modulecontent copyright".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "more_link".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "tbutton b".equalsIgnoreCase(startTag.getAttributeValue("class"))
							//|| "tab wotabs".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "voting_stars_display".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "pagination".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "tab_wrapper".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "pagination ".equalsIgnoreCase(startTag.getAttributeValue("class"))
							|| "overflow:hidden".equalsIgnoreCase(startTag.getAttributeValue("style"))
							|| "pagination".equalsIgnoreCase(startTag.getAttributeValue("form"))
							|| "themes_postings_module".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "afterhead".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "breadcrumb".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "adsd_billboard_div".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "sitehead".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "footer".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "userDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "postingDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "Ads_TFM_BS".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "viewModeDD".equalsIgnoreCase(startTag.getAttributeValue("id"))
							|| "userbar".equalsIgnoreCase(startTag.getAttributeValue("id"));
					}
				};
			
				String plainText = woGenericSiteTextExtractor.setIncludeAttributes(true).toString();
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
		
		JSONObject pageJson = createPageJsonObject(title, description, page, text, url, truncated);
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
	
	
	
	// multi threading implementation
	@Override
	public void run() {
		// TODO implement run method for multi threaded parsing
	}
	@Override
	public Object execute(String page, URL url) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean canExecute(URL url) {
		// TODO Auto-generated method stub
		return false;
	}
	

	@Override
	protected Boolean parse(String page) {logger.warn("method not impleented");return false;}
	@Override
	protected Boolean parse(InputStream is) {logger.warn("method not impleented");return false;}
	

	@SuppressWarnings("unchecked")
	protected JSONObject createPageJsonObject(String title, String description, String page, String text, URL url, Boolean truncated){
		JSONObject pageJson = new JSONObject();
		truncated = Boolean.parseBoolean("false");
		
		// put some data in the json
		pageJson.put("subject", title);
		pageJson.put("teaser", description);
		pageJson.put("raw_text", page);
		pageJson.put("text", text);
		pageJson.put("source", url.toString());
		pageJson.put("page_id", UniqueIdServices.createId(url.toString()).toString()); // the url is parsed and converted into a long number (returned as a string)
		pageJson.put("lang", "DE"); // TODO implement language recognition
		pageJson.put("truncated", truncated);
		String s = Objects.toString(System.currentTimeMillis(), null);
		pageJson.put("created_at", s);
		
		pageJson.put("user_id", "0"); // TODO find a way to extract user information from page
		//JSONObject userJson = new JSONObject();
		//userJson.put("id", "0");
		//pageJson.put("user", userJson);
		return pageJson;
	}
}

