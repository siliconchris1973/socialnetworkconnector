package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.1
 * @status		productive
 * 
 * @description provides constants for use with the Neo4J Graph DB persistence manager. 
 * 				This class is instantiated by Neo4JPersistence and Neo4JEmbeddedDB and the
 * 				values are referenced therein
 *
 * @changelog	0.1 (Chris)		class created
 * 
 */
public class Neo4JConstants {

	// extension to the URI of a node to store relationships
	public static final String RELATIONSHIP_LOC = "/relationships";
	// extension of the URI of the graph db for cypher queries
	public static final String CYPHERENDPOINT_LOC = "/cypher";
	// extension of the URI to a node
	public static final String NODE_LOC = "/node";
	// extension of the URI for Properties
	public static final String PROPERTY_LOC = "/properties";
	// extension to the URI for the label of a node
	public static final String LABEL_LOC = "/labels";
	
	// if set SNC will automatically create connections between nodes
	// for example: if a new post and a new user is passed, SNC will automatically create the 
	// 				relationship of type authored between these two nodes.
	public static final boolean AUTO_CREATE_EDGE = true;

}
