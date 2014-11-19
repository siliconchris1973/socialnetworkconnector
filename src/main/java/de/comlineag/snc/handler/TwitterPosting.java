package de.comlineag.snc.handler;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.data.TwitterPostingData;

/**
 * 
 * @author 		Magnus Leinemann, Christian Günther
 * @category 	Handler
 * @version		0.1					- 11.11.2014
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
 * 				0.2	(Chris)			added call to graph database
 * 
 */

public class TwitterPosting extends GenericDataManager<TwitterPostingData> {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private TwitterPostingData data;
	
	@SuppressWarnings("unchecked")
	public TwitterPosting(JSONObject jsonObject) {
		// first extract ONLY the twitter-posting from the bigger json object
		data = new TwitterPostingData(jsonObject);
		
		// next call the graph engine and store data also in the external graph
		if (rtc.getBooleanValue("ActivateGraphDatabase", "runtime")) {
			JSONObject bigJson=new JSONObject(data.getJson());
			
			logger.trace("this is the BIG dataset: {}", bigJson);
			System.exit(-1);
			logger.info("calling graph database for {}-{} ", bigJson.get("sn_id").toString(), bigJson.get("id").toString());
			graphPersistenceManager.saveNode(jsonObject);
		}
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
