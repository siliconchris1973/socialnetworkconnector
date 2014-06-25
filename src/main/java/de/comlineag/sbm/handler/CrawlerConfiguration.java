package de.comlineag.sbm.handler;

import java.util.ArrayList;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		1.0
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
}
