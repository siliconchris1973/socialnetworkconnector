package de.comlineag.sbm.handler;

import org.apache.log4j.*;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * 
 * @description	GenericParser ist die abstrakte Basisklasse fuer Parser der einzelnen sozialen Netzwerke
 * 				GenericParser implementiert die save-Methode und stellt die abstrakte Methode bigParser bereit
 *
 */
public abstract class GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	protected GenericParser() {
		// TODO Auto-generated constructor stub
		logger.debug("constructor of class" + getClass().getName() + " called");
	}
	
	protected abstract void parse(String strPosting);
	
	public final void process(String strPosting){
		// log the startup message
		logger.info("method process from class " + getClass().getName() + " called");
				
		if(strPosting != null)
			parse(strPosting);
	}

}
