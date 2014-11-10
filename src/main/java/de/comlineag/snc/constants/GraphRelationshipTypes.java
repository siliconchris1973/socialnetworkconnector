package de.comlineag.snc.constants;

import org.neo4j.graphdb.RelationshipType;


/**
 * 
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.2				- 08.11.2014
 * @status		productive
 * 
 * @description describes the different possible connection types between two nodes in the graph
 * 				the enum contains a static for the field within the twitter/facebook etc. data object
 * 				and a corresponding attribute for the edge within the graph
 * 				This enum is currently only used by the Neo4J Graph DB, but might be used by the HANA
 * 				Graph Engine as well. 
 * 				
 * 				ENUM Static				attribute		cypher as ascii art example
 * 				---------------------------------------------------------------------------
 * 				IN_REPLY_TO_STATUS		replied_on		(p:Post-ID) -[REPLIED_ON]-> (p:Post-ID)
 * 				IN_REPLY_TO_USER		replied_on		(p:Post-ID) -[REPLIED_ON]-> (u:User-ID) 
 * 				USER_MENTIONS			mentioned		(p:Post-ID) -[MENTIONED]-> (u:User-ID)
 * 				RETWEETED				reposted		(u:User-ID) -[REPOSTED]-> (p:Post-ID)
 * 				FAVORITED				favorited		(u:User-ID) -[FAVORITED]-> (p:Post-ID)
 * 				FOLLOWS					follows			(u:User-ID) -[KNOWS]-> (u:User-ID)
 * 				AUTHORED				wrote			(u:User-ID) -[wrote]-> (p:Post-ID)
 * 				
 * @changelog	0.1 (Chris)		class created
 * 				0.2 			changed properties to lower case
 * 
 */
public enum GraphRelationshipTypes implements RelationshipType {
	REPLIED_ON, 
	MENTIONED, 
	REPOSTED, 
	FAVORITED, 
	RATED, 
	KNOWS, 
	WROTE, 
	TALKS_ABOUT, 
	REFERENCED, 
	IS_ABOUT, 
	CONTAINS, 
	BELONGS_TO;
	
	/*	IN_REPLY_TO_STATUS	("REPLIED_ON"),
	IN_REPLY_TO_POST	("REPLIED_ON"),
	IN_REPLY_TO_USER	("REPLIED_ON"),
	USER_MENTIONS		("MENTIONED"),
	MENTIONED			("MENTIONED"),
	RETWEETED			("REPOSTED"),
	FAVORITED			("FAVORITED"),
	RATED				("RATED"),
	FOLLOWS				("KNOWS"),
	IS_CONNECTED		("KNOWS"),
	FRIEND				("KNOWS"),
	AUTHORED			("WROTE"),
	TALKS_ABOUT			("TALKS_ABOUT"),
	LINKS_TO			("REFERENCED"),
	LINKED_FROM			("REFERENCED"),
	IS_ABOUT			("IS_ABOUT"),
	CONTAINS			("CONTAINS"),
	BELONGS_TO			("BELONGS_TO");
	
	
	private final String value;
	
	private GraphRelationshipTypes(final String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
*/
}
