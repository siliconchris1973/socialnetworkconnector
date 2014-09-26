package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.WebPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1		- 25.09.2014
 * @status		productive
 * 
 * @description Implementation of the web parser posting manager - extends
 *              GenericDataManager This handler is used to save a new page or
 *              update an existing one. SimpleWebPosting is called after a
 *              page with all relevant information about it is decoded by
 *              SimpleWebParser.
 * 
 * @param <WebPage>
 * 
 * @changelog	0.1 (Chris)		class created
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
}
