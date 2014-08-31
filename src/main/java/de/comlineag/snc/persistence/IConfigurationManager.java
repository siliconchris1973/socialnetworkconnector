package de.comlineag.snc.persistence;

import java.util.ArrayList;

import org.json.simple.JSONObject;


/**
 * 
 * @author 		Christian Guenther
 * @category	interface
 * @version		0.5a			- 17.07.2014
 * @status		productive
 * 
 * @description	The interface IConfigurationManager must be implemented by all configuration
 * 				manager classes. It defines methods to get the terms, users and 
 * 				languages to track in the social networks. 
 * 
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		first implementation as copy from IPersistenceManager
 * 				0.2				added support for SocialNetwork specific configuration
 * 				0.3 			removed the 5 getTrack[Terms, Sites, Languages, Users, Location]()
 * 								functions and established one that takes the category as a parameter
 * 				0.4 			added generic type T to getConstraint, so that we can return String or 
 * 								Long or whatever we need
 * 				0.5				Added possibility to pass a customer name for customer specific configurations
 * 				0.5a			changed method signature to use JSON object for network, customer and domain instead of String
 *
 */
public interface IConfigurationManager<T> {
	
	
	/**
	 * @description returns the active domain
	 */
	public abstract String getDomain();
	public abstract void setDomain(String domain);
	/**
	 * @description returns the active domain
	 */
	public abstract String getCustomer();
	public abstract void setCustomer(String customer);
	/**
	 * @description returns a json with domain and customer
	 */
	public abstract JSONObject getCrawlerConfigurationScope();
	
	/**
	 * 
	 * @description	takes a constraint category (either term, user, site, geoLocation or language),
	 * 				a social network identifier and a customer name (or null) and returns the constraints 
	 * 				of that category for the specified social network for the specified customer from 
	 * 				the configuration as an ArrayList of type T.
	 * 
	 * @param 		constraint
	 * @param 		SN
	 * @return		ArrayList<T>
	 */
	public abstract ArrayList<T> getConstraint(String constraint, JSONObject configurationScope);
	
	/**
	 * @description	returns the value of a single configuration element
	 * 
	 * @param 		key
	 * @param		path
	 * @return 		String
	 */
	public abstract String getConfigurationElement(String key, String path);
		
	/**
	 * @description	sets the value of a single configuration element
	 * 
	 * @param 		key
	 * @param 		value
	 * @param 		path
	 */
	public abstract void setConfigurationElement(String key, String value, String path);
	
	/**
	 * @description	writes a complete new configuration xml file to the path and file name
	 * 				configured in applicationContext.xml
	 * 
	 * @param 		xml
	 */
	public abstract void writeNewConfiguration(String xml);
}
