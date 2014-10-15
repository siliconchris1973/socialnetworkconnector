package de.comlineag.snc.persistence;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.ini4j.Ini;
import org.ini4j.InvalidIniFormatException;
import org.json.simple.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.8a			- 14.10.2014
 * @status		productive - but some functions are still missing
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
 *				0.7a			Added method parameter customer. Has no effect here but needed
 * 								for XMLFileCustomerSpecificConfiguration
 * 				0.7b			Adapted signature to match JSON Object instead of String 
 * 				0.8				Added support for getRunState
 * 				0.8a			changed access to runtime configuration to non-static
 *  
 *  TODO 1. implement code to insert/update a value and write a new config file
 */
public class IniFileConfigurationPersistence<T> implements IConfigurationManager<T>  {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// the path to the configuration file
	private String configDbHandler;
	
	@Override
	public Boolean getRunState(String socialNetwork) {
		if (rtc.getWarnOnSimpleConfig())
			logger.warn("no possibility to activate/deactivate certain crawler - consider using simple or complex xml or db configuration manager. \nyou can turn off this warning by setting WARN_ON_SIMPLE_CONFIG to false in " + rtc.getRuntimeConfigFilePath());
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		assert (!"term".equals(category) && !"site".equals(category) && !"language".equals(category)) : "ERROR :: can only use term, site and language";
		
		if (!"term".equals(category) && !"site".equals(category) && !"language".equals(category)) {
			logger.warn("received "+category+" as category, but can only process term, site and language");
			ArrayList<String> ar = new ArrayList<String>();
			return (ArrayList<T>)ar;
		} else {
			logger.debug("reading constraints on " + category + " from configuration file " + getConfigDbHandler().substring(getConfigDbHandler().lastIndexOf("/")+1));
			//if ((rtc.getCustomerIsActive() || rtc.getDomainIsActive()) && rtc.getWarnOnSimpleConfig())
			if (rtc.getWarnOnSimpleConfig())
				logger.warn("no customer and network specific configuration and no type safety guranteed - consider using simple or complex xml or db configuration manager. \nyou can turn off this warning by setting WARN_ON_SIMPLE_CONFIG to false in " + rtc.getRuntimeConfigFilePath().substring(rtc.getRuntimeConfigFilePath().lastIndexOf("/")+1));
			
			return (ArrayList<T>)getDataFromIni(category);
		}
	}
	
	private ArrayList<String> getDataFromIni(String section) {
		ArrayList<String> ar = new ArrayList<String>();
		Ini ini = null;
		
		if (!"term".equals(section) && !"site".equals(section) && !"language".equals(section))
			return ar;
		
		try {
			ini = new Ini(new FileReader((String)getConfigDbHandler()));
		} catch (InvalidIniFormatException e1) {
			logger.error("EXCEPTION :: invalid ini format " + e1.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		} catch (IOException e2) {
			logger.error("EXCEPTION :: error reading configuration file " + e2.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
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
		logger.warn("The method getConfigurationElement is currently not supported on configuration type ini-file");
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String value, String path) {
		logger.warn("The method setConfigurationElement is currently not supported on configuration type ini-file");
	}
	
	@Override
	public void writeNewConfiguration(String xml) {
		logger.warn("The method writeNewConfiguration from XML is currently not supported on configuration type ini-file");
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {return rtc.returnQualifiedConfigPath(this.configDbHandler);}
	public void setConfigDbHandler(String configDb) {this.configDbHandler = configDb;}
	
	// getter and setter for the domain data
	@Override
	public String getDomain() {
		return "undefined";
	}
	@Override
	public void setDomain(String domain) {}

	@Override
	public String getCustomer() {
		return "undefined";
	}
	@Override
	public void setCustomer(String customer) {}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getCrawlerConfigurationScope() {
		JSONObject crawlerConfigurationScope = new JSONObject();
		crawlerConfigurationScope.put((String) rtc.getDomainidentifier(), (String) "undefined");
		crawlerConfigurationScope.put((String) rtc.getCustomeridentifier(), (String) "undefined");
		return crawlerConfigurationScope;
	}
}
