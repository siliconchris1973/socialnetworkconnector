package de.comlineag.snc.data;

import java.util.ArrayList;
import java.util.List;

import org.geojson.GeoJsonObject;
import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;

/**
 * 
 * @author 		Magnus Leinemann, Christian Guenther
 * @category 	data class
 * @version 	0.6				- 18.11.2014
 * @status		productive
 * 
 * @description Data Class for one post object as tracked from a website or social network. 
 * 				All elements of the post are available as fields, plus there is a json-object
 * 				that also contains all fields as key: value store. 
 * 				Additionally the data class contains data objects for the user, the domain of 
 * 				interest, the customer, the social network and the trackterms because of which  
 * 				the post was tracked. 
 * 				The user is available in two forms: 1st as a userData object AND as json-object.
 * 				Domain and customer is available as a field and as an internal json object.
 * 				The social network is available as a 2-digit field, containing the code of the 
 * 				network, and as an embedded json object with information filled from the
 * 				SocialNetworkDefinitions xml-file. 
 * 				The track-terms are stored as an array-list.
 * 
 * 
 * @changelog	0.1 (Magnus)	class created according to twitter user needs
 * 				0.2 (Chris)		added field raw-text for Lithium postings which contain html 
 * 								- the markups are stripped for the normal text field
 * 				0.2a			added domain and customer
 * 				0.3				added method to fill all values from a passed json string 
 * 								for use with the FsCrawler, which creates a posData object 
 * 								from a stored json file and then invokes the persistence layer
 * 				0.4				added possibility to store an embedded UserData object within the 
 * 								PostingData object
 * 				0.5				changed post_id from long to String
 * 				0.6				added field internalJson (to hold all fields in a json structure) and 
 * 								embedded json for the domain of interest, the customer and the social 
 * 								network. Also added an arraylist of strings with the keywords because 
 * 								of which the post was tracked
 * 
 * TODO move GeneralDataDefinitions to RuntimeConfiguration and source it in from XML 
 */
@SuppressWarnings("unchecked")
public class PostingData implements ISncDataObject{
	
	public PostingData(){
		if (internalJson == null)
			internalJson = new JSONObject();
	}
	
	/*
	 * was the object initially saved correctly by the persistence manager or not 
	 */
	protected String objectStatus;	// can be ok or fail
	
	/*
	 * holds all fields of the data set as a json object 
	 */
	protected JSONObject internalJson;
	
	/*
	 * all track terms for which the post was tracked 
	 */
	protected ArrayList<String> trackterms;
	
	/*
	 * DomainData domainData - an embedded domain object within the page/post object
	 */
	protected DomainData domainData;
	
	/*
	 * CustomerData customerData - an embedded customer object within the page/post object
	 */
	protected CustomerData customerData;
	
	/*
	 * SocialNetworkData socialNetworkData - an embedded social network object within the page/post object
	 */
	protected SocialNetworkData socialNetworkData;
	
	/*
	 * UserData userData - an embedded user object within the page/post object
	 */
	protected UserData userData;
	
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
	 * TODO change this to string does not need to be long and creates only issues with the web parser
	 */
	protected String id; // ID from the social network

	/*
	 * User ID of the posting user
	 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 */
	protected String userId;

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
	protected boolean truncated;

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
	protected ArrayList<String> hashtags;
	protected ArrayList<String> symbols;
	protected ArrayList<String> mentions;
	
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
	
	public String toJsonString(){return internalJson.toJSONString();}
	
	// getter and setter
	public JSONObject getJson(){return internalJson;}
	
	// embedded user data
	public UserData getUserData(){return userData;}
	public void setUserData(UserData userData){
		//System.out.println("adding user object "+userData.toString()+" as embedded object");
		this.userData = userData;
		internalJson.put("USER", userData.getJson());
	}
	
	// embedded domain data
	public DomainData getDomainData(){return domainData;}
	public void setDomainData(DomainData domainData){
		//System.out.println("adding domain object "+domainData.toString()+" as embedded object");
		this.domainData = domainData; 
		internalJson.put("DOMAIN", domainData.getJson());
	}
	
	// embedded customer data
	public CustomerData getCustomerData(){return customerData;}
	public void setCustomerData(CustomerData customerData){
		//System.out.println("adding customer object "+customerData.toString()+" as embedded object");
		this.customerData = customerData; 
		internalJson.put("CUSTOMER", customerData.getJson());
	}
	
	// embedded social network data
	public SocialNetworkData getSocialNetworkData(){return socialNetworkData;}
	public void setSocialNetworkData(SocialNetworkData socialNetworkData){
		//System.out.println("adding socnet object "+socialNetworkData.toString()+" as embedded object");
		this.socialNetworkData = socialNetworkData; 
		internalJson.put("SOCIALNETWORK", socialNetworkData.getJson());
	}
	
	// track-terms aka keywords
	public ArrayList<String> getTrackTerms() {return trackterms;}
	public void setTrackTerms(List<String> trackterms) {
		assert (socialNetworkData != null) : "ERROR :: cannot operate on empty input";
		
		for (int i=0;i<trackterms.size();i++)
			this.trackterms.add(trackterms.get(i).toString());
	}
	
