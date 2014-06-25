package de.comlineag.sbm.persistence;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidIniFormatException;

import de.comlineag.sbm.data.SocialNetworks;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.9
 * 
 * @description	A configuration manager for the crawler using flat ini files for the configuration
 *
 */
public class IniFileConfigurationPersistence implements IConfigurationManager  {
	
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
	public ArrayList<String> getTrackSites() {
		logger.warn("no type safety guranteed for configuration element sites - consider using xml configuration");
		return getDataFromIni("trackSites");
	}
	
	@Override
	public ArrayList<String> getTrackLocations() {
		logger.warn("no type safety guranteed for configuration element location - consider using xml configuration");
		return getDataFromIni("trackLocas");
	}
	
	@Override
	public ArrayList<String> getTrackUsers() {
		logger.warn("no type safety guranteed for configuration element users - consider using xml configuration");
		return getDataFromIni("trackUsers");
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
		// TODO implement return a single element
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String vakue, String path) {
		// TODO implement insert a single element
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}
}
