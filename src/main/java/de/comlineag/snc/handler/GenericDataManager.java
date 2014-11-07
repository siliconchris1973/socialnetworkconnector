package de.comlineag.snc.handler;

import de.comlineag.snc.appstate.AppContext;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.persistence.IPersistenceManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.1
 * @status		productive
 * 
 * @description GenericDataManager is the abstract base class for the data handler. 
 * 				It gets the active persistence manager (as defined in applicationContext.xml)
 * 				and passes it along to the actual data handler - like post and user data.
 * 				The implementation inherits its type safety via the <T> parameter, 
 * 				which is substituted by [SocialNetwork]PostingData and [SocialNetwork]UserData  
 * 
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public abstract class GenericDataManager<T> {

	protected IPersistenceManager persistenceManager;
	protected SocialNetworks sourceSocialNetwork;

	protected GenericDataManager() {
		persistenceManager = (IPersistenceManager) AppContext.Context.getBean("persistenceManager");
	}

	public abstract void save();

}
