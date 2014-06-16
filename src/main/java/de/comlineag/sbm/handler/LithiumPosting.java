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
 * 				The data type Lithium posting consists of these elements
 *	            	id						Long 
 *					created_at				String 
 *					text					String 
 *					raw_text				String 
 *					source					String
 *            		truncated				Boolean 
 
 * 
 * @param <LithiumPostingData>
 * 					Data type 
 * 
 */

public class LithiumPosting extends GenericDataManager<LithiumPostingData> {
	/*
	 * Die nachfolgenden Elemente des Posts sollen weiter verarbeitet und
	 * gespeichert werden
	 * 
	 * key="cl_postID" 					value="id" 
	 * key="cl_postTime" 				value="created_at"
	 * key="cl_posting" 				value="text" 
	 * key="cl_posting_raw" 			value="raw_text" 
	 * key="cl_postClient" 				value="source"
	 * key="cl_postTruncated" 			value="truncated" 
	 */
	
	private LithiumPostingData data;

	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
