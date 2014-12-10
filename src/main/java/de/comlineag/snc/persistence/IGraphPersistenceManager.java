package de.comlineag.snc.persistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.json.simple.JSONObject;

import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.GraphRelationshipTypes;


/**
 * 
 * @author		Christian Günther
 * @category	Interface
 * @version 	0.1
 * @status		productive
 * 
 * @description Interface definition for a graph-db persistence manager
 * 
 * @changelog	0.1 (Chris)		initial version
 * 
 */
public interface IGraphPersistenceManager {

	/**
	 * @description	implementation for saving the node(s) in the graph
	 * 
	 * @param		a complex JSONObject containing everything needed to create a node with
	 * 				it's relationships:
	 * 				The post, the user, the social network, the domain of interest and 
	 * 				the customer and an array of keywords with which the post was tracked
	 * 
	 * Structure:
	 * 		{
	 * 			POST: {
	 * 					id: "id", 
	 * 					text: "text", 
	 * 					teaser: "teaser", 
	 * 					subject: "subject", 
	 * 					lang: "lang"
	 * 				  },
	 * 			USER: {
	 * 					id: "id",
	 * 					name: "name",
	 * 					screen_name: "screen_name"
	 * 				  },
	 * 			DOMAIN {
	 * 						name: "name"
	 * 				   },
	 * 			CUSTOMER: {
	 * 						name: "name"
	 * 					  },
	 * 			SOCIALNETWORK: {
	 * 								sn_id: "sn_id",
	 * 								name: "name",
	 * 								domain: "www-domain",
	 * 								description: "description"
	 * 						   },
	 * 			KEYWORD: {
	 * 						name: "name"
	 * 					 }
	 * 		}
	 */
	public void createNodeObject(JSONObject nodeObject);
}
