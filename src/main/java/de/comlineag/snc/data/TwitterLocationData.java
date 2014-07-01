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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author 		Christian Guenther
 * @category	data type
 * @version		0.1
 * 
 * @description	a data type for locations within the twitter network
 * 				
 * 				The Twitter API provides a json entry place with a bounding_box
 * 				In this bounding_box a polygon is stored which center is probably 
 * 				the origin of the tweet. 
 * 
 * 
 * Structure:
 * 		"place":{
 * 			"id":"2ecc0df58a30d37b",
 * 
 * 			"bounding_box":{
 * 				"type":"Polygon",
 * 				"coordinates":[[
 * 					[10.992998,48.087582],
 * 					[10.992998,48.296783],
 * 					[11.412601,48.296783],
 * 					[11.412601,48.087582]
 * 					]]
 * 			},
 * 
 * 			"place_type":"admin",
 * 			"contained_within":[],
 * 			"name":"Fuerstenfeldbruck",
 * 			"attributes":{},
 * 			"country_code":"DE",
 * 			"url":"https:\/\/api.twitter.com\/1.1\/geo\/id\/2ecc0df58a30d37b.json",
 * 			"country":"Deutschland",
 * 			"full_name":"Fuerstenfeldbruck, Bayern"
 * 		},
 * 
 * 
 * Structure 
 * 		Coordinates {
 * 			"type":"Point",
 * 			"coordinates":[-84.497553,33.944551]
 * 		}
 * 
 * Weitere Infos hier: http://www.geojson.org/geojson-spec.html#polygon
 * TODO check if is possible to use this: http://docs.geotools.org/latest/userguide/faq.html
 * 
 * @changelog	0.1 first initial version
 *
 */
public class TwitterLocationData extends LocationData{
	
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public TwitterLocationData(JSONObject jsonObject){
		logger.trace("geo location service called with " + jsonObject.toString());
		setGeoPlaceId(jsonObject.get("id").toString());
		prepareGeoData((JSONObject) jsonObject.get("bounding_box"));
	}
	

	
	private void setGeoPlaceId(String string) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * @description	this method is used to set a simple coordinati
	 * Structure
	 *		Coordinates {
	 * 			"type":"Point",
 	 * 			"coordinates":[-84.497553,33.944551]
 	 * 		}
	 */
	private void setGeoLocation(String lon, String lat){
		this.geoLongitude = lon;
		this.geoLatitude = lat;
		
	}
	
	/** 
	 * 
	 * Diese Methode uebernimmt den JSON String aus einer "bounding_box" und generiert zunuechst ein
	 * allgemeines GeoJsonObject welches in der place Variable hinterlegt wird. Dann wird aus den 
	 * Koordinaten ermittelt wo der Mittelpunkt
	 * liegt.
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
		 * 			place {
		 * 					"id":"e229de11a7eb6823",
		 * 					"bounding_box":{
		 * 						"type":"Polygon",
		 * 						"coordinates":[
		 * 							[[-84.616812,33.895088],[-84.616812,34.0011594],[-84.46746,34.0011594],[-84.46746,33.895088]]
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
			// generelles GeoJson in der place-Variable ablegen:
			geoObject = new ObjectMapper().readValue(_b_box.toString().getBytes(), GeoJsonObject.class);
			setGeoPlace(geoObject);
			logger.debug("geo information place initialized");

			
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

	private void setGeoPlace(GeoJsonObject geoObject) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * Setter fuer 0.00/0.00 damit da dann immer was drin steht und die OData Aufbereitung da einen Eintrag finden kann
	 */
	private void setGeoDefault() {
		setGeoLatitude("0.00");
		setGeoLongitude("0.00");

	}
}
