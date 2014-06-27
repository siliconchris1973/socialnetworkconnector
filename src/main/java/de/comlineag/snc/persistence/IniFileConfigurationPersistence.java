package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidIniFormatException;

import de.comlineag.snc.data.SocialNetworks;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		1.1
 * 
 * @description	A configuration manager for the crawler using flat ini files for the configuration
 * 
 * @changelog	0.9	initial version retrieves terms, locations, users, sites and languages
 * 					from the ini file and returns these as an array list of strings
 *				1.0	implement methods getConfigurationElement and setConfigurationElemenet
 *					to retrieve a single value and either add a new value 
 *					or update an existing one in the file
 *				1.1	added support for SocialNetwork specific configuration
 *  
 */
public class IniFileConfigurationPersistence implements IConfigurationManager  {
	
	// the path to the configuration file
	private String configDbHandler;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public ArrayList<String> getTrackTerms(SocialNetworks SN) {
		return getDataFromIni("trackTerms", SN);
	}

	@Override
	public ArrayList<String> getTrackLanguages(SocialNetworks SN) {
		return getDataFromIni("trackLanguages", SN);
	}

	@Override
	public ArrayList<String> getTrackSites(SocialNetworks SN) {
		logger.warn("no type safety guranteed for configuration element sites - consider using xml configuration");
		return getDataFromIni("trackSites", SN);
	}
	
	@Override
	public ArrayList<String> getTrackLocations(SocialNetworks SN) {
		logger.warn("no type safety guranteed for configuration element location - consider using xml configuration");
		return getDataFromIni("trackLocas", SN);
	}
	
	@Override
	public ArrayList<String> getTrackUsers(SocialNetworks SN) {
		logger.warn("no type safety guranteed for configuration element users - consider using xml configuration");
		return getDataFromIni("trackUsers", SN);
	}
	
	private ArrayList<String> getDataFromIni(String section, SocialNetworks SN) {
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
		Ini ini = null;
		
		try {
			ini = new Ini(new FileReader((String)getConfigDbHandler()));
		} catch (InvalidIniFormatException e1) {
			logger.error("EXCEPTION :: invalid ini format " + e1.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (IOException e2) {
			logger.error("EXCEPTION :: error reading configuration file " + e2.getLocalizedMessage() + ". You sure we are up and running?");
			System.exit(-1);
		}
		
		return ini.get(path).fetch(key);
	}

	@Override
	public void setConfigurationElement(String key, String value, String path) {
		Ini ini = null;
		
		try {
			ini = new Ini(new FileReader((String)getConfigDbHandler()));
			Ini.Section section = ini.get(path);
			section.put(key, value);
			
			//TODO implement write to ini support
			//Writer wri = new FileWriter((String)getConfigDbHandler());
			//wri.close();
			/*
			FileOutputStream fos = new FileOutputStream((String)getConfigDbHandler());
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
			ini.store(oos);
			*/
			logger.warn("adding values is not yet implemented - nothing was done");
		} catch (InvalidIniFormatException e1) {
			logger.error("EXCEPTION :: invalid ini format " + e1.getLocalizedMessage()+ ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (IOException e2) {
			logger.error("EXCEPTION :: error reading configuration file " + e2.getLocalizedMessage() + ". Your changes/additions are probably lost, sorry!");
		}
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}
}
