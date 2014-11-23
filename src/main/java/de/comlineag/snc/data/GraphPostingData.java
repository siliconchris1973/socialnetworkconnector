package de.comlineag.snc.data;

import java.util.ArrayList;

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

public class GraphPostingData implements ISncDataObject{
	
	protected final GraphNodeTypes gnt = GraphNodeTypes.POST;
	protected String id; // ID from the social network
	protected String sn_id;
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
	
	// in this list we keep the trackterms, that this post contained
	protected ArrayList<String> trackTerms;
	
	// the id of the object that referenced to this post - as in linked to or the like 
	protected String referenceObjectId;
	
	
	@SuppressWarnings("unchecked")
	public GraphPostingData(JSONObject obj){
		setId(obj.get("id").toString());
		internalJson.put("id", getId());
		
		setSnId(obj.get("sn_id").toString());
		internalJson.put("sn_id", getSnId());
		
		setTeaser(obj.get("teaser").toString());
		internalJson.put("teaser", getTeaser());
		
		setSubject(obj.get("subject").toString());
		internalJson.put("subject", getSubject());
		
		setLang(obj.get("lang").toString());
		internalJson.put("lang", getLang());
		
		setTruncated((boolean) obj.get("truncated"));
		internalJson.put("truncated", getTruncated());
		
		//setTimestamp((LocalDateTime) obj.get(timestamp));
		//internalJson.put("timestamp", getTimestamp());
		
		setText(obj.get("text").toString());
		internalJson.put("text", getText());
		
		
		/*
		if (obj.containsKey("view_count")){
			setViewCount((Long) obj.get("view_count"));
			internalJson.put("view_count", getViewCount());
		}
		if (obj.containsKey("favorite_count")){
			setFavoriteCount((Long) obj.get("favorite_count"));
			internalJson.put("favorite_count", getFavoriteCount());
		}
		if (obj.containsKey("longitude")){
			setGeoLongitude(obj.get("longitude").toString());
			internalJson.put("longitude", getGeoLongitude());
		}
		if (obj.containsKey("latitude")){
			setGeoLatitude(obj.get("latitude").toString());
			internalJson.put("latitude", getGeoLatitude());
		}
		if (obj.containsKey("around_longitude")){
			setGeoAroundLongitude(obj.get("around_longitude").toString());
			internalJson.put("around_longitude", getGeoAroundLongitude());
		}
		if (obj.containsKey("around_latitude")){
			setGeoAroundLatitude(obj.get("around_latitude").toString());
			internalJson.put("around_latitude", getGeoAroundLatitude());
		}
		if (obj.containsKey("place_id")){
			setGeoPlaceId(obj.get("place_id").toString());
			internalJson.put("place_id", getGeoPlaceId());
		}
		if (obj.containsKey("place_name")){
			setGeoPlaceName(obj.get("place_name").toString());
			internalJson.put("place_name", getGeoPlaceName());
		}
		if (obj.containsKey("place_country")){
			setGeoPlaceCountry(obj.get("place_country").toString());
			internalJson.put("place_country", getGeoPlaceCountry());
		}
		if (obj.containsKey("referencedObjectId")){
			setReferenceObjectId(obj.get("referenceObjectId").toString());
			internalJson.put("referenceObjectId", getReferenceObjectId());
		}
		if (obj.containsKey("tTerms")){
			String[] tTerms = (String[]) obj.get("tTerms");
			
			for (int i=0;i<tTerms.length;i++)
				putTrackTerms(tTerms[i]);
			
			internalJson.put("tTerms", getTrackTerms());
		}
		*/
	}
	

	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	public String toJsonString(){return internalJson.toJSONString();}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	private void setId(String id) {this.id = id;}
	
	public String getSnId() {return sn_id;}
	private void setSnId(String sn_id) {this.sn_id = sn_id;}
	
	public String getText() {return text;}
	private void setText(String text) {this.text = text;}
	
	public String getSubject() {return subject;}
	private void setSubject(String subject) {this.subject = subject;}
	
	public String getTeaser() {return teaser;}
	private void setTeaser(String teaser) {this.teaser = teaser;}

	public long getViewCount() {return viewcount;}
	private void setViewCount(long viewcount) {this.viewcount = viewcount;}
	
	public long getFavoriteCount() {return favoritecount;}
	private void setFavoriteCount(long favoritecount) {this.favoritecount = favoritecount;}
	
	public String getTime() {return time;}
	private void setTime(String postTime) {this.time = postTime;}
	public LocalDateTime getTimestamp() {return timestamp;}
	private void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp;}
	
	public boolean getTruncated() {return truncated;}
	private void setTruncated(boolean isTruncated) {this.truncated = isTruncated;}
	
	public GeoJsonObject getPlace() {return place;}
	private void setPlace(GeoJsonObject place) {this.place = place;}

	public String getLang() {return lang;}
	private void setLang(String lang) {this.lang = lang;}
	
	public String getReferenceObjectId(){ return referenceObjectId; }
	private void setReferenceObjectId(String referenceOId) { this.referenceObjectId = referenceOId;}
	
	public ArrayList<String> getTrackTerms() {return trackTerms;}
	private void putTrackTerms(String t) {this.trackTerms.add(t);}

	// GEO data
	public String getGeoLongitude() {return geoLongitude;}
	private void setGeoLongitude(String geoLongitude) {this.geoLongitude = geoLongitude;}
	public String getGeoLatitude() {return geoLatitude;}
	private void setGeoLatitude(String geoLatitude) {this.geoLatitude = geoLatitude;}
	public String getGeoPlaceId() {return geoPlaceId;}
	private void setGeoPlaceId(String geoPlaceId) {this.geoPlaceId = geoPlaceId;}
	public String getGeoPlaceName() {return geoPlaceName;}
	private void setGeoPlaceName(String geoPlaceName) {this.geoPlaceName = geoPlaceName;}
	public String getGeoPlaceCountry() {return geoPlaceCountry;}
	private void setGeoPlaceCountry(String geoPlaceCountry) {this.geoPlaceCountry = geoPlaceCountry;}
	public String getGeoAroundLongitude() {return geoAroundLongitude;}
	private void setGeoAroundLongitude(String geoAroundLongitude) {this.geoAroundLongitude = geoAroundLongitude;}
	public String getGeoAroundLatitude() {return geoAroundLatitude;}
	private void setGeoAroundLatitude(String geoAroundLatitude) {this.geoAroundLatitude = geoAroundLatitude;}
}
