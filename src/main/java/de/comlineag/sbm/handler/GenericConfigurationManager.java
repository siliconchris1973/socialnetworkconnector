package de.comlineag.sbm.handler;

import de.comlineag.sbm.data.SocialNetworks;
import de.comlineag.sbm.persistence.AppContext;
import de.comlineag.sbm.persistence.IConfigurationManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		1.0
 * 
 * @description GenericConfigurationManager is the abstract base class for the 
 * 				configuration handler. It gets the active configuration persistence 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual configuration manager - like CrawlerConfiguration.
 * 
 */
public abstract class GenericConfigurationManager {

	protected IConfigurationManager configurationManager;
	protected SocialNetworks sourceSocialNetwork;

	protected GenericConfigurationManager() {
		configurationManager = (IConfigurationManager) AppContext.Context.getBean("configurationManager");
	}
	
	@SuppressWarnings("unused")
	private static String getConfigDbHandler() {
		return (String) AppContext.Context.getBean("configurationManager");
	}
}
