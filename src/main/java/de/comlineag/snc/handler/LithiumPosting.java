package de.comlineag.snc.handler;

import org.json.simple.JSONObject;

import de.comlineag.snc.data.LithiumPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.2				- 11.11.2014
 * @status		productive
 * 
 * @description Implementation of the Lithium posting handler - extends
 *              GenericDataManager. This handler is used to save a new post or
 *              update an existing one. LithiumPostingManager is called after a
 *              posting with all relevant information about the posting is decoded 
 *              by LithiumParser.
 * 
 * 
 * @param <LithiumPostingData>
 * 					Data type 
 * 
 * @changelog	0.1 (Chris)		class created as copy from TwitterUser
 * 				0.2				added call to graph database
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
}
