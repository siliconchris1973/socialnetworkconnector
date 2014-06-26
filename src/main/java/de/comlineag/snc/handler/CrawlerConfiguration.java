package de.comlineag.snc.handler;

import java.util.ArrayList;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		1.1
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
 * @changelog	0.9 first initial version with terms, languages, sites, locations and users
 * 				1.0 bug free
 * 				1.1 added getConfigurationElement and setConfigurationElement
 * 
 */
public class CrawlerConfiguration extends GenericConfigurationManager {
	public ArrayList<String> getTrackTerms() {
		return configurationManager.getTrackTerms();
	}
	public ArrayList<String> getTrackLanguages() {
		return configurationManager.getTrackLanguages();
	}
	public ArrayList<String> getTrackSites() {
		return configurationManager.getTrackSites();
	}
	public ArrayList<String> getTrackLocations() {
		return configurationManager.getTrackLocations();
	}
	public ArrayList<String> getTrackUsers() {
		return configurationManager.getTrackUsers();
	}
	public String getConfigurationElement(String key, String path){
		return configurationManager.getConfigurationElement(key, path);
	}
	public void setConfigurationElement(String key, String value, String path){
		configurationManager.setConfigurationElement(key, value, path);
	}
}
