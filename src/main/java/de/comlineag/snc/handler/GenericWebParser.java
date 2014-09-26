package de.comlineag.snc.handler;

import java.net.URL;
import java.util.List;

//import org.apache.logging.log4j.Logger;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.1			- 25.07.2014
 * @status		productive
 * 
 * @description GenericWebParser is the abstract base class for web site parsing. It is derived from
 * 				generic parser and defines one new, abstract method parse with parameters for the
 * 				content, the url and the searched terms.
 * 
 * @changelog	0.1 (Chris)		first version
 * 
 */
public abstract class GenericWebParser extends GenericParser {
	
	public GenericWebParser() {}

	// START OF PARSER SPECIFIC
	public abstract void parse(String page, URL url, List<String> tokens);
	// END OF PARSER SPECIFIC
	
}
