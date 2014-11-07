package de.comlineag.snc.persistence;

import java.io.File;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.data.PostingData;
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
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// define where and the files shall be saved
	private String savePoint = rtc.getStringValue("JsonBackupStoreagePath", "runtime");
	private String objectStatusPriorSaving; // was storing of the object prior saving to disk (e.g. n a db) successful (ok) or not (fail)
	private String objectTypeToSave;		// can either be user or post
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
		
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
	 * @param		PostingData
	 */
	public FailsavePersistence(PostingData postingData) {
		logger.trace("checking if storage directory "+savePoint+" exists");
		File f = new File(savePoint);
		if (!f.isDirectory()) {
			// create the json diretory
			f.mkdir();
		}
		savePosts(postingData);
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
	 * @param		PostingData
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void savePosts(PostingData postingData) {
		objectTypeToSave = "post";
		objectStatusPriorSaving = postingData.getObjectStatus(); // ok or fail
		if (objectStatusPriorSaving == null)
			objectStatusPriorSaving = "ok";
		
		FileWriter file;
		try {
			file = new FileWriter(savePoint+File.separator+objectTypeToSave+"_"+postingData.getSnId()+"-"+postingData.getId()+"_"+objectStatusPriorSaving+".json");
			
			JSONObject obj = new JSONObject();
			obj.put("sn_id", postingData.getSnId());
			obj.put("post_id", postingData.getId());
			obj.put("user_id", postingData.getUserId());
			obj.put("timestamp", postingData.getTimestamp().toString());
			obj.put("postLang", postingData.getLang());
			
			obj.put("text", postingData.getText());
			obj.put("raw_text", postingData.getRawText());
			obj.put("teaser", postingData.getTeaser());
			obj.put("subject", postingData.getSubject());
			
			obj.put("viewcount", new Long(postingData.getViewCount()).toString());
			obj.put("favoritecount", new Long(postingData.getFavoriteCount()).toString());
			
			obj.put("client", postingData.getClient());
			obj.put("truncated", new Boolean(postingData.getTruncated()).toString());

			obj.put("inReplyTo", postingData.getInReplyTo());
			obj.put("inReplyToUserID", new Long(postingData.getInReplyToUser()).toString());
			obj.put("inReplyToScreenName", postingData.getInReplyToUserScreenName());
			
			obj.put("geoLocation_longitude", postingData.getGeoLongitude());
			obj.put("geoLocation_latitude", postingData.getGeoLatitude());
			obj.put("placeID", postingData.getGeoPlaceId());
			obj.put("plName",  postingData.getGeoPlaceName());
			obj.put("plCountry", postingData.getGeoPlaceCountry());
			obj.put("plAround_longitude", postingData.getGeoAroundLongitude());
			obj.put("plAround_latitude", postingData.getGeoAroundLatitude());
			
			file.write(dataCryptoProvider.encryptValue(obj.toJSONString()));
	        logger.info("Successfully copied JSON post object for "+postingData.getSnId()+"-"+postingData.getId()+" to File...");
	        
	        file.flush();
			file.close();
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing post "+postingData.getSnId()+"-"+postingData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
		}
	}
}
