package de.comlineag.snc.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Helper Class
 * @version 	0.8
 * @status		productive
 * 
 * @description Tools for managing special Requests and String manipulation for the crawler
 * 
 * @changelog	0.1 (Magnus)	class created with timestamp for twitter
 * 				0.2 (Chris)		try and error for lithium post date time
 * 				0.3 (Thomas)	bugfixing and productive version with support for lithium
 * 				0.4 (Chris)		added method to convert UTF-8 to US-ASCII and  
 * 				0.4a 			one to strip html strings while maintaining tag-integrity
 * 				0.5 			added a more sophisticated method to strip the html
 * 								kudos go to http://stackoverflow.com/questions/2496372/html-truncator-in-java
 * 				0.6				moved LocalDateTime to DateTimeServices and renamed this class to StringServices 
 * 				0.7				marked old methods as deprecated
 * 				0.8				removed deprecate methods
 * 		
 */

public class StringServices {
	/**
	 * 
	 * @description returns a normalized version of the given string by converting "umlaute"
	 * 	
	 * @param 		String s
	 * 					the text to normalize
	 * @return 		normalized text
	 */
	public static String normalizeText(String s) {
	    return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","");
	}
	
	
	/**
	 * @deprecated
	 * @description	convert an html text to plain text
	 * @param 		object
	 * @return		plan text
	 */
	public static String stripHTML(Object object) {
		String html = object.toString();
		return Jsoup.parse(html).text();
	}
	
	
	/**
	 * @description	this function truncates a string up to a number of characters 
	 * 				while preserving whole words and HTML tags.
	 * 
	 * @param 		String text
	 *					text to strip
	 * @param 		int length
	 * 					length to strip down to
	 * @return		stripped html text
	 */
	public static String htmlTruncator(String text, int length) {
	    /* if the plain text is shorter than the maximum length, return the whole text
	    if (text.replaceAll("<.*?>", "").length() <= length) {
	        return text;
	    }
	    */
	    String result = "";
	    @SuppressWarnings("unused")
		boolean trimmed = false;
	    
	    /*
	     * This pattern creates tokens, where each line starts with the tag.
	     * For example, "One, <b>Two</b>, Three" produces the following:
	     *     One,
	     *     <b>Two
	     *     </b>, Three
	     */
	    Pattern tagPattern = Pattern.compile("(<.+?>)?([^<>]*)");

	    /*
	     * Checks for an empty tag, for example img, br, etc.
	     */
	    Pattern emptyTagPattern = Pattern.compile("^<\\s*(img|br|input|hr|area|base|basefont|col|frame|isindex|link|meta|param).*>$");

	    /*
	     * Modified the pattern to also include H1-H6 tags
	     * Checks for closing tags, allowing leading and ending space inside the brackets
	     */
	    Pattern closingTagPattern = Pattern.compile("^<\\s*/\\s*([a-zA-Z]+[1-6]?)\\s*>$");

	    /*
	     * Modified the pattern to also include H1-H6 tags
	     * Checks for opening tags, allowing leading and ending space inside the brackets
	     */
	    Pattern openingTagPattern = Pattern.compile("^<\\s*([a-zA-Z]+[1-6]?).*?>$");

	    /*
	     * Find &nbsp; &gt; ...
	     */
	    Pattern entityPattern = Pattern.compile("(&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};)");

	    // splits all html-tags to scanable lines
	    Matcher tagMatcher =  tagPattern.matcher(text);
	    @SuppressWarnings("unused")
		int numTags = tagMatcher.groupCount();
	    
	    int totalLength = 0;
	    List<String> openTags = new ArrayList<String>();

	    boolean proposingChop = false;
	    while (tagMatcher.find()) {
	        String tagText = tagMatcher.group(1);
	        String plainText = tagMatcher.group(2);

	        if (proposingChop &&
	                tagText != null && tagText.length() != 0 &&
	                plainText != null && plainText.length() != 0) {
	            trimmed = true;
	            break;
	        }

	        // if there is any html-tag in this line, handle it and add it (uncounted) to the output
	        if (tagText != null && tagText.length() > 0) {
	            boolean foundMatch = false;

	            // if it's an "empty element" with or without xhtml-conform closing slash
	            Matcher matcher = emptyTagPattern.matcher(tagText);
	            if (matcher.find()) {
	                foundMatch = true;
	                // do nothing
	            }

	            // closing tag?
	            if (!foundMatch) {
	                matcher = closingTagPattern.matcher(tagText);
	                if (matcher.find()) {
	                    foundMatch = true;
	                    // delete tag from openTags list
	                    String tagName = matcher.group(1);
	                    openTags.remove(tagName.toLowerCase());
	                }
	            }

	            // opening tag?
	            if (!foundMatch) {
	                matcher = openingTagPattern.matcher(tagText);
	                if (matcher.find()) {
	                    // add tag to the beginning of openTags list
	                    String tagName = matcher.group(1);
	                    openTags.add(0, tagName.toLowerCase());
	                }
	            }

	            // add html-tag to result
	            result += tagText;
	        }

	        // calculate the length of the plain text part of the line; handle entities (e.g. &nbsp;) as one character
	        int contentLength = plainText.replaceAll("&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};", " ").length();
	        if (totalLength + contentLength > length) {
	            // the number of characters which are left
	            int numCharsRemaining = length - totalLength;
	            int entitiesLength = 0;
	            Matcher entityMatcher = entityPattern.matcher(plainText);
	            while (entityMatcher.find()) {
	                String entity = entityMatcher.group(1);
	                if (numCharsRemaining > 0) {
	                    numCharsRemaining--;
	                    entitiesLength += entity.length();
	                } else {
	                    // no more characters left
	                    break;
	                }
	            }

	            // keep us from chopping words in half
	            int proposedChopPosition = numCharsRemaining + entitiesLength;
	            int endOfWordPosition = plainText.indexOf(" ", proposedChopPosition-1);
	            if (endOfWordPosition == -1) {
	                endOfWordPosition = plainText.length();
	            }
	            int endOfWordOffset = endOfWordPosition - proposedChopPosition;
	            if (endOfWordOffset > 6) { // chop the word if it's extra long
	                endOfWordOffset = 0;
	            }

	            proposedChopPosition = numCharsRemaining + entitiesLength + endOfWordOffset;
	            if (plainText.length() >= proposedChopPosition) {
	                result += plainText.substring(0, proposedChopPosition);
	                proposingChop = true;
	                if (proposedChopPosition < plainText.length()) {
	                    trimmed = true;
	                    break; // maximum length is reached, so get off the loop
	                }
	            } else {
	                result += plainText;
	            }
	        } else {
	            result += plainText;
	            totalLength += contentLength;
	        }
	        // if the maximum length is reached, get off the loop
	        if(totalLength >= length) {
	            trimmed = true;
	            break;
	        }
	    }

	    for (String openTag : openTags) {
	        result += "</" + openTag + ">";
	    }
	    
	    return result;
	}
}
