package de.comlineag.sbm.persistence;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

/**
 * 
 * @author Magnus Leinemann
 * @version 1.0
 * 
 * @description Interface Definition for any Persistence Implementation
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
	 * @throws NoBase64EncryptedValue 
	 * 
	 */
	public void savePosts(PostData postData);

}
