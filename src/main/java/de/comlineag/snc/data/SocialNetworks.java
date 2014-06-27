package de.comlineag.snc.data;

/**
 * 
 * @author 		Christian Guenther
 * @category 	enum
 * @version 	1.4
 * 
 * @description contains an enumeration with shortcuts referencing the social networks
 * 
 * @changelog	1.0 enum created with unknown, twitter, facebook, google+ and linkedin
 * 				1.1 added xing
 * 				1.2 added instagram, foursquare and streamwork
 * 				1.3 added lithium
 * 				1.4 added youtube and finanzforum
 */
public enum SocialNetworks {
	UNKNOWN		("XY", "Unknown"),
	TWITTER		("TW", "Twitter"), 
	FACEBOOK	("FB", "Facebook"), 
	GOOGLE		("G+", "Google+"), 
	LINKEDIN	("LI", "Linkedin"), 
	XING		("XI", "XING"), 
	STREAMWORK	("SW", "Streamwork"), 
	INSTAGRAM	("IN", "Instagram"),
	FOURSQUARE	("FS", "Foursquare"),
	LITHIUM		("LT", "Lithium"),
	YOUTUBE		("YT", "Youtube"),
	FINANZFORUM	("FF", "Finanzforum");
	
	private final String value;
	private final String type;
	
	private SocialNetworks(final String value, final String type) {
		this.value = value;
		this.type = type;
	}
	
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
	
	public static SocialNetworks getNetworkNameByValue(String value){
		for (SocialNetworks name : SocialNetworks.values()) {
			if(name.getValue() == value)
				return name;
		}
		return SocialNetworks.UNKNOWN;
	}
}
