package de.comlineag.snc.data;

/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.1
 * 
 * @description	a data type for the twitter location as used in the configuration
 * 
 * @changelog	0.1 first initial version
 *
 */
public class TwitterConfigLocation {
	private String longitude;
	private String langitude;
	
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	public String getLangitude() {
		return langitude;
	}
	public void setLangitude(String langitude) {
		this.langitude = langitude;
	}
}
