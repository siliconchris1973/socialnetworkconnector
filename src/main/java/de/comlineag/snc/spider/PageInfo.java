package de.comlineag.snc.spider;

/** PageInfo - Web Page Information object
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
import java.net.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class PageInfo {
	private URL url;
	private URL parentUrl;
	private String title;
	private URL[] links;
	private URL[] images;
	private boolean valid;
	private int responseCode;
	private String contentType;
	private int contentLength;
	private final static URL[] dummy = new URL[1];
	private final static String HTML = "text/html";
	
	/** Constructor */
	public PageInfo(URL url, URL parentUrl, String contentType, int contentLength, int responseCode) {
		this.url = url;
		this.parentUrl = parentUrl;
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.responseCode = responseCode;
		valid = false;
	}
	
	// Accessors
	public URL getUrl() { return(url); }
	public URL getParentUrl() { return(parentUrl); }
	public String getTitle() { return(title); }
	public URL[] getLinks() { return(links); }
	public URL[] getImages() { return(images); }
	public String getContentType() { return(contentType); }
	public boolean isValid() { return(valid); }
	public int getResponseCode() { return responseCode; }
	
	/** Call WebPageXtractor and process WebPage */
	public void extract(Reader reader) throws IOException
	{
		// Note: contentLength of -1 means UNKNOWN
		if (reader == null  || url == null || 
			responseCode != HttpURLConnection.HTTP_OK || 
			contentLength == 0 || contentType.equalsIgnoreCase(HTML) == false) {	
			valid = false;
			return;
		}
		WebPageXtractor x = new WebPageXtractor();
		try { x.parse(reader); }
		catch(EOFException e) {
			valid = false;
			return;
		}
		catch(SocketTimeoutException e) {
			valid = false;
			throw(e);
		}
		catch(IOException e) { 
			valid = false;
			return;
		}
		ArrayList rawlinks = x.getLinks();
		ArrayList rawimages = x.getImages();
		
		// Get web page title (1st title if more than one!)
		ArrayList rawtitle = x.getTitle();
		if (rawtitle.isEmpty()) title = null; 
		else title = new String((String)rawtitle.get(0));
		
		// Get links
		int numelem = rawlinks.size();
		if (numelem == 0) links = null;
		else {
			ArrayList t = new ArrayList();
			for (int i=0; i<numelem; ++i) {
				String slink = (String)rawlinks.get(i);
				try {
					URL link = new URL(url,slink);
					t.add(link);
				}
				catch(MalformedURLException e) { /* Ignore */ }
			}
			if (t.isEmpty()) links = null;
			else links = (URL[])t.toArray(dummy);
		}
		
		// Get images
		numelem = rawimages.size();
		if (numelem == 0) images = null;
		else {
			ArrayList t = new ArrayList();
			for (int i=0; i<numelem; ++i) {
				String simage = (String)rawimages.get(i);
				try {
					URL image = new URL(url,simage);
					t.add(image);
				}
				catch(MalformedURLException e) { }
			}
			if (t.isEmpty()) images = null;
			else images = (URL[])t.toArray(dummy);
		}

		// Set valid flag
		valid = true;
	}
	
	/** For debugging - dump page information */
	public void dump() {	
		System.out.println("URL: "+url);
		System.out.println("Parent URL: "+parentUrl);
		System.out.println("Title: "+title);
		if (links != null) {
			System.out.print("Links: [");
			for (int i=0; i<links.length; ++i) {
				System.out.print(links[i]);
				if (i<(links.length-1)) System.out.print(", ");
			}
			System.out.println("]");
		}
		if (images != null) {
			System.out.print("Images: [");
			for (int i=0; i<images.length; ++i) {
				System.out.print(images[i]);
				if (i<(images.length-1)) System.out.print(", ");
			}
			System.out.println("]");
		}
		System.out.println("Valid: "+valid);
		System.out.println("Response Code: "+responseCode);
		System.out.println("Content Type: "+contentType);
		System.out.println("Content Length: "+contentLength);
	}
}
