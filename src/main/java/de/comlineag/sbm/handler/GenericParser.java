package de.comlineag.sbm.handler;

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
	 * @description abstract declaration of the parse method
	 * 				implementation is specific to the network
	 */
	protected abstract void parse(String strPosting);
	
	/**
	 * @name process
	 * @param strPosting
	 * @description passes the given string (containing a message, post, tweet etc.) to the parse method
	 * 
	 */
	public final void process(String strPosting) {
		assert (strPosting != null) : "ERROR :: cannot parse empty string";
		
		parse(strPosting);
	}

}
