package de.comlineag.snc.handler;

import java.util.ArrayList;

import de.comlineag.snc.data.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.5
 * 
 * @description	invokes the configuration manager as defined in applicationContext.xml
 * 				returns ArrayLists for 
 * 				- terms
 * 				- languages
 * 				- sites
 * 				- locations
 * 				- user
 * 
 * 				ATTENTION: The current implementation uses string for everything and is NOT type safe
 * 
 * @changelog	0.1 first initial version with terms, languages, sites, locations and users
 * 				0.2 added getConfigurationElement and setConfigurationElement
 *				0.3	added support for SocialNetwork specific configuration
 *				0.4 changed method calls according to IConfigurationManager version 0.3
 *				0.5 added explicit constructor
 * 
 */
public class CrawlerConfiguration extends GenericConfigurationManager {
	public CrawlerConfiguration(){}
	
	public ArrayList<String> getConstraint(String category, SocialNetworks SN) {
		return configurationManager.getConstraint(category , SN);
	}
	
	public String getConfigurationElement(String key, String path){
		return configurationManager.getConfigurationElement(key, path);
	}
	public void setConfigurationElement(String key, String value, String path){
		configurationManager.setConfigurationElement(key, value, path);
	}
	public void writeNewConfiguration(String xml){
		configurationManager.writeNewConfiguration(xml);
	}
}
