package de.comlineag.snc.parser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;


/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.3				- 01.10.2014
 * @status		productive
 * 
 * @description GenericWebParser is the abstract base class for web site parsing. It is derived from
 * 				generic parser and defines one new, abstract method parse with parameters for the
 * 				content, the url and the searched terms.
 * 
 * @changelog	0.1 (Chris)		first version
 * 				0.2				added method to return list of indices for the needle in the haystack
 * 				0.3				changed return value of parse to Boolean
 * 
 */
public abstract class GenericWebParser extends GenericParser {
	
	public GenericWebParser() {}

	public abstract Boolean parse(String page, URL url, List<String> tokens);
	
	protected abstract JSONObject parseAndCreateJsonFromPage(String page, URL url, List<String> tokens);
	
	/**
	 * @description takes a string and a token and returns a list of index position(s)  
	 * 				of any occurrences of the given word within the given text
	 * 
	 * @param 		text
	 * @param 		token
	 * @return 		array list of positions as integer
	 */
	public ArrayList<Integer> returnTokenPosition(String haystack, String needle) {
		ArrayList<Integer> positions = new ArrayList<Integer>();
		Pattern p = Pattern.compile(needle);  // insert your pattern here
		Matcher m = p.matcher(haystack);
		while (m.find()) {
		   positions.add(m.start());
		}
		
		return positions;
    }
}