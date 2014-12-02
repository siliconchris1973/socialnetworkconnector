package de.comlineag.snc.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.helper.UniqueIdServices;

/**
 * 
 * @author		Christian Guenther
 * @category	Data Class
 * @version		0.1				- 11.11.2014
 * @status		productive
 * 
 * @description data type representing a social_network in the graph
 * 
 * @changelog	0.1	(Chris)		class created as copy from DomainData Version 0.1
 * 
 */
public class SocialNetworkData implements ISncDataObject{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	protected final GraphNodeTypes gnt = GraphNodeTypes.SOCIALNETWORK;
	protected String id;							// the id of the social network
	protected String sn_id;							// the 2-digit code of the social network 
	protected String name;							// the name of social network 
	protected String domain;						// the www-domain of social network 
	protected String description;					// a description about the social network 
	
	protected JSONObject internalJson = new JSONObject();
	
	public SocialNetworkData(){}
	
	public SocialNetworkData(String jsonString){
		try {
		// now do the check on json error details within  the returned JSON object
		JSONParser objParser = new JSONParser();
		Object obj = objParser.parse(jsonString);
		JSONObject jsonObj = obj instanceof JSONObject ?(JSONObject) obj : null;
		
		createFromJson(jsonObj);
		
		} catch (ParseException e) {
			logger.error("ERROR :: parsing given json string {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public SocialNetworkData(JSONObject jsonObj){
		createFromJson(jsonObj);
	}
	
	
	private void createFromJson(JSONObject obj){
		if(obj.containsKey("name") || obj.containsKey("sn_id")){
			logger.debug("creating new social network object ({}) from JSON", obj.get("name").toString());
			
			if (obj.containsKey("id"))
				setId(obj.get("id").toString());
			else
				if (obj.containsKey("name"))
					setId(UniqueIdServices.createMessageDigest(obj.get("name").toString()));
				else
					setId(UniqueIdServices.createMessageDigest(obj.get("sn_id").toString()));
			
			setSnId(obj.get("sn_id").toString());
			setName(obj.get("name").toString());
			setDomain(obj.get("domain").toString());
			setDescription(obj.get("description").toString());
			
			internalJson = obj;
		} else {
			logger.error("ERROR :: cannot create a social network without a name");
		}
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	public String toJsonString(){return internalJson.toJSONString();}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String snid) {this.sn_id = snid;}
	
	public String getName() {return name;}
	public void setName(String value) {this.name = value;}
	
	public String getDomain() {return domain;}
	public void setDomain(String dom) {this.domain = dom;}
	
	public String getDescription() {return description;}
	public void setDescription(String desc) {this.description = desc;}
}
