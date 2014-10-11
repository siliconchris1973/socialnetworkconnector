package de.comlineag.snc.data;

import java.util.List;

import org.geojson.GeoJsonObject;
import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;

/**
 * 
 * @author 		Magnus Leinemann, Christian Guenther
 * @category 	data class
 * @version 	0.4				- 10.10.2014
 * @status		productive
 * 
 * @description Data Class representing a Post from the OData Service
 * 
 * @changelog	0.1 (Magnus)	class created according to twitter user needs
 * 				0.2 (Chris)		added field raw-text for Lithium postings which contain html 
 * 								- the markups are stripped for the normal text field
 * 				0.2a			added domain and customer
 * 				0.3				added method to fill all values from a passed json string 
 * 								for use with the FsCrawler, which creates a posData object 
 * 								from a stored json file and then invokes the persistence layer
 * 				0.4				added possibility to store an embedded UserData object within the PostData object
 * 
 * TODO move GeneralDataDefinitions to RuntimeConfiguration and source it in from XML 
 */

public class PostData {
	/*
	 * was the object initially saved correctly by the persistence manager or not 
	 */
	protected String objectStatus;	// can be ok or fail
	
	/*
	 * domain (stored as json within the db) e.g. banking
	 * <Property Name="domain" Type="Edm.String" Nullable="false" MaxLength="1024"/>
	 */
	protected String domain; 	// string identifying the domain of interest, this post was tracked for
	protected String customer;	// same for customer
	
	/*
	 * SocialNetworkID e.g. TW, FB
	 * <Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
	 */
	protected String sn_id; // string identifying the social network - taken from SocialNetworks enum

	/*
	 * the ID of the current Post in the Social Network
	 * <Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
	 */
	protected long id; // ID from the social network

	/*
	 * User ID of the posting user
	 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 */
	protected long userId;

	/*
	 * post Text
	 * <Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
	 */
	protected String text;

	/*
	 * post Text with html elements
	 * <Property Name="raw_text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
	 */
	protected String raw_text;
	
	/*
	 * a teaser (short version) of the post
	 * <Property Name="teaser" Type="Edm.String" MaxLength="256"/>
	 */
	protected String teaser;
	
	/*
	 * a subject of the post
	 * <Property Name="subject" Type="Edm.String" MaxLength="20"/>
	 */
	protected String subject;
	
	/*
	 * Language of post
	 * <Property Name="postLang" Type="Edm.String" MaxLength="64"/>
	 */
	protected String lang;
		
	/*
	 * count of views
	 * <Property Name="viewcount" Type="Edm.int"/>
	 */
	protected long viewcount;
	
	/*
	 * count of favorite 
	 * <Property Name="favoritecount" Type="Edm.int"/>
	 */
	protected long favoritecount;
	
	/*
	 * Timestamp
	 * <Property Name="timestamp" Type="Edm.DateTime"/>
	 */
	protected String time;
	protected LocalDateTime timestamp;

	/*
	 * Client for posting
	 * <Property Name="client" Type="Edm.String" MaxLength="2048"/>
	 */
	protected String posted_from_client;

	/*
	 * <Property Name="truncated" Type="Edm.Byte" DefaultValue="0"/>
	 */
	protected Boolean truncated;

	/*
	 * Metadata for post replies
	 * <Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
	 */
	protected long in_reply_to_post;
	protected long in_reply_to_user;
	protected String in_reply_to_user_screen_name;

	/*
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

	/*
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 */
	protected String geoAroundLongitude;
	protected String geoAroundLatitude;
	
	/*
	 * hashtags, symbols and mentions - yet to be implemenetd
	 */
	protected List<?> hashtags;
	protected List<?> symbols;
	protected List<?> mentions;
	
	/*
	 * UserData userData - an embedded user object within th epage/post object
	 */
	protected UserData userData;
	
	// returns a string with all initialized variables as a concatenated string
	public String getAllContent(){
		String p = "objectStatus : " + getObjectStatus() + " / "
				+ "SN_ID : " + getSnId() + " / "
				+ "ID : " + getId() + " / "
				+ "Domain :" + getDomain()  + " / "
				+ "Customer :" + getCustomer()  + " / "
				+ "User Id :" + getUserId()  + " / "
				+ "text :" + getText().length()  + " / "
				+ "raw text :" + getRawText().length()+ " / "
				+ "teaser :" + getTeaser().length()  + " / "
				+ "subject :" + getSubject().length()  + " / "
				+ "lang :" + getLang()  + " / "
				+ "view count :" + getViewCount()  + " / "
				+ "favorite count :" + getFavoriteCount() + " / " 
				+ "time :" + getTime()  + " / "
				+ "timestamp :" + getTimestamp()  + " / "
				+ "client :" + getClient()  + " / "
				+ "truncated :" + getTruncated()   + " / "
				+ "in reply to post :" + getInReplyTo()  + " / "
				+ "in reply to user id :" + getInReplyToUser()  + " / "
				+ "in reply to user name :" + getInReplyToUserScreenName() + " / "
				+ "longitude :" + getGeoLongitude()  + " / "
				+ "latitude :" + getGeoLatitude() + " / " 
				+ "place id :" + getGeoPlaceId()  + " / "
				+ "place name :" + getGeoPlaceName()  + " / "
				+ "place country :" + getGeoPlaceCountry() + " / " 
				+ "around longitude :" + getGeoAroundLongitude()  + " / "
				+ "around latitude :" + getGeoAroundLatitude();
				
				//protected GeoJsonObject place;
				//protected List<?> hashtags;
				//protected List<?> symbols;
				//protected List<?> mentions;
				
		return p;
	}
	
