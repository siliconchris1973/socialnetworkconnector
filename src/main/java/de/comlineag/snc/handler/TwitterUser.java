package de.comlineag.snc.handler;

//import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		1.0
 * 
 * @description Implementation of the twitter user manager - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. TwitterUserManager is called after a
 *              user with all relevant information about the user (posting
 *              user as well as mentioned users) is decoded by TwitterParser.
 * 
 * @param  <TwitterPosting>
 * 
 */

public class TwitterUser extends GenericDataManager<TwitterUserData> {

	/*
	 * 
	 * Die nachfolgenden Elemente des Users sollen weiter verarbeitet und
	 * gespeichert werden
	 * 
	 * key="cl_userID" value="id" key="cl_userName" value="name"
	 * key="cl_userScreenName" value="screen_name" key="cl_userLocation"
	 * value="location" key="cl_userFollower" value="followers_count"
	 * key="cl_userFriends" value="friends_count" key="cl_userPostingsCount"
	 * value="statuses_count" key="cl_userFavoritesCount"
	 * value="favourites_count" key="cl_userListsAndGroupsCount"
	 * value="listed_count" key="cl_userLang" value="lang"
	 */
	private TwitterUserData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());

	public TwitterUser(JSONObject jsonObject) {
		data = new TwitterUserData(jsonObject);
	}

	// public void save(List<TwitterUser> users){
	public void save() {
		persistenceManager.saveUsers(data);
	}

}