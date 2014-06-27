package de.comlineag.snc.handler;

import de.comlineag.snc.data.SocialNetworks;
import de.comlineag.snc.persistence.AppContext;
import de.comlineag.snc.persistence.IConfigurationManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		1.1
 * 
 * @description GenericConfigurationManager is the abstract base class for the 
 * 				configuration handler. It gets the active configuration persistence 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual configuration manager - like CrawlerConfiguration.
 * 
 * @changelog	0.9 first version as copy from GenericDataManager
 * 				1.0 added @SuppressWarnings
 *				1.1	added support for SocialNetwork specific configuration
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
