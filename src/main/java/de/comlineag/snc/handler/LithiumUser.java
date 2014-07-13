package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.LithiumUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1 
 * @status		productive
 * 
 * @description	Implementation of the Lithium user handler - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. LithiumUserManager is called after a
 *              user with all relevant information about it is decoded 
 *              by LithiumParser.
 * 
 * 				
 * @param <LithiumUserData>
 * 					Data type 
 * 
 * @changelog	0.1 (Chris)		class created as copy from TwitterUser
 * 
 */

public class LithiumUser extends GenericDataManager<LithiumUserData> {
	
	public LithiumUser() {}
	
	private LithiumUserData data;

	public LithiumUser(JSONObject jsonObject) {
		data = new LithiumUserData(jsonObject);
	}
	
	public void save() {
		persistenceManager.saveUsers(data);
	}
}
