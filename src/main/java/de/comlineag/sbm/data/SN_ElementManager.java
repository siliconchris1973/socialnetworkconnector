package de.comlineag.sbm.data;

import java.util.List;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 *
 * @description	SN_ElementManager ist die abstrakte Basisklasse fuer die Handler 
 * 				der Elemente (Posts und User) der einzelnen sozialen Netzwerke
 * 
 * @param <T>
 * 
 */
public abstract class SN_ElementManager <T>   {
	
	public SN_ElementManager(){}
	
	public abstract void save(List<T> objTweet);
}
