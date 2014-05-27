package de.comlineag.sbm.handler;

//import org.apache.log4j.Logger;

/**
 * 
 * @author Christian Guenther
 * @category Parser
 * 
 * @description GenericParser ist die abstrakte Basisklasse fuer Parser der
 *              einzelnen sozialen Netzwerke.
 *              GenericParser definiert die process-Methode - uebergibt im wesentlichen ein neues 
 *              posting als String an die (in den abgeleiteten Klassen) zu Implementierende
 *              parse Methode 
 * 
 */
public abstract class GenericParser {
	
	//private final Logger logger = Logger.getLogger(getClass().getName());
	
	protected GenericParser() {}
	
	/**
	 * @name parse
	 * @param strPosting
	 * @description abstrakte deklaration der Parsing Methode
	 */
	protected abstract void parse(String strPosting);
	
	/**
	 * @name process
	 * @param strPosting
	 * @description Implementierung der process-Methode. Uebergibt ein Posting als String an
	 * 				die Methode parse
	 */
	public final void process(String strPosting) {
		if (strPosting != null)
			parse(strPosting);
	}

}
