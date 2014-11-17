package de.comlineag.snc.handler;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.data.LithiumPostingData;
import de.comlineag.snc.persistence.Neo4JPersistence;

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
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private LithiumPostingData data;

	@SuppressWarnings("unchecked")
	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
		
		// next call the graph engine and store data also in the external graph
		if (rtc.getBooleanValue("ActivateGraphDatabase", "runtime")) {
			if (!jsonObject.containsKey("sn_id"))
				jsonObject.put("sn_id", data.getSnId());
			if (!jsonObject.containsKey("teaser"))
				jsonObject.put("teaser", data.getTeaser());
			if (!jsonObject.containsKey("subject"))
				jsonObject.put("subject", data.getSubject());
			if (!jsonObject.containsKey("domain"))
				jsonObject.put("domain", data.getDomain());
			if (!jsonObject.containsKey("customer"))
				jsonObject.put("customer", data.getCustomer());
			logger.info("calling graph database for {}-{} ", data.getSnId().toString(), jsonObject.get("id"));
			graphPersistenceManager.saveNode(jsonObject, GraphNodeTypes.POST);
		}
	}

	@Override
	public void save() {
		persistenceManager.savePosts(data);
	}
}
