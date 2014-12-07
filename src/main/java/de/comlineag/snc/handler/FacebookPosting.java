package de.comlineag.snc.handler;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.data.CustomerData;
import de.comlineag.snc.data.DomainData;
import de.comlineag.snc.data.FacebookPostingData;
import de.comlineag.snc.data.FacebookUserData;
import de.comlineag.snc.data.SocialNetworkData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.3				- 28.11.2014
 * @status		productive
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
 *	            	sn_id					String
 *					id						String 
 *					created_at				String
 *					source					String
 *            		lang					String 
 *					text					String
 *					raw_text				String
 *					subject					String
 *					teaser					String
 *					source					String
 *            		truncated				Boolean 
 *            		in_reply_to_status_id	Long
 *            		in_reply_to_user_id		Long 
 *            		in_reply_to_screen_name	String
 *            		geoLocation				List 
 *            		user_mentions			List
 *            		USER					embedded UserData object
 *            		DOMAIN					embedded domain of interest object
 *            		CUSTOMER				embedded customer object
 *            		SOCIALNETWORK			embedded social network object
 *            		KEYWORD					embedded object with list of keywords
 * 
 * @param <FacebookPosting>
 * 
 * @changelog	0.1 (Chris)		copy from TwitterPosting
 * 				0.2				added getJson() and addEmbeddedUserData method
 * 				0.3				added call to graph database
 * 
 */

public class FacebookPosting extends GenericDataManager<FacebookPostingData> {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private FacebookPostingData data;
	
	public FacebookPosting(JSONObject jsonObject) {
		data = new FacebookPostingData(jsonObject);
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
			graphPersistenceManager.createNodeObject(bigJson);
		} catch (Exception e) {
			logger.error("ERROR :: during call of graph-db layer {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public JSONObject getJson(){return(data.getJson());}
	public JSONObject getUserAsJson(){return(data.getUserData().getJson());}
	
	public void setUserObject(FacebookUserData userData){ data.setUserData(userData);}
	public FacebookUserData getUserObject(){return((FacebookUserData) data.getUserData());}
	
	public void setDomainObject(DomainData domData){ data.setDomainObject(domData);}
	public DomainData getDomainObject(){return((DomainData) data.getDomainObject());}
	
	public void setCustomerObject(CustomerData subData){ data.setCustomerObject(subData);}
	public CustomerData getCustomerObject(){return((CustomerData) data.getCustomerObject());}
	
	public void setSocialNetworkObject(SocialNetworkData socData){ data.setSocialNetworkObject(socData);}
	public SocialNetworkData getSocialNetworkObject(){return((SocialNetworkData) data.getSocialNetworkObject());}
	
	public void setTrackTerms(ArrayList<String> tTerms){ data.setTrackTerms(tTerms);}
	public ArrayList<String> getTrackTerms(){return((ArrayList<String>) data.getTrackTerms());}
}
