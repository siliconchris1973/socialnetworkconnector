package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.1
 * 
 * @description provides constants for use with the Twitter Network crawler and parser. 
 * 				This class is instantiated by TwitterCrawler and TwitterParser and the
 * 				values are referenced therein
 *
 * @changelog	0.1 class created
 * 
 */
public final class TwitterConstants {
	public final int MESSAGE_BLOCKING_QUEUE_SIZE = 100000;
	public final int EVENT_BLOCKING_QUEUE_SIZE = 1000;
	
	public TwitterConstants(){}
}
