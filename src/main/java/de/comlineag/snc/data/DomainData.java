package de.comlineag.snc.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.LithiumConstants;
import de.comlineag.snc.constants.LithiumStatusCode;
import de.comlineag.snc.helper.UniqueIdServices;

/**
 * 
 * @author		Christian Guenther
 * @category	Data Class
 * @version		0.1				- 11.11.2014
 * @status		productive
 * 
 * @description data type representing a domain of interest in the graph
 * 
 * @changelog	0.1	(Chris)		class created as copy from KeywordData Version 0.1
 * 
 */
public class DomainData implements ISncDataObject{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	protected final GraphNodeTypes gnt = GraphNodeTypes.DOMAIN;
	protected String id;							// the id of the user within the social network
	protected String name;						// the name of domain itself
	protected String lang;							// default language of the domain
	
	protected JSONObject internalJson = new JSONObject();
	
	public DomainData(){}
	
	public DomainData(String jsonString){
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
	
	public DomainData(JSONObject jsonObj){
		createFromJson(jsonObj);
	}
	
	
	private void createFromJson(JSONObject obj){
		if(obj.containsKey("name")){
			logger.debug("creating new domain object ({}) from JSON", obj.get("name").toString());
			
			if (obj.containsKey("id"))
				setId(obj.get("id").toString());
			else
				setId(UniqueIdServices.createMessageDigest(obj.get("name").toString()));
				
			setName(obj.get("name").toString());
			if(obj.containsKey("lang"))
				setLang(obj.get("lang").toString());
			else 
				setLang("DE");
			
			internalJson = obj;
		} else {
			logger.error("ERROR :: cannot create a domain of interest without a name");
		}
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	public String toJsonString(){return internalJson.toJSONString();}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getName() {return name;}
	public void setName(String value) {this.name = value;}
	
	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}
}
