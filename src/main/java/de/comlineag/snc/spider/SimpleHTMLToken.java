package de.comlineag.snc.spider;

/**
 * SimpleHTMLToken - an HTML Token
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

public class SimpleHTMLToken {
	public static final int TAG = 0;
	public static final int ENDTAG = 1;
	public static final int CONTENT = 2;
	private static final int UNDEFINED = -1;

	private int type;
	private String content;
	
	/**
	 * Constructor for SimpleHTMLToken.
	 */
	public SimpleHTMLToken() { 
		type = UNDEFINED;
		content = null;
	}
	
	/**
	 * Constructor for SimpleHTMLToken.
	 */
	public SimpleHTMLToken(int type, String content) {
		this.type = type;
		this.content = content;
	}	
	
	/**
	 * Returns the content.
	 * @return String
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Returns the type.
	 * @return int
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the content.
	 * @param content The content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * dump - used for debugging
	 */
	public void dump(PrintStream out)
	{
		switch(type) {
			case UNDEFINED:	out.println("Error!");		
								break;
			case TAG:			out.println("<" + content + ">");
								break;
			case ENDTAG:		out.println("</"+ content + ">");
								break;
			case CONTENT:		out.println("\"" + content + "\"");
								break;
			default:			out.println("Error!");
								break;
		}
	
	}
}
