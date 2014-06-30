package de.comlineag.snc.handler;

import de.comlineag.snc.data.SocialNetworks;
import de.comlineag.snc.persistence.AppContext;
import de.comlineag.snc.persistence.IConfigurationManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.4
 * 
 * @description GenericConfigurationManager is the abstract base class for the 
 * 				configuration handler. It gets the active configuration persistence 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual configuration manager - like CrawlerConfiguration.
 * 
 * @changelog	0.1 first version as copy from GenericDataManager
 * 				0.2 added @SuppressWarnings
 *				0.3	added support for SocialNetwork specific configuration
 *				0.4 added support for generic type casting
 *
 */
public abstract class GenericConfigurationManager {
	
	protected IConfigurationManager<?> configurationManager;
	protected SocialNetworks sourceSocialNetwork;

	protected GenericConfigurationManager() {
		configurationManager = (IConfigurationManager<?>) AppContext.Context.getBean("configurationManager");
	}
	
	@SuppressWarnings("unused")
	private static String getConfigDbHandler() {
		return (String) AppContext.Context.getBean("configurationManager");
	}
}
