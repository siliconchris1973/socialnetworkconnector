package de.comlineag.snc.constants;


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
public enum GraphNodeTypes {
	DOMAIN		("domain"),
	CUSTOMER	("customer"),
	USER		("user"),
	POST		("post"),
	SITE		("post"),
	TWEET		("post"),
	KEYWORD		("keyword"),
	HASHTAG		("hashtag");
	
	
	private final String value;
	
	private GraphNodeTypes(final String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
}
