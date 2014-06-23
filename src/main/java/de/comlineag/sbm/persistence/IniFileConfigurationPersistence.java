package de.comlineag.sbm.persistence;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidIniFormatException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author		Christian Guenther
 * @version		0.9
 * 
 * @description	A configuration manager for the crawler using flat ini files for the configuration
 *
 */
public class IniFileConfigurationPersistence implements IConfigurationPersistence  {
	
	// the path to the configuration file
	private String configDbHandler;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public ArrayList<String> getTrackTerms() {
		return getDataFromIni("trackTerms");
	}

	@Override
	public ArrayList<String> getTrackLanguages() {
		return getDataFromIni("trackLanguages");
	}

	@Override
	public ArrayList<String> getTrackUsers() {
		return getDataFromIni("trackUsers");
	}
	
	@Override
	public ArrayList<String> getTrackSites() {
		return getDataFromIni("trackSites");
	}
	
	@Override
	public ArrayList<String> getTrackLocations() {
		return getDataFromIni("trackLocations");
	}
	
	private ArrayList<String> getDataFromIni(String section) {
		ArrayList<String> ar = new ArrayList<String>();
		Ini ini = null;
		
		try {
			ini = new Ini(new FileReader((String)getConfigDbHandler()));
		} catch (InvalidIniFormatException e1) {
			logger.error("EXCEPTION :: invalid ini format " + e1.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (IOException e2) {
			logger.error("EXCEPTION :: error reading configuration file " + e2.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}

        for (String key : ini.get(section).keySet()) {
        	ar.add(ini.get(section).fetch(key));
        	logger.trace(ini.get(section).getName() + " = " + ini.get(section).fetch(key));
        }
		
		return ar;
	}
	
	@Override
	public String getConfigurationElement(String key, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String vakue, String path) {
		// TODO Auto-generated method stub
	}
	
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}
}
