package de.comlineag.sbm.handler;

import java.io.InputStream;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * 
 * @description GenericParser is the abstract base class for the specific parser of the 
 * 				social networks. Each social network needs to implement it's own 
 * 				specific version of this GenericParser.
 * 				GenericParser defines the process method - which simply invokes the parse 
 * 				method (to be implemented by specialized versions) either with data 
 * 				represented as a string or as an input stream from a http connection.
 * 				 
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
	protected abstract void parse(String strPosting);
	
	/**
	 * @name 		parse
	 * @param 		InputStream is
	 * @description abstract declaration of the parse method
	 * 				implementation is specific to the social network.
	 * 				parse can either receive string or input stream
	 * 				this one is for input streams 
	 */
	protected abstract void parse(InputStream is);
	
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
