package de.comlineag.snc.data;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.4
 * @status		productive
 * 
 * @description	core data type for locations 
 * 
 * @changelog	0.1 (Chris)		first initial version
 * 				0.2 			added getter and setter for geoLocation, geoAroundLongitude + geoAroundLatitude and geoCoordinates
 * 				0.3 			added new constructor for json-object 
 * 				0.4 			added constants for geoLocation object parser
 * 
 * TODO 1. check if is possible to use this: http://docs.geotools.org/latest/userguide/faq.html
 * TODO 2. check if this class is ok for other social networks
 */
public class LocationData {
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// some static constants used to find the type and elements of geo the geoLocation object
	private final static String GEO_LOCATION_COMPLEX_TYPE_TEXT = "geoLocation";  
	private final static String GEO_LOCATION_SIMPLE_TYPE_TEXT = "geoLocation";
	private final static String GEO_LOCATION_ID_TEXT = "id";
	private final static String GEO_LOCATION_NAME_TEXT = "name";
	private final static String GEO_LOCATION_FULL_NAME_TEXT = "full_name";
	private final static String GEO_LOCATION_COUNTRY_TEXT = "country";
	private final static String GEO_LOCATION_COUNTRY_CODE_TEXT = "country_code";
	private final static String GEO_LOCATION_PLACE_TYPE_TEXT = "place_type";
	private final static String GEO_LOCATION_COORDINATES_TEXT = "coordinates";
	private final static String GEO_LOCATION_URL_TEXT = "url";
	
	
	/*
	 * a simple coordinate consisting of latitude and longitude
	 * 
	 * Structure
	 *		Coordinates {
	 * 			"type":"Point",
 	 * 			"coordinates":[-84.497553,33.944551]
 	 * 		} 
 	 * 
 	 * stored in the database as
	 * 		<Property Name="geoLocation_longitude" 	Type="Edm.String" MaxLength="40"/>
	 * 		<Property Name="geoLocation_latitude" 	Type="Edm.String" MaxLength="40"/>
	 */
	protected String simpleGeoLocation[];
	protected String geoLatitude;
	protected String geoLongitude;
	
	
	/*
	 * geo geoLocation as a JSON object
	 * for the specification see 
	 * 		http://geojson.org/geojson-spec.html 
	 * 
	 * Structure
	 * 		geoLocation {
	 * 			"id":"e229de11a7eb6823",
	 * 			"bounding_box":{
	 * 				"type":"Polygon",
	 * 				"coordinates":[
	 * 					[
	 * 						[-84.616812,33.895088],
	 * 						[-84.616812,34.0011594],
	 * 						[-84.46746,34.0011594],
	 * 						[-84.46746,33.895088]
	 * 					]
	 * 				]
	 * 			},
	 * 			"place_type":"city",
	 * 			"name":"Marietta",
	 * 			"attributes":{},
	 * 			"country_code":"US",
	 * 			"url":"https:\/\/api.twitter.com\/1.1\/geo\/id\/e229de11a7eb6823.json",
	 * 			"country":"United States",
	 * 			"full_name":"Marietta, GA"
	 * 		}
	 * 
	 * stored in the database as simple a simple coordinate
	 * 		<Property Name="geoLocation_longitude" 	Type="Edm.String" MaxLength="40"/>
	 * 		<Property Name="geoLocation_latitude" 	Type="Edm.String" MaxLength="40"/>
	 */
	protected GeoJsonObject geoLocation; 
	
	
	/*
	 * circle around a point in a gis
	 * 
	 * <Property Name="plAround_longitude" 	Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" 	Type="Edm.String" MaxLength="40"/>
	 */
	protected String geoAroundLatitude;
	protected String geoAroundLongitude;
	
	
	/*
	 * geoLocation - 	set in the form of country and name, with an optional id
	 * 
	 * <Property Name="placeID" 	Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" 		Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" 	Type="Edm.String" MaxLength="128"/>
	 */
	protected String geoPlaceId;
	protected String geoPlaceName;
	protected String geoPlaceCountry;
	protected String geoPlaceCountryCode;
	protected String geoPlaceType;
	protected String geoPlaceUrl;
	protected String geoPlaceFullName;
	
