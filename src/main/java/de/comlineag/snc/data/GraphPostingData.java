package de.comlineag.snc.data;

import org.geojson.GeoJsonObject;
import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;

import de.comlineag.snc.constants.GraphNodeTypes;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Data Class
 * @version 	0.1				- 11.11.2014
 * @status		productive
 * 
 * @description data type representing a post in the graph
 * 
 * @changelog	0.1 (Chris)		class created as copy from PostingData Version 0.5
 */

public class GraphPostingData {
	
	protected final GraphNodeTypes gnt = GraphNodeTypes.POST;
	protected String id; // ID from the social network
	protected String text;
	protected String teaser;
	protected String subject;
	protected String lang;
	
	// all the fields below are unused at the moment
	protected boolean truncated;
	
	protected String time;
	protected LocalDateTime timestamp;
	
	protected long viewcount;
	protected long favoritecount;
	
	protected GeoJsonObject place; // GEO Info aus Stream
	protected String geoLongitude;
	protected String geoLatitude;
	protected String geoAroundLongitude;
	protected String geoAroundLatitude;
	protected String geoPlaceId;
	protected String geoPlaceName;
	protected String geoPlaceCountry;
	
	protected JSONObject internalJson = new JSONObject();
	
	public GraphPostingData(JSONObject obj){
		setId(obj.get("id").toString());
		setText(obj.get("text").toString());
		setTeaser(obj.get("teaser").toString());
		setSubject(obj.get("subject").toString());
		setLang(obj.get("lang").toString());
		setTruncated((boolean) obj.get("truncated"));
		setTimestamp((LocalDateTime) obj.get(timestamp));
		
		if (obj.containsKey("view_count"))	
			setViewCount((Long) obj.get("view_count"));
		if (obj.containsKey("favorite_count"))
			setFavoriteCount((Long) obj.get("favorite_count"));
		if (obj.containsKey("longitude"))
			setGeoLongitude(obj.get("longitude").toString());
		if (obj.containsKey("lattitude"))
			setGeoLatitude(obj.get("latitude").toString());
		if (obj.containsKey("around_longitude"))
			setGeoAroundLongitude(obj.get("around_longitude").toString());
		if (obj.containsKey("around_lattitude"))
			setGeoAroundLatitude(obj.get("around_latitude").toString());
		if (obj.containsKey("place_id"))
			setGeoPlaceId(obj.get("place_id").toString());
		if (obj.containsKey("place_name"))
			setGeoPlaceName(obj.get("place_name").toString());
		if (obj.containsKey("place_country"))
			setGeoPlaceCountry(obj.get("place_country").toString());
		
		internalJson = obj;
		//internalJson.put("Label", gnt);
	}
	
	
	private String toJsonString(){
		/*
		JSONObject obj = new JSONObject();
		obj.put("id", getId());
		obj.put("text", getText());
		obj.put("teaser", getTeaser());
		obj.put("subject", getSubject());
		obj.put("lang", getLang());
		
		obj.put("truncated", getTruncated());
		
		obj.put("time", getTime());
		obj.put("timestamp", getTimestamp());
		
		obj.put("view_count", getViewCount());
		obj.put("favorite_count", getFavoriteCount()); 
		
		obj.put("longitude", getGeoLongitude());
		obj.put("latitude", getGeoLatitude());
		obj.put("around_longitude", getGeoAroundLongitude());
		obj.put("around_latitude", getGeoAroundLatitude());
		obj.put("place_id", getGeoPlaceId());
		obj.put("place_name", getGeoPlaceName());
		obj.put("place_country", getGeoPlaceCountry()); 
		
		return obj.toJSONString();
		*/
		return internalJson.toJSONString();
	}
	
	
	/**
	 * @description	creates a string which can be passed to the neo4j cypher engine to create a node
	 * @return		cypher string
	 */
	public String createCypher(){
		return "\""+gnt.toString()+"\" "+toJsonString();
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getText() {return text;}
	public void setText(String text) {this.text = text;}
	
	public String getSubject() {return subject;}
	public void setSubject(String subject) {this.subject = subject;}
	
	public String getTeaser() {return teaser;}
	public void setTeaser(String teaser) {this.teaser = teaser;}

	public long getViewCount() {return viewcount;}
	public void setViewCount(long viewcount) {this.viewcount = viewcount;}
	
	public long getFavoriteCount() {return favoritecount;}
	public void setFavoriteCount(long favoritecount) {this.favoritecount = favoritecount;}
	
	public String getTime() {return time;}
	public void setTime(String postTime) {this.time = postTime;}
	public LocalDateTime getTimestamp() {return timestamp;}
	public void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp;}
	
	public boolean getTruncated() {return truncated;}
	public void setTruncated(boolean isTruncated) {this.truncated = isTruncated;}
	
	public GeoJsonObject getPlace() {return place;}
	public void setPlace(GeoJsonObject place) {this.place = place;}

	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}

	// GEO data
	public String getGeoLongitude() {return geoLongitude;}
	public void setGeoLongitude(String geoLongitude) {this.geoLongitude = geoLongitude;}
	public String getGeoLatitude() {return geoLatitude;}
	public void setGeoLatitude(String geoLatitude) {this.geoLatitude = geoLatitude;}
	public String getGeoPlaceId() {return geoPlaceId;}
	public void setGeoPlaceId(String geoPlaceId) {this.geoPlaceId = geoPlaceId;}
	public String getGeoPlaceName() {return geoPlaceName;}
	public void setGeoPlaceName(String geoPlaceName) {this.geoPlaceName = geoPlaceName;}
	public String getGeoPlaceCountry() {return geoPlaceCountry;}
	public void setGeoPlaceCountry(String geoPlaceCountry) {this.geoPlaceCountry = geoPlaceCountry;}
	public String getGeoAroundLongitude() {return geoAroundLongitude;}
	public void setGeoAroundLongitude(String geoAroundLongitude) {this.geoAroundLongitude = geoAroundLongitude;}
	public String getGeoAroundLatitude() {return geoAroundLatitude;}
	public void setGeoAroundLatitude(String geoAroundLatitude) {this.geoAroundLatitude = geoAroundLatitude;}
}
