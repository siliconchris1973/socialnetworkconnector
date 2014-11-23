package de.comlineag.snc.data;

import org.json.simple.JSONObject;

import de.comlineag.snc.constants.GraphNodeTypes;

/**
 * 
 * @author		Christian Guenther
 * @category	Data Class
 * @version		0.1				- 11.11.2014
 * @status		productive
 * 
 * @description data type representing a keyword in the graph
 * 
 * @changelog	0.1	(Chris)		class created as copy from GraphUserData Version 0.1
 * 
 */
public class KeywordData implements ISncDataObject{

	protected final GraphNodeTypes gnt = GraphNodeTypes.KEYWORD;
	protected String id;							// the id of the user within the social network
	protected String keyword;						// the value aka keyword itself
	protected String lang;							// default language of the keyword
	
	protected JSONObject internalJson = new JSONObject();
	
	public KeywordData(JSONObject obj){
		setId(obj.get("id").toString());
		setKeyword(obj.get("keyword").toString());
		setLang(obj.get("lang").toString());
		
		internalJson = obj;
		//internalJson.put("Label", gnt);
	}
	
	private String toJsonString(){
		/*
		JSONObject obj = new JSONObject();
		obj.put("id", getId());
		obj.put("keyword", getKeyword());
		obj.put("lang", getLang());
		
		return obj.toJSONString();
		*/
		
		return internalJson.toJSONString();
	}
	
	/**
	 * @description	creates a string which can be passed to the neo4j cypher engine 
	 * @return		cypher string
	 */
	public String createCypher(){
		return "\""+gnt.toString()+"\" "+toJsonString();
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getKeyword() {return keyword;}
	public void setKeyword(String value) {this.keyword = value;}
	
	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}
}
