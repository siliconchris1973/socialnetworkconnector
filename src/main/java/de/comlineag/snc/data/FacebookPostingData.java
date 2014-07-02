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
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.1
 * 
 * @description Describes a single facebook posting with all relevant informations. 
 * 				The class shall be used to make all methods handling a facebook posting type save.
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
 *            "geoLocation" List
 *            "lang" String
 *            "hashtags" List
 *            "symbols" List
 *            "mentions" List
 * 
 * 				@changelog 0.1 class created as copy from TwitterPostingData
 * 
 */

public final class FacebookPostingData extends PostData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor, based on the JSONObject sent from Facebook the Data Object is prepared
	 * 
	 * @param jsonObject
	 *            one single post in Facebook
	 */
	public FacebookPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("constructing new subset of data of post (ID: " + jsonObject.get("id") + ") from facebook post-object");
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

		// Geodaten des Posts - es gibt coordinates und geoLocation, geoLocation wird gefuellt wenn bspw. im Web ein Ort fuer den Tweet angegeben wird:
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
		 * mein Tweet mit Ortsangabe JSON siehe unten hatte in geoLocation die Daten
		 
		 // TODO implement proper geo handling - this currently kills the parser
		if (jsonObject.get("geoLocation") != null) {
			logger.trace("Found geoLocation " + jsonObject.get("geoLocation"));
			JSONObject geoLocation = (JSONObject) jsonObject.get("geoLocation");

			setGeoPlaceId(geoLocation.get("id").toString());
			preparePostGeoData((JSONObject) geoLocation.get("bounding_box"));

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
		sn_id = SocialNetworks.FACEBOOK.getValue();

		text = null;
		time = null;
		lang = null;
		posted_from_client = null;
		truncated = null;
		
		in_reply_to_post = 0;
		in_reply_to_user = 0;
		in_reply_to_user_screen_name = null;
		
		place = null;
		geoLatitude = null;
		geoLongitude = null;
		geoAroundLongitude = null;
		geoAroundLatitude = null;
		geoPlaceId = null;
		geoPlaceName = null;
		geoPlaceCountry = null;
		
		hashtags = null;
		symbols = null;
		mentions = null;
	}
}