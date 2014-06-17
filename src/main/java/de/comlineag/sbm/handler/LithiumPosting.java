package de.comlineag.sbm.handler;

import org.json.simple.JSONObject;

import de.comlineag.sbm.data.LithiumPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * 
 * @description Implementation of the Lithium posting manager - extends
 *              GenericDataManager This handler is used to save a new post or
 *              update an existing one. LithiumPostingManager is called after a
 *              posting with all relevant information about the posting is decoded 
 *              by LithiumParser.
 * 
 * 
 * @param <LithiumPostingData>
 * 					Data type 
 * 
 */

public class LithiumPosting extends GenericDataManager<LithiumPostingData> {
	private LithiumPostingData data;

	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
