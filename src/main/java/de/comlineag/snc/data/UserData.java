package de.comlineag.snc.data;

import org.json.simple.JSONObject;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther
 * @category	Data Class
 * @version		0.4c			- 22.10.2014
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
 * 				0.4c			changed user_id from Long to String
 * 
 */
@SuppressWarnings("unchecked")
public class UserData implements ISncDataObject{

	protected String id;							// the id of the user within the social network
	protected String domain;						// in which domain context was the user tracked
	protected String customer;						// in which customer context was the user tracked
	protected String sn_id;							// the social network id from enum SocialNetworks
	protected String user_name;						// the actual user name, might be different from the screen name 
	protected String screen_name;					// the user_name as shown on the network (sometimes named nick name)
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
	
	protected JSONObject internalJson;				// an internal json object containing all values
	
	public UserData(){
		if (internalJson == null)
			internalJson = new JSONObject();
	}
	
	public UserData(JSONObject userData){
		if (internalJson == null)
			internalJson = new JSONObject();
		
		if (userData.containsKey("id"))
			setId(userData.get("id").toString());
		else if (userData.containsKey("user_id"))
			setId(userData.get("user_id").toString());
		if (userData.containsKey("sn_id"))
			setSnId(userData.get("sn_id").toString());
		if (userData.containsKey("domain"))
			setDomain(userData.get("domain").toString());
		if (userData.containsKey("customer"))
			setCustomer(userData.get("customer").toString());
		if (userData.containsKey("user_name"))
			setUserName(userData.get("user_name").toString());
		else if (userData.containsKey("username"))
			setUserName(userData.get("username").toString());
		if (userData.containsKey("screen_name"))
			setScreenName(userData.get("screen_name").toString());
		if (userData.containsKey("lang"))
			setLang(userData.get("lang").toString());
		if (userData.containsKey("geoLocation"))
			setGeoLocation(userData.get("geoLocation").toString());
		if (userData.containsKey("objectStatus"))
			setObjectStatus(userData.get("objectStatus").toString());
		
		if (userData.containsKey("followers_count"))
			setFollowersCount((Long)userData.get("followers_count"));
		if (userData.containsKey("friends_count"))
			setFriendsCount((Long)userData.get("friends_count"));
		if (userData.containsKey("postings_count"))
			setPostingsCount((Long)userData.get("postings_count"));
		if (userData.containsKey("favorites_count"))
			setFavoritesCount((Long)userData.get("favorites_count"));
		if (userData.containsKey("lists_and_groups_count"))
			setListsAndGroupsCount((Long)userData.get("lists_and_groups_count"));
		
		if (userData.containsKey("average_rating_value"))
			setAverageRatingValue((Float)userData.get("average_rating_value"));
		if (userData.containsKey("average_posting_rating_value"))
			setAveragePostingRatingValue((Float)userData.get("average_posting_rating_value"));
		if (userData.containsKey("average_posting_ratio"))
			setAveragePostingRatio((Float)userData.get("average_posting_ratio"));
	}
	
	public String getAllContent(){
		String u = ""
				+ "objectStatus : " + getObjectStatus() + " / "
				+ "sn_id : " + getSnId() + " / "
				+ "id : " + getId() + " / "
				+ "domain : " + getDomain() + " / "
				+ "customer : " + getCustomer() + " / "
				+ "user_name : " + getUserName() + " / "
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
				+ "average posting rating : " + getAveragePostingRatio();
		
		return u;
	}
	
	public String toJsonString(){return internalJson.toJSONString();}
	
	// getter and setter
	public JSONObject getJson(){return internalJson;}
	
	public String getDomain() {return domain;}
	public void setDomain(String dom) {this.domain = dom; internalJson.put("domain", dom);}
	
	public String getCustomer() {return customer;}
	public void setCustomer(String sub) {this.customer = sub; internalJson.put("customer", sub);}
	
	public String getObjectStatus() {return objectStatus;}
	public void setObjectStatus(String ostatus) {this.objectStatus = ostatus; internalJson.put("objectStatus", ostatus);}
	
	public String getId() {return id;}
	public void setId(String uId) {this.id = uId; internalJson.put("id", uId);}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String snId) {this.sn_id = snId; internalJson.put("sn_id", snId);}
	
	public String getUserName() {return user_name;}
	public void setUserName(String userName) {this.user_name = userName; internalJson.put("user_name", userName);}
	
	public String getScreenName() {return screen_name;}
	public void setScreenName(String screenName) {this.screen_name = screenName; internalJson.put("screen_name", screen_name);}
	
	public String getGeoLocation() {return geoLocation;}
	public void setGeoLocation(String geoLoc) {this.geoLocation = geoLoc; internalJson.put("geoLocation", geoLoc);}
	
	public long getFollowersCount() {return followers_count;}
	public void setFollowersCount(long followersCount) {this.followers_count = followersCount; internalJson.put("followers_count", followersCount);}
	
	public long getFriendsCount() {return friends_count;}
	public void setFriendsCount(long friendsCount) {this.friends_count = friendsCount; internalJson.put("friends_count", friendsCount);}
	
	public long getPostingsCount() {return postings_count;}
	public void setPostingsCount(long postingsCount) {this.postings_count = postingsCount; internalJson.put("postings_count", postingsCount);}
	
	public float getAverageRatingValue() {return average_rating_value;}
	public void setAverageRatingValue(float avgRatingValue) {this.average_rating_value = avgRatingValue; internalJson.put("average_rating_value", avgRatingValue);}
	
	public float getAveragePostingRatingValue() {return average_posting_rating_value;}
	public void setAveragePostingRatingValue(float avgPostingRatingValue) {this.average_posting_rating_value = avgPostingRatingValue; internalJson.put("average_posting_rating_value", avgPostingRatingValue);}
	
	public float getAveragePostingRatio() {return average_posting_ratio;}
	public void setAveragePostingRatio(float avgPostingRatio) {this.average_posting_ratio = avgPostingRatio; internalJson.put("average_posting_ratio", avgPostingRatio);}
	
	public long getListsAndGroupsCount() {return lists_and_groups_count;}
	public void setListsAndGroupsCount(long listsAndGroupsCount) {this.lists_and_groups_count = listsAndGroupsCount; internalJson.put("lists_and_groups_count", listsAndGroupsCount);}
	
	public long getFavoritesCount() {return favorites_count;}
	public void setFavoritesCount(long favoritesCount) {this.favorites_count = favoritesCount; internalJson.put("favorites_count", favoritesCount);}
	
	public String getLang() {return lang;}
	public void setLang(String lng) {this.lang = lng; internalJson.put("lang", lng);}
}
