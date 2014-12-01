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
 * @description data type representing a customer in the graph
 * 
 * @changelog	0.1	(Chris)		class created as copy from DomainData Version 0.1
 * 
 */
public final class CustomerData implements ISncDataObject{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	protected final GraphNodeTypes gnt = GraphNodeTypes.CUSTOMER;
	protected String id;						// the id of the customer - if not passed, it is created as hash from name
	protected String name;						// the name of the customer 
	protected String url;						// the ww-url of the customer 
	protected String lang;						// default language of the customer entry
	
	protected JSONObject internalJson = new JSONObject();
	
	public CustomerData(){}
	
	public CustomerData(JSONObject obj){
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
			
			if(obj.containsKey("url"))
				setUrl(obj.get("url").toString());
			
			internalJson = obj;
		} else {
			logger.error("ERROR :: cannot create a customer without a name");
		}
		
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	public String toJsonString(){return internalJson.toJSONString();}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getName() {return name;}
	public void setName(String value) {this.name= value;}
	
	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}
	
	public String getUrl() {return url;}
	public void setUrl(String url) {this.url = url;}
}
