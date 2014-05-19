package de.comlineag.sbm.handler;

import de.comlineag.sbm.data.SocialNetworks;
import de.comlineag.sbm.persistence.AppContext;
import de.comlineag.sbm.persistence.IPersistenceManager;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description GenericDataManager ist die abstrakte Basisklasse fuer die Handler
 *              der Elemente (Posts und User) der einzelnen sozialen Netzwerke
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