	//
	// constructor
	//
	/**
	 * @description	This constructor takes a JSON object which ought to conform to the GeoJSON standard 
	 * 
	 * @param JSONObject locationObject
	 */
	public LocationData(JSONObject locationObject){
		logger.trace("json geoLocation object received: " + locationObject.toString());
		try {
			if(locationObject.containsKey(GEO_LOCATION_COMPLEX_TYPE_TEXT)){
				logger.trace("found complex geoLocation of type " + GEO_LOCATION_COMPLEX_TYPE_TEXT);
				setComplexGeoLocation(locationObject);
			} else if(locationObject.containsKey(GEO_LOCATION_SIMPLE_TYPE_TEXT)){
				logger.trace("found simple point geoLocation of type " + GEO_LOCATION_SIMPLE_TYPE_TEXT);
				setSimpleGeoLocation(locationObject);
			} else {
				logger.error("ERROR :: could not identify either "+GEO_LOCATION_COMPLEX_TYPE_TEXT+" (complex place) or "+GEO_LOCATION_SIMPLE_TYPE_TEXT+" (simple coordinates) - setting everything to null");
				setEverythingToNull();
			}
		} catch (Exception e){
			logger.error("EXCEPTION :: parsing json object - setting everything to null: " + e.getLocalizedMessage());
			e.printStackTrace();
			setEverythingToNull();
		}
		
	}
	
	
	//
	// methods
	//
	/** 
	 * 
	 * @description	This method takes a json object from a "bounding_box" and generates a generic 
	 * 				GeoJsonObject stored in the geoLocation variable. It then calculates the center
	 * 				of that bounding box and stores it in geoLatitude and geoLongitude
	 * 
	 * @param _b_box
	 *            bounding_box Obejct in Twitter String
	 * 
	 */
	private void setComplexGeoLocation(JSONObject _b_box) {
		double rootCoordLat = 0.0;
		double rootCoordLon = 0.0;
		GeoJsonObject geoObject = null;
		 /* Structure
		 * 			geoLocation {
		 * 					"id":"e229de11a7eb6823",
		 * 					"bounding_box":{
		 * 						"type":"Polygon",
		 * 						"coordinates":[
		 * 							[
		 * 								[-84.616812,33.895088],
		 * 								[-84.616812,34.0011594],
		 * 								[-84.46746,34.0011594],
		 * 								[-84.46746,33.895088]
		 * 							]
		 * 						]
		 * 					},
		 * 					"place_type":"city",
		 * 					"name":"Marietta",
		 * 					"attributes":{},
		 * 					"country_code":"US",
		 * 					"url":"https:\/\/api.twitter.com\/1.1\/geo\/id\/e229de11a7eb6823.json",
		 * 					"country":"United States",
		 * 					"full_name":"Marietta, GA"
		 * 			}
		 */
		try {
			// put the generic GeoJson object in the geoLocation-Variable
			geoObject = new ObjectMapper().readValue(_b_box.toString().getBytes(), GeoJsonObject.class);
			setGeoLocation(geoObject);
			logger.debug("geo information geoLocation initialized for " + _b_box.get(GEO_LOCATION_NAME_TEXT).toString());
			
			// welche Info haben wir denn im Objekt verfuegbar, damit dann den Mittelpunkt berechnen
			if (geoObject instanceof Polygon) {
				logger.trace("the given geo object is of type polygon");
				/*
				 * Fall 1: Polygon
				 * beinhaltet eine List der Koordinaten, diese ist eine 2-stufige Liste (Outer, Inner)
				 * Es wird ueber die Liste geschleift und die Longitude/Latitude Mittelwerte gebildet
				 */
				Polygon geoOPolygon = (Polygon) geoObject;
				List<LngLatAlt> coords = geoOPolygon.getCoordinates().get(0);

				if (coords.size() > 0) {
					for (int ii = 0; ii < coords.size(); ii++) {
						rootCoordLat += coords.get(ii).getLatitude();
						rootCoordLon += coords.get(ii).getLongitude();
					}
					rootCoordLat = rootCoordLat / (coords.size() + 1);
					rootCoordLon = rootCoordLon / (coords.size() + 1);
				}
			} else if (geoObject instanceof Point) {
				logger.trace("the given geo object is of type point");
				/*
				 * Fall 2: Punkt
				 * Fuer einen Punkt muss nur aus den Koordinaten abgelesen werden
				 */
				Point geoOPoint = (Point) geoObject;
				rootCoordLat = geoOPoint.getCoordinates().getLatitude();
				rootCoordLon = geoOPoint.getCoordinates().getLongitude();

			} else if (geoObject instanceof LineString) {
				logger.trace("the given geo object is of type line");
				/*
				 * Fall 3: Linie
				 * beinhaltet eine List der Koordinaten, diese ist aber 1-Stufig im Gegensatz zum Polygon
				 * Es wird ueber die Liste geschleift und die Longitude/Latitude Mittelwerte gebildet
				 */
				LineString geoOLine = (LineString) geoObject;
				List<LngLatAlt> coords = geoOLine.getCoordinates();

				if (coords.size() > 0) {
					for (int ii = 0; ii < coords.size(); ii++) {
						rootCoordLat += coords.get(ii).getLatitude();
						rootCoordLon += coords.get(ii).getLongitude();
					}
					rootCoordLat = rootCoordLat / (coords.size() + 1);
					rootCoordLon = rootCoordLon / (coords.size() + 1);
				}
			} else {
				logger.trace("the given geo object is of unspecified type");
				setGeoDefaultAsZero();
			}
			
			// now let's set the received geoLocation in the latitude and longitude and also in the array simpleGeoLocation
			// this is done in below method
			setSimpleGeoLocation(new Double(rootCoordLat).toString(), new Double(rootCoordLon).toString());
			
			logger.debug("Posting coordinates: " + rootCoordLat + " (lat) / " + rootCoordLon + " (long)");
			
			if (_b_box.containsKey(GEO_LOCATION_ID_TEXT))
				setGeoPlaceId(_b_box.get(GEO_LOCATION_ID_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_NAME_TEXT))
				setGeoPlaceName(_b_box.get(GEO_LOCATION_NAME_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_COUNTRY_TEXT))
				setGeoPlaceCountry(_b_box.get(GEO_LOCATION_COUNTRY_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_COUNTRY_CODE_TEXT))
				setGeoPlaceCountryCode(_b_box.get(GEO_LOCATION_COUNTRY_CODE_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_FULL_NAME_TEXT))
				setGeoPlaceFullName(_b_box.get(GEO_LOCATION_FULL_NAME_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_PLACE_TYPE_TEXT))
				setGeoPlaceType(_b_box.get(GEO_LOCATION_PLACE_TYPE_TEXT).toString());
			
			if (_b_box.containsKey(GEO_LOCATION_URL_TEXT))
				setGeoPlaceUrl(_b_box.get(GEO_LOCATION_URL_TEXT).toString());
			
		} catch (JsonParseException e) {
			logger.error(e.getMessage());
			setGeoDefaultAsZero();
		} catch (JsonMappingException e) {
			logger.error(e.getMessage());
			setGeoDefaultAsZero();
		} catch (IOException e) {
			logger.error(e.getMessage());
			setGeoDefaultAsZero();
		}
	}
	
	
	/**
	 * 
	 * @description	this method is used to set a simple coordinate consisting of longitude and latitude 
	 * 				in 
	 * 
	 * Structure
	 *		Coordinates {
	 * 			"type":"Point",
 	 * 			"coordinates":[-84.497553,33.944551]
 	 * 		}
	 */
	private void setSimpleGeoLocation(JSONObject jsonObject){
		try {
			JSONParser parser = new JSONParser();
			Object simpleGeoLocationObj = parser.parse(jsonObject.get(GEO_LOCATION_COORDINATES_TEXT).toString());	
			JSONObject jsonObj = simpleGeoLocationObj instanceof JSONObject ?(JSONObject) simpleGeoLocationObj : null;
			String t = new String(jsonObj.get(GEO_LOCATION_COORDINATES_TEXT).toString());
			logger.trace("retrieved coordinates: " + t + " / " + t.substring(1, t.indexOf(",")) + " (lat) / " +t.substring(t.indexOf(",")+1,t.length()-1)+ " (long)");
			
			// now let's set the received geoLocation in the latitude and longitude and also in the array simpleGeoLocation
			// this is done in below method
			setSimpleGeoLocation(t.substring(1, t.indexOf(",")), t.substring(t.indexOf(",")+1,t.length()-1));
		} catch (ParseException e) {
			logger.error("error parsing json coordinates object: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
	}
	private void setSimpleGeoLocation(String geoLatitude, String geoLongitude){
		simpleGeoLocation = new String[2];
		this.simpleGeoLocation[0] = geoLatitude;
		this.simpleGeoLocation[1] = geoLongitude;
		setGeoLatitude(geoLatitude);
		setGeoLongitude(geoLongitude);
	}
	
	
	//
	// simple methods to set every member to null
	//
	private void setEverythingToNull(){
		geoLocation = null;
		simpleGeoLocation = null;
		geoLatitude = null;
		geoLongitude = null;
		geoAroundLatitude = null;
		geoAroundLongitude = null;
		geoPlaceId = null;
		geoPlaceName = null;
		geoPlaceFullName = null;
		geoPlaceCountry = null;
		geoPlaceCountryCode = null;
		geoPlaceType = null;
		geoPlaceUrl = null;
	}
	private void setGeoDefaultAsZero() {
		setGeoLatitude("0.00");
		setGeoLongitude("0.00");
	}
	
	//
	// standard getter and setter
	//
	@SuppressWarnings("unused")
	private String[] getSimpleGeoLocation(){
		return simpleGeoLocation;
	}
	public GeoJsonObject getGeoLocation() {
		return geoLocation;
	}
	public void setGeoLocation(GeoJsonObject geoLocation) {
		this.geoLocation = geoLocation;
	}
	
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
	public String getGeoAroundLongitude() {
		return geoAroundLongitude;
	}
	public void setGeoAroundLongitude(String geoAroundLongitude) {
		this.geoAroundLongitude = geoAroundLongitude;
	}
	public String getGeoAroundLatiitude() {
		return geoAroundLatitude;
	}
	public void setGeoAroundLatitude(String geoAroundLatitude) {
		this.geoAroundLatitude = geoAroundLatitude;
	}
	
	// now textual geoLocation representations
	public String getGeoPlaceId() {
		return geoPlaceId;
	}
	public void setGeoPlaceId(String placeId) {
		this.geoPlaceId = placeId;
	}
	public String getGeoPlaceCountry() {
		return geoPlaceCountry;
	}
	public void setGeoPlaceCountry(String country) {
		this.geoPlaceCountry = country;
	}
	public String getGeoPlaceCountryCode() {
		return geoPlaceCountryCode;
	}
	public void setGeoPlaceCountryCode(String countryCode) {
		this.geoPlaceCountryCode = countryCode;
	}
	public String getGeoPlaceName() {
		return geoPlaceName;
	}
	public void setGeoPlaceName(String name) {
		this.geoPlaceName = name;
	}
	public String getGeoPlaceFullName() {
		return geoPlaceFullName;
	}
	public void setGeoPlaceFullName(String fullName) {
		this.geoPlaceFullName = fullName;
	}
	public String getGeoPlaceType() {
		return geoPlaceType;
	}
	public void setGeoPlaceType(String type) {
		this.geoPlaceType = type;
	}
	
	public String getGeoPlaceUrl() {
		return geoPlaceUrl;
	}
	public void setGeoPlaceUrl(String url) {
		this.geoPlaceUrl = url;
	}
}
