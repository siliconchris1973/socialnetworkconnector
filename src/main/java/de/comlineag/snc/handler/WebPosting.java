package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.WebUserData;
import de.comlineag.snc.data.WebPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.3				- 19.11.2014
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
 * @param <WebPage>
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				implemented getUser method to return embedded user object from page object
 * 				0.3				added getJson() and addEmbeddedUserData method
 * 				0.4				added call to graph database 
 * 
 */

public class WebPosting extends GenericDataManager<WebPostingData> {
	// this holds a reference to the runtime configuration
	//private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	//private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private WebPostingData data;
	
	public WebPosting(JSONObject jsonObject) {
		// first create new data element
		data = new WebPostingData(jsonObject);
		
		// TODO add graph db call
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
	
	// introduced getUser method to get the embedded user object from the page object
	public JSONObject getUser(){
		return data.getUser();
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public void addEmbeddedUserData(WebUserData userData){
		data.setUserData(userData);
	}
}
