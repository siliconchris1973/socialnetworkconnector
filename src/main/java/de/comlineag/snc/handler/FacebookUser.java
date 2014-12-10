package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.FacebookUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2
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
 * 				0.2				added getJson() and getUserData() method
 * 
 * TODO implement code for facebook user handling
 */

public class FacebookUser extends GenericDataManager<FacebookUserData> {
	
	private FacebookUserData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());

	public FacebookUser(JSONObject jsonObject) {
		data = new FacebookUserData(jsonObject);
	}

	// public void save(List<FacebookUser> users){
	public void save() {
		persistenceManager.saveUsers(data);
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public FacebookUserData getUserData(){
		return(data);
	}
}
