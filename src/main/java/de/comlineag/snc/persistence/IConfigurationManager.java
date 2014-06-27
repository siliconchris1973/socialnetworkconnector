package de.comlineag.snc.persistence;

import java.util.ArrayList;

import de.comlineag.snc.data.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category	interface
 * @version		1.0
 * 
 * @description	The interface IConfigurationManager must be implemented by all configuration
 * 				manager classes. It defines methods to get the terms, users and 
 * 				languages to track in the social networks. 
 * 
 * @changelog	0.9 first implementation as copy from IPersistenceManager	chris
 * 				1.0	added support for SocialNetwork specific configuration
 *
 */
public interface IConfigurationManager {
	
	/**
	 * 
	 * @return		an array of terms to track in the social networks
	 * 
	 * @description	returns an array of terms (simple words) which the crawler should use to narrow the 
	 * 				resultset, when crawling the social network 
	 */
	public abstract ArrayList<String> getTrackTerms(SocialNetworks SN);
	
	/**
	 * 
	 * @return	an array of terms to track in the social networks
	 * @description	returns an array of languages which the crawler should use to narrow the 
	 * 				resultset, when crawling the social network 
	 */
	public abstract ArrayList<String> getTrackLanguages(SocialNetworks SN);
	
	/**
	 * 
	 * @return		an array of sites to track in the social networks
	 * 
	 * @description	returns an array of sites (could also be used for specific blogs etc.) 
	 * 				which the crawler should use to narrow the resultset, when crawling 
	 * 				the social network 
	 * 
	 * 				Does NOT work on all social network - namely Twitter does not support this
	 */
	public abstract ArrayList<String> getTrackSites(SocialNetworks SN);
	
	/**
	 * 
	 * @return		an array of locations to track in the social networks
	 * 
	 * @description	returns an array of geo locations which the crawler should use to narrow 
	 * 				the resultset, when crawling the social network
	 * 
	 * 				Does NOT work on all social network - namely Lithium does not support this 
	 */
	public abstract ArrayList<String> getTrackLocations(SocialNetworks SN);
	
	/**
	 * 
	 * @return		an array of user-ids to track in the social networks
	 * 
	 * @description	returns an array of users which the crawler should use to narrow 
	 * 				the resultset, when crawling the social network
	 */
	public abstract ArrayList<String> getTrackUsers(SocialNetworks SN);
	
	
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
}
