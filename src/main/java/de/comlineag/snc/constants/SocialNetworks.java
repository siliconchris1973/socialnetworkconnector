package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category 	enum
 * @version 	0.6
 * 
 * @description contains an enumeration with shortcuts referencing the social networks
 * 
 * @changelog	0.1 enum created with unknown, twitter, facebook, google+ and linkedin
 * 				0.2 added xing
 * 				0.3 added instagram, foursquare and streamwork
 * 				0.4 added lithium
 * 				0.5 added youtube and finanzforum
 * 				0.6 added ALL as an indicator for xml configuration for all networks
 * 
 */
public enum SocialNetworks {
	ALL			("AL", "All networks"),
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
