package de.comlineag.snc.data;

import java.io.IOException;
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
 * @author 		Christian Guenther, Magnus Leinemann
 * @category 	data type
 * @version 	1.1
 * 
 * @description Describes a single twitter posting with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              posting type save.
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
 * @changelog	0.1 class created
 * 				0.2 added all simple fields
 * 				0.3 added constant for social network
 * 				0.4 - 0.8 work on geo location - as of v1.1 still not implemented
 * 				0.9 skeleton for symbols, hashtags and mentions - as of v1.1 still not implemented
 * 				1.0 first productive version without geo location and hashtag, symbols, mentions
 * 				1.1 minor bugfixings
 * 
 * @TODO implement geo, hashtags, symbols and mentions
 * 
 */

public final class TwitterPostingData extends PostData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Twitter the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Twitter
	 */
	public TwitterPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("constructing new subset of data of tweet (ID: " + jsonObject.get("id") + ") from twitter post-object");
		logger.trace("  working on " + jsonObject.toString());
		
		// alles auf Null und die SocialNetworkID schon mal parken
		initialize();

		// ID des Posting
		setId((Long) jsonObject.get("id"));

		// User ID
		JSONObject user = (JSONObject) jsonObject.get("user");
		setUserId((Long) user.get("id"));
		
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
		
		
		/*
		 * @description		Structur of geo coordinates:
		 * 					{"type":"Point",
		 * 						"coordinates":[-90.06779631,29.95202616]
		 * 					}
		 * 
		 * TODO check if is possible to use this: http://docs.geotools.org/latest/userguide/faq.html
		 * 
		 */
		/*
		 // TODO implement proper geo handling - this currently kills the parser
		if (jsonObject.get("place") != null) {
			logger.trace("Found place " + jsonObject.get("place"));
			JSONObject place = (JSONObject) jsonObject.get("place");

			setGeoPlaceId(place.get("id").toString());
			preparePostGeoData((JSONObject) place.get("bounding_box"));

		}
		*/
		
		/*
		 * 
		 * @description	Structure of Hashtag and Symbols - not yet implemented
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
		sn_id = SocialNetworks.TWITTER.getValue();

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
	 * Die Twitter API liefert einen JSON-Eintrag "place" der die "bounding_box" enthaelt. 
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
	 * "url":"https:\/\/api.twitter.com\/1.1\/geo\/id\/2ecc0df58a30d37b.json",
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
	 *            bounding_box Obejct in Twitter String
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