package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.WebUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2				- 19.11.2014
 * @status		productive
 * 
 * @description Implementation of the web parser user manager - extends
 *              GenericDataManager This handler is used to save a new user or
 *              update an existing one. WebUser is called after a
 *              page with all relevant information about it is decoded and
 *              the user data from within this page is extracted. In very 
 *              simple cases, where there is no real user, this data is 
 *              created from the page data with the dns domain name as the name
 *              and the id created as a hash from the url.
 *               
 *              The WebUserData handling differs from, say, Twitter User handling
 *              in that the user-object is embedded within the page object.
 *              As a consequence, the parser and the crawler have to operate
 *              a bit different on the posting (aka page) and user-object.
 * 
 * @param <WebUserData>
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				added getJson() and getUserData() method
 * 
 */

public class WebUser extends GenericDataManager<WebUserData> {
	
	private WebUserData data;
	
	public WebUser(JSONObject jsonObject) {
		data = new WebUserData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.saveUsers(data);
	}
	
	public JSONObject getJson(){
		return(data.getJson());
	}
	
	public WebUserData getUserData(){
		return(data);
	}
}
