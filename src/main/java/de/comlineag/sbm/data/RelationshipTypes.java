package de.comlineag.sbm.data;

public enum RelationshipTypes {
	IN_REPLY_TO_STATUS("replied"),
	IN_REPLY_TO_USER("replied by"),
	USER_MENTIONS("mentioned by"),
	RETWEETED("retweeted by"),
	FAVORITED("favorited by");

	private final String value;
	
	private RelationshipTypes(final String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
}
