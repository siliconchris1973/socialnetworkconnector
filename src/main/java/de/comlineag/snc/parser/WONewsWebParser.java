package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
 * 				It tries to get the relevant content out of a given wo news-site and returns a 
 * 				list of WebPosting objects with extracted page content to the crawler
 * 
 * @changelog	0.1 (Chris)		created as extraction from WOPostingWebParser version 0.2
 * 
 * TODO implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * TODO extract user information from the website
 * 
 */
public final class WONewsWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	
	private final boolean rtcGetOnlyRelevantPages = rtc.getBooleanValue("WcGetOnlyRelevantPages", "crawler");
	//private final int rtcWordDistanceCutoffMargin = rtc.getIntValue("WcWordDistanceCutoffMargin", "crawler");
	//private final boolean rtcPersistenceThreading = rtc.getBooleanValue("PersistenceThreadingEnabled", "runtime");
	private final int rtcPersistenceThreadingPoolSize = rtc.getIntValue("PersistenceThreadingPoolSize", "runtime");
	
	
	public WONewsWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public WONewsWebParser(String page, URL url, ArrayList<String> tTerms, String sn_id, String curCustomer, String curDomain) {
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
		String PARSER_NAME="Wallstreet Online News";
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
		String user_name = "";
		String screen_name = "";
		String user_id = "0";
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
			logger.trace("extracted description {} and keywords {}", description, keywords.toString());
			
			List<Element> siteElements = source.getAllElementsByClass("container");
			logger.debug("received {} site elements", siteElements.size());
			for (int i=0;i<siteElements.size();i++) {
				List<Element> subElements = siteElements.get(i).getAllElements("id", "newsArticle", false);
				logger.debug("received {} sub elements", subElements.size());
				for (int ii=0;ii<subElements.size();ii++) {
					logger.trace("working on sub element {} as {}...", i, subElements.get(ii).toString().substring(0, 20));
					
					plainText = getPlainText(subElements.get(ii));
					text = plainText;
					
					logger.trace("Truncated text >>> " + text);
					
					page_id = UniqueIdServices.createMessageDigest(text);
					user_name = url.toString();
					screen_name = user_name;
					user_id = UniqueIdServices.createMessageDigest(user_name);
					long s = System.currentTimeMillis();
					// converting to 06.11.14 17:28:37
					Date date = new Date(s);
					DateFormat formatter = new SimpleDateFormat("dd.MM.YY HH:mm:ss");
					created_at = formatter.format(date);
					
					logger.debug("no explicit post information on site, using {} as page_id", page_id);
					logger.debug("no explicit user information on site, using {} as user_id and {} as name", user_id, user_name);
					logger.debug("no explicit time information on site, using {} for created_at value", created_at);
					
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
			}
			
			//logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
			e.printStackTrace();
		}
		
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds for " + pageSize +"Kb");
		return postings;
	}
	
	
	// start of some jericho magic methods
	/**
	 * takes a site element and returns the plain text as a combination of allowed and disallowed site tags
	 * @param siteElement
	 * @return plainText
	 */
	private String getPlainText(Element element) {
		return new TextExtractor(element) {
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
	
	@Override
	protected boolean parse(String page) {logger.warn("method not implemented");return false;}
	@Override
	protected boolean parse(InputStream is) {logger.warn("method not implemented");return false;}
}

