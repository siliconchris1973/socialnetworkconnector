package de.comlineag.snc.data;

/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.1
 * 
 * @description	a data type for locations within the twitter network
 * 
 * @changelog	0.1 first initial version - not yet implemeneted
 *
 */
public class TwitterLocation {
	private String longitude;
	private String lattitude;
	private String locCountry;
	private String locRegion;
	private String locCity;
	private String locStreet;
	
	
	public TwitterLocation(String longitude, String lattitude){
		this.longitude = longitude;
		this.lattitude = lattitude;
	}
	
	// getter and setter
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getLattitude() {
		return lattitude;
	}
	public void setLattitude(String lattitude) {
		this.lattitude = lattitude;
	}
	public String getLocCountry() {
		return locCountry;
	}
	public void setLocCountry(String locCountry) {
		this.locCountry = locCountry;
	}
	public String getLocRegion() {
		return locRegion;
	}
	public void setLocRegion(String locRegion) {
		this.locRegion = locRegion;
	}
	public String getLocCity() {
		return locCity;
	}
	public void setLocCity(String locCity) {
		this.locCity = locCity;
	}
	public String getLocStreet() {
		return locStreet;
	}
	public void setLocStreet(String locStreet) {
		this.locStreet = locStreet;
	}
}
