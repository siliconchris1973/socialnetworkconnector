package de.comlineag.snc.persistence;

import java.net.URI;
import java.net.URL;

import org.json.simple.JSONObject;

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
	 * @description	implementation for saving the a node in the graph
	 * 
	 * @param		JSONObject of the node
	 */
	public void saveNode(JSONObject nodeObject);
	
	/**
	 * @description	find a node by a key value pair and return the url to it
	 * 
	 * @param 		String key		- search for key
	 * @param		String value	- with this vlaue
	 * @param		String label	- in nodes with label
	 */
	public URL findNode(String key, String value, String label);
	
	/**
	 * @description	create a relationship between two nodes (given as URI) of a type defined in GraphRelationshipTypes 
	 * 
	 * @param 		URI source node
	 * @param 		URI target node
	 * @param		RelationshipTypes type of relationship
	 * @param		addition data for the relationship (properties)
	 */
	public void createRelationship(URI sourceNode, URI targetNode, GraphRelationshipTypes relationshipType, String additionalData);
}
