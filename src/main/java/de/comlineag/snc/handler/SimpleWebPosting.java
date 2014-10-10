package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.WebPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2				- 10.10.2014
 * @status		productive
 * 
 * @description Implementation of the web parser posting manager - extends
 *              GenericDataManager This handler is used to save a new page or
 *              update an existing one. SimpleWebPosting is called after a
 *              page with all relevant information about it is decoded by
 *              SimpleWebParser.
 *              The WebUserData handling differs from, say, Twitter User handling
 *              in that the user-object is embedded within the page object.
 *              As a consequence, the parser and the crawler have to operate
 *              a bit different on the posting (aka page) and user-object. One
 * 				consequence is, that the SimpleWebPosting class introduces a
 * 				new method getUser.
 * 
 * @param <WebPage>
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				implemented getUser method to return embedded user object from page object
 * 
 */

public class SimpleWebPosting extends GenericDataManager<WebPostingData> {
	
	private WebPostingData data;
	
	public SimpleWebPosting(JSONObject jsonObject) {
		data = new WebPostingData(jsonObject);
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
	
	// introduced getUser method to get the embedded user object from the page object
	public JSONObject getUser(){
		return data.getUser();
	}
}
