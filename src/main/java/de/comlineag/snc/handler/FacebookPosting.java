package de.comlineag.snc.handler;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.data.FacebookPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.1
 * @status		not implemented
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
 *            		geoLocation				List 
 *            		lang					String 
 *            		hashtags				List
 *            		symbols					List
 *            		user_mentions			List
 * 
 * @param <FacebookPosting>
 * 
 * @changelog	0.1 (Chris)		copy from TwitterPosting
 * 
 * TODO implement real code
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
	 * key="cl_postPlace"				value="geoLocation" 
	 * key="cl_postLang" 				value="lang" 
	 * key="cl_postHashtags" 			value="hashtags" 
	 * key="cl_postSymbols" 			value="symbols"
	 * key="cl_userMentions" 			value="mentions"
	 */

	private FacebookPostingData data;
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * Baut aus dem JSON String ein FacebookPostingData Objekt
	 * 
	 * @param jsonObject
	 */
	@SuppressWarnings("unchecked")
	public FacebookPosting(JSONObject jsonObject) {
		data = new FacebookPostingData(jsonObject);
		
		// next call the graph engine and store data also in the external graph
		if (rtc.getBooleanValue("ActivateGraphDatabase", "runtime")) {
			if (!jsonObject.containsKey("sn_id"))
				jsonObject.put("sn_id", data.getSnId());
			if (!jsonObject.containsKey("teaser"))
				jsonObject.put("teaser", data.getTeaser());
			if (!jsonObject.containsKey("subject"))
				jsonObject.put("subject", data.getSubject());
			if (!jsonObject.containsKey("domain"))
				jsonObject.put("domain", data.getDomain());
			if (!jsonObject.containsKey("customer"))
				jsonObject.put("customer", data.getCustomer());
			logger.info("calling graph database for {}-{} ", data.getSnId().toString(), jsonObject.get("id"));
			graphPersistenceManager.saveNode(jsonObject, GraphNodeTypes.POST);
		}
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
