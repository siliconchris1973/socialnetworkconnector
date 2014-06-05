package de.comlineag.sbm.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.log4j.*;
import org.json.simple.JSONObject;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.SocialNetworkEntryTypes;
import de.comlineag.sbm.data.UserData;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 *
 */
public final class FileWriter extends File implements IPersistenceManager {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String FILE_PATH = "storedJSON";
	
	String fileName;

	public FileWriter(String pathname) {
		super(pathname);
		// log the startup message
		logger.debug("initiating new filewriter to " + pathname);
	}

	public FileWriter(URI uri) {
		super(uri);
		// log the startup message
		logger.debug("initiating new filewriter to " + uri);
	}

	public FileWriter(String parent, String child) {
		super(parent, child);
		// log the startup message
		logger.debug("initiating new filewriter with parent " + parent + " and child " + child);
	}

	public FileWriter(File parent, String child) {
		super(parent, child);
		// log the startup message
		logger.debug("initiating new filewriter with parent " + parent.toString() + " and child " + child);
	}
	
	@SuppressWarnings("unchecked")
	public void savePosts(PostData postData) {
		// set json payload
		JSONObject p = new JSONObject();
		
		p.put("type", SocialNetworkEntryTypes.POSTING.toString());		// --> Fixed value Post
		p.put("sn_id", postData.getSnId());								// Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"		--> Fixed value TW
		p.put("post_id", postData.getId());								// Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"
		p.put("timestamp", "\"" + postData.getTimestamp() + "\""); 		// Property Name="timestamp" Type="Edm.DateTime"
		p.put("postLang", postData.getLang());							// Property Name="postLang" Type="Edm.String" MaxLength="64"
		p.put("text", postData.getText().toString());					// Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="1024"
		p.put("truncated", postData.getTruncated());					// Property Name="truncated" Type="Edm.Byte" DefaultValue="0"					--> true or false
		p.put("client", postData.getClient()); 							// OBSOLETE Name="client" Type="Edm.String" MaxLength="2048"
		
		// TODO check implementation of geo location
		if (postData.getGeoLongitude() != null) 
			p.put("geoLocation_longitude", postData.getGeoLongitude());	// Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"
		if (postData.getGeoLatitude() != null)
			p.put("geoLocation_latitude", postData.getGeoLatitude());	// Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"
		if (postData.getGeoPlaceId() != null)
			p.put("placeID", postData.getGeoPlaceId());					// Property Name="placeID" Type="Edm.String" MaxLength="16"
		if (postData.getGeoPlaceName() != null)
			p.put("plName", postData.getGeoPlaceName());				// Property Name="plName" Type="Edm.String" MaxLength="256"
		if (postData.getGeoPlaceCountry() != null)
			p.put("plCountry", postData.getGeoPlaceCountry());			// Property Name="plCountry" Type="Edm.String" MaxLength="128"
		//TODO check where to get plAround_longitude and _latitude
		//p.put("plAround_longitude", "00 00 00 00 00");				// Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"
		//p.put("plAround_latitude", "00 00 00 00 00");					// Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"
		
		p.put("user_id", postData.getUserId()); 						// CONNECTION Name="user_id" Type="AUTHORED"
		p.put("inReplyTo", postData.getInReplyTo()); 					// CONNECTION Name="inReplyTo" Type="REPLIED_ON"
		p.put("inReplyToUserID", postData.getInReplyToUser()); 			// CONNECTION Name="inReplyToUserID" Type="REPLIED_TO"
		
		logger.trace("about to insert the following data in the file: " + p.toString());
		
		fileName = SocialNetworkEntryTypes.POSTING.toString() + postData.getId() + ".json";
		
		FileWriter postFile = new FileWriter(FILE_PATH + "/" + fileName);
		
		try {
			postFile.createNewFile();
		} catch (IOException e) {
			logger.error("EXCEPTION :: could not create file " + postFile + ": " + e.getLocalizedMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void saveUsers(UserData userData) {
		
		// set json payload 
		JSONObject u = new JSONObject();
		
		u.put("type", SocialNetworkEntryTypes.USER.toString());				// -- Fixed value User  
		u.put("sn_id", userData.getSnId());									// {name = "sn_id"; sqlType = NVARCHAR; nullable = false; length = 2;},
		u.put("user_id", userData.getId());									// {name = "user_id"; sqlType = NVARCHAR; nullable = false; length = 20;},
		u.put("userName", userData.getUsername());							// {name = "userName"; sqlType = NVARCHAR; nullable = true; length = 128;},
		u.put("nickName", userData.getScreenName());						// {name = "nickName"; sqlType = NVARCHAR; nullable = true; length = 128;},
		u.put("userLang", userData.getLang());								// {name = "userLang"; sqlType = NVARCHAR; nullable = true; length = 64;},
		u.put("location", userData.getLocation());							// {name = "location"; sqlType = NVARCHAR; nullable = true; length = 1024;},
		u.put("follower", userData.getFollowersCount());					// {name = "follower"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("friends", userData.getFriendsCount());						// {name = "friends"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("postingsCount", userData.getPostingsCount());				// {name = "postingsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("favoritesCount", userData.getFavoritesCount());				// {name = "favoritesCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("listsAndGroupsCount", userData.getListsAndGrooupsCount());	// {name = "listsAndGroupsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";}
		
		logger.trace("about to insert the following data in the file: " + u.toString());
		
		fileName = SocialNetworkEntryTypes.USER.toString() + userData.getId() + ".json";
		
		FileWriter userFile = new FileWriter(FILE_PATH + "/" + fileName);
		
		try {
			userFile.createNewFile();
		} catch (IOException e) {
			logger.error("EXCEPTION :: could not create file " + userFile + ": " + e.getLocalizedMessage());
		}
	}
}
