package de.comlineag.sbm.data;

/**
 * 
 * @author Christian Guenther
 * @category data enumeration
 * 
 * @description contains an enumeration with shortcuts referencing the social networks
 * @version 1.0
 * 
 */
public enum SocialNetworks {
	TWITTER		("TW", "Tweet"), 
	FACBOOKk	("FB", "Post"), 
	GOOGLE		("G+", "Post"), 
	LINKEDIN	("LI", "Post"), 
	XING		("XI", "Post"), 
	STREAMWORK	("SW", "Post"), 
	INSTAGRAM	("IN", "Message"),
	FOURSQUARE	("FS", "Post"),
	LITHIUM		("LT", "Post");

	private final String value;
	private final String type;

	private SocialNetworks(final String value, final String type) {
		this.value = value;
		this.type = type;
	}

	/**
	 * access method for the ID
	 * 
	 * @return current ID of Social Network
	 * 
	 */
	public String getValue() {
		return value;
	}

	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return getValue();
	}

}
