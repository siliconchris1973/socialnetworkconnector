package de.comlineag.snc.data;

import org.json.simple.JSONObject;

/**
 * 
 * @author		Christian Günther
 * @category	Interface
 * @version 	0.1
 * @status		productive
 * 
 * @description Interface definition all data objects must implement.
 * 				Enforces the availability of the public method getJson()
 * 
 * @changelog	0.1 (Chris)		initial version
 * 
 */
public interface ISncDataObject {
	/**
	 * @description every data type in the SNC must provide a method to return
	 * 				it's content as a json object
	 * @return		JSONObject
	 */
	public abstract JSONObject getJson();
	
}
