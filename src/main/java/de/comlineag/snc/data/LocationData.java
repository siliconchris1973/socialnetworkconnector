package de.comlineag.snc.data;

import org.geojson.GeoJsonObject;


/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.1
 * 
 * @description	core data type for locations 
 * 
 * @changelog	0.1 first initial version
 * 
 * TODO check if is possible to use this: http://docs.geotools.org/latest/userguide/faq.html
 * 
 */
public class LocationData {
	/**
	 * Coordinates
	 * <Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 */
	protected GeoJsonObject place; // GEO Info aus Stream
	protected String geoLongitude;
	protected String geoLatitude;
	protected String geoPlaceId;
	protected String geoPlaceName;
	protected String geoPlaceCountry;

	/**
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 */
	protected String geoAroundLongitude;
	protected String geoAroundLatitude;
	
	protected String coordinates; // obsolete
	
	
	
	// getter and setter
	public String getGeoLongitude() {
		return geoLongitude;
	}
	public void setGeoLongitude(String longitude) {
		this.geoLongitude = longitude;
	}
	public String getGeoLatitude() {
		return geoLatitude;
	}
	public void setGeoLatitude(String latitude) {
		this.geoLatitude = latitude;
	}
	public String getGeoPlaceCountry() {
		return geoPlaceCountry;
	}
	public void setGeoPlaceCountry(String country) {
		this.geoPlaceCountry = country;
	}
	public String getGeoPlaceName() {
		return geoPlaceName;
	}
	public void setGeoPlaceName(String name) {
		this.geoPlaceName = name;
	}
}
