package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.FacebookUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1 
 * @status		not implemented
 * 
 * @description Implementation of the facebook user manager - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. FacebookUserManager is called after a
 *              posting with all relevant information about the user (posting
 *              user as well as mentioned users) is decoded by FacebookParser.
 * 				
 * 				The data type facebook user consists of these elements
 *            		id 					Long 
 *            		name 				String 
 *            		screen_name 		String 
 *            		geoLocation 			List
 *            		followers_count		Long 
 *            		friends_count 		Long 
 *            		statuses_count 		Long
 *            		favourites_count	Long 
 *            		listed_count		Long 
 *            		lang				String
 * 
 * @param none
 * 
 * @changelog	0.1 (CHris)		copy from TwitterUser
 * 
 * TODO implement code for facebook user handling
 */

public class FacebookUser extends GenericDataManager<FacebookUserData> {

	/*
	 * 
	 * Die nachfolgenden Elemente des Users sollen weiter verarbeitet und
	 * gespeichert werden
	 * 
	 * key="cl_userID" value="id" key="cl_userName" value="name"
	 * key="cl_userScreenName" value="screen_name" key="cl_userLocation"
	 * value="geoLocation" key="cl_userFollower" value="followers_count"
	 * key="cl_userFriends" value="friends_count" key="cl_userPostingsCount"
	 * value="statuses_count" key="cl_userFavoritesCount"
	 * value="favourites_count" key="cl_userListsAndGroupsCount"
	 * value="listed_count" key="cl_userLang" value="lang"
	 */
	private FacebookUserData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());

	public FacebookUser(JSONObject jsonObject) {
		data = new FacebookUserData(jsonObject);
	}

	// public void save(List<FacebookUser> users){
	public void save() {
		persistenceManager.saveUsers(data);
	}

}
