package de.comlineag.snc.parser;

import java.io.InputStream;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.4				- 01.10.2014
 * @status		productive
 * 
 * @description GenericParser is the abstract base class for the specific parser of the 
 * 				social networks. Each social network needs to implement it's own 
 * 				specific version of this GenericParser.
 * 				GenericParser defines the process method - which simply invokes the parse 
 * 				method (to be implemented by specialized versions) either with data 
 * 				represented as a string or as an input stream from a http connection.
 * 				 
 * @changelog	0.1 (Chris)		first skeleton
 * 				0.2				first productive version
 * 				0.3				added definition of 2nd input channel (InputStream)
 * 				0.4				changed return value of method parse to boolean
 * 
 */
public abstract class GenericParser {
	
	protected GenericParser() {}
	
	/**
	 * @name 		parse
	 * @param 		String strPosting
	 * @description abstract declaration of the parse method
	 * 				implementation is specific to the social network.
	 * 				parse can either receive string or input stream
	 * 				this one is for strings
	 */
	protected abstract boolean parse(String strPosting);
	
	/**
	 * @name 		parse
	 * @param 		InputStream is
	 * @description abstract declaration of the parse method
	 * 				implementation is specific to the social network.
	 * 				parse can either receive string or input stream
	 * 				this one is for input streams 
	 */
	protected abstract boolean parse(InputStream is);
	
	/**
	 * @name 		process
	 * @param 		String strPosting
	 * @description passes the given string (containing a message, post, tweet etc.) to the parse method
	 */
	public final void process(String strPosting) {
		assert (strPosting != null) : "ERROR :: cannot parse empty string";
		
		parse(strPosting);
	}

	/**
	 * @name 		process
	 * @param 		InputStream is
	 * @description passes the given input stream of the http request to the parse method
	 */
	public final void process(InputStream is) {
		assert (is != null) : "ERROR :: cannot parse on empty input stream";
		
		parse(is);
	}
}
