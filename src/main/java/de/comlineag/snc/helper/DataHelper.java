package de.comlineag.snc.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Helper Class
 * @version 	0.5
 * 
 * @description Tools for managing special Requests in the Crawler logic
 * 
 * @changelog	0.1 class created												Magnus
 * 				0.2 try and error for lithium post date time					Chris
 * 				0.3 bugfixing and productive version with support for lithium	Thomas
 * 				0.4 added method to convert UTF-8 to US-ASCII and one to strip 
 * 					html strings while maintaining tag-integrity				Chris
 * 				0.5 added a more sophisticated method to strip the html
 * 					kudos go to http://stackoverflow.com/questions/2496372/html-truncator-in-java
 */

public class DataHelper {

	private static Logger logger = Logger.getLogger("de.comlineag.snc.data.DataHelper");

	/**
	 * 
	 * @description		create a timestamp for the OData Service Interface from the social media timestamp
	 * 					as each network is expected to act a little bit different, the snId is sent to decide 
	 * 					which algorithm is used. Implemented is the algorithm for Twitter and Lithium
	 * 					all other types return the current timestamp
	 * 
	 * @param 			_timestamp
	 *            			the timestamp in the social network
	 * @param 			_snId
	 *            			social network identifier
	 * @return			formatted timestamp
	 */
	public static LocalDateTime prepareLocalDateTime(String _timestamp, String _snId) {

		// Datumsformatierung fuer den Formatter
		String snPattern = "";
		Locale snLocale = Locale.getDefault();

		try {

			if (_snId.equalsIgnoreCase(SocialNetworks.TWITTER.getValue())) {
				logger.trace("formatting date time for use with twitter");
				snPattern = "EEE MMM d H:m:s Z yyyy";
				// the date time format for twitter must be set to US, otherwise english designators will not be translated correctly
				snLocale = Locale.US;
			} else if (_snId.equalsIgnoreCase(SocialNetworks.LITHIUM.getValue())) {
				logger.trace("formatting date time for use with Lithium");
				// 2014-01-08T12:21:42+00:00
				// date time pattern provided by Thomas Nowak
				snPattern = "yyyy-MM-dd'T'HH:mm:ssZZ";
				snLocale = Locale.GERMANY;
			} else {
				logger.warn("no specific conversion for system " + _snId);
			}

			// convert the datum
			DateTimeFormatter formatter =
					DateTimeFormat.forPattern(snPattern).withLocale(snLocale);

			DateTime dateTime = formatter.parseDateTime(_timestamp);
			return dateTime.toLocalDateTime();

		} catch (Exception e) {
			logger.error(e.getMessage());
			// default: current Timestamp in case any error occurs
			DateTime dt = new DateTime();
			return new LocalDateTime(dt);
		}
	}
	
	/**
	 * 
	 * @description returns a normalized version of the given string by converting every ä to a, ö to o and so on
	 * 	
	 * @param 		String s
	 * 					the text to normalize
	 * @return 		normalized text
	 */
	public static String normalizeText(String s) {
	    return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","");
	}
	
	
	/**
	 * 
	 * @description	convert an html text to plain text
	 * @param 		object
	 * @return		plan text
	 */
	public static String stripHTML(Object object) {
		String html = object.toString();
		return Jsoup.parse(html).text();
	}
	
	
	/**
	 * @description	this function truncates a string up to a number of characters while preserving whole words and HTML tags.
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
	
	
	/**
	 * @description	truncate a given html to a fixed length
	 * @param html
	 * @param length
	 * @return
	 */
	public static String truncateHtmlWords(String html, int length){
		if (length <= 0)
			return new String();
		
		List<String> html4Singlets = Arrays.asList(
												"br", "col", "link", "base", "img",
												"param", "area", "hr", "input");
		// Set up regular expressions
		Pattern pWords = Pattern.compile("&.*?;|<.*?>|(\\w[\\w-]*)");
		Pattern pTag = Pattern.compile("<(/)?([^ ]+?)(?: (/)| .*?)?>");
		Matcher mWords = pWords.matcher(html);
		// Count non-HTML words and keep note of open tags
		int endTextPos = 0;
		int words = 0;
		List<String> openTags = new ArrayList<String>();
		while (words <= length) {
			if (!mWords.find())
				break;
			if (mWords.group(1) != null) {
				// It's an actual non-HTML word
				words += 1;
				if (words == length)
					endTextPos = mWords.end();     
				continue;
			}
			// Check for tag
			Matcher tag = pTag.matcher(mWords.group());
			if (!tag.find() || endTextPos != 0)
				// Don't worry about non tags or tags after our
				// truncate point
				continue;
			String closingTag  = tag.group(1);
			// Element names are always case-insensitive
			String tagName     = tag.group(2).toLowerCase();
			String selfClosing = tag.group(3);
			if (closingTag != null) {
				int i = openTags.indexOf(tagName);
				if (i != -1)
					openTags = openTags.subList(i + 1, openTags.size());
			} else if (selfClosing == null && !html4Singlets.contains(tagName))
				openTags.add(0, tagName);
		}
		  
		if (words <= length)
			return html;
		StringBuilder out = new StringBuilder(html.substring(0, endTextPos));
		for (String tag: openTags)
			out.append("");
		
		return out.toString();
	}
	
	/**
	 * @deprecated
	 * @description	strips a given html text to the given size, while maintaining tag-integrity
	 * 				by closing all stripped off tags at the end of the text.
	 * 
	 * @param 		String s
	 *					text to strip
	 * @param 		int l
	 * 					length to strip down to
	 * @return		stripped html text
	 */
	public static String truncateHtml(String s, int l) {
		  Pattern p = Pattern.compile("<[^>]+>([^<]*)");

		  int i = 0;
		  List<String> tags = new ArrayList<String>();

		  Matcher m = p.matcher(s);
		  while(m.find()) {
		      if (m.start(0) - i >= l) {
		          break;
		      }

		      String t = StringUtils.split(m.group(0), " \t\n\r\0\u000B>")[0].substring(1);
		      if (t.charAt(0) != '/') {
		          tags.add(t);
		      } else if ( tags.get(tags.size()-1).equals(t.substring(1))) {
		          tags.remove(tags.size()-1);
		      }
		      i += m.start(1) - m.start(0);
		  }

		  Collections.reverse(tags);
		  return s.substring(0, Math.min(s.length(), l+i))
		      + ((tags.size() > 0) ? "</"+StringUtils.join(tags, "></")+">" : "")
		      + ((s.length() > l) ? "\u2026" : "");

		}
}
