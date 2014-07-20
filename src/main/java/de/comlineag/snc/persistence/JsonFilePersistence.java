package de.comlineag.snc.persistence;

import java.io.FileWriter;

import org.apache.log4j.Logger;

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
 * @description fallback persistence to store JSON files on disk, in case saving to db fails
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
	public JsonFilePersistence(PostData postData) {
		FileWriter file;
		try {
			file = new FileWriter(savePoint+"/post_"+postData.getSnId()+postData.getId()+".json");
		
			file.write(dataCryptoProvider.encryptValue(postData.toString()));
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
	public JsonFilePersistence(UserData userData) {
		FileWriter file;
		try {
			// first check if the entry already exists
			file = new FileWriter(savePoint+"/user_"+userData.getSnId()+userData.getId()+".json");
			
			file.write(dataCryptoProvider.encryptValue(userData.toString()));
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
