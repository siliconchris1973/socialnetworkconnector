package de.comlineag.sbm.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.User;
import de.comlineag.sbm.data.*;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description Implementation of the twitter user manager - extends GenericDataManager. 
 * 				This handler is used to save a new user or update an existing one.
 * 				TwitterUserManager is called after a posting with all relevant information
 * 				about the user (posting user as well as mentioned users) is decoded by TwitterParser. 
 * 				
 *
 * @param		none
 * 
 *				"id"				Long
 *				"name"				String
 *				"screen_name"		String
 * 				"location"			List
 *				"followers_count"	Long
 * 				"friends_count"		Long
 * 				"statuses_count"	Long
 *				"favourites_count"	Long
 *				"listed_count" 		Long
 *				"lang"				String
 */
public final class TwitterUserManager extends GenericDataManager<TwitterUser> {
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterUserManager() {
		// TODO Auto-generated constructor stub
		logger.debug("constructor of class" + getClass().getName() + " called");
	}
	
//	public void save(List<TwitterUser> users){
	public void save(){
		// log the startup message
		logger.debug("method save from class " + getClass().getName() + " called");
				
	}
	
	
}
