package de.comlineag.snc.persistence;

import java.util.ArrayList;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	interface
 * @version		0.4
 * @param <T>
 * 
 * @description	The interface IConfigurationManager must be implemented by all configuration
 * 				manager classes. It defines methods to get the terms, users and 
 * 				languages to track in the social networks. 
 * 
 * @changelog	0.1 first implementation as copy from IPersistenceManager					chris
 * 				0.2	added support for SocialNetwork specific configuration
 * 				0.3 removed the 5 getTrack[Terms, Sites, Languages, Users, Location]()
 * 					functions and established one that takes the category as a parameter
 * 				0.4 added generic type T to getConstraint, so that we can return String or 
 * 					Long or whatever we need
 *
 */
public interface IConfigurationManager<T> {
	
	
	/**
	 * 
	 * @description	takes a constraint category (either term, user, site, location or language) and
	 * 				a social network identifier and returns the constraints of that category
	 * 				for the specified social network from the configuration as an ArrayList of type T.
	 * 
	 * @param 		constraint
	 * @param 		SN
	 * @return		ArrayList<T>
	 */
	public abstract ArrayList<T> getConstraint(String constraint, SocialNetworks SN);
	
	/**
	 * 
	 * @param key
	 * @param path
	 * @return
	 */
	public abstract String getConfigurationElement(String key, String path);
		
	/**
	 * 
	 * @param key
	 * @param value
	 * @param path
	 */
	public abstract void setConfigurationElement(String key, String value, String path);
	/**
	 * 
	 * @param xml
	 */
	public abstract void writeNewConfiguration(String xml);
}
