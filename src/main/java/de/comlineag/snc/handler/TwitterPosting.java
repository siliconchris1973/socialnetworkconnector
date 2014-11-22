package de.comlineag.snc.handler;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.data.TwitterPostingData;
import de.comlineag.snc.data.TwitterUserData;

/**
 * 
 * @author 		Magnus Leinemann, Christian Günther
 * @category 	Handler
 * @version		0.3					- 19.11.2014
 * @status		productive
 * 
 * @description Implementation of the twitter posting manager - extends
 *              GenericDataManager This handler is used to save a new tweet or
 *              update an existing one. TwitterPostingManager is called after a
 *              posting with all relevant information about it (the original
 *              as well as the retweeted one) is decoded by TwitterParser.
 * 
 *              The handler also provides a method to get the underlying data object 
 *              as a json structure (getJson) and a method to add an embedded user-
 *              object to the post object - addEmbeddedUserData(UserData). 
 *              These last two methods are needed for neo4j, the graph database. 
 *              Neo4j, in contrast to the HANA db, expects all data of a post, including 
 *              the user, domain, customer, social network and keyword, to be passed 
 *              along in one json structure.
 *              
 * @param <TwitterPosting>
 * 
 * @changelog	0.1 (Magnus)		class created
 * 				0.2	(Chris)			added call to graph database
 * 				0.3					added getJson() and addEmbeddedUserData() method
 * 
 */

public class TwitterPosting extends GenericDataManager<TwitterPostingData> {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private TwitterPostingData data;
	
	public TwitterPosting(JSONObject jsonObject) {
		// first extract ONLY the twitter-posting from the bigger json object
		data = new TwitterPostingData(jsonObject);
	}

	@Override
	public void save() {
		try {
			persistenceManager.savePosts(data);
		} catch (Exception e) {
			logger.error("ERROR :: during call of persistence layer {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public void saveInGraph(){
		try {
			JSONObject bigJson=new JSONObject(data.getJson());
			logger.trace("this is the big json with all entities {}", bigJson.toJSONString());
			
			logger.info("calling graph database for {}-{} ", bigJson.get("sn_id").toString(), bigJson.get("id").toString());
			graphPersistenceManager.saveNode(bigJson);
		} catch (Exception e) {
			logger.error("ERROR :: during call of graph-db layer {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public void addEmbeddedUserData(TwitterUserData userData){
		data.setUserData(userData);
	}
}
