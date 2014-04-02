package de.comlineag.sbm.data;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * 
 * @author Christian Guenther, Magnus Leinemann
 * @category data type
 * 
 * @description Describes a single twitter user with all relevant informations.
 *              The class shall be used to make all methods handling a twitter
 *              user type save.
 * 
 * @param <JSonObject>
 *            "id" Long
 *            "sn_id" String
 *            "name" String
 *            "screen_name" String
 *            "location" List
 *            "followers_count" Long
 *            "friends_count" Long
 *            "statuses_count" Long
 *            "favourites_count" Long
 *            "lists_and_groups_count" Long
 *            "lang" String
 */

public final class TwitterUserData {

	private long id;
	private String sn_id;
	private String username;
	private String screen_name;
	private List<?> location;
	private long followers_count;
	private long friends_count;
	private long postings_count;
	private long favorites_count;
	private long lists_and_groups_count;
	private String lang;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public TwitterUserData(JSONObject jsonObject) {
		// log the startup message
		logger.debug("creating new user within class " + getClass().getName());

		// setting everything to 0 or null default value.
		// so I can check on initialized or not initialized values for the
		// posting
		id = 0;
		// ACHTUNG, wenn die Klasse fuer Facebook u.a. kopiert wird,
		// daa muss dieses Value natuerlich umgesetzt werden
		sn_id = SocialNetworks.TWITTER.getValue();
		username = null;
		screen_name = null;
		location = null;
		followers_count = 0;
		friends_count = 0;
		postings_count = 0;
		favorites_count = 0;
		lists_and_groups_count = 0;
		lang = null;

		setId((Long) jsonObject.get("id"));
		setUsername((String) jsonObject.get("name"));
		setScreenName((String) jsonObject.get("screen_name"));
		// setLocation((List)jsonObject.get("location"));

		if (jsonObject.get("followers_count") != null)
			setFollowersCount((Long) jsonObject.get("followers_count"));
		if (jsonObject.get("friends_count") != null)
			setFriendsCount((Long) jsonObject.get("friends_count"));
		if (jsonObject.get("statuses_count") != null)
			setPostingsCount((Long) jsonObject.get("statuses_count"));
		if (jsonObject.get("favorites_count") != null)
			setFavoritesCount((Long) jsonObject.get("favorites_count"));
		if (jsonObject.get("lists_and_groups_count") != null)
			setListsAndGroupsCount((Long) jsonObject.get("lists_and_groups_count"));

		setLang((String) jsonObject.get("lang"));
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
