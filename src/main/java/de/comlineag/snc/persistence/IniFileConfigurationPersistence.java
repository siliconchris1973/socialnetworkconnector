package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidIniFormatException;

import de.comlineag.snc.constants.SocialNetworks;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.7
 * @status		productive
 * 
 * 
 * @description	A very simple configuration manager for the crawler using flat ini files for the configuration
 * 
 * @param <T>
 * 
 * @changelog	0.1	(Chris)		initial version 
 * 				0.2 			retrieves terms, locations, users, sites and languages
 * 								from the ini file and returns these as an array list of strings
 *				0.3				implemented method getConfigurationElement to retrieve a single value
 *				0.4				added support for SocialNetwork specific configuration
 *				0.5 			changed naming convention for constraints according to XMLFileConfiguration
 *				0.6 			changed methods according to IConfigurationManager version 0.3
 *				0.7 			added warning to unimplemented methods
 *  
 *  TODO 1. implement code to insert/update a value
 */
public class IniFileConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<T> getConstraint(String category, SocialNetworks SN) {
		assert (category != "term" && category != "site" && category != "user" && category != "language" && category != "location")  : "ERROR :: can only accept term, site, user, language or location as category";
		
		logger.warn("no type safety guranteed for configuration elements - consider using xml or db configuration");
		return (ArrayList<T>)getDataFromIni(category, SN);
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
		
		// now add config elements one by one to array
        for (String key : ini.get(section).keySet()) {
        	ar.add(ini.get(section).fetch(key));
        	logger.trace(ini.get(section).getName() + " = " + ini.get(section).fetch(key));
        }
		
		return ar;
	}
	
	@Override
	public String getConfigurationElement(String key, String path) {
		//TODO implement code
		logger.warn("The method getConfigurationElement is currently not supported on configuration type ini-file");
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String value, String path) {
		//TODO implement code
		logger.warn("The method setConfigurationElement is currently not supported on configuration type ini-file");
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}

	@Override
	public void writeNewConfiguration(String xml) {
		//TODO implement code
		logger.warn("The method writeNewConfiguration from XML is currently not supported on configuration type ini-file");
	}
}
