package de.comlineag.snc.handler;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.persistence.AppContext;
import de.comlineag.snc.persistence.IConfigurationManager;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.3		- 08.07.2014
 * @status		productive
 * 
 * @description GenericConfigurationManager is the abstract base class for the 
 * 				configuration handler. It gets the active configuration persistence 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual configuration manager - like CrawlerConfiguration.
 * 
 * @changelog	0.1 (Chris)		first version as copy from GenericDataManager
 * 				0.1a 			added @SuppressWarnings to get rid of exclamation mark
 *				0.2				added support for SocialNetwork specific configuration
 *				0.3 			added support for generic type casting
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
