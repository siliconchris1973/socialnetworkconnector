package de.comlineag.snc.data;

import org.json.simple.JSONObject;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther
 * @category	Data Class
 * @version		0.4a			- 10.10.2014
 * @status		productive
 * 
 * @description core data type for the User Data
 * 
 * @changelog	0.1	(Magnus)	class created according to twitter user needs
 * 				0.2 (Chris)		added fields for counters
 * 				0.3				added fields average_rating_value, average_posting_rating_value and average_posting_ratio for Lithium user 
 * 				0.4				changed field name from location to geoLocation
 * 				0.4a			added fields for domain and customer
 * 				0.4b			added method toJsonString() - returning all uerData content as a json string 
 * 
 * TODO 1. check if we need more/other fields for other social networks
 */
public class UserData {

	protected long id;								// the id of the user within the social network
	protected String domain;						// in which domain context was the user tracked
	protected String customer;						// in which customer context was the user tracked
	protected String sn_id;							// the social network id from enum SocialNetworks
	protected String username;						// the actual user name, might be different from the screen name 
	protected String screen_name;					// the username as shown on the network (sometimes named nick name)
	protected String geoLocation;					// a simple geo location representation (like a town, or country name)
	protected long followers_count;					// how many people is the user following
	protected long friends_count;					// how many friends does the user have
	protected long postings_count;					// how many posts did the user write
	protected long favorites_count;					// how many posts where favorited
	protected long lists_and_groups_count;			// how many groups and lists did the user subscribe to
	protected float average_rating_value;			// how the user rates other peoples posts
	protected float average_posting_rating_value;	// how the postings of the user is rated by others
	protected float average_posting_ratio;			// how many posts per year
	protected String lang;							// default language of the user
	protected String objectStatus;					// can be new, old, ok or fail
	
	
	public String getAllContent(){
		String u = ""
				+ "objectStatus : " + getObjectStatus() + " / "
				+ "sn_id : " + getSnId() + " / "
				+ "id : " + getId() + " / "
				+ "domain : " + getDomain() + " / "
				+ "customer : " + getCustomer() + " / "
				+ "username : " + getUsername() + " / "
				+ "lang : " + getLang() + " / "
				+ "screen name : " + getScreenName() + " / "
				+ "geoLocation : " + getGeoLocation() + " / "
				+ "followers count : " + getFollowersCount() + " / "
				+ "friends count : " + getFriendsCount() + " / "
				+ "postings count : " + getPostingsCount() + " / "
				+ "favorites count : " + getFavoritesCount() + " / "
				+ "lists and groups count : " + getListsAndGroupsCount() + " / "
				+ "average rating : " + getAverageRatingValue() + " / "
				+ "average posting rating : " + getAveragePostingRatingValue() + " / "
				+ "average posting rating : " + getAveragePostingRating();
		
		return u;
	}
	
	@SuppressWarnings("unchecked")
	public String toJsonString(){
		JSONObject obj = new JSONObject();
		obj.put("objectStatus", getObjectStatus());
		obj.put("sn_id", getSnId());
		obj.put("id", getId());
		obj.put("domain", getDomain());
		obj.put("customer", getCustomer());
		obj.put("username", getUsername());
		obj.put("lang", getLang());
		obj.put("screen name", getScreenName());
		obj.put("geoLocation", getGeoLocation());
		obj.put("followers count", getFollowersCount());
		obj.put("friends count", getFriendsCount());
		obj.put("postings count", getPostingsCount());
		obj.put("favorites count", getFavoritesCount());
		obj.put("lists and groups count", getListsAndGroupsCount());
		obj.put("average rating", getAverageRatingValue());
		obj.put("average posting rating", getAveragePostingRatingValue());
		obj.put("average posting rating", getAveragePostingRating());
		
		return obj.toJSONString();
	}
	
	// getter and setter
	public String getDomain() {return domain;}
	public void setDomain(String dom) {this.domain = dom;}
	
	public String getCustomer() {return customer;}
	public void setCustomer(String sub) {this.customer = sub;}
	
	public String getObjectStatus() {return objectStatus;}
	public void setObjectStatus(String ostatus) {this.objectStatus = ostatus;}
	
	public long getId() {return id;}
	public void setId(long id) {this.id = id;}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String sn_id) {this.sn_id = sn_id;}
	
	public String getUsername() {return username;}
	public void setUsername(String username) {this.username = username;}
	
	public String getScreenName() {return screen_name;}
	public void setScreenName(String screenName) {this.screen_name = screenName;}
	
	public String getGeoLocation() {return geoLocation;}
	public void setGeoLocation(String geoLocation) {this.geoLocation = geoLocation;}
	
	public long getFollowersCount() {return followers_count;}
	public void setFollowersCount(long followersCount) {this.followers_count = followersCount;}
	
	public long getFriendsCount() {return friends_count;}
	public void setFriendsCount(long friendsCount) {this.friends_count = friendsCount;}
	
	public long getPostingsCount() {return postings_count;}
	public void setPostingsCount(long postingsCount) {this.postings_count = postingsCount;}
	
	public float getAverageRatingValue() {return average_rating_value;}
	public void setAverageRatingValue(float average_rating_value) {this.average_rating_value = average_rating_value;}
	
	public float getAveragePostingRatingValue() {return average_posting_rating_value;}
	public void setAveragePostingRatingValue(float average_posting_rating_value) {this.average_posting_rating_value = average_posting_rating_value;}
	
	public float getAveragePostingRating() {return average_posting_ratio;}
	public void setAveragePostingRating(float average_posting_ratio) {this.average_posting_ratio = average_posting_ratio;}
	
	public long getListsAndGroupsCount() {return lists_and_groups_count;}
	public void setListsAndGroupsCount(long listsAndGroupsCount) {this.lists_and_groups_count = listsAndGroupsCount;}
	
	public long getFavoritesCount() {return favorites_count;}
	public void setFavoritesCount(long favoritesCount) {this.favorites_count = favoritesCount;}
	
	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}
}
