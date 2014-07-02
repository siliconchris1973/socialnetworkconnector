package de.comlineag.snc.data;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
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
 * @version		0.2
 * 
 * @description	core data type for locations 
 * 
 * @changelog	0.1 first initial version
 * 				0.2 added getter and setter for geoLocation, geoAroundLongitude + geoAroundLatitude and geoCoordinates
 * 
 * TODO check if is possible to use this: http://docs.geotools.org/latest/userguide/faq.html
 * 
 */
public class LocationData {
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
		
	
	/*
	 * geo location as a JSON object
	 * for the specification see 
	 * 		http://geojson.org/geojson-spec.html 
	 * 
	 * Structure
	 * 		geoLocation {
	 * 			"id":"e229de11a7eb6823",
	 * 			"bounding_box":{
	 * 				"type":"Polygon",
	 * 				"coordinates":[
	 * 					[[-84.616812,33.895088],[-84.616812,34.0011594],[-84.46746,34.0011594],[-84.46746,33.895088]]
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
	 */
	protected GeoJsonObject geoLocation; 
	
	/*
	 * a simple coordinate consisting of latitude and longitude
	 * 
	 * Structure
	 *		Coordinates {
	 * 			"type":"Point",
 	 * 			"coordinates":[-84.497553,33.944551]
 	 * 		} 
	 */
	protected String simpleGeoLocation[];
	
	/*
	 * location - 	can either be in the form of latitude and longitude (precise) 
	 * 				or in the form of country and name, with an optional id (less precise)
	 * 
	 * <Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 */
	protected String geoLatitude;
	protected String geoLongitude;
	protected String geoPlaceId;
	protected String geoPlaceName;
	protected String geoPlaceCountry;

	/*
	 * circle around a point in a gis
	 * 
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 */
	protected String geoAroundLatitude;
	protected String geoAroundLongitude;
	
	
	
	//
	// constructor
	//
	public LocationData(JSONObject locationObject){
		logger.trace("json location object received: " + locationObject.toString());
		try {
			prepareGeoData(locationObject);
					
		} catch (Exception e){
			e.printStackTrace();
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
	private void prepareGeoData(JSONObject _b_box) {
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
			// put the generric GeoJson object in the geoLocation-Variable
			geoObject = new ObjectMapper().readValue(_b_box.toString().getBytes(), GeoJsonObject.class);
			setGeoLocation(geoObject);
			logger.debug("geo information geoLocation initialized for " + _b_box.get("name").toString());
			/*
			setGeoPlaceId((String)geoObject.getProperty("id"));
			setGeoPlaceName((String)geoObject.getProperty("name"));
			setGeoPlaceCountry((String)geoObject.getProperty("country"));
			*/
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
				 * beinhaltet eine List der Koordinaten, diese ist aber 1-stufig im Gegensatz zum Polygon
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
				setGeoDefault();
			}

			setGeoLatitude(new Double(rootCoordLat).toString());
			setGeoLongitude(new Double(rootCoordLon).toString());

			logger.debug("Posting coordinates: " + rootCoordLat + " / " + rootCoordLon);
			
			setGeoPlaceId(_b_box.get("id").toString());
			setGeoPlaceName(_b_box.get("name").toString());
			setGeoPlaceCountry(_b_box.get("country").toString());

		} catch (JsonParseException e) {
			logger.error(e.getMessage());
			setGeoDefault();
		} catch (JsonMappingException e) {
			logger.error(e.getMessage());
			setGeoDefault();
		} catch (IOException e) {
			logger.error(e.getMessage());
			setGeoDefault();
		}
	}
	
	
	
	/**
	 * 
	 * @description set a default value (0.00/0.00) in geoLatitude and geoLongitude
	 * 
	 */
	private void setGeoDefault() {
		setGeoLatitude("0.00");
		setGeoLongitude("0.00");
	}
	
	
	//
	// GETTER AND SETTER
	//
	/**
	 * 
	 * @description	this method is used to set a simple coordinate consisting of longitude and latitude 
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
			Object simpleGeoLocationObj = parser.parse(jsonObject.get("coordinates").toString());	
			JSONObject jsonObj = simpleGeoLocationObj instanceof JSONObject ?(JSONObject) simpleGeoLocationObj : null;
			String t = new String(jsonObj.get("coordinates").toString());
			logger.trace("retrieved coordinates: " + t + " / lat: " + t.substring(1, t.indexOf(",")) + " / long: " +t.substring(t.indexOf(",")+1,t.length()-1));
			setGeoLatitude(t.substring(1, t.indexOf(","))); 
			setGeoLongitude(t.substring(t.indexOf(",")+1,t.length()-1));
		} catch (ParseException e) {
			logger.error("error parsing json coordinates object: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
	}
	private void setSimpleGeoLocation(String geoLongitude, String geoLatitude){
		simpleGeoLocation = new String[2];
		this.simpleGeoLocation[0] = geoLongitude;
		this.simpleGeoLocation[1] = geoLatitude;
	}
	private String[] getSimpleGeoLocation(){
		return simpleGeoLocation;
	}
	
	
	// getter and setter
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
	public String getGeoPlaceName() {
		return geoPlaceName;
	}
	public void setGeoPlaceName(String name) {
		this.geoPlaceName = name;
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
}
