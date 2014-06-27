package de.comlineag.snc.handler;

import java.util.ArrayList;

import de.comlineag.snc.data.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		1.2
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
 * 				1.0 first productive version
 * 				1.1 added getConfigurationElement and setConfigurationElement
 *				1.2	added support for SocialNetwork specific configuration
 * 
 */
public class CrawlerConfiguration extends GenericConfigurationManager {
	public ArrayList<String> getTrackTerms(SocialNetworks SN) {
		return configurationManager.getTrackTerms(SN);
	}
	public ArrayList<String> getTrackLanguages(SocialNetworks SN) {
		return configurationManager.getTrackLanguages(SN);
	}
	public ArrayList<String> getTrackSites(SocialNetworks SN) {
		return configurationManager.getTrackSites(SN);
	}
	public ArrayList<String> getTrackLocations(SocialNetworks SN) {
		return configurationManager.getTrackLocations(SN);
	}
	public ArrayList<String> getTrackUsers(SocialNetworks SN) {
		return configurationManager.getTrackUsers(SN);
	}
	public String getConfigurationElement(String key, String path){
		return configurationManager.getConfigurationElement(key, path);
	}
	public void setConfigurationElement(String key, String value, String path){
		configurationManager.setConfigurationElement(key, value, path);
	}
}
