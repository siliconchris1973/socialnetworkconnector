package de.comlineag.sbm.data;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.User;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description implementation of the twitter user manager. This handler is used to save a new user or update
 * 				an existing user 
 *  
 */
public class TwitterUserManager extends ElementManager<DT_TwitterUser> {
	
	/*
	 * 
	 * Die nachfolgenden Elemente des Users sollen weiter verarbeitet und gespeichert werden
	 *				
	 *	key="cl_userID" 					value="id"
	 *	key="cl_userName" 					value="name"
	 *	key="cl_userScreenName" 			value="screen_name"
	 * 	key="cl_userLocation" 				value="location"
	 *	key="cl_userFollower" 				value="followers_count"
	 * 	key="cl_userFriends" 				value="friends_count"
	 * 	key="cl_userPostingsCount"			value="statuses_count"
	 *	key="cl_userFavoritesCount"			value="favourites_count"
	 *	key="cl_userListsAndGroupsCount"	value="listed_count" 
	 *	key="cl_userLang" 					value="lang"
	 */
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterUserManager() {
		// TODO Auto-generated constructor stub
		logger.debug("constructor of class" + getClass().getName() + " called");
	}
	
	public void save(List<DT_TwitterUser> users){
		// log the startup message
		logger.debug("method save from class " + getClass().getName() + " called");
				
	}
	
	
}
