package de.comlineag.snc.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.TextExtractor;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.helper.UniqueIdServices;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ArticleSentencesExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.LargestContentExtractor;


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
public final class SimpleWebParser extends GenericWebParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	public SimpleWebParser() {}

	@Override
	protected void parse(String page) {}

	@Override
	protected void parse(InputStream is) {}
	
	@Override
	public void parse(String page, URL url, List<String> tokens) {
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// log the startup message
		logger.debug("Simple Web parser START");
		
		JSONObject parsedPageJson = null;
		try {
			parsedPageJson = parseAndCreateJsonFromPage(page, url, tokens);
			SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(parsedPageJson);
			postings.add(parsedPageSimpleWebPosting);
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		
		for (int ii = 0; ii < postings.size(); ii++) {
			SimpleWebPosting post = (SimpleWebPosting) postings.get(ii);
			post.save();
		}
		
		
		logger.debug("Simple Web parser END\n");
	}
	
	// START OF PARSER SPECIFIC
	@SuppressWarnings("unchecked")
	private JSONObject parseAndCreateJsonFromPage(String page, URL url, List<String> tokens) {
		JSONObject pageJson = new JSONObject();
		
		try {
			// Jericho is a library to parse html pages and identify the parts of it.
			// we use it to clean the html content prior feeding it to boilerpipe 
			// and also so set some arbritary data (like title and teaser)
			Source source = new Source(page);
			source.fullSequentialParse();
			Segment htmlSeg = new Segment(source, 0, page.length());
			Renderer htmlRend = new Renderer(htmlSeg);
		    String sanitisedPage = htmlRend.toString();
		    
			String title=getTitle(source);
			String description=getMetaValue(source,"description");
			String keywords=getMetaValue(source,"keywords");
			
			// put some data in the json
			pageJson.put("subject", title);
			pageJson.put("teaser", description);
			pageJson.put("raw_text", page);
			pageJson.put("source", url.toString());
			pageJson.put("page_id", UniqueIdServices.createId(url.toString()).toString()); // the url is parsed and converted into a long number (returned as a string)
			pageJson.put("lang", "DE"); // TODO implement language recognition
			boolean boolean2 = Boolean.parseBoolean("true");
			pageJson.put("truncated", boolean2);
			String s = Objects.toString(System.currentTimeMillis(), null);
			pageJson.put("created_at", s);
			
			pageJson.put("user_id", "0"); // TODO find a way to extract user information from page
			//JSONObject userJson = new JSONObject();
			//userJson.put("id", "0");
			//pageJson.put("user", userJson);
			
			
			
			// Boilerpipe is a content extraction library for html pages 
			// We use it to get the relevant content from the page and only store that
			// please see http://boilerpipe-web.appspot.com for a short demo on that
			String text1 = null;
			
			try {
				text1 = ArticleExtractor.INSTANCE.getText(page); // this can also be sanitisedPage
			} catch (BoilerpipeProcessingException e1) {
				logger.error("could not process page ", e1);
				//e1.printStackTrace();
			}
			
			if (containsWord(text1, tokens)){
				logger.debug("working on page " + title + " (" + description + ")");
				String fileName = url.toString().replaceAll("http://", "").replaceAll("/", "_")+"_article.html";
				try {
					writeContentToDisk(url, fileName, text1);
					
				} catch (Exception e) {
					logger.error("could not hand page to persistence layer ", e);
				}
			}
		    
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parsing of site content ", e );
			e.printStackTrace();
		}

		return pageJson;
		
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
	// END OF PARSER SPECIFIC
	
	
	/**
	 * @description 	checks if any word of a given set of tokens is found in the given text
	 * @param 			text
	 * @param 			tokens
	 * @return 			true if any of the tokens was found, otherwise false
	 */
	private boolean containsWord(String text, List<String> tokens) throws NullPointerException {
		assert (text != null && tokens != null) : "ERROR :: cannot operate on empty text";
		
		String patternString = "\\b(" + StringUtils.join(tokens, "|") + ")\\b";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(text);

		while (matcher.find()) {
		    logger.trace("found the token " + matcher.group(1));
		    return true;
		}
		
		return false;
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
				// and leaned content in it's own file
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
