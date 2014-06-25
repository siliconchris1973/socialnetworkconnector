package de.comlineag.sbm.handler;

import de.comlineag.sbm.data.SocialNetworks;
import de.comlineag.sbm.persistence.AppContext;
import de.comlineag.sbm.persistence.IPersistenceManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		1.0
 * 
 * @description GenericDataManager is the abstract base class for the 
 * 				data handler. It gets the active persistence manager 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual data handler - like post and user data. 
 * 				The implementation inherits its type safety via the <T> parameter, 
 * 				which is substituted by [SocialNetwork]PostData and [SocialNetwork]UserData  
 * 
 * @param <T>
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
