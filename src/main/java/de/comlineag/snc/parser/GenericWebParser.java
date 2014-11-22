package de.comlineag.snc.parser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import de.comlineag.snc.handler.WebPosting;
import de.comlineag.snc.helper.UniqueIdServices;


/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		1.0b			- 09.10.2014
 * @status		productive
 * 
 * @description GenericWebParser is the abstract base class for web site parsing. It is derived from
 * 				generic parser and defines one new, abstract method parse with parameters for the
 * 				content, the url and the searched terms.
 * 				
 * 				The web parser creates a web-page data object and an embedded user object within
 * 				the web page object.
 * 
 * @changelog	0.1 (Chris)		first version
 * 				0.2				added method to return list of indices for the needle in the haystack
 * 				0.3				changed return value of parse to boolean
 * 				0.4				introduced IWebParser Interface
 * 				1.0				productive version without deprecated writeContentToDisk method
 * 				1.0a			removed abstract method extractContent because only needed by SimpleWebParser
 * 				1.0b			added methods returnTokenPosition and trimStringAtPosition
 * 
 */
public abstract class GenericWebParser extends GenericParser implements IWebParser {
	
	public GenericWebParser() {}

	public abstract List<WebPosting> parse(String page, URL url, List<String> tokens, String sn_id, String curCustomer, String curDomain);
	
	
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
	
	/**
	 * @description takes a string and a token and returns a list of index position(s)  
	 * 				of any occurrences of the given word within the given text
	 * 
	 * @param 		text - the haystack
	 * @param 		a single token - the needle
	 * @param		list of positions where needles where already found
	 * @param		int lowBorderMark - how far before the searched token do we check for already found token
	 * @param		int highBorderMark - how far after the searched token do we check for already found token
	 * 
	 * @return 		list of positions as integer
	 */
	protected ArrayList<Integer> returnTokenPosition(String haystack, String needle, ArrayList<Integer> positions, int lowBorderMark, int highBorderMark) {
		//logger.trace("positions so far: ");
		Pattern p = Pattern.compile(needle);
		Matcher m = p.matcher(haystack);
		while (m.find()) {
			boolean putitin = true;
			// only add the position of a token to the list, if it is not within a range of already added tokens
			// we track 30 words before and after the given token position, so a token to add to the list should be at
			// least 180 chars away (30 words á 5 characters plus 30 white spaces)
			for (int i=0 ; i < positions.size();i++) {
				int lowBorder = positions.get(i) - lowBorderMark;
				int highBorder = positions.get(i) + highBorderMark;
				//logger.trace("token: " + needle + " low border " + lowBorder + " position " + m.start() + " high border " + highBorder);
				
				if (lowBorder < m.start() && m.start() < highBorder){
					//logger.trace("position "+m.start()+" is between low border mark ("+lowBorder+") and high border mark ("+highBorder+"), not adding position to list");
					putitin = false;
				} else {
					//logger.trace("position "+m.start()+" is outside of low border mark ("+lowBorder+") and high border mark ("+highBorder+"), adding position to list");
					putitin = true;
				}
			}
			
			if (putitin) positions.add(m.start());
		}
		
		return positions;
    }
	
	
	/**
	 * @description	gets a page (or any other text), a position to look for and returns a substring with 
	 * 				given number of words before and after the given token 
	 * @param 		original page
	 * @param 		wordsBefore
	 * @param 		wordsAfter
	 * @param 		token
	 * @return 		trimmed text
	 */
	protected String trimStringAtPosition(String haystack, int position, int wordsBefore, int wordsAfter) {
		if(haystack == null || haystack.trim().isEmpty()){
			return haystack;
		}
		String textsegments = "";
		
		// first get the word we are looking for - that is the needle in the haystack 
		// we find this word via the given position within the text and check for the 
		// first whitespace to get the end of the word 
		int endpos = haystack.indexOf(" ", position);
		String needle = haystack.substring(position, endpos);
		
		//logger.info("found the needle "+needle+" in the haystack from position " + position + " to " + endpos + " creating new text segment to store in db");
		
		String patternString = "((?:[a-zA-Z'-]+[^a-zA-Z'-]+){0,"+wordsBefore+"}\\b)" + needle + "(\\b(?:[^a-zA-Z'-]+[a-zA-Z'-]+){0,"+wordsAfter+"})";
		
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(haystack);
		
		while(matcher.find()){
			String segText = matcher.group(1) + needle + matcher.group(2);
			textsegments += segText;
		}
		
		return textsegments;
	}
	
	@SuppressWarnings("unchecked")
	protected JSONObject createPageJsonObject(String sn_id, 
												String title, 
												String description, 
												String page, 
												String text, 
												String created_at,
												URL url, 
												boolean truncated, 
												String lang, 
												String page_id, 
												String user_id, 
												String user_name, 
												String screen_name, 
												String user_lang, 
												long postings_count,
												String curCustomer,
												String curDomain) {
		JSONObject pageJson = new JSONObject();
				
		// put some data in the json
		pageJson.put("sn_id", sn_id);
		pageJson.put("page_id", page_id);
		pageJson.put("user_id", user_id);
		pageJson.put("Customer", curCustomer);
		pageJson.put("Domain", curDomain);
		pageJson.put("subject", title);
		pageJson.put("teaser", description);
		pageJson.put("source", url.toString());
		pageJson.put("lang", lang);
		pageJson.put("truncated", truncated);
		pageJson.put("created_at", created_at);
		pageJson.put("raw_text", page);
		pageJson.put("text", text);
		
		JSONObject userJson = new JSONObject();
		userJson.put("sn_id", sn_id);
		userJson.put("id", user_id);
		userJson.put("name", user_name);
		userJson.put("screen_name", screen_name);
		userJson.put("lang", user_lang);
		
		pageJson.put("USER", userJson);
		
		return pageJson;
	}
}
