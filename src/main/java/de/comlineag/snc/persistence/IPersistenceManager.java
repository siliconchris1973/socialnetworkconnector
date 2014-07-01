package de.comlineag.snc.persistence;

import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.LocationData;
import de.comlineag.snc.data.UserData;

/**
 * 
 * @author		Magnus Leinemann
 * @category	Interface
 * @version 	1.1
 * 
 * @description Interface definition for any persistence implementation
 * 
 * @changelog	1.0 initial version
 * 				1.0 added declaration for saveLocation				chris
 * 
 */
public interface IPersistenceManager {

	/**
	 * implementation for saving the User Data provided by social network in the DB
	 * 
	 * @param userData
	 *            Object User Data
	 * 
	 */
	public void saveUsers(UserData userData);

	/**
	 * implementation for saving the Post Data provided by social network in the DB
	 * 
	 * @param postData
	 *            Object Post Data 
	 * 
	 */
	public void savePosts(PostData postData);

	/**
	 * implementation for saving the Location Data provided by social network in the DB
	 * 
	 * @param locationData
	 *            Object Location Information for users and posts
	 * 
	 */
	public void saveLocation(LocationData locationData);

}
