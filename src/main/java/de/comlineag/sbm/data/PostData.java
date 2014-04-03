package de.comlineag.sbm.data;

import java.util.List;

public class PostData {

	protected long id; // ID from Twitter
	protected String sn_id;
	protected String text;
	protected String time;
	protected String posted_from_client;
	protected Boolean truncated;
	protected long in_reply_to_post;
	protected long in_reply_to_user;
	protected String in_reply_to_user_screen_name;
	protected String coordinates;
	protected List<?> place;
	protected String lang;
	protected List<?> hashtags;
	protected List<?> symbols;

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
