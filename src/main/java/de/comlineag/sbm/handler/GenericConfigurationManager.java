package de.comlineag.sbm.handler;

import de.comlineag.sbm.persistence.AppContext;
import de.comlineag.sbm.persistence.IConfigurationPersistence;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		1.0
 * 
 * @description GenericConfigurationManager is the abstract base class for the configuration handler
 * 				It gets the active configuration persistence from ApplicationContext.xml
 * 
 */
public abstract class GenericConfigurationManager {

	protected IConfigurationPersistence configurationManager;
	protected GenericConfigurationManager() {
		configurationManager = (IConfigurationPersistence) AppContext.Context.getBean("configurationManager");
	}
	
	@SuppressWarnings("unused")
	private static String getConfigDbHandler() {
		return (String) AppContext.Context.getBean("configurationManager");
	}
}