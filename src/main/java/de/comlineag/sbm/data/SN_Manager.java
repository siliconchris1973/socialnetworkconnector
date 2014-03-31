package de.comlineag.sbm.data;

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

	protected SN_Manager() {
		// TODO Auto-generated constructor stub
	}
	
	protected abstract void bigParser(String strPosting);
	
	public final void save(String strPosting){
		if(strPosting != null)
			bigParser(strPosting);
	}

}
