package de.comlineag.snc.handler;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.5
 * @status		productive
 * 
 * @description	invokes the configuration manager as defined in applicationContext.xml
 * 				returns ArrayLists for 
 * 				- terms
 * 				- languages
 * 				- sites
 * 				- locations
 * 				- user
 * 
 * 				ATTENTION: The current implementation uses a generic <T> and is NOT type safe
 * 
 * @param <T>
 * 
 * @changelog	0.1 first initial version with terms, languages, sites, locations and users
 * 				0.2 added getConfigurationElement and setConfigurationElement
 *				0.3	added support for SocialNetwork specific configuration
 *				0.4 changed method calls according to IConfigurationManager version 0.3
 *				0.5 added generic type arguments
 * 
 * TODO 1. check if there is a better way for arbitrary data types AND type safety
 */
public class CrawlerConfiguration<T> extends GenericConfigurationManager {
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
		
	@SuppressWarnings("unchecked")
	public ArrayList<T> getConstraint(String category, SocialNetworks SN) {
		logger.trace("retrieving "+category+" configuration for " +SN+ " via " + configurationManager.getClass().getCanonicalName().toString());
		return (ArrayList<T>) configurationManager.getConstraint(category , SN);
	}
	
	public String getConfigurationElement(String key, String path){
		logger.trace("retrieving "+key+"  from " +path+ " via " + configurationManager.getClass().getCanonicalName().toString());
		return configurationManager.getConfigurationElement(key, path);
	}
	public void setConfigurationElement(String key, String value, String path){
		logger.trace("setting "+key+" = "+value+" at " +path+ " via " + configurationManager.getClass().getCanonicalName().toString());
		configurationManager.setConfigurationElement(key, value, path);
	}
	public void writeNewConfiguration(String xml){
		configurationManager.writeNewConfiguration(xml);
	}
}
