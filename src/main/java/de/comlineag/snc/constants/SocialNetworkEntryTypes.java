package de.comlineag.snc.constants;

/**
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.2
 * @status		productive
 * 
 * @description an enum with types of data in a social network and whether or not this type should be tracked
 * 
 * @changelog	0.1 (Chris)		enum created
 * 				0.2 			added app, game, forum and wiki
 * 
 */
public enum SocialNetworkEntryTypes {
	// attention: looks like this will be handled by trackterms from applicationContext.xml 
	// because there it can be configured individually for each crawler
	UNKNOWN 		("Unknown"	, false),
	POSTING			("Post"		, true),
	POST			("Post"		, true),
	MESSAGE			("Post"		, true),
	USER			("User"		, true),
	FOLLOWER		("User"		, true),
	FRIEND			("User"		, true),
	FAN				("User"		, true),
	BLOG			("Blog"		, true),
	ADVERTISEMENT	("Ad"		, false),
	APP				("App"		, false),
	GAME			("Game"		, false),
	FORUM			("Forum"	, true),
	WIKI			("Wiki"		, false);
	
	
	private final String value;
	private final boolean name;

	private SocialNetworkEntryTypes(String value, boolean name) {
		this.value = value;
		this.name = name;
	}

	public boolean isOk() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return getValue();
	}
}
