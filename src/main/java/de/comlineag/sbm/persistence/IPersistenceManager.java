package de.comlineag.sbm.persistence;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

public interface IPersistenceManager {

	public void saveUsers(UserData userData);

	public void savePosts(PostData postData);

}
