package de.comlineag.snc.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.handler.SimpleWebPosting;


/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.4				- 06.10.2014
 * @status		productive
 * 
 * @description GenericWebParser is the abstract base class for web site parsing. It is derived from
 * 				generic parser and defines one new, abstract method parse with parameters for the
 * 				content, the url and the searched terms.
 * 
 * @changelog	0.1 (Chris)		first version
 * 				0.2				added method to return list of indices for the needle in the haystack
 * 				0.3				changed return value of parse to Boolean
 * 				0.4				introduced IWebParser Interface
 * 
 */
public abstract class GenericWebParser extends GenericParser implements IWebParser {
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	public GenericWebParser() {}

	public abstract List<SimpleWebPosting> parse(String page, URL url, List<String> tokens);
	
	protected abstract JSONObject extractContent(String page, URL url, List<String> tokens);
	
	
	
	
	
	
	/**
	 * @description checks if ANY word of a given set of tokens is found in the given text
	 * @param 		haystack
	 * @param 		needle
	 * @return 		true if any of the tokens was found, otherwise false
	 * 
	 */
	protected boolean findNeedleInHaystack(String haystack, List<String> needle) throws NullPointerException {
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
	 * @description takes a string and a token and returns a list of index position(s)  
	 * 				of any occurrences of the given word within the given text
	 * 
	 * @param 		text
	 * @param 		token
	 * @return 		array list of positions as integer
	 */
	protected ArrayList<Integer> returnTokenPosition(String haystack, String needle) {
		ArrayList<Integer> positions = new ArrayList<Integer>();
		Pattern p = Pattern.compile(needle);  // insert your pattern here
		Matcher m = p.matcher(haystack);
		while (m.find()) {
		   positions.add(m.start());
		}
		
		return positions;
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
	protected String trimStringAtWordBoundary(String haystack, int wordsBefore, int wordsAfter, String needle) {
		if(haystack == null || haystack.trim().isEmpty()){
			return haystack ;
		}
		
		String textsegments = "";
		
		String patternString = "((?:[a-zA-Z'-]+[^a-zA-Z'-]+){0,"+wordsBefore+"}\\b)" + needle + "(\\b(?:[^a-zA-Z'-]+[a-zA-Z'-]+){0,"+wordsAfter+"})";
		//results in: "((?:[a-zA-Z'-]+[^a-zA-Z'-]+){0,5}\b)needle(\b(?:[^a-zA-Z'-]+[a-zA-Z'-]+){0,5})";
		
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
	 * @description	writes the content of a page to disk - THIS IS JUST DURING DEV-TIME
	 * 				storing anything anywhere should be handled by the persistence layer 
	 * @param 		fileName
	 * @param 		content
	 * @return 		true on success and false on error
	 * @throws 		IOException
	 */
	@Deprecated
	protected boolean writeContentToDisk(URL url, String fileName, String content) throws IOException {
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
}
