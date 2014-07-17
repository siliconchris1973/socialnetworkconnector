package de.comlineag.snc.handler;

import java.util.ArrayList;
import org.json.simple.JSONObject;
import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.5b 	- 17.07.2014
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
 * @changelog	0.1 (Chris)		first initial version with terms, languages, sites, locations and users
 * 				0.2 			added getConfigurationElement and setConfigurationElement
 *				0.3				added support for SocialNetwork specific configuration
 *				0.4 			changed method calls according to IConfigurationManager version 0.3
 *				0.5 			added generic type arguments
 *				0.5a			added parameter for customer
 *				0.5b			changed signatur to use JSON Object instead of String
 * 
 * TODO 1. check if there is a better way for arbitrary data types AND type safety
 */
public class CrawlerConfiguration<T> extends GenericConfigurationManager {
	
	@SuppressWarnings("unchecked")
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		return (ArrayList<T>) configurationManager.getConstraint(category, configurationScope);
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
