package de.comlineag.sbm.data;

import java.util.List;

public class UserData {

	protected long id;
	protected String sn_id;
	protected String username;
	protected String screen_name;
	protected List<?> location;
	protected long followers_count;
	protected long friends_count;
	protected long postings_count;
	protected long favorites_count;
	protected long lists_and_groups_count;
	protected String lang;

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

	public List<?> getLocation() {
		return location;
	}

	public void setLocation(List<?> location) {
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
