package de.comlineag.snc.data;

import org.json.simple.JSONObject;


/**
 * 
 * @author		Christian Guenther
 * @category	Data Class
 * @version		0.1				- 18.11.2014
 * @status		productive
 * 
 * @description data type representing a various cascaded jsons to represent a 
 * 				post, user, domain, customer, social network and keywords
 * 
 * @changelog	0.1	(Chris)		class created
 * 
 */
public class GraphMasterData implements ISncDataObject{

	protected JSONObject postJson = new JSONObject();
	protected JSONObject userJson = new JSONObject();
	protected JSONObject domainJson = new JSONObject();
	protected JSONObject customerJson = new JSONObject();
	protected JSONObject socialNetworkJson = new JSONObject();
	protected JSONObject keywordJson = new JSONObject();
	
	public GraphMasterData(JSONObject obj){}
	

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getJson() {
		JSONObject bigJson = new JSONObject();
		bigJson.put("POST", postJson);
		bigJson.put("USER", userJson);
		bigJson.put("DOMAIN", domainJson);
		bigJson.put("CUSTOMER", customerJson);
		bigJson.put("SOCIALNETWORK", socialNetworkJson);
		bigJson.put("KEYWORDT", keywordJson);
		return bigJson;
	}
	
}
