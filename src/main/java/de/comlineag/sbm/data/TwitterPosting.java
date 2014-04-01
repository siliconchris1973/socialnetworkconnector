package de.comlineag.sbm.data;

import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author 		Christian Guenther
 * @category 	data type
 *
 * @description Die Klasse DT_TwitterPosting stellt den notwendigen Datentyp TwitterPosting bereit, damit
 *				nachfolgende Klassen und Methoden auf diesem Datentyp operieren koennen und somit typsafe sind
 */
public class TwitterPosting{
		
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private long id;
	private String text;
	private String pTime;
	private String pClient;
	private Boolean pTruncated;
	private long pInReplyTo;
	private long pInReplyToUser;
	private String pInReplyToUserScreenName;
	private String pGeoLocation;
	private List<?> pPlace;
	private String pLang;
	private List<?> pHashtags;
	private List<?> pSymbols;

	public TwitterPosting(JSONObject jsonObject){
		
		// log the startup message
		logger.debug("new tweet created within class " + getClass().getName());
					
		
		setId((Long)jsonObject.get("id"));
		setpTime((String)jsonObject.get("created_at"));
		setText((String)jsonObject.get("text"));		 
		setpClient((String)jsonObject.get("source"));
		setpTruncated((Boolean)jsonObject.get("truncated"));
		
		if (jsonObject.get("in_reply_to_status_id") != null)
			setpInReplyTo((Long)jsonObject.get("in_reply_to_status_id"));
		if (jsonObject.get("in_reply_to_user_id") != null)
			setpInReplyToUser((Long)jsonObject.get("in_reply_to_user_id"));
		if (jsonObject.get("in_reply_to_screen_name") != null)
			setpInReplyToUserScreenName((String)jsonObject.get("in_reply_to_screen_name"));
		
		//if (jsonObject.get("coordinates") != null)
		//	setpGeoLocation((String)jsonObject.get("coordinates"));
		
		//setpPlace((List)jsonObject.get("place"));
		setpLang((String)jsonObject.get("lang"));
		//setpHashtags((List)jsonObject.get("hashtags"));
		//setpSymbols((List)jsonObject.get("symbols"));
		//TODO: implement the List setters for Place, Hashtags and Symbols
	}
	
	// getter and setter
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getpTime() {
		return pTime;
	}
	public void setpTime(String pTime) {
		this.pTime = pTime;
	}
	public String getpClient() {
		return pClient;
	}
	public void setpClient(String pClient) {
		this.pClient = pClient;
	}
	public Boolean getpTruncated() {
		return pTruncated;
	}
	public void setpTruncated(Boolean pTruncated) {
		this.pTruncated = pTruncated;
	}
	public long getpInReplyTo() {
		return pInReplyTo;
	}
	public void setpInReplyTo(Long pInReplyTo) {
		this.pInReplyTo = pInReplyTo;
	}
	public long getpInReplyToUser() {
		return pInReplyToUser;
	}
	public void setpInReplyToUser(Long pInReplyToUser) {
		this.pInReplyToUser = pInReplyToUser;
	}
	public String getpInReplyToUserScreenName() {
		return pInReplyToUserScreenName;
	}
	public void setpInReplyToUserScreenName(String pInReplyToUserScreenName) {
		this.pInReplyToUserScreenName = pInReplyToUserScreenName;
	}
	public String getpGeoLocation() {
		return pGeoLocation;
	}
	public void setpGeoLocation(String pGeoLocation) {
		this.pGeoLocation = pGeoLocation;
	}
	public List<?> getpPlace() {
		return pPlace;
	}
	public void setpPlace(List<?> pPlace) {
		this.pPlace = pPlace;
	}
	public String getpLang() {
		return pLang;
	}
	public void setpLang(String pLang) {
		this.pLang = pLang;
	}
	public List<?> getpHashtags() {
		return pHashtags;
	}
	public void setpHashtags(List<?> pHashtags) {
		this.pHashtags = pHashtags;
	}
	public List<?> getpSymbols() {
		return pSymbols;
	}
	public void setpSymbols(List<?> pSymbols) {
		this.pSymbols = pSymbols;
	}
}