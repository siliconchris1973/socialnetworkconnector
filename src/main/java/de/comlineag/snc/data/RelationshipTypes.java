package de.comlineag.snc.data;

import org.neo4j.graphdb.RelationshipType;


/**
 * 
 * @author 		Christian Guenther
 * @category	enum
 * @version 	1.1
 * 
 * @description describes the different possible connection types between two nodes in the graph
 * 				the enum contains a static for the field within the twitter/facebook etc. data object
 * 				and a corresponding attribute for the edge within the graph
 * 				This enum is currently only used by the Neo4J Graph DB, but might be used by the HANA
 * 				Graph Engine as well. 
 * 				
 * 				ENUM Static				attribute		cypher as ascii art example
 * 				---------------------------------------------------------------------------
 * 				IN_REPLY_TO_STATUS		REPLIED_ON		(p:Post-ID) -[REPLIED_ON]-> (p:Post-ID)
 * 				IN_REPLY_TO_USER		REPLIED_TO		(p:Post-ID) -[REPLIED_TO]-> (u:User-ID) 
 * 				USER_MENTIONS			MENTIONED		(p:Post-ID) -[MENTIONED]-> (u:User-ID)
 * 				RETWEETED				RETWEETED		(u:User-ID) -[RETWEETED]-> (p:Post-ID)
 * 				FAVORITED				FAVORITED		(u:User-ID) -[FAVORITED]-> (p:Post-ID)
 * 				FOLLOWS					FOLLOWS			(u:User-ID) -[FOLLOWS]-> (u:User-ID)
 * 				AUTHORED				AUTHORED		(u:User-ID) -[AUTHORED]-> (p:Post-ID)
 * 				
 *
 */
public enum RelationshipTypes implements RelationshipType {
	IN_REPLY_TO_STATUS("REPLIED_ON"),
	IN_REPLY_TO_USER("REPLIED_TO"),
	USER_MENTIONS("MENTIONED"),
	RETWEETED("RETWEETED"),
	FAVORITED("FAVORITED"),
	FOLLOWS("FOLLOWS"),
	AUTHORED("AUTHORED"),
	MENTIONED("MENTIONED");

	private final String value;
	
	private RelationshipTypes(final String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
}
