package de.comlineag.snc.handler;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.data.CustomerData;
import de.comlineag.snc.data.DomainData;
import de.comlineag.snc.data.SocialNetworkData;
import de.comlineag.snc.data.TwitterUserData;
import de.comlineag.snc.data.WebUserData;
import de.comlineag.snc.data.WebPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.4				- 28.11.2014
 * @status		productive
 * 
 * @description Implementation of the web parser posting manager - extends
 *              GenericDataManager This handler is used to save a new page or
 *              update an existing one. WebPosting is called after a
 *              page with all relevant information about it is decoded by
 *              SimpleWebParser.
 *              The WebUserData handling differs from, say, Twitter User handling
 *              in that the user-object is embedded within the page object.
 *              As a consequence, the parser and the crawler have to operate
 *              a bit different on the posting (aka page) and user-object. One
 * 				consequence is, that the WebPosting class introduces a new method 
 * 				getUser() to return an embedded json object of a user.
 * 
 *              The handler also provides a method to get the underlying data object 
 *              as a json structure (getJson) and a method to add an embedded user-
 *              object to the post object - addEmbeddedUserData(UserData). 
 *              These last two methods are needed for neo4j, the graph database. 
 *              Neo4j, in contrast to the HANA db, expects all data of a post, including 
 *              the user, domain, customer, social network and keyword, to be passed 
 *              along in one json structure.
 * 				
 * 				The data type lithium posting consists of these elements
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
 *            		USER					embedded UserData object
 *            		DOMAIN					embedded domain of interest object
 *            		CUSTOMER				embedded customer object
 *            		SOCIALNETWORK			embedded social network object
 *            		KEYWORD					embedded object with list of keywords
 *            
 * @param <WebPage>
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				implemented getUser method to return embedded user object from page object
 * 				0.3				added getJson() and addEmbeddedUserData method
 * 				0.4				added call to graph database 
 * 
 */

public class WebPosting extends GenericDataManager<WebPostingData> {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private WebPostingData data;
	
	public WebPosting(JSONObject jsonObject) {
		data = new WebPostingData(jsonObject);
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
	
	// introduced getUser method to get the embedded user object from the page object
	public JSONObject getJson(){return(data.getJson());}
	public JSONObject getUserAsJson(){return(data.getUserData().getJson());}
	
	public void setUserObject(WebUserData userData){ data.setUserData(userData);}
	public WebUserData getUserObject(){return((WebUserData) data.getUserData());}
	
	public void setDomainObject(DomainData domData){ data.setDomainObject(domData);}
	public DomainData getDomainObject(){return((DomainData) data.getDomainObject());}
	
	public void setCustomerObject(CustomerData subData){ data.setCustomerObject(subData);}
	public CustomerData getCustomerObject(){return((CustomerData) data.getCustomerObject());}
	
	public void setSocialNetworkObject(SocialNetworkData socData){ data.setSocialNetworkObject(socData);}
	public SocialNetworkData getSocialNetworkObject(){return((SocialNetworkData) data.getSocialNetworkObject());}
	
	public void setTrackTerms(ArrayList<String> tTerms){ data.setTrackTerms(tTerms);}
	public ArrayList<String> getTrackTerms(){return((ArrayList<String>) data.getTrackTerms());}
}
