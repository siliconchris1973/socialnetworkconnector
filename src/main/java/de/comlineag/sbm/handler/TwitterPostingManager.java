package de.comlineag.sbm.handler;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.TweetEntity;
import twitter4j.api.TweetsResources;
import de.comlineag.sbm.data.*;


/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 *
 * @description	Implementation of the twitter posting manager - extends ElementManager 
 * 				This handler is used to save a new tweet or update an existing one.
 * 				TwitterPostingManager is called after a posting with all relevant information
 * 				about the posting (the original as well as the retweeted one) is decoded by TwitterParser.
 * 				
 * 
 * @param		none
 * 
 * 				"id"						Long 
 * 				"created_at" 				String
 * 				"text" 						String
 * 				"source" 					String
 * 				"truncated" 				Boolean
 * 				"in_reply_to_status_id"		Long
 * 				"in_reply_to_user_id" 		Long
 * 				"in_reply_to_screen_name"	String
 * 				"coordinates" 				List
 * 				"place" 					List
 * 				"lang" 						String
 * 				"hashtags" 					List
 * 				"symbols" 					List
 */
public final class TwitterPostingManager extends ElementManager<TwitterPosting> {
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterPostingManager() {		
		// TODO Auto-generated constructor stub
		logger.debug("constructor of class" + getClass().getName() + " called");
	}
	
	@Override
//	public void save(List<TwitterPosting> posting){
	public void save(){
		// log the startup message
		logger.debug("method save from class " + getClass().getName() + " called");
		// TODO: implement save interface either to file, db or the like
	}	
}
