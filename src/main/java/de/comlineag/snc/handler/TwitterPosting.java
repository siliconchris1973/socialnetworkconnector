package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterPostingData;

/**
 * 
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	Handler
 * @version		1.0
 * 
 * @description Implementation of the twitter posting manager - extends
 *              GenericDataManager This handler is used to save a new tweet or
 *              update an existing one. TwitterPostingManager is called after a
 *              posting with all relevant information about it (the original
 *              as well as, in a future version, the retweeted one) is decoded by
 *              TwitterParser.
 * 
 * @param <TwitterPosting>
 * 
 * @changelog	1.0 class created
 *
 */

public class TwitterPosting extends GenericDataManager<TwitterPostingData> {
	
	private TwitterPostingData data;
	
	public TwitterPosting(JSONObject jsonObject) {
		data = new TwitterPostingData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
