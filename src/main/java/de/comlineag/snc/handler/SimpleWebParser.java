package de.comlineag.snc.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.helper.UniqueIdServices;



/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.1			- 25.07.2014
 * @status		in development
 * 
 * @description SimpleWebParser is the implementation of the generic web parser for web sites.
 * 				It tries to get the relevant content out of a given website and calls the 
 * 				persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		first skeleton
 * 
 */
public final class SimpleWebParser extends GenericWebParser implements Runnable {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	// TODO can I put initialization of executor service someplace else? Maybe in the execute method?
	ExecutorService persistenceExecutor = Executors.newFixedThreadPool(RuntimeConfiguration.getPERSISTENCE_THREADING_POOL_SIZE());
	
	public SimpleWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public SimpleWebParser(String page, URL url, ArrayList<String> tTerms) {
		parse(page, url, tTerms);
	}
	
	@Override
	protected void parse(String page) {logger.warn("method not impleented");}

	@Override
	protected void parse(InputStream is) {logger.warn("method not impleented");}
	
	@Override
	public void parse(String page, URL url, List<String> tokens) {
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// log the startup message
		logger.info("Simple Web parser START");
		
		JSONObject parsedPageJson = null;
		try {
			parsedPageJson = parseAndCreateJsonFromPage(page, url, tokens);
			SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(parsedPageJson);
			// now check if we really really have the searched word within the text and only if so,
			// write the content to disk. We should probably put this before calling the persistence
			if (containsWord(parsedPageSimpleWebPosting.toString(), tokens)){
				String fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_myOwnParser.txt";
				try {
					writeContentToDisk(url, fileName, parsedPageSimpleWebPosting.toString());
					
				} catch (Exception e) {
					logger.error("could not write page to disk ", e);
				}
				
				postings.add(parsedPageSimpleWebPosting);
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		
		for (int ii = 0; ii < postings.size(); ii++) {
			logger.info("calling persistence layer to save the post");
			if (RuntimeConfiguration.isPERSISTENCE_THREADING_ENABLED()){
				// execute persistence layer in a new thread, so that it does NOT block the crawler
				logger.debug("execute persistence layer in a new thread...");
				final SimpleWebPosting postT = (SimpleWebPosting) postings.get(ii);
				/* TODO find out how to use executor Service for multi threaded persistence call
				if (!persistenceExecutor.isShutdown()) {
					persistenceExecutor.submit(new Thread(new Runnable() {
							
							@Override
							public void run() {
									postT.save();
							}
						}).start());
				}
				*/
				new Thread(new Runnable() {
					
					@Override
					public void run() {
							postT.save();
					}
				}).start();
				
			} else {
				// otherwise just call it sequentially
				SimpleWebPosting post = (SimpleWebPosting) postings.get(ii);
				post.save();
			}
		}
		
		
		logger.info("Simple Web parser END\n");
	}
	
	
	
	
	
	
	// START OF PARSER SPECIFIC
	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject parseAndCreateJsonFromPage(String page, URL url, List<String> tokens) {
		logger.info("parsing site " + url.toString() + " and removing clutter");
		JSONObject pageJson = new JSONObject();
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		boolean truncated = Boolean.parseBoolean("false");
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			
			TextExtractor textExtractor=new TextExtractor(source) {
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
			String plainText = textExtractor.setIncludeAttributes(true).toString();
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			//logger.trace("Plaintext: >>> " + plainText);
			/*
			String segmentText = "";
			for (int i=0; i < tokens.size(); i++) {
				String token = tokens.get(i);
				ArrayList<Integer> indexes = returnTokenPosition(plainText, token);
				// did we find the track term in question???
				if (indexes.size() > 0) {
					for (int ii=0; ii<indexes.size(); ii++) {
						logger.trace(">>> found word " + token + " at position " + indexes.get(ii) + " in url " + url.toString());
						segmentText += trimStringAtWordBoundary(plainText, 
													RuntimeConfiguration.getWC_WORD_DISTANCE_CUTOFF_MARGIN(), 
													RuntimeConfiguration.getWC_WORD_DISTANCE_CUTOFF_MARGIN(), 
													token);
						logger.trace("TruncatedText: >>> " + segmentText);
					}
				}
			}
			*/
			// now put the reduced text in the original text variable, so that it gets added to the json below
			text = plainText;
			// and also make sure that the truncated flag is set correctly
			truncated = Boolean.parseBoolean("true");
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parsing of site content ", e );
			e.printStackTrace();
		}
		
		
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
	
	
	
	
	
	/**
	 * @description checks if ANY word of a given set of tokens is found in the given text
	 * @param 		haystack
	 * @param 		needle
	 * @return 		true if any of the tokens was found, otherwise false
	 * 
	 */
	private boolean containsWord(String haystack, List<String> needle) throws NullPointerException {
		assert (haystack != null && needle != null) : "ERROR :: cannot operate on empty text";
		
		String patternString = "\\b(" + StringUtils.join(needle, "|") + ")\\b";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(haystack);

		while (matcher.find()) {
		    logger.trace("found the token " + matcher.group(1));
		    return true;
		}
		
		return false;
	}
	
	/**
	 * @description	gets a page (or any other text), a token to look for and returns a substring with 
	 * 				given number of words before and after the given token 
	 * @param 		original page
	 * @param 		wordsBefore
	 * @param 		wordsAfter
	 * @param 		token
	 * @return 		trimmed text
	 */
	private String trimStringAtWordBoundary(String haystack, int wordsBefore, int wordsAfter, String needle) {
		if(haystack == null || haystack.trim().isEmpty()){
			return haystack ;
		}
		
		String textsegments = "";
		
		String patternString = "((?:[a-zA-Z'-]+[^a-zA-Z'-]+){0,"+wordsBefore+"}\\b)" + needle + "(\\b(?:[^a-zA-Z'-]+[a-zA-Z'-]+){0,"+wordsAfter+"})";
		//String patternString = "((?:[a-zA-Z'-]+[^a-zA-Z'-]+){0,5}\b)needle(\b(?:[^a-zA-Z'-]+[a-zA-Z'-]+){0,5})";
		
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(haystack);
		
		while(matcher.find()){
			logger.trace(">>> found you between " + matcher.regionStart() + " and " + matcher.regionEnd());
			//textsegments += m.group(); //.group(1);
			String segText = matcher.group(1);
			textsegments += segText + "...";
		}
		
		return textsegments;
	}
	
	/**
	 * @description	writes the content of a page to disk 
	 * @param 		fileName
	 * @param 		content
	 * @return 		true on success and false on error
	 * @throws 		IOException
	 */
	private boolean writeContentToDisk(URL url, String fileName, String content) throws IOException {
		File f1 = new File(fileName);
		if (!f1.isFile() || f1.getTotalSpace()<1) {
			//FileWriter rawFile;
			FileWriter f2 = null;
			
			try {
				// write content in file
				f2 = new FileWriter("storage"+System.getProperty("file.separator")+"websites"+System.getProperty("file.separator")+fileName);
				f2.write(""+url.toString() +"");
				f2.write(dataCryptoProvider.encryptValue(content));
				f2.flush();
				f2.close();
			} catch (GenericCryptoException e) {
				logger.error("ERROR :: could not encrypt data prior writing the file ", e);
				f2.close();
				return false;
			}
		}
		return true;
	}
	
	
	
	
	
	// mult threading implementation
	@Override
	public void run() {
		// TODO Auto-generated method stub
	}
}

class ThreadedParser implements Runnable {
	// we use simple org.apache.log4j.Logger for lgging
	//private final Logger logger = Logger.getLogger(getClass().getName());
	private final Logger logger = Logger.getLogger("de.comlineag.snc.handler.SimpleWebParser.ThreadedParser");
		
	private int i;
	private JSONObject parsedPageJson = null;
	private String page;
	
	ThreadedParser(JSONObject parsedPageJson, String page) {
		this.parsedPageJson = parsedPageJson;
		this.page = page;
	}

	public void run() {
		logger.info("decoding page in a thread");
	}
}

/*
			String fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_jericho1.html";
			String text = source.getTextExtractor().setIncludeAttributes(true).toString();
			TextExtractor textExtractor=new TextExtractor(source) {
				public boolean excludeElement(StartTag startTag) {
					return startTag.getName()==HTMLElementName.P || startTag.getName()==HTMLElementName.DIV || "control".equalsIgnoreCase(startTag.getAttributeValue("class"));
				}
			};
			if (text != null && tokens != null) {
				if (containsWord(text, tokens)){
					logger.debug("working on page " + title + " (" + description + ")");
					
					try {
						writeContentToDisk(url, fileName, text);
					} catch (Exception e) {
						logger.error("could not hand page to persistence layer ", e);
					}
				}
			} else {
				logger.error("ERROR :: could not check for track terms in page. Either track terms or text is null");
			}
			
			
			text = textExtractor.setIncludeAttributes(true).toString();
			fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_jericho2.html";
			if (containsWord(text, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				
				try {
					writeContentToDisk(url, fileName, text);
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}
			
			
 			// Boilerpipe is a content extraction library for html pages 
			// We use it to get the relevant content from the page and only store that
			// please see http://boilerpipe-web.appspot.com for a short demo on that
			String text1 = null;
			String text2 = null;
			String text3 = null;
			String text4 = null;
			try {
				
				//text1 = ArticleExtractor.INSTANCE.getText(sanitisedPage);
				//text2 = DefaultExtractor.INSTANCE.getText(sanitisedPage);
				//text3 = LargestContentExtractor.INSTANCE.getText(sanitisedPage);
				//text4 = ArticleSentencesExtractor.INSTANCE.getText(sanitisedPage);
				
				text1 = ArticleExtractor.INSTANCE.getText(page);
				text2 = DefaultExtractor.INSTANCE.getText(page);
				text3 = LargestContentExtractor.INSTANCE.getText(page);
				text4 = ArticleSentencesExtractor.INSTANCE.getText(page);
				
			} catch (BoilerpipeProcessingException e1) {
				logger.error("could not process page ", e1);
				//e1.printStackTrace();
			}
			
			
			// DEBUG DEBUG DEBUG - remove in production
			if (containsWord(text1, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_article.html";
				try {
					writeContentToDisk(url, fileName, text1);
					
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}
			if (containsWord(text2, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_default.html";
				try {
					// MOVE THIS TO THE PROPER LOCATION WHEN READY
					pageJson.put("text", text2);
					// MOVE THIS TO THE PROPER LOCATION WHEN READY
					writeContentToDisk(url, fileName, text2);
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}	
			if (containsWord(text3, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_largest.html";
				try {
					writeContentToDisk(url, fileName, text3);
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}	
			if (containsWord(text4, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_sentences.html";
				try {
					writeContentToDisk(url, fileName, text4);
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}	
			// DEBUG DEBUG DEBUG - remove in production 
 */
