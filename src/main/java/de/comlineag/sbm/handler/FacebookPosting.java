package de.comlineag.sbm.handler;

import org.json.simple.JSONObject;

import de.comlineag.sbm.data.FacebookPostingData;

/**
 * 
 * @author Christian Guenther
 * @category Parser
 * 
 * @description Implementation of the facebook posting manager - extends
 *              GenericDataManager This handler is used to save a new post or
 *              update an existing one. FacebookPostingManager is called after a
 *              posting with all relevant information about the posting (the
 *              original as well as the reposted one) is decoded by
 *              FacebookParser.
 * 
 * 				The data type facebook posting consists of these elements
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
 * @param <FacebookPosting>
 * 
 */

public class FacebookPosting extends GenericDataManager<FacebookPostingData> {
	/*
	 * Die nachfolgenden Elemente des Posts sollen weiter verarbeitet und
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

	private FacebookPostingData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Baut aus dem JSON String ein FacebookPostingData Objekt
	 * 
	 * @param jsonObject
	 */
	public FacebookPosting(JSONObject jsonObject) {
		data = new FacebookPostingData(jsonObject);
	}

	@Override
	// public void save(List<FacebookPosting> posting){
	public void save() {
		persistenceManager.savePosts(data);
	}
}
