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
 * 				IN_REPLY_TO_STATUS		REPLIED_ON		(p:Post-ID) 	  -[REPLIED_ON]->	(p:Post-ID)
 * 				IN_REPLY_TO_USER		REPLIED_ON		(p:Post-ID) 	  -[REPLIED_ON]->	(u:User-ID) 
 * 				USER_MENTIONS			MENTIONED		(p:Post-ID) 	  -[MENTIONED]->	(u:User-ID)
 * 				RETWEETED				REPOSTED		(u:User-ID) 	  -[REPOSTED]->		(p:Post-ID)
 * 				FAVORITED				FAVORITED		(u:User-ID) 	  -[FAVORITED]->	(p:Post-ID)
 * 				FOLLOWS					FOLLOWS			(u:User-ID) 	  -[KNOWS]->		(u:User-ID)
 * 				AUTHORED				WROTE			(u:User-ID) 	  -[WROTE]->		(p:Post-ID)
 * 				TRACKED_FOR				TRACKED_FOR		(p:Post-ID) 	  -[TRACKED_FOR]->	(d:Domain-name)
 * 				BELONGS_TO				BELONGS_TO		(c:Customer-name) -[BELONGS_TO]->	(d:Domain-name)
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
	BELONGS_TO,
	TRACKED_FOR,
	FETCHED_FROM,
	RELEVANT_FOR;
}
