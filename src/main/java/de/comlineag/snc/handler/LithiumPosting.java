package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.LithiumPostingData;
import de.comlineag.snc.data.LithiumUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2				- 19.11.2014
 * @status		productive
 * 
 * @description Implementation of the Lithium posting handler - extends
 *              GenericDataManager. This handler is used to save a new post or
 *              update an existing one. LithiumPostingManager is called after a
 *              posting with all relevant information about the posting is decoded 
 *              by LithiumParser.
 * 
 *              The handler also provides a method to get the underlying data object 
 *              as a json structure (getJson) and a method to add an embedded user-
 *              object to the post object - addEmbeddedUserData(UserData). 
 *              These last two methods are needed for neo4j, the graph database. 
 *              Neo4j, in contrast to the HANA db, expects all data of a post, including 
 *              the user, domain, customer, social network and keyword, to be passed 
 *              along in one json structure.
 * 
 * @param <LithiumPostingData>
 * 					Data type 
 * 
 * @changelog	0.1 (Chris)		class created as copy from TwitterUser
 * 				0.2				added getJson() and addEmbeddedUserData method
 * 
 */

public class LithiumPosting extends GenericDataManager<LithiumPostingData> {
	// this holds a reference to the runtime configuration
	//private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	//private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private LithiumPostingData data;
	
	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
		
		// TODO add graph db call
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public void addEmbeddedUserData(LithiumUserData userData){
		data.setUserData(userData);
	}
}
