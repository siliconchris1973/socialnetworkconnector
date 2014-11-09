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
 * 				IN_REPLY_TO_STATUS		replied_on		(p:Post-ID) -[replied_on]-> (p:Post-ID)
 * 				IN_REPLY_TO_USER		replied_on		(p:Post-ID) -[replied_on]-> (u:User-ID) 
 * 				USER_MENTIONS			mentioned		(p:Post-ID) -[mentioned]-> (u:User-ID)
 * 				RETWEETED				reposted		(u:User-ID) -[reposted]-> (p:Post-ID)
 * 				FAVORITED				favorited		(u:User-ID) -[favorited]-> (p:Post-ID)
 * 				FOLLOWS					follows			(u:User-ID) -[follows]-> (u:User-ID)
 * 				AUTHORED				wrote			(u:User-ID) -[wrote]-> (p:Post-ID)
 * 				
 * @changelog	0.1 (Chris)		class created
 * 				0.2 			changed properties to lower case
 * 
 */
public enum GraphRelationshipTypes implements RelationshipType {
	IN_REPLY_TO_STATUS	("replied_on"),
	IN_REPLY_TO_POST	("replied_on"),
	IN_REPLY_TO_USER	("replied_on"),
	USER_MENTIONS		("mentioned"),
	RETWEETED			("reposted"),
	FAVORITED			("favorited"),
	FOLLOWS				("follows"),
	AUTHORED			("wrote"),
	MENTIONED			("mentioned"),
	TALKS_ABOUT			("talks_about"),
	LINKS_TO			("linked"),
	LINKED_FROM			("linked"),
	IS_ABOUT			("is_about"),
	CONTAINS			("contains"),
	BELONGS_TO			("belongs_to");
	
	
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
}
