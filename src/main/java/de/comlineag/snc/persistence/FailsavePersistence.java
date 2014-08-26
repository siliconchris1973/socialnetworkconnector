package de.comlineag.snc.persistence;

import java.io.File;
import java.io.FileWriter;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.DataCryptoHandler;

/**
 *
 * @author 		Christian Guenther
 * @category 	Persistence Manager
 * @version 	0.2a			- 25.07.2014
 * @objectStatusPriorSaving		productive
 *
 * @description persistence manager to simply save JSON files on disk 
 * 				it can either be used stand alone, if activated as persistence manager in
 * 				applicationContext.xml, or it is used as a fall back solution in case
 * 				storing the data in one of the other available persistence manager fails.
 * 				To activate it as a fall back, the keys
 * 					CreatePostJsonOnError		- create a post json on error  
 * 					CreateUserJsonOnError		- create a user json on error
 * 					CreatePostJsonOnSuccess		- create a post json on success as well
 * 					CreateUserJsonOnSuccess		- create a user json on success as well
 * 				from RuntimeConfiguration.xml must be set to true. 
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				changed class to implement IPersistenceManager
 * 				0.2a			changed file pattern naming to TYPE_SCCODE-NUMBER_STATUS.json (e.g.: post_TW-34567_fail.json)
 * 
 */
public class FailsavePersistence implements IPersistenceManager {
	
	// define where and the files shall be saved
	private String savePoint = RuntimeConfiguration.getJSON_BACKUP_STORAGE_PATH();
	private String objectStatusPriorSaving; // was storing of the object prior saving to disk (e.g. n a db) successful (ok) or not (fail)
	private String objectTypeToSave;		// can either be user or post
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
		
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	
	public FailsavePersistence() {
		File f = new File(savePoint);
		if (!f.isDirectory()) {
			// create the json diretory
			f.mkdir();
		}
	}
	
	/**
	 * @description	constructor to save a post from social network to the file-system
	 * @param		PostData
	 */
	public FailsavePersistence(PostData postData) {
		logger.trace("checking if storage directory "+savePoint+" exists");
		File f = new File(savePoint);
		if (!f.isDirectory()) {
			// create the json diretory
			f.mkdir();
		}
		savePosts(postData);
	}
	
	/**
	 * @description	constructor to save a user from social network to the file-system
	 * @param		UserData
	 */
	public FailsavePersistence(UserData userData) {
		logger.trace("checking if storage directory "+savePoint+" exists");
		File f = new File(savePoint);
		if (!f.isDirectory()) {
			// create the json diretory
			f.mkdir();
		}
		saveUsers(userData);
	}
	
	
	
	/**
	 * @description	save a user from social network to the file-system
	 * @param		UserData
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void saveUsers(UserData userData) {
		objectTypeToSave = "user";
		objectStatusPriorSaving = userData.getObjectStatus(); // ok or fail
		if (objectStatusPriorSaving == null)
			objectStatusPriorSaving = "ok";
		
		FileWriter file;
		try {
			// first check if the entry already exists
			file = new FileWriter(savePoint+File.separator+objectTypeToSave+"_"+userData.getSnId()+"-"+userData.getId()+"_"+objectStatusPriorSaving+".json");
			
			JSONObject obj = new JSONObject();
			obj.put("sn_id", userData.getSnId());
			obj.put("user_id", new Long(userData.getId()).toString());
			obj.put("userName", userData.getUsername());
			obj.put("nickName", userData.getScreenName());
			obj.put("userLang", userData.getLang());
			obj.put("geoLocation", userData.getGeoLocation().toString());
			obj.put("follower", new Long(userData.getFollowersCount()).toString());
			obj.put("friends", new Long(userData.getFriendsCount()).toString());
			obj.put("postingsCount", new Long(userData.getPostingsCount()).toString());
			obj.put("favoritesCount", new Long(userData.getFavoritesCount()).toString());
			obj.put("listsAndGroupsCount", new Long(userData.getListsAndGroupsCount()).toString());
						
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

	/**
	 * @description	save a post from social network to the file-system
	 * @param		PostData
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void savePosts(PostData postData) {
		objectTypeToSave = "post";
		objectStatusPriorSaving = postData.getObjectStatus(); // ok or fail
		if (objectStatusPriorSaving == null)
			objectStatusPriorSaving = "ok";
		
		FileWriter file;
		try {
			file = new FileWriter(savePoint+File.separator+objectTypeToSave+"_"+postData.getSnId()+"-"+postData.getId()+"_"+objectStatusPriorSaving+".json");
			
			JSONObject obj = new JSONObject();
			obj.put("sn_id", postData.getSnId());
			obj.put("post_id", postData.getId());
			obj.put("user_id", postData.getUserId());
			obj.put("timestamp", postData.getTimestamp().toString());
			obj.put("postLang", postData.getLang());
			
			obj.put("text", postData.getText());
			obj.put("raw_text", postData.getRawText());
			obj.put("teaser", postData.getTeaser());
			obj.put("subject", postData.getSubject());
			
			obj.put("viewcount", new Long(postData.getViewCount()).toString());
			obj.put("favoritecount", new Long(postData.getFavoriteCount()).toString());
			
			obj.put("client", postData.getClient());
			obj.put("truncated", new Boolean(postData.getTruncated()).toString());

			obj.put("inReplyTo", postData.getInReplyTo());
			obj.put("inReplyToUserID", new Long(postData.getInReplyToUser()).toString());
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
}
