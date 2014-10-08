package de.comlineag.snc.parser;

import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import de.comlineag.snc.handler.SimpleWebPosting;


/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		1.0				- 08.10.2014
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
 * 				1.0				productive version without deprecated writeContentToDisk method
 * 
 */
public abstract class GenericWebParser extends GenericParser implements IWebParser {
	
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
			return true;
		}
		
		return false;
	}
}
