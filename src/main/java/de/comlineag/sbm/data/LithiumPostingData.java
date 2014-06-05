package de.comlineag.sbm.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
 * @author Christian Guenther
 * @category data type
 * 
 * @description Describes a single lithium posting with all relevant informations. 
 * 				The class shall be used to make all methods handling a lithium posting type save.
 * 
 * @param <JSonObject>
 *            "id" Long
 *            "sn_id" String
 *            "created_at" String
 *            "text" String
 *            "source" String
 *            "truncated" Boolean
 *            "in_reply_to_status_id" Long
 *            "in_reply_to_user_id" Long
 *            "in_reply_to_screen_name" String
 *            "coordinates" List
 *            "place" List
 *            "lang" String
 *            "hashtags" List
 *            "symbols" List
 *            "mentions" List
 * 
 * 
 */

public final class LithiumPostingData extends PostData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Lithium the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Lithium
	 */
	public LithiumPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("constructing new subset of data of post (ID: " + jsonObject.get("id") + ") from lithium post-object");
		logger.trace("  working on " + jsonObject.toString());
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();

		// ID des Posting
		setId((Long) jsonObject.get("id"));

		// User ID
		JSONObject user = (JSONObject) jsonObject.get("user");
		setUserId((Long) user.get("id"));
		
		// fuer Debugging der Nachrichten in den verschiedenen Faellen
		//String tUser = new String(new Long(getUserId()).toString());
		//if (tUser.contains("754994"))
		//	logger.trace("Post von siliconchris in Json: " + jsonObject);
		// END Debugging fuer Nachrichtenausgaben
		
		// Sprache
		setLang((String) jsonObject.get("lang"));

		// Timestamp als String und dann als Objekt fuer den oDATA Call
		setTime((String) jsonObject.get("created_at"));
		setTimestamp(DataHelper.prepareLocalDateTime(getTime(), getSnId()));

		// Text des Post
		setText((String) jsonObject.get("text"));

		// Metadaten zum Post:
		// von wo erzeugt:
		setClient((String) jsonObject.get("source"));
		// Flag gekuerzt....was auch immer damit dann passieren wird...
		setTruncated((Boolean) jsonObject.get("truncated"));

		// Information zu Reply
		if (jsonObject.get("in_reply_to_status_id") != null)
			setInReplyTo((Long) jsonObject.get("in_reply_to_status_id"));
		if (jsonObject.get("in_reply_to_user_id") != null)
			setInReplyToUser((Long) jsonObject.get("in_reply_to_user_id"));
		if (jsonObject.get("in_reply_to_screen_name") != null)
			setInReplyToUserScreenName((String) jsonObject.get("in_reply_to_screen_name"));

		// Geodaten des Posts - es gibt coordinates und place, place wird gefuellt wenn bspw. im Web ein Ort fuer den Tweet angegeben wird:
		if (jsonObject.get("coordinates") != null)
			logger.debug("Found Coordinates " + jsonObject.get("coordinates").toString() + "");
		
		/**
		 * Struktur der Coordinates:
		 * {"type":"Point","coordinates":[-90.06779631,29.95202616]}
		 * zufaellig entdeckt (kein eigener Post!), der Spassvogel befindet sich wohl am Suedpol (-90)
		 * 
		 * damit vielleicht besser moeglich den Kram zu verarbeiten?
		 * http://docs.geotools.org/latest/userguide/faq.html
		 * 
		 * mein Tweet mit Ortsangabe JSON siehe unten hatte in place die Daten
		 
		 // TODO implement proper geo handling - this currently kills the parser
		if (jsonObject.get("place") != null) {
			logger.trace("Found place " + jsonObject.get("place"));
			JSONObject place = (JSONObject) jsonObject.get("place");

			setGeoPlaceId(place.get("id").toString());
			preparePostGeoData((JSONObject) place.get("bounding_box"));

		}
		*/
		
		/**
		 * Structure of Hashtag and Symbols - not yet implemented
		 * 
		 * "entities": {
		 * 		"trends":[],
		 * 		"symbols":[],
		 * 		"urls":[],
		 * 		"hashtags":[{
		 * 			"text":"SocialBrandMonitor",
		 * 			"indices":[20,39]}],
		 * 		"user_mentions":[]
		 * 	}
		 * 
		 */
		// TODO implement proper handling of hashtags, symbols and mentions - this currently kills the parser
		//setHashtags((List<?>)jsonObject.get("hashtags"));
		//setSymbols((List<?>)jsonObject.get("symbols"));
		//setMentions((List<?>)jsonObject.get("user_mentions"));
	}
	
	
	public void setMentions(List<?> listOfMentions) {
		// TODO Implement proper algorithm to deal with user mentions
		logger.trace("List of mentioned users received, creating something different from it");
		Iterator<?> itr = listOfMentions.iterator();
		while(itr.hasNext()){
			logger.trace("found user " + itr.next());
		}
	}
	
	public void setSymbols(List<?> listOfSymbols) {
		// TODO Implement proper algorithm to deal with symbols
		logger.trace("List of symbols received, creating something different from it");
		Iterator<?> itr = listOfSymbols.iterator();
		while(itr.hasNext()){
			logger.trace("found symbol " + itr.next());
		}
	}
	
	public void setHashtags(List<?> listOfHashtags) {
		// TODO Implement proper algorithm to deal with hashtags
		logger.trace("List of Hashtags received, creating something different from it");
		Iterator<?> itr = listOfHashtags.iterator();
		while(itr.hasNext()){
			logger.trace("found hashtag " + itr.next());
		}
	}
	
	
	
	/**
	 * setup the Object with NULL
	 */
	private void initialize() {
		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;

		// ACHTUNG, wenn die Klasse fuer Facebook u.a. kopiert wird,
		// daa muss dieses Value natuerlich umgesetzt werden
		sn_id = SocialNetworks.LITHIUM.getValue();

		text = null;
		time = null;
		posted_from_client = null;
		truncated = null;
		in_reply_to_post = 0;
		in_reply_to_user = 0;
		in_reply_to_user_screen_name = null;
		coordinates = null;
		geoLatitude = null;
		geoLongitude = null;
		place = null;
		lang = null;
		hashtags = null;
		symbols = null;
		mentions = null;
	}

	/**
	 * Die Lithium API liefert einen JSON-Eintrag "place" der die "bounding_box" enthaelt. 
	 * Darin ist bisher immer ein Polygon aufgetreten.
	 * Hier ein Datenbeispiel:
	 * 
	 * "place":{
	 * "id":"2ecc0df58a30d37b",
	 * 
	 * "bounding_box":{
	 * "type":"Polygon","coordinates":[
	 * [
	 * [10.992998,48.087582],
	 * [10.992998,48.296783],
	 * [11.412601,48.296783],
	 * [11.412601,48.087582]
	 * ]]
	 * },
	 * 
	 * "place_type":"admin",
	 * "contained_within":[],
	 * "name":"Fuerstenfeldbruck",
	 * "attributes":{},
	 * "country_code":"DE",
	 * "url":"https:\/\/api.lithium.com\/1.1\/geo\/id\/2ecc0df58a30d37b.json",
	 * "country":"Deutschland",
	 * "full_name":"Fuerstenfeldbruck, Bayern"
	 * },
	 * 
	 * Weitere Infos hier: http://www.geojson.org/geojson-spec.html#polygon
	 * 
	 * Diese Methode uebernimmt den JSON String aus einer "bounding_box" und generiert zunuechst ein
	 * allgemeines GeoJsonObject welches in der place Variable hinterlegt wird. Dann wird aus den 
	 * Koordinaten ermittelt wo der Mittelpunkt
	 * liegt.
	 * 
	 * @param _b_box
	 *            bounding_box Obejct in Lithium String
	 * 
	 */
	private void preparePostGeoData(JSONObject _b_box) {

		GeoJsonObject geoObject;
		double rootCoordLat = 0.00;
		double rootCoordLon = 0.00;

		try {
			// generelles GeoJson in der place-Variable ablegen:
			geoObject = new ObjectMapper().readValue(_b_box.toString().getBytes(), GeoJsonObject.class);
			setPlace(geoObject);
			logger.debug("geo information place initialized");

			// welche Info haben wir denn im Objekt verfuegbar, damit dann den Mittelpunkt berechnen
			if (geoObject instanceof Polygon) {
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
				/*
				 * Fall 2: Punkt
				 * Fuer einen Punkt muss nur aus den Koordinaten abgelesen werden
				 */
				Point geoOPoint = (Point) geoObject;
				rootCoordLat = geoOPoint.getCoordinates().getLatitude();
				rootCoordLon = geoOPoint.getCoordinates().getLongitude();

			} else if (geoObject instanceof LineString) {
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

	/**
	 * Setter fuer 0.00/0.00 damit da dann immer was drin steht und die OData Aufbereitung da einen Eintrag finden kann
	 */
	private void setGeoDefault() {
		setGeoLatitude("0.00");
		setGeoLongitude("0.00");

	}
}