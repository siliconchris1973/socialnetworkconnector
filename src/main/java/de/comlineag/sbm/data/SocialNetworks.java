package de.comlineag.sbm.data;

/**
 * 
 * @author Christian Guenther, Magnus Leinemann
 * @category data enumeration
 * 
 * @description contains an enumeration with shortcuts referencing the social networks
 * @version 1.0
 * 
 */
public enum SocialNetworks {
	TWITTER("TW"), 
	FACBOOKk("FB"), 
	GOOGLE("GO"), 
	LINKEDIN("LI"), 
	XING("XI"), 
	STREAMWORK("SW"), 
	INSTAGRAM("IN"),
	FOURSQUARE("FO"),
	FINANZFORUM("FF");

	private final String value;

	private SocialNetworks(final String value) {
		this.value = value;
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

	@Override
	public String toString() {
		return getValue();
	}

}
