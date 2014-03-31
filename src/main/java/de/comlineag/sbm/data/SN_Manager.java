package de.comlineag.sbm.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * 
 * @description	SN_Manager ist die abstrakte Basisklasse fuer Parser der einzelnen sozialen Netzwerke
 * 				SN_Manager implementiert die save-Methode und stellt die abstrakte Methode bigParser bereit
 *
 */
public abstract class SN_Manager {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	protected SN_Manager() {
		// TODO Auto-generated constructor stub
	}
	
	protected abstract void bigParser(String strPosting);
	
	public final void save(String strPosting){
		// log the startup message
		logger.debug("method " + getClass().getEnclosingMethod().getName() + " save from class " + getClass().getName() + " called");
				
		if(strPosting != null)
			bigParser(strPosting);
	}

}
