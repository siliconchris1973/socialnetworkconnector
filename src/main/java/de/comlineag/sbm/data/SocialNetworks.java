package de.comlineag.sbm.data;

/**
 * 
 * @author 		Christian Guenther
 * @category	data
 * 
 * @description	contains an enum with codes and shortcuts to the social networks
 *
 */
public enum SocialNetworks {
	TWITTER("TW"), 
	FACBOOKk("FB"), 
	GOOGLE("GO"), 
	LINKEDIN("LI"), 
	XING("XI"), 
	STREAMWORK("SW"),
	INSTAGRAM("IN");
	
	private final String value;
	private SocialNetworks(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	 
	@Override
	public String toString() {
		return getValue();
	}
}

