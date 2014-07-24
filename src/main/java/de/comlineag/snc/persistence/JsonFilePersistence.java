package de.comlineag.snc.persistence;

import java.io.FileWriter;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.DataCryptoHandler;

/**
 *
 * @author 		Christian Guenther
 * @category 	Persistence Manager
 * @version 	0.1	- 20.07.2014
 * @status		productive
 *
 * @description fallback persistence to store JSON files on disk - called up in case saving to db fails
 * 
 * @changelog	0.1 (Chris)			class created
 * 
 */
public class JsonFilePersistence {
	
	// Servicelocation 
	private String savePoint = "./json"; 
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();

	
	public JsonFilePersistence() {}

	//public void savePosts(PostData postData) {
	@SuppressWarnings("unchecked")
	public JsonFilePersistence(PostData postData) {
		FileWriter file;
		try {
			file = new FileWriter(savePoint+"/post_"+postData.getSnId()+postData.getId()+".json");
			
			JSONObject obj = new JSONObject();
			obj.put("sn_id", postData.getSnId());
			obj.put("post_id", postData.getId());
			obj.put("user_id", postData.getUserId());
			obj.put("timestamp", postData.getTimestamp());
			obj.put("postLang", postData.getLang());
			
			obj.put("text", postData.getText());
			obj.put("raw_text", postData.getRawText());
			obj.put("teaser", postData.getTeaser());
			obj.put("subject", postData.getSubject());
			
			obj.put("viewcount", postData.getViewCount());
			obj.put("favoritecount", postData.getFavoriteCount());
			
			obj.put("client", postData.getClient());
			obj.put("truncated", postData.getTruncated());

			obj.put("inReplyTo", postData.getInReplyTo());
			obj.put("inReplyToUserID", postData.getInReplyToUser());
			obj.put("inReplyToScreenName", postData.getInReplyToUserScreenName());
			
			obj.put("geoLocation_longitude", postData.getGeoLongitude());
			obj.put("geoLocation_latitude", postData.getGeoLatitude());
			obj.put("placeID", postData.getGeoPlaceId());
			obj.put("plName",  postData.getGeoPlaceName());
			obj.put("plCountry", postData.getGeoPlaceCountry());
			obj.put("plAround_longitude", postData.getGeoAroundLongitude());
			obj.put("plAround_latitude", postData.getGeoAroundLatitude());
			
			file.write(dataCryptoProvider.encryptValue(obj.toJSONString()));
	        logger.info("Successfully copied JSON post object for "+postData.getSnId()+"-"+postData.getId()+" to File...");
	        
	        file.flush();
			file.close();
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing post "+postData.getSnId()+"-"+postData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
		}
	}
	
	
	/**
	 * @description	save a user from social network to the HANA DB
	 * @param		UserData
	 */
	//public void saveUsers(UserData userData) {
	@SuppressWarnings("unchecked")
	public JsonFilePersistence(UserData userData) {
		FileWriter file;
		try {
			// first check if the entry already exists
			file = new FileWriter(savePoint+"/user_"+userData.getSnId()+userData.getId()+".json");
			
			JSONObject obj = new JSONObject();
			obj.put("sn_id", userData.getSnId());
			obj.put("user_id", userData.getId());
			obj.put("userName", userData.getUsername());
			obj.put("nickName", userData.getScreenName());
			obj.put("userLang", userData.getLang());
			obj.put("geoLocation", userData.getGeoLocation().toString());
			obj.put("follower", userData.getFollowersCount());
			obj.put("friends", userData.getFriendsCount());
			obj.put("postingsCount", userData.getPostingsCount());
			obj.put("favoritesCount", userData.getFavoritesCount());
			obj.put("listsAndGroupsCount", userData.getListsAndGrooupsCount());
						
			file.write(dataCryptoProvider.encryptValue(obj.toJSONString()));
			logger.info("Successfully copied JSON user object for "+userData.getSnId()+"-"+userData.getId()+" to File...");
	        
	        file.flush();
			file.close();
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing user "+userData.getSnId()+"-"+userData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
		}
	}
}