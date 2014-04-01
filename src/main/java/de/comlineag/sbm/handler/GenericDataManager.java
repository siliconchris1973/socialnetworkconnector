package de.comlineag.sbm.handler;

import java.util.List;

import de.comlineag.sbm.data.SocialNetworks;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 *
 * @description	GenericDataManager ist die abstrakte Basisklasse fuer die Handler 
 * 				der Elemente (Posts und User) der einzelnen sozialen Netzwerke
 * 
 * @param		<T>
 * 
 */
public abstract class GenericDataManager <T>   {
	
	SocialNetworks sourceSocialNetwork;
	
	public GenericDataManager(){}
	
	public abstract void save();
	
	
}
