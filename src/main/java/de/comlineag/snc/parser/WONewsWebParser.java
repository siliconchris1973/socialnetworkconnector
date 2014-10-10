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
 * @version		0.1				- 09.10.2014
 * @status		in development
 * 
 * @description WONewsWebParser is the implementation of the generic web parser for 
 * 				wallstreet-online news web sites.
 * 				It tries to get the relevant content out of a given website and calls the 
 * 				persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		created as extraction from WOPostingWebParser version 0.2
 * 
 * TODO 2 implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO 3 extract user information from the website
 * 
 */
public final class WONewsWebParser extends GenericWebParser implements IWebParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public WONewsWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WONewsWebParser(String page, URL url, ArrayList<String> tTerms) {
		parse(page, url, tTerms);
	}
	

	// multi threading implementation
	public void run() {
		// TODO implement run method for multi threaded parsing
	}
	@Override
	public Object execute(String page, URL url) {
		ExecutorService persistenceExecutor = Executors.newFixedThreadPool(RuntimeConfiguration.getPERSISTENCE_THREADING_POOL_SIZE());
		
		// TODO implement execute-method to make parser thread save
		return null;
	}
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens) {
		// log the startup message
		logger.info("Wallstreet Online News parser START for url " + url.toString());
				
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		String plainText = "";
		boolean truncated = Boolean.parseBoolean("false");
		
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			// first we set some preliminary data for the json object
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			List<Element> siteElements = source.getAllElementsByClass("container");
			for (int i=0;i<siteElements.size();i++) {
				List<Element> subElements = siteElements.get(i).getAllElements("id", "newsArticle", false);
				for (int ii=0;ii<subElements.size();ii++) {
					plainText = new TextExtractor(subElements.get(ii)) {
						public boolean includeElement(StartTag startTag) {
							return "content".equalsIgnoreCase(startTag.getAttributeValue("class"));
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
									|| "newsArticleIdentity".equalsIgnoreCase(startTag.getAttributeValue("id"))
									|| "userbar".equalsIgnoreCase(startTag.getAttributeValue("id"));
							}
						}.toString();
					//logger.trace("the plaintext >>> " + plainText);
					text = plainText;
					
					JSONObject pageJson = createPageJsonObject(title, description, plainText, text, url, truncated);
					SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(pageJson);
					// now check if we really really have the searched word within the text and only if so,
					// write the content to disk. We should probably put this before calling the persistence
					if (findNeedleInHaystack(pageJson.toString(), tokens)){
						// add the parsed site to the message list for saving in the DB
						postings.add(parsedPageSimpleWebPosting);
					}
				}
			}
			
			//logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
			e.printStackTrace();
		}
		
		
		logger.info("Wallstreet Online News parser END\n");
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
		// there are two indication whether the parser can work with this page:
		// 1st. the url contains diskussion or forum and
		// 2nd. the page source code has class-tags "posting" in it
		// if both conditions are met, this is the right parser for this site
		boolean iAmTheOne = false;
		int hitRatio = 0;
		int elementCount = 0;
		
		// we know, that discussion contains posting tags, so we give 5 points 
		if (url.getHost().contains("wallstreet-online")) hitRatio += 3;
		if (url.getPath().contains("nachricht")) hitRatio += 3;
		
		Source source=new Source(page);
		source.fullSequentialParse();
		
		List<Element> siteElements = source.getAllElementsByClass("container");
		for (int i=0;i<siteElements.size();i++) {
			List<Element> subElements = siteElements.get(i).getAllElements("id", "newsArticle", false);
			for (int ii=0;ii<subElements.size();ii++) {
				elementCount += 1;
				logger.trace("#" +elementCount+ " container element(s) found");
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
		userJson.put("name", url.getHost());
		userJson.put("screen_name", url.getHost());
		userJson.put("lang", "DE"); // TODO implement language recognition
		
		
		pageJson.put("user", userJson);
		
		logger.trace("the json object:: " + pageJson.toJSONString());
		return pageJson;
	}
	
	@Override
	protected Boolean parse(String page) {logger.warn("method not implemented");return false;}
	@Override
	protected Boolean parse(InputStream is) {logger.warn("method not implemented");return false;}
}

