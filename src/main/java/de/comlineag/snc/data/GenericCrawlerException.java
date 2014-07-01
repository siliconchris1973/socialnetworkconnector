package de.comlineag.snc.data;

/**
 * 
 * @author		Christian Guenther
 * @category	program flow and structure
 * @version 	1.0
 *
 * @description	throws a custom exception for the crawler
 * 
 * @changelog	1.0 class created
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
