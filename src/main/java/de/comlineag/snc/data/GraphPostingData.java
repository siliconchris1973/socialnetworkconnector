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
		try {
			if (obj.containsKey("sn_id")){
				setSnId(obj.get("sn_id").toString());
			}
			if (obj.containsKey("id")){
				setId(obj.get("id").toString());
			} else if (obj.containsKey("page_id")){
				setId(obj.get("page_id").toString());
			} else if (obj.containsKey("post_id")){
				setId(obj.get("post_id").toString());
			}
			if (obj.containsKey("lang")){
				setLang(obj.get("lang").toString());
			}
			if (obj.containsKey("truncated")){
				setTruncated((boolean) obj.get("truncated"));
			}
			if (obj.containsKey("timestamp")){
				setTimestamp((LocalDateTime) obj.get(timestamp));
			}
			if (obj.containsKey("teaser")){
				setTeaser(obj.get("teaser").toString());
			}
			if (obj.containsKey("subject")){
				setSubject(obj.get("subject").toString());
			}
			if (obj.containsKey("text")){
				setText(obj.get("text").toString());
			}
			
			/*
			if (obj.containsKey("view_count")){
				setViewCount((Long) obj.get("view_count"));
			}
			if (obj.containsKey("favorite_count")){
				setFavoriteCount((Long) obj.get("favorite_count"));
			}
			
			if (obj.containsKey("longitude")){
				setGeoLongitude(obj.get("longitude").toString());
			}
			if (obj.containsKey("latitude")){
				setGeoLatitude(obj.get("latitude").toString());
			}
			if (obj.containsKey("around_longitude")){
				setGeoAroundLongitude(obj.get("around_longitude").toString());
			}
			if (obj.containsKey("around_latitude")){
				setGeoAroundLatitude(obj.get("around_latitude").toString());
			}
			if (obj.containsKey("place_id")){
				setGeoPlaceId(obj.get("place_id").toString());
			}
			if (obj.containsKey("place_name")){
				setGeoPlaceName(obj.get("place_name").toString());
			}
			if (obj.containsKey("place_country")){
				setGeoPlaceCountry(obj.get("place_country").toString());
			}
			*/
			
			if (obj.containsKey("referencedObjectId")){
				setReferenceObjectId(obj.get("referenceObjectId").toString());
			}
			
			if (obj.containsKey("keywords")){
				String[] tTerms = (String[]) obj.get("tTerms");
				
				for (int i=0;i<tTerms.length;i++)
					putTrackTerms(tTerms[i]);
				
				internalJson.put("keywords", getTrackTerms());
			}
			
		} catch (Exception e){
			System.out.println("something went wrong during GraphPostingData initializing " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	

	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	public String toJsonString(){return internalJson.toJSONString();}
	
	public GraphNodeTypes getGnt() {return this.gnt;}
	
	public String getId() {return id;}
	private void setId(String pid) {this.id = pid;internalJson.put("id", pid);}
	
	public String getSnId() {return sn_id;}
	private void setSnId(String snId) {this.sn_id = snId;internalJson.put("sn_id", snId);}
	
	public String getText() {return text;}
	private void setText(String txt) {this.text = txt;internalJson.put("text", txt);}
	
	public String getSubject() {return subject;}
	private void setSubject(String subj) {this.subject = subj;internalJson.put("subject", subj);}
	
	public String getTeaser() {return teaser;}
	private void setTeaser(String teas) {this.teaser = teas;internalJson.put("teaser", teas);}

	public long getViewCount() {return viewcount;}
	private void setViewCount(long viewcnt) {this.viewcount = viewcnt;internalJson.put("view_count", viewcnt);}
	
	public long getFavoriteCount() {return favoritecount;}
	private void setFavoriteCount(long favoritecnt) {this.favoritecount = favoritecnt;internalJson.put("favorite_count", favoritecnt);}
	
	public String getTime() {return time;}
	private void setTime(String postTime) {this.time = postTime;internalJson.put("time", postTime);}
	public LocalDateTime getTimestamp() {return timestamp;}
	private void setTimestamp(LocalDateTime postTime) {this.timestamp = postTime;internalJson.put("timestamp", postTime);}
	
	public boolean getTruncated() {return truncated;}
	private void setTruncated(boolean isTruncated) {this.truncated = isTruncated;internalJson.put("truncated", isTruncated);}
	
	public GeoJsonObject getPlace() {return place;}
	private void setPlace(GeoJsonObject plce) {this.place = plce;internalJson.put("place", plce);}
	
	public String getLang() {return lang;}
	private void setLang(String lng) {this.lang = lng;internalJson.put("lang", lng);}
	
	public String getReferenceObjectId(){ return referenceObjectId; }
	private void setReferenceObjectId(String referenceOId) { this.referenceObjectId = referenceOId;internalJson.put("referenceObjectId", referenceOId);}
	
	public ArrayList<String> getTrackTerms() {return trackTerms;}
	private void putTrackTerms(String t) {this.trackTerms.add(t);}

	// GEO data
	public String getGeoLongitude() {return geoLongitude;}
	private void setGeoLongitude(String geoLong) {this.geoLongitude = geoLong;internalJson.put("longitude", geoLong);}
	public String getGeoLatitude() {return geoLatitude;}
	private void setGeoLatitude(String geoLati) {this.geoLatitude = geoLati;internalJson.put("latitude", geoLati);}
	public String getGeoPlaceId() {return geoPlaceId;}
	private void setGeoPlaceId(String geoPlaceId) {this.geoPlaceId = geoPlaceId;internalJson.put("place_id", geoPlaceId);}
	public String getGeoPlaceName() {return geoPlaceName;}
	private void setGeoPlaceName(String geoPlaceName) {this.geoPlaceName = geoPlaceName;internalJson.put("place_name", geoPlaceName);}
	public String getGeoPlaceCountry() {return geoPlaceCountry;}
	private void setGeoPlaceCountry(String geoPlaceCountry) {this.geoPlaceCountry = geoPlaceCountry;internalJson.put("place_country", geoPlaceCountry);}
	public String getGeoAroundLongitude() {return geoAroundLongitude;}
	private void setGeoAroundLongitude(String geoAroundLongi) {this.geoAroundLongitude = geoAroundLongi; internalJson.put("around_longitude", geoAroundLongi);}
	public String getGeoAroundLatitude() {return geoAroundLatitude;}
	private void setGeoAroundLatitude(String geoAroundLati) {this.geoAroundLatitude = geoAroundLati;internalJson.put("around_latitude", geoAroundLati);}
}
