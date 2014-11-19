package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.FacebookPostingData;
import de.comlineag.snc.data.FacebookUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.2
 * @status		not implemented
 * 
 * @description Implementation of the facebook posting manager - extends
 *              GenericDataManager This handler is used to save a new post or
 *              update an existing one. FacebookPostingManager is called after a
 *              posting with all relevant information about the posting (the
 *              original as well as the reposted one) is decoded by FacebookParser.
 *              
 *              The handler also provides a method to get the underlying data object 
 *              as a json structure (getJson) and a method to add an embedded user-
 *              object to the post object - addEmbeddedUserData(UserData). 
 *              These last two methods are needed for neo4j, the graph database. 
 *              Neo4j, in contrast to the HANA db, expects all data of a post, including 
 *              the user, domain, customer, social network and keyword, to be passed 
 *              along in one json structure.
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
 *            		USER					embedded UserData object
 * 
 * @param <FacebookPosting>
 * 
 * @changelog	0.1 (Chris)		copy from TwitterPosting
 * 				0.2				added getJson() and addEmbeddedUserData method
 * 
 * TODO implement real code
 */

public class FacebookPosting extends GenericDataManager<FacebookPostingData> {
	
	private FacebookPostingData data;
	// this holds a reference to the runtime configuration
	//private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	//private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * Baut aus dem JSON String ein FacebookPostingData Objekt
	 * 
	 * @param jsonObject
	 */
	public FacebookPosting(JSONObject jsonObject) {
		data = new FacebookPostingData(jsonObject);
		
		// TODO add graph db call
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public void addEmbeddedUserData(FacebookUserData userData){
		data.setUserData(userData);
	}
}
