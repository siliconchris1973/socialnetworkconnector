package de.comlineag.snc.handler;

/**
 * 
 * @author		Christian Guenther
 * @category	program flow and structure
 * @version 	0.1
 * @status		productive
 *
 * @description	throws a custom exception for the crawler
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public class GenericCrawlerException extends Exception {
	
	private static final long serialVersionUID = -3476297635427110635L;

	public GenericCrawlerException(String string) {
		super(string);
	}
	
	public GenericCrawlerException(Throwable t){
		super(t);
	}
}
