package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.WebUserData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1				- 10.10.2014
 * @status		productive
 * 
 * @description Implementation of the web parser user manager - extends
 *              GenericDataManager This handler is used to save a new user or
 *              update an existing one. SimpleWebUser is called after a
 *              page with all relevant information about it is decoded and
 *              the user data from within this page is extracted. In very 
 *              simple cases, where there is no real user, this data is 
 *              created from the page data. 
 *              The WebUserData handling differs from, say, Twitter User handling
 *              in that the user-object is embedded within the page object.
 *              As a consequence, the parser and the crawler have to operate
 *              a bit different on the posting (aka page) and user-object.
 * 
 * @param <WebUserData>
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */

public class SimpleWebUser extends GenericDataManager<WebUserData> {
	
	private WebUserData data;
	
	public SimpleWebUser(JSONObject jsonObject) {
		data = new WebUserData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.saveUsers(data);
	}
}
