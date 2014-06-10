package de.comlineag.sbm.handler;

import org.json.simple.JSONObject;
import de.comlineag.sbm.data.LithiumUserData;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description Implementation of the lithium user manager - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. LithiumUserManager is called after a
 *              posting with all relevant information about the user (posting
 *              user as well as mentioned users) is decoded by LithiumParser.
 * 				
 * 				The data type lithium user consists of these elements
 *            		id 					Long 
 *            		name 				String 
 *            		screen_name 		String 
 *            		location 			List
 *            		followers_count		Long 
 *            		friends_count 		Long 
 *            		statuses_count 		Long
 *            		favourites_count	Long 
 *            		listed_count		Long 
 *            		lang				String
 * 
 * @param none
 * 
 */

public class LithiumUser extends GenericDataManager<LithiumUserData> {

	private LithiumUserData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());
	public LithiumUser(JSONObject jsonObject) {
		data = new LithiumUserData(jsonObject);
	}
	
	// public void save(List<LithiumUser> users){
	public void save() {
		persistenceManager.saveUsers(data);
	}

}
