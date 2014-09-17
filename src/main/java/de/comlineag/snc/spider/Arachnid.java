package de.comlineag.snc.spider;

/** Arachnid - Abstract Web spider class
 * To use, derive class from Arachnid,
 * Add handleLink(), handleBadLink(), handleNonHTMLlink(),
 * handleExternalLink(), and handleBadIO() methods
 * Instantiate and call traverse()
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
import java.net.*;
import java.util.*;

public abstract class Arachnid {
	private String base;
	private URL baseUrl;
	private HashSet visited;
	private int delay;
	private static final String HTML = "text/html";
	
	/** Constructor */
	public Arachnid(URL base) throws MalformedURLException {
		baseUrl = base;
		visited = new HashSet();
		delay = 2;
	}
	
	/** Traverse Web site */
	public void traverse() { traverse(baseUrl,null); }
	
	private void traverse(URL url, URL parent)
	{
		boolean isHTMLfile = true;
		PageInfo p = null;
		try { p = getWebPage(url,parent); }
		catch(IOException e) {
			handleBadIO(url,parent);
			sleep(delay);
			return;
		}
		if (p == null) {
			handleBadLink(url,parent,null);
			sleep(delay);
			return;
		}
		if (p.isValid() == false) {
			if (p.getContentType().equalsIgnoreCase(HTML) == false) 
				handleNonHTMLlink(url,parent,p);
			else handleBadLink(url,parent,p);
			sleep(delay);
			return;
		}
		else handleLink(p);
		
		// Navigate through links on page
		URL[] links = p.getLinks();
		if (links == null) {
			sleep(delay);
			return;
		}
		int n = links.length;
		for (int i=0; i<n; ++i) {
			if (isOKtoVisit(links[i])) {
				visited.add(links[i]);
				traverse(links[i],url);
			}
			else if (isExternalSite(links[i])) handleExternalLink(links[i],url);
		}
		sleep(delay);
		return;
	}
	
	/** (Abstract) Handle bad URL */
	protected abstract void handleBadLink(URL url,URL parent,PageInfo p);
	
	/** (Abstract) Handle a link; a Web page in the site */
	protected abstract void handleLink(PageInfo p);
	
	/** (Abstract) Handle a non-HTML link */
	protected abstract void handleNonHTMLlink(URL url, URL parent, PageInfo p);
	
	/** (Abstract) Handle an external (outside of Web site) link */
	protected abstract void handleExternalLink(URL url, URL parent);
	
	/** (Abstract) Handle an I/O Exception (server problem) */
	protected abstract void handleBadIO(URL url, URL parent);
	
	/** Return true if it's OK to visit the link,
	    false if it's not */
	private boolean isOKtoVisit(URL link) {
		// Return false if it's not HTTP protocol
		if (!link.getProtocol().equals("http")) return(false);
		// Return false if it's an external site
		else if (isExternalSite(link)) return(false);
		else if (visited.contains(link)) return(false);
		else return(true);
	}
	
	private boolean isExternalSite(URL link) {
		// Return true if link host is different from base or
		// if path of link is not a superset of base URL
		if (link.getAuthority() != baseUrl.getAuthority() ||
			(!UrlPathDir(link).startsWith(UrlPathDir(baseUrl)))) return(true);
		else return(false);
	}
	
	private String UrlPathDir(URL u) {
		String p = u.getPath();
		if (p == null || p.equals("")) return("/");
		int i = p.lastIndexOf("/");
		if (i == -1) return("/");
		else p = p.substring(0,i+1);
		return(p);
	}
	
	// Populate a PageInfo object from a URL
	private PageInfo getWebPage(URL url, URL parentUrl) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		int responseCode = connection.getResponseCode();
		String contentType = connection.getContentType();
		// Note: contentLength == -1 if NOT KNOWN (i.e. not returned from server)
		int contentLength = connection.getContentLength();	
		PageInfo p = new PageInfo(url,parentUrl,contentType,contentLength,responseCode);
		InputStreamReader rdr =
			new InputStreamReader(connection.getInputStream());
		p.extract(rdr);
		rdr.close();
		connection.disconnect();
		return(p);
	}
	
	/** Get contents of a URL */
	public byte[] getContent(URL url) {
		byte[] buf = null;
		try { 
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			int responseCode = connection.getResponseCode();
			int contentLength = connection.getContentLength();
			// System.out.println("Content length: "+contentLength);
			if (responseCode != HttpURLConnection.HTTP_OK || contentLength <= 0) return(null);
			InputStream in = connection.getInputStream();
			BufferedInputStream bufIn = new BufferedInputStream(in);
			buf = new byte[contentLength];
			// Added code to handle blocked reads
			int bytesToRead = contentLength;
			int flag = 10;
			while(bytesToRead != 0 && flag != 0) {
				int bytesRead = bufIn.read(buf,(contentLength-bytesToRead),bytesToRead);
				bytesToRead = bytesToRead - bytesRead;
				flag--;
				if (flag <= 5) sleep(1);
			}
			in.close();
			connection.disconnect();
			if (flag == 0) return(null);
		}
		catch(Exception e) {
			// System.out.println(e);
			// e.printStackTrace();
			return(null);
		}
		
		return(buf);
	}
		
	/** Return base URL (starting point for Web traversal) */
	public URL getBaseUrl() { return(baseUrl); }	
	
	// Sleep N seconds
	private void sleep(int n) {
		if (n <= 0) return;
		Thread mythread = Thread.currentThread();
		try { mythread.sleep(n*1000); }
		catch(InterruptedException e) { // Ignore
		}
	}
	/**
	 * Returns delay (N second pause after processing EACH web page)
	 * @return int
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets delay (N second pause after processing EACH web page)
	 * @param delay The delay to set
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}

}