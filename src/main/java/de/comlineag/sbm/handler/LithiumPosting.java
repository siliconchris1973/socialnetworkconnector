package de.comlineag.sbm.handler;

import org.json.simple.JSONObject;

import de.comlineag.sbm.data.LithiumPostingData;

/**
 * 
 * @author Christian Guenther
 * @category Parser
 * 
 * @description Implementation of the lithium posting manager - extends
 *              GenericDataManager This handler is used to save a new post or
 *              update an existing one. LithiumPostingManager is called after a
 *              posting with all relevant information about the posting (the
 *              original as well as the reposted one) is decoded by
 *              LithiumParser.
 * 
 * 				The data type lithium posting consists of these elements
 *	            	id						Long 
 *					created_at				String 
 *					text					String 
 *					source					String
 *            		truncated				Boolean 
 *            		in_reply_to_status_id	Long
 *            		in_reply_to_user_id		Long 
 *            		in_reply_to_screen_name	String
 *            		coordinates				List 
 *            		place					List 
 *            		lang					String 
 *            		hashtags				List
 *            		symbols					List
 *            		user_mentions			List
 * 
 * @param <LithiumPosting>
 * 
 */

public class LithiumPosting extends GenericDataManager<LithiumPostingData> {
	/*
	 * Die nachfolgenden Elemente des Tweets sollen weiter verarbeitet und
	 * gespeichert werden
	 * 
	 * key="cl_postID" 					value="id" 
	 * key="cl_postTime" 				value="created_at"
	 * key="cl_posting" 				value="text" 
	 * key="cl_postClient" 				value="source"
	 * key="cl_postTruncated" 			value="truncated" 
	 * key="cl_postInReplyTo" 			value="in_reply_to_status_id" 
	 * key="cl_postInReplyToUserID" 	value="in_reply_to_user_id" 
	 * key="cl_postInReplyToScreenName"	value="in_reply_to_screen_name" 
	 * key="cl_postGeoLocation"			value="coordinates" 
	 * key="cl_postPlace"				value="place" 
	 * key="cl_postLang" 				value="lang" 
	 * key="cl_postHashtags" 			value="hashtags" 
	 * key="cl_postSymbols" 			value="symbols"
	 * key="cl_userMentions" 			value="mentions"
	 */

	private LithiumPostingData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Baut aus dem JSON String ein LithiumPostingData Objekt
	 * 
	 * @param jsonObject
	 */
	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
	}

	@Override
	// public void save(List<LithiumPosting> posting){
	public void save() {
		persistenceManager.savePosts(data);
	}
}
