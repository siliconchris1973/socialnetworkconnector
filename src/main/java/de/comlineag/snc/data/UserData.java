package de.comlineag.snc.data;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther
 * @category	Data Class
 * @version		1.1
 * 
 * @description core data type for the User Data
 * 
 */
public class UserData {

	protected long id;								// the id of the positng within the social network
	protected String sn_id;							// the social network id from enum SocialNetworks
	protected String username;						// the actual user name, might be different from the screen name 
	protected String screen_name;					// the username as shown on the network (sometimes named nick name)
	protected String location;						// a simple location representation (like a town, or country name)
	protected long followers_count;					// how many people is the user following
	protected long friends_count;					// how many friends does the user have
	protected long postings_count;					// how many posts did the user write
	protected long favorites_count;					// how many posts where favorited
	protected long lists_and_groups_count;			// how many groups and lists did the user subscribe to
	protected float average_rating_value;			// how the user rates other peoples posts
	protected float average_posting_rating_value;	// how the postings of the user is rated by others
	protected float average_posting_ratio;			// how many posts per year
	protected String lang;							// default language of the user

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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getScreenName() {
		return screen_name;
	}

	public void setScreenName(String screenName) {
		this.screen_name = screenName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public long getFollowersCount() {
		return followers_count;
	}

	public void setFollowersCount(long followersCount) {
		this.followers_count = followersCount;
	}

	public long getFriendsCount() {
		return friends_count;
	}

	public void setFriendsCount(long friendsCount) {
		this.friends_count = friendsCount;
	}

	public long getPostingsCount() {
		return postings_count;
	}

	public void setPostingsCount(long postingsCount) {
		this.postings_count = postingsCount;
	}

	public long getFavoritesCount() {
		return favorites_count;
	}

	public void setFavoritesCount(long favoritesCount) {
		this.favorites_count = favoritesCount;
	}

	public long getListsAndGrooupsCount() {
		return lists_and_groups_count;
	}

	public void setListsAndGroupsCount(long listsAndGroupsCount) {
		this.lists_and_groups_count = listsAndGroupsCount;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

}
