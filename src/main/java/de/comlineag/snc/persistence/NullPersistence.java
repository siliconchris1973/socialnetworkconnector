package de.comlineag.snc.persistence;


import de.comlineag.snc.data.PostingData;
import de.comlineag.snc.data.UserData;

/**
 *
 * @author 		Christian Guenther
 * @category 	Persistence Manager
 * @version 	0.1				- 21.11.2014
 * @status		productive
 *
 * @description persistence manager to discard tracked posts and users 
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public class NullPersistence implements IPersistenceManager {
	public NullPersistence() {}
	
	/**
	 * @description	discards a user from social network
	 * @param		UserData
	 */
	@Override
	public void saveUsers(UserData userData) {}

	/**
	 * @description	discards a post from social network
	 * @param		PostingData
	 */
	@Override
	public void savePosts(PostingData postingData) {}
}