	@SuppressWarnings("unchecked")
	public String toJsonString(){
		JSONObject obj = new JSONObject();
		obj.put("objectStatus", getObjectStatus());
		obj.put("SN_ID", getSnId());
		obj.put("ID", getId());
		obj.put("Domain", getDomain());
		obj.put("Customer", getCustomer());
		obj.put("User Id", getUserId());
		obj.put("text", getText().length());
		obj.put("raw text", getRawText().length());
		obj.put("teaser", getTeaser().length());
		obj.put("subject", getSubject().length());
		obj.put("lang", getLang());
		obj.put("view count", getViewCount());
		obj.put("favorite count", getFavoriteCount()); 
		obj.put("time", getTime());
		obj.put("timestamp", getTimestamp());
		obj.put("client", getClient());
		obj.put("truncated", getTruncated());
		obj.put("in reply to post", getInReplyTo());
		obj.put("in reply to user id", getInReplyToUser());
		obj.put("in reply to user name", getInReplyToUserScreenName());
		obj.put("longitude", getGeoLongitude());
		obj.put("latitude", getGeoLatitude());
		obj.put("place id", getGeoPlaceId());
		obj.put("place name", getGeoPlaceName());
		obj.put("place country", getGeoPlaceCountry()); 
		obj.put("around longitude", getGeoAroundLongitude());
		obj.put("around latitude", getGeoAroundLatitude());
				
				//protected GeoJsonObject place;
				//protected List<?> hashtags;
				//protected List<?> symbols;
				//protected List<?> mentions;
				
		return obj.toJSONString();
	}
	
	// getter and setter
	public String getObjectStatus() {return objectStatus;}
	public void setObjectStatus(String ostatus) {this.objectStatus = ostatus;}
	
	public String getDomain() {return domain;}
	public void setDomain(String dom) {this.domain = dom;}
	
	public String getCustomer() {return customer;}
	public void setCustomer(String sub) {this.customer = sub;}
	
	public long getId() {return id;}
	public void setId(long id) {this.id = id;}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String sn_id) {this.sn_id = sn_id;}
	
	public String getText() {return text;}
	public void setText(String text) {this.text = text;}
	
	public String getRawText() {return raw_text;}
	public void setRawText(String text) {this.raw_text = text;}
	
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
	
	public String getClient() {return posted_from_client;}
	public void setClient(String postClient) {this.posted_from_client = postClient;}
	
	public Boolean getTruncated() {return truncated;}
	public void setTruncated(Boolean isTruncated) {this.truncated = isTruncated;}
	
	public long getInReplyTo() {return in_reply_to_post;}
	public void setInReplyTo(Long inReplyTo) {this.in_reply_to_post = inReplyTo;}
	
	public long getInReplyToUser() {return in_reply_to_user;}
	public void setInReplyToUser(Long inReplyToUser) {this.in_reply_to_user = inReplyToUser;}
	
	public String getInReplyToUserScreenName() {return in_reply_to_user_screen_name;}
	public void setInReplyToUserScreenName(String inReplyToUserScreenName) {this.in_reply_to_user_screen_name = inReplyToUserScreenName;}

	public GeoJsonObject getPlace() {return place;}
	public void setPlace(GeoJsonObject place) {this.place = place;}

	public List<?> getHashtags() {return hashtags;}
	public void setHashtags(List<?> hashtags) {this.hashtags = hashtags;}

	public List<?> getSymbols() {return symbols;}
	public void setSymbols(List<?> symbols) {this.symbols = symbols;}

	public List<?> getMentions() {return symbols;}
	public void setMentions(List<?> mentions) {this.mentions = mentions;}
	
	public long getUserId() {return userId;}
	public void setUserId(long userId) {this.userId = userId;}

	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}

	// embedded user data
	public UserData getUserData(){return userData;}
	public void setUserData(UserData userData){this.userData = userData;}
		
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
