package de.comlineag.snc.handler;

//import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1
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

}
