package de.comlineag.snc.constants;

import org.neo4j.graphdb.Label;


/**
 * 
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.1				- 09.11.2014
 * @status		productive
 * 
 * @description describes the different possible nodes within the graph
 * 				
 * @changelog	0.1 (Chris)		class created
 * 
 */
public enum GraphNodeTypes implements Label{
	DOMAIN			("DOMAIN"),
	CUSTOMER		("CUSTOMER"),
	USER			("USER"),
	POST			("POST"),
	SITE			("POST"),
	TWEET			("POST"),
	KEYWORD			("KEYWORD"),
	HASHTAG			("HASHTAG"),
	SOCIALNETWORK	("SOCIALNETWORK");
	
	
	private final String value;
	
	private GraphNodeTypes(final String val) {
		this.value = val;
	}
	
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
}
