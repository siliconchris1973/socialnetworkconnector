package de.comlineag.snc.persistence;

import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;

/**
 * 
 * @author		Magnus Leinemann
 * @category	Interface
 * @version 	0.3
 * @status		productive
 * 
 * @description Interface definition for any persistence implementation
 * 
 * @changelog	0.1 (Magnus)		initial version
 * 				0.2 (Chris)			added declaration for saveLocation
 * 				0.3					removed declaration for saveLocation
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
}
