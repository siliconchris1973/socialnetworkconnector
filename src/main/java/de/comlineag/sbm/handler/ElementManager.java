package de.comlineag.sbm.handler;

import java.util.List;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 *
 * @description	ElementManager ist die abstrakte Basisklasse fuer die Handler 
 * 				der Elemente (Posts und User) der einzelnen sozialen Netzwerke
 * 
 * @param <T>
 * 
 */
public abstract class ElementManager <T>   {
	
	public ElementManager(){}
	
	public abstract void save();
	
}
