package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2				- 19.11.2014
 * @status		productive
 * 
 * @description Implementation of the twitter user manager - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. TwitterUserManager is called after a
 *              user with all relevant information about the user (posting
 *              user as well as mentioned users) is decoded by TwitterParser.
 * 
 * @param  <TwitterUserData>
 * 
 * @changelog	0.1 (Chris)		class created as copy from TwitterPosting
 * 				0.2				added getJson() and getUserData() method
 * 
 */

public class TwitterUser extends GenericDataManager<TwitterUserData> {

	private TwitterUserData data;
	
	public TwitterUser(JSONObject jsonObject) {
		data = new TwitterUserData(jsonObject);
	}
	
	public void save() {
		persistenceManager.saveUsers(data);
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public TwitterUserData getUserData(){
		return(data);
	}
}
