package de.comlineag.snc.data;

import org.json.simple.JSONObject;

import de.comlineag.snc.constants.GraphNodeTypes;

/**
 * 
 * @author		Christian Guenther
 * @category	Data Class
 * @version		0.1				- 11.11.2014
 * @status		productive
 * 
 * @description data type representing a user in the graph
 * 
 * @changelog	0.1	(Chris)		class created as copy from UserData Version 0.4c
 * 
 */
public class GraphUserData {

	protected final GraphNodeTypes gnt = GraphNodeTypes.USER;
	protected String sn_id;							// the 2-digit code of the social network 
	protected String id;							// the id of the user within the social network
	protected String username;						// the actual user name, might be different from the screen name 
	protected String screen_name;					// the username as shown on the network (sometimes named nick name)
	protected String lang;							// default language of the user
	
	protected long followers_count;					// how many people is the user following
	protected long friends_count;					// how many friends does the user have
	protected long postings_count;					// how many posts did the user write
	protected long favorites_count;					// how many posts where favorited
	protected long lists_and_groups_count;			// how many groups and lists did the user subscribe to
	protected float average_rating_value;			// how the user rates other peoples posts
	protected float average_posting_rating_value;	// how the postings of the user is rated by others
	protected float average_posting_ratio;			// how many posts per year
	protected String geoLocation;					// a simple geo location representation (like a town, or country name)
	
	protected JSONObject internalJson = new JSONObject();
	
	public GraphUserData(JSONObject obj){
		setSnId(obj.get("sn_id").toString());
		setId(obj.get("id").toString());
		setUsername(obj.get("username").toString());
		setScreenName(obj.get("screen_name").toString());
		setLang(obj.get("lang").toString());
		
		if (obj.containsKey("followers_count"))
			setFollowersCount((Long) obj.get("followers_count"));
		if (obj.containsKey("friends_count"))
			setFriendsCount((Long) obj.get("friends_count"));
		if (obj.containsKey("postings_count"))
			setPostingsCount((Long) obj.get("postings_count"));
		if (obj.containsKey("favorites_count"))
			setFavoritesCount((Long) obj.get("favorites_count"));
		if (obj.containsKey("lists_and_groups_count"))
			setListsAndGroupsCount((Long) obj.get("lists_and_groups_count"));
		
		internalJson = obj;
	}
	
	
	private String toJsonString(){
		/*
		JSONObject obj = new JSONObject();
		obj.put("id", getId());
		obj.put("username", getUsername());
		obj.put("screen_name", getScreenName());
		obj.put("lang", getLang());
		
		obj.put("followers_count", getFollowersCount());
		obj.put("friends_count", getFriendsCount());
		obj.put("postings_count", getPostingsCount());
		obj.put("favorites_count", getFavoritesCount());
		obj.put("lists_and_groups_count", getListsAndGroupsCount());
		
		obj.put("average_rating", getAverageRatingValue());
		obj.put("average_posting_rating", getAveragePostingRatingValue());
		obj.put("average_posting_rating", getAveragePostingRating());
		
		obj.put("geoLocation", getGeoLocation());
		
		return obj.toJSONString();
		*/
		return internalJson.toJSONString();
	}
	
	private String toMyJsonString(){
		JSONObject obj = new JSONObject();
		obj.put("sn_id", getSnId());
		obj.put("id", getId());
		obj.put("username", getUsername());
		obj.put("screen_name", getScreenName());
		obj.put("lang", getLang());
		/*
		obj.put("followers_count", getFollowersCount());
		obj.put("friends_count", getFriendsCount());
		obj.put("postings_count", getPostingsCount());
		obj.put("favorites_count", getFavoritesCount());
		obj.put("lists_and_groups_count", getListsAndGroupsCount());
		
		obj.put("average_rating", getAverageRatingValue());
		obj.put("average_posting_rating", getAveragePostingRatingValue());
		obj.put("average_posting_rating", getAveragePostingRating());
		
		obj.put("geoLocation", getGeoLocation());
		*/
		return obj.toJSONString();
		
	}
	
	
	/**
	 * @description	creates a string which can be passed to the neo4j cypher engine 
	 * @return		cypher string
	 */
	public String createCypher(){
		return "\""+gnt.toString()+"\" "+toMyJsonString();
	}
	
	// getter and setter
	public JSONObject getJson() {return this.internalJson;}
	
	public String getSnId() {return sn_id;}
	public void setSnId(String snid) {this.sn_id = snid;}
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getUsername() {return username;}
	public void setUsername(String username) {this.username = username;}
	
	public String getScreenName() {return screen_name;}
	public void setScreenName(String screenName) {this.screen_name = screenName;}
	
	public String getLang() {return lang;}
	public void setLang(String lang) {this.lang = lang;}
	
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
}
