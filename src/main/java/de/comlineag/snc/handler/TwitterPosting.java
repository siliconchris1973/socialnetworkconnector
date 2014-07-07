package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.TwitterPostingData;

/**
 * 
 * @author 		Magnus Leinemann
 * @category 	Handler
 * @version		0.1
 * @status		productive
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
 * @changelog	0.1 (Magnus)		class created
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
