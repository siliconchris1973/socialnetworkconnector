package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterLocationData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1
 * 
 * @description Implementation of the twitter location manager - extends
 *              GenericDataManager. This handler is used to save a new geo location or
 *              update an existing one. TwitterLocation is called after a
 *              geo location with all relevant information about it is decoded by TwitterParser.
 * 
 * @param  <TwitterLocationData>
 * 
 * @changelog	0.1 class created as copy from TwitterUser
 * 
 */

public class TwitterLocation extends GenericDataManager<TwitterLocationData> {

	private TwitterLocationData data;
	
	public TwitterLocation(JSONObject jsonObject) {
		data = new TwitterLocationData(jsonObject);
	}
	
	public void save() {
		persistenceManager.saveLocation(data);
	}

}
