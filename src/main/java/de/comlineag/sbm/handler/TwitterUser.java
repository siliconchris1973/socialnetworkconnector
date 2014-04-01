package de.comlineag.sbm.handler;

import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.User;
import de.comlineag.sbm.data.*;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description implementation of the twitter user manager. This handler is used to save a new user or update
 * 				an existing user 
 *  
 */
public class TwitterUser extends ElementManager<TwitterUserData> {
	
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
	private TwitterUserData data;
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterUser(JSONObject jsonObject) {
		// TODO Auto-generated constructor stub
		logger.debug("constructor of class" + getClass().getName() + " called");
		data = new TwitterUserData(jsonObject);
	}
	
//	public void save(List<TwitterUser> users){
	public void save(){
		// log the startup message
		logger.debug("method save from class " + getClass().getName() + " called");
				
	}
	
	
}
