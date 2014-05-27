package de.comlineag.sbm.data;

/**
 * 
 * @author Christian Guenther
 * @description the different possible connection types between two nodes in the graph
 * 				the enum contains a static for the field within the twitter/facebook etc. data object
 * 				and a corresponding attribute for the edge within the graph
 * 				
 * 				ENUM Static				attribute		cypher as ascii art
 * 				---------------------------------------------------------------------------
 * 				IN_REPLY_TO_STATUS		replied on		(u:John) -[replied on]-> (p:Post-ID)
 * 				IN_REPLY_TO_USER		replied to		(u:John) -[replied to]-> (u:Jane) 
 * 				USER_MENTIONS			mentioned		(u:John) -[mentioned]-> (u:Jane)
 * 				RETWEETED				retweeted		(u:John) -[retweeted]-> (p:Post-ID)
 * 				FAVORITED				favorited		(u:John) -[favorited]-> (p:Post ID)
 *
 */
public enum RelationshipTypes {
	IN_REPLY_TO_STATUS("replied on"),
	IN_REPLY_TO_USER("replied to"),
	USER_MENTIONS("mentioned"),
	RETWEETED("retweeted"),
	FAVORITED("favorited");

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
