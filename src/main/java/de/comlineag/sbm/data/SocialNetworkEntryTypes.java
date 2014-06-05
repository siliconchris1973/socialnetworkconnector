package de.comlineag.sbm.data;

/**
 * @description contains a enum with types of data in a social network and whether or not this type should be tracked
 * @author chris
 *
 */
public enum SocialNetworkEntryTypes {
	UNKNOWN 		("Unknown"	, false),
	POSTING			("Post"		, true),
	USER			("User"		, true),
	BLOG			("Blog"		, false),
	ADVERTISEMENT	("Ad"		, false),
	APP				("App"		, false);
	
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
