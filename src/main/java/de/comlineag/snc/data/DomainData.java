package de.comlineag.snc.data;

import org.json.simple.JSONObject;
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
	
	public DomainData(JSONObject obj){
		if(obj.containsKey("name")){
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
