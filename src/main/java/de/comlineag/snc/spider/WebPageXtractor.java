package de.comlineag.snc.spider;

/**
 * WebPageXtractor - extracts information from a WebPage
 * passed as an input stream.  Makes use of SimpleHTMLParser
 * object.  Used to use HTMLEditorKit and HTMLEditorKit.Parser. 
 * This turned out to be too buggy for this application.
 * Cannot use XML parser as HTML does not follow stricter XML
 * syntax rules.  In fact many Web pages are a "tag salad" that
 * don't even follow proper HTML syntax.  WebPageXtractor parses
 * a page and extracts links, images, and title(s).
 * 
 * Copyright 2002, Robert L. Platt, All rights reserved
 * @author Robert L. Platt 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
 import java.io.*;
 import java.util.*;
 
 public class WebPageXtractor extends SimpleHTMLParser {
 	private ArrayList links;
	private ArrayList images;
	private ArrayList title;
	private boolean inTitle;
	
	/** Constructor */
	public WebPageXtractor() {
		super();
		links = new ArrayList();
		images = new ArrayList();
		title = new ArrayList();
	}
	
	/**
	 * If we're within TITLE tags - save the title
	 * @see SimpleHTMLParser#processContent(SimpleHTMLToken)
	 */
	public void processContent(SimpleHTMLToken token) {
		String s = token.getContent().trim();
		if (s != null && s.length() != 0) {
		 	if (inTitle) title.add(s);
		}
	}

	/**
	 * Look for </title> tags
	 * @see SimpleHTMLParser#processEndTag(SimpleHTMLToken)
	 */
	public void processEndTag(SimpleHTMLToken token) throws IOException
	{
		String tag = SimpleHTMLParser.getTagType(token,true);
		if (tag == null) throw new IOException("HTML parsing error");
		else if (tag.equals("title")) inTitle = false;
	}

	/**
	 * Handle Anchor, Image, Frame, and Title tags
	 * @see SimpleHTMLParser#processTag(SimpleHTMLToken)
	 */
	public void processTag(SimpleHTMLToken token) throws IOException
	{
		String tag = SimpleHTMLParser.getTagType(token,true);
		if (tag == null) throw new IOException("HTML parsing error");
		else if (tag.equals("a")) {
			String link = extractHref(token.getContent());
			if (link != null) links.add(link);
		}
		else if (tag.equals("img")) {
			String image = extractSrc(token.getContent());
			if (image != null) images.add(image);
		}
		else if (tag.equals("frame")) {
			String link = extractSrc(token.getContent());
			if (link != null) links.add(link);
		}
		else if (tag.equals("title")) inTitle = true;
	}
	
	
	// Utility method for extracting href attribute
	private String extractHref(String tag)
	{
		String delims="\t\r\f\n \'\"=";
		StringTokenizer tt = new StringTokenizer(tag,delims);
		while(tt.hasMoreElements()) {
			String s = tt.nextToken();
			if (s.equalsIgnoreCase("href")) {
				if (!tt.hasMoreElements()) return(null);
				else return(tt.nextToken());
			}
		}
		return(null);
	}
	
	// Utility method for extracting src attribute
	private String extractSrc(String tag)
	{
		String delims="\t\r\f\n \'\"=";
		StringTokenizer tt = new StringTokenizer(tag,delims);
		while(tt.hasMoreElements()) {
			String s = tt.nextToken();
			if (s.equalsIgnoreCase("src")) {
				if (!tt.hasMoreElements()) return(null);
				else return(tt.nextToken());
			}
		}
		return(null);
	}
	/**
	 * Returns the images.
	 * @return ArrayList
	 */
	public ArrayList getImages() {
		return images;
	}

	/**
	 * Returns the links.
	 * @return ArrayList
	 */
	public ArrayList getLinks() {
		return links;
	}

	/**
	 * Returns the title.
	 * @return ArrayList
	 */
	public ArrayList getTitle() {
		return title;
	}
}
