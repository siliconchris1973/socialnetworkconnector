package de.comlineag.snc.parser;

import java.net.URL;
import java.util.List;

import de.comlineag.snc.handler.SimpleWebPosting;

/**
 * @author 		Christian Guenther
 * @version		0.1
 * @status		Beta		- 05.10.2014
 * 
 * @description	THis interface IWebParser is implemented by all web parser. 
 * 				I has an abstract method execute(String page, URL url), which all parser must
 * 				implement. There is also a canExecute(URL url) method in the interface. Each parser
 * 				must also implement this method and decide, based on the url if it can cope with
 * 				the page to come.
 * 
 * 				There is also a separate class ParserControl that has a submit(String page) method. 
 * 				In the ProcessorControl we maintain a list (and not map) of parsers. 
 * 				Before a page is submitted to a specific parser implementation, we loop through 
 * 				the parser-list and call canExecute on each one of them. The one that returns 
 * 				true is the right one. This has the advantage that the code to check for parser 
 * 				compatibility is now in the parser control which allows addition of new parsers 
 * 				without any code change as long as the registering part is data-driven 
 * 				(using a property file, say) 
 * 				
 * 				Whenever a new page comes, afetr checking which specific parser to call, we simply 
 * 				do ParserControl.submit(page).
 * 				
 * 				Now ParserControl has a method addParser(Parser proc, URL url) which adds 
 * 				the parser to a hashtable with domain as the key. Hence each parser is now assigned 
 * 				with a domain. In the submit method, just get hashtable.get(type).execute(proc)
 * 
 * @changelog	0.1 (Chris)	interface created
 */
public interface IWebParser {
	public abstract Object execute(String page, URL url);
	public abstract List<SimpleWebPosting> parse(String page, URL url, List<String> tokens);
	
	public abstract boolean canExecute(URL url);
}
