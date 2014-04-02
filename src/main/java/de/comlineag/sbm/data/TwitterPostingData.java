package de.comlineag.sbm.data;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * 
 * @author Christian Guenther, Magnus Leinemann
 * @category data type
 * 
 * @description Describes a single twitter posting with all relevant
 *              informations.
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
 * 
 * 
 */

public final class TwitterPostingData {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private long id; // ID from Twitter
	private String sn_id;
	private String text;
	private String time;
	private String posted_from_client;
	private Boolean truncated;
	private long in_reply_to_post;
	private long in_reply_to_user;
	private String in_reply_to_user_screen_name;
	private String coordinates;
	private List<?> place;
	private String lang;
	private List<?> hashtags;
	private List<?> symbols;

	public TwitterPostingData(JSONObject jsonObject) {

		// log the startup message
		logger.debug("creating new tweet within class " + getClass().getName());

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
		place = null;
		lang = null;
		hashtags = null;
		symbols = null;

		setId((Long) jsonObject.get("id"));
		setTime((String) jsonObject.get("created_at"));
		setText((String) jsonObject.get("text"));
		setClient((String) jsonObject.get("source"));
		setTruncated((Boolean) jsonObject.get("truncated"));

		if (jsonObject.get("in_reply_to_status_id") != null)
			setInReplyTo((Long) jsonObject.get("in_reply_to_status_id"));
		if (jsonObject.get("in_reply_to_user_id") != null)
			setInReplyToUser((Long) jsonObject.get("in_reply_to_user_id"));
		if (jsonObject.get("in_reply_to_screen_name") != null)
			setInReplyToUserScreenName((String) jsonObject.get("in_reply_to_screen_name"));

		// if (jsonObject.get("coordinates") != null)
		// setpGeoLocation((String)jsonObject.get("coordinates"));

		// setpPlace((List)jsonObject.get("place"));
		setLang((String) jsonObject.get("lang"));
		// setpHashtags((List)jsonObject.get("hashtags"));
		// setpSymbols((List)jsonObject.get("symbols"));
		// TODO: implement the List setters for Place, Hashtags and Symbols
	}

	// getter and setter
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSnId() {
		return sn_id;
	}

	public void setSnId(String sn_id) {
		this.sn_id = sn_id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String postTime) {
		this.time = postTime;
	}

	public String getClient() {
		return posted_from_client;
	}

	public void setClient(String postClient) {
		this.posted_from_client = postClient;
	}

	public Boolean getTruncated() {
		return truncated;
	}

	public void setTruncated(Boolean isTruncated) {
		this.truncated = isTruncated;
	}

	public long getInReplyTo() {
		return in_reply_to_post;
	}

	public void setInReplyTo(Long inReplyTo) {
		this.in_reply_to_post = inReplyTo;
	}

	public long getInReplyToUser() {
		return in_reply_to_user;
	}

	public void setInReplyToUser(Long inReplyToUser) {
		this.in_reply_to_user = inReplyToUser;
	}

	public String getInReplyToUserScreenName() {
		return in_reply_to_user_screen_name;
	}

	public void setInReplyToUserScreenName(String inReplyToUserScreenName) {
		this.in_reply_to_user_screen_name = inReplyToUserScreenName;
	}

	public String getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(String coordinates) {
		this.coordinates = coordinates;
	}

	public List<?> getPlace() {
		return place;
	}

	public void setPlace(List<?> place) {
		this.place = place;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public List<?> getHashtags() {
		return hashtags;
	}

	public void setHashtags(List<?> hashtags) {
		this.hashtags = hashtags;
	}

	public List<?> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<?> symbols) {
		this.symbols = symbols;
	}
}