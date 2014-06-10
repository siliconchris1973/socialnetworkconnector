package de.comlineag.sbm.data;

/**
 * @description contains a enum with types of data in a social network and whether or not this type should be tracked
 * @author Christian Guenther
 *
 */
public enum SocialNetworkEntryTypes {
	// attention: looks like this will be handled by trackterms from applicationContext.xml 
	// because there it can be configured individually for each crawler
	UNKNOWN 		("Unknown"	, false),
	POSTING			("Post"		, true),
	POST			("Post"		, true),
	USER			("User"		, true),
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
