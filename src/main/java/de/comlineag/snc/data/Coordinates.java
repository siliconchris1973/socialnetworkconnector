package de.comlineag.snc.data;

/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.1
 * 
 * @description	a data type storing a longitude and a latitude
 * 
 * @changelog	0.1 first initial version
 *
 */
public class Coordinates {
	private Long longitude;
	private Long latitude;
	
	public Coordinates(){}
	public Coordinates(Long longitude, Long latitude){
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public Long getLongitude() {
		return longitude;
	}
	public void setLongitude(Long longitude) {
		this.longitude = longitude;
	}
	public Long getLatitude() {
		return latitude;
	}
	public void setLatitude(Long latitude) {
		this.latitude = latitude;
	}
	
	
}