	// the standard fields
	public String getObjectStatus() {return objectStatus;}
	public void setObjectStatus(String ostatus) {this.objectStatus = ostatus; internalJson.put("objectStatus", ostatus);}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id; internalJson.put("id", id);}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String sn_id) {this.sn_id = sn_id; internalJson.put("sn_id", sn_id);}
	
	public String getDomain() {return domain;}
	public void setDomain(String dom) {this.domain = dom; internalJson.put("domain", dom);}
	
	public String getCustomer() {return customer;}
	public void setCustomer(String sub) {this.customer = sub; internalJson.put("customer", sub);}
	
	public String getText() {return text;}
	public void setText(String text) {this.text = text; internalJson.put("text", text);}
	
	public String getRawText() {return raw_text;}
	public void setRawText(String text) {this.raw_text = text; internalJson.put("raw_text", text);}
	
	public String getSubject() {return subject;}
	public void setSubject(String subject) {this.subject = subject; internalJson.put("subject", subject);}
	
	public String getTeaser() {return teaser;}
	public void setTeaser(String teaser) {this.teaser = teaser; internalJson.put("teaser", teaser);}

	public long getViewCount() {return viewcount;}
	public void setViewCount(long viewcount) {this.viewcount = viewcount; internalJson.put("viewcount", viewcount);}
	
	public long getFavoriteCount() {return favoritecount;}
	public void setFavoriteCount(long favoritecount) {this.favoritecount = favoritecount; internalJson.put("favoritecount", favoritecount);}
	
	public String getTime() {return time;}
	public void setTime(String postTime) {this.time = postTime; internalJson.put("time", postTime);}
	public LocalDateTime getTimestamp() {return timestamp;}
	public void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp; internalJson.put("timestamp", timestamp);}
	
	public String getClient() {return posted_from_client;}
	public void setClient(String postClient) {this.posted_from_client = postClient; internalJson.put("posted_from_client", posted_from_client);}
	
	public boolean getTruncated() {return truncated;}
	public void setTruncated(boolean isTruncated) {this.truncated = isTruncated; internalJson.put("truncated", isTruncated);}
	
	public long getInReplyTo() {return in_reply_to_post;}
	public void setInReplyTo(Long inReplyTo) {this.in_reply_to_post = inReplyTo; internalJson.put("in_reply_to_post", inReplyTo);}
	
	public long getInReplyToUser() {return in_reply_to_user;}
	public void setInReplyToUser(Long inReplyToUser) {this.in_reply_to_user = inReplyToUser; internalJson.put("in_reply_to_user", inReplyToUser);}
	
	public String getInReplyToUserScreenName() {return in_reply_to_user_screen_name;}
	public void setInReplyToUserScreenName(String inReplyToUserScreenName) {this.in_reply_to_user_screen_name = inReplyToUserScreenName; internalJson.put("in_reply_to_user_screen_name", inReplyToUserScreenName);}

	public GeoJsonObject getPlace() {return place;}
	public void setPlace(GeoJsonObject place) {this.place = place; internalJson.put("place", place);}
	
	public String getUserId() {return userId;}
	public void setUserId(String userId) {this.userId = userId; internalJson.put("user_id", userId);}

	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang; internalJson.put("lang", lang);}
	
	
	// GEO data
	public String getGeoLongitude() {return geoLongitude;}
	public void setGeoLongitude(String geoLongitude) {this.geoLongitude = geoLongitude; internalJson.put("geoLongitude", geoLongitude);}
	
	public String getGeoLatitude() {return geoLatitude;}
	public void setGeoLatitude(String geoLatitude) {this.geoLatitude = geoLatitude; internalJson.put("geoLatitude", geoLatitude);}
	
	public String getGeoPlaceId() {return geoPlaceId;}
	public void setGeoPlaceId(String geoPlaceId) {this.geoPlaceId = geoPlaceId; internalJson.put("geoPlaceId", geoPlaceId);}
	
	public String getGeoPlaceName() {return geoPlaceName;}
	public void setGeoPlaceName(String geoPlaceName) {this.geoPlaceName = geoPlaceName; internalJson.put("geoPlaceName", geoPlaceName);}
	
	public String getGeoPlaceCountry() {return geoPlaceCountry;}
	public void setGeoPlaceCountry(String geoPlaceCountry) {this.geoPlaceCountry = geoPlaceCountry; internalJson.put("geoPlaceCountry", geoPlaceCountry);}
	
	public String getGeoAroundLongitude() {return geoAroundLongitude;}
	public void setGeoAroundLongitude(String geoAroundLongitude) {this.geoAroundLongitude = geoAroundLongitude; internalJson.put("geoAroundLongitude", geoAroundLongitude);}
	
	public String getGeoAroundLatitude() {return geoAroundLatitude;}
	public void setGeoAroundLatitude(String geoAroundLatitude) {this.geoAroundLatitude = geoAroundLatitude; internalJson.put("geoAroundLatitude", geoAroundLatitude);}
	
	
	public ArrayList<String> getHashtags() {return hashtags;}
	public void setHashtags(List<String> hashtags) {
		JSONObject hasht = new JSONObject();
		for (int i=0;i<hashtags.size();i++) {
			hasht.put("hashtag", hashtags.get(i));
			this.hashtags.add(hashtags.get(i).toString());
		}
		internalJson.put("hashtags", hasht);
	}

	public ArrayList<String> getSymbols() {return symbols;}
	public void setSymbols(List<String> symbols) {
		JSONObject symbo = new JSONObject();
		for (int i=0;i<symbols.size();i++) {
			symbo.put("symbol", symbols.get(i));
			this.symbols.add(symbols.get(i).toString());
		}
		internalJson.put("symbols", symbo);
	}

	public ArrayList<String> getMentions() {return symbols;}
	public void setMentions(List<String> mentions) {
		JSONObject mentio = new JSONObject();
		for (int i=0;i<mentions.size();i++) {
			mentio.put("mention", mentions.get(i));
			this.mentions.add(mentions.get(i).toString());
		}
		internalJson.put("mentions", mentio);
	}
}
