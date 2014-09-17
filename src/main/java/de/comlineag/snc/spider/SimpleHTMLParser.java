package de.comlineag.snc.spider;

/**
 * SimpleHTMLParser object - simple parser for HTML
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
import java.net.*;

public abstract class SimpleHTMLParser {
	
	/**
	 * Constructor for HTMLParser.
	 */
	public SimpleHTMLParser() { }

	/** Parse an HTML page from an input stream
	 * Handles three types of tokens - TAG, ENDTAG,
	 * and CONTENT.  Throws out any comments.
	 */
	public void parse(Reader r) throws IOException
	{
		char buf[] = new char[10];
		
		BufferedReader in = new BufferedReader(r);
		
		// Get rid of any initial (not-tag) chars.
		while(true) {
			read(in,buf,1);
			if (buf[0] == '<') break;
		}
		
		// Process page
		int readahead;
		while(true) {
			// Process tag or comment
			readahead = 3;
			in.mark(readahead);
			read(in,buf,readahead);
			if (buf[0] == '!' && buf[1] == '-' && buf[2] == '-') handle_comment(in);
			else if (buf[0] == '/') {
				in.reset();
				read(in,buf,1);
				handle_tag(SimpleHTMLToken.ENDTAG,in);
			}
			else {
				in.reset();
				handle_tag(SimpleHTMLToken.TAG,in);
			}
			
			// determine if next char is start of new tag or content
			readahead = 1;
			in.mark(readahead);
			try { read(in,buf,readahead); }
			catch(SocketTimeoutException e) { throw(e); } // Re-throw exception
			catch(EOFException e) { return; } // EOF is OK after tag
			catch(IOException e) { throw(e); } // Re-throw exception
			if (buf[0] != '<') {
				in.reset();
				if (handle_content(in) == false) return; // EOF is OK
			}
		}
	}
	
	// Handle a tag
	private void handle_tag(int type, BufferedReader in) throws IOException
	{
		char buf[] = new char[10];
		StringBuffer guts = new StringBuffer();
		while(true) {
			read(in,buf,1);
			if (buf[0] == '>') break;
			guts.append(buf[0]);
		}
		SimpleHTMLToken token = new SimpleHTMLToken(type,guts.toString());
		if (type == SimpleHTMLToken.TAG) processTag(token);	
		else processEndTag(token);
	}
	
	// Throw away comment
	private void handle_comment(BufferedReader in) throws IOException
	{
		char buf[] = new char[10];
		while(true) {
			read(in,buf,1);
			if (buf[0] == '-') {
				int readahead = 2;
				in.mark(readahead);
				read(in,buf,readahead);
				if (buf[0] == '-' && buf[1] == '>') return;
				else in.reset();
			}
		}
			
	}
	
	// Handle tag content - return true if more content, false if EOF
	private boolean handle_content(BufferedReader in) throws IOException
	{
		char buf[] = new char[10];
		StringBuffer guts = new StringBuffer();
		while(true) {
			try { read(in,buf,1); }
			catch(SocketTimeoutException e) { throw(e); } // Re-throw exception
			catch(EOFException e) { return(false); } // EOF is OK after tag
			catch(IOException e) { throw(e); } // Re-throw exception
			if (buf[0] == '<') break;
			else guts.append(buf[0]);
		}
		SimpleHTMLToken token = new SimpleHTMLToken(SimpleHTMLToken.CONTENT,guts.toString());
		processContent(token);
		return(true);
	}
	
	/** processTag - process a tag */
	public abstract void processTag(SimpleHTMLToken token) throws IOException;
	
	/** processEndTag - process an end tag */
	public abstract void processEndTag(SimpleHTMLToken token) throws IOException;
	
	/** processContent - process content */
	public abstract void processContent(SimpleHTMLToken token) throws IOException;
	
	/** Process a token and return the tag or null
	 * flag indicates whether tag is to be converted to lower case 
	 */
	public static String getTagType(SimpleHTMLToken token, boolean lowerCaseFlag) {
		if (token.getType() == SimpleHTMLToken.CONTENT) return(null);
		String content = token.getContent();
		if (content == null || content.length() == 0) return(null);
		StringTokenizer tt = new StringTokenizer(content);
		String tag = null;
		try { tag = tt.nextToken(); }
		catch(NoSuchElementException e) { return(null); }
		return((lowerCaseFlag?tag.toLowerCase():tag));
	}
	
	// Read() - handle blocking / EOF
	private void read(BufferedReader r,char[] buf,int nchars) throws IOException
	{
		int flag = 10;
		int charsToRead = nchars;
		
		while (charsToRead != 0 && flag != 0) {
			int charsRead = r.read(buf,0,nchars);
			if (charsRead == -1) throw new EOFException("Premature EOF while parsing HTML");
			charsToRead = charsToRead - charsRead;
			flag--;
			if (flag<=5) {
				// Wait a second
				Thread mythread = Thread.currentThread();
				try { mythread.sleep(1000,0); }
				catch(InterruptedException e) { /* Ignore it */ }
			}
		}
		if (flag == 0) throw new SocketTimeoutException("Input timed-out while parsing HTML");
	}
}
