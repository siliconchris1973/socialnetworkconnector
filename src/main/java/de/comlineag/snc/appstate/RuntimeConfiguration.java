package de.comlineag.snc.appstate;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.comlineag.snc.data.RuntimeOption;
import de.comlineag.snc.handler.RuntimeOptionsParser;


/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @revision	1.0				- 29.10.2014
 * @status		productive
 * 
 * @description	this class is used to setup the overall configuration of the SNC.
 * 				All runtime configuration options, like whether or not to warn on 
 * 				weak encryption or simple configuration options, are configured via 
 * 				this class.
 * 				RuntimeConfiguration uses a singleton design, so that it is only instantiated
 * 				once.
 * 
 * @limitation	xml structure is not fetched from RuntimeConfiguration.xml but hard coded
 * 
 * @changelog	0.1 (Chris) 	class created
 * 				0.1a			renamed to GeneralConfiguration
 * 				0.2				added own configuration file RuntimeConfiguration.xml
 * 				0.3				added xml structure of crawler configuration file
 * 								and create json on user or post creation error
 * 				0.3a			renamed to RuntimeConfiguration 
 * 				0.4				added simple web crawler and data definition configuration options
 * 								moved the Social Network Definitions in their own file 
 * 				0.5				added support for CrawlerRun and tidied up
 * 				0.5a			added thread-pool size for multi-threaded web crawler
 * 				0.5b			added stayBelowGivenPath for the web crawler
 * 				0.5c			added wcWordDistanceCutoffMargin for the web crawler
 * 				0.6				added threading options
 * 				0.6a			added Parser.xml options ParserListFilePath
 * 				0.7				changed RuntimeConfiguration to be called by AppContext instead of as 
 * 								a job from quartz job control. Also made it a singleton 
 * 				0.7a			changed calling of RuntimeConfiguration to be issued by the crawler
 * 								and not by AppContxt (did not work).
 * 				0.8				made all static vars non-static and changed access to non-static
 * 				0.9				changed singleton pattern to use initialization on demand holder design
 * 				0.9a			implemented ResourcePathHolder class to get the real path to the WEB-INF directory
 * 				0.9b			added support for shutdown on any error in XML-file
 * 				0.9c			added twMaxTweetsPerCrawlerRun to define how many tweets the crawler shall track per job run
 * 				1.0				changed programming to make use of RuntimeOptionsParser together with a Map of 
 * 								options (data type defined in RuntimeOption). The Map is populated from the
 * 								SNC_Runtime_Configuration.xml file and new methods are defined to get a value by
 * 								name (for String, Int or Boolean value). In turn, all the old methods and variables 
 * 								are removed!
 *
 */
public final class RuntimeConfiguration { 
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// singleton design pattern using Initialization-on-demand holder idiom, 
	private static class Holder { static final RuntimeConfiguration instance = new RuntimeConfiguration(); }
    public static RuntimeConfiguration getInstance() { return Holder.instance; }
    
    
    // in case you want to change the name of the runtime configuration file - you need to change this variable and recompile
	private static final String RTCF = "SNC_Runtime_Configuration-1.0.xml";
	
	
	// configuration file path variables - these variables are used internally by the RuntimeConfiguration class in case
	// any of these sections are moved out of the runtime configuration xml in other files.
	private String 		runtimeConfigFilePath;	// this is the standard runtime configuration file (usually SNC_Runtime_Configuration.xml) 
	private String 		hanaConfigFilePath;		// this is the hana data definition file (usually SNC_HANA_Configuration.xml)
	private String 		neo4JConfigFilePath;	// this is the Neo4J data definition file (usually SNC_Neo4J_Configuration.xml)
	private String 		socialNetworkFilePath;	// this is file containing all social network and web page definitions (usually SocialNetworkDefinition.xml)
	private String 		webParserListFilePath; 	// this file contains all available web parser (usually properties/webparser.xml)
	private String		crawlerConfigFilePath;	// this file contains definitions for the crawler (usually in SNC_Runtime_Configuration.xml)
	private String		dataConfigFilePath;		// this file contains global data definitions (usually in SNC_Runtime_Configuration.xml)
	private String		threadingConfigFilePath;// this file contains the threading configuration (usually in SNC_Runtime_Configuration.xml)
	private String		xmlLayoutFilePath;		// this file contains the XML Layout information for the configuration files (usually in SNC_Runtime_Configuration.xml)
	
	/**
	 * @description returns for a given configuration area the corresponding file path
	 * @param 	configArea
	 * @return 	the corresponding file path
	 */
	private String getConfigFilePath(String configArea){
		String configFilePath = null;
		
		if (configArea.equals("XmlLayout"))
			configFilePath=getXmlLayoutFilePath();
		else if (configArea.equals("crawler"))
			configFilePath=getCrawlerConfigFilePath();
		else if (configArea.equals("threading"))
			configFilePath=getThreadingConfigFilePath();
		else if (configArea.equals("data"))
			configFilePath=getDataConfigFilePath();
		else if (configArea.equals("hana"))
			configFilePath=getHanaConfigFilePath();
		else if (configArea.equals("neo4j"))
			configFilePath=getNeo4JConfigFilePath();
		else if (configArea.equals("socialnetwork"))
			configFilePath=getSocialNetworkFilePath();
		else if (configArea.equals("webparserlist"))
			configFilePath=getWebParserListFilePath();
		else
			configFilePath=getRuntimeConfigFilePath();
		return configFilePath;
	}
	
	// return a string element
	public String getStringValue(String parameter, String configArea){
		String temp =null;
		
		try {
			temp = getValueByName(parameter, getConfigFilePath(configArea));
		} catch (SAXException | ParserConfigurationException e) {
            logger.error("EXCEPTION :: could not parse the config file while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (NullPointerException e){ 
        	logger.error("EXCEPTION :: element or file not found while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea));
        	e.printStackTrace();
        	System.exit(-1);
		} catch (IOException e){
	        	logger.error("EXCEPTION :: IO Error while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (Exception e){
        	logger.error("EXCEPTION :: Somethig went wrong while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
        }
		
		//logger.trace("received " + temp + " for " + parameter + " in section " +configArea+ " of file " + getConfigFilePath(configArea));
		
		if (temp == null) {
			logger.warn("Did not receive any information for "+parameter+" in area "+configArea+" - returning null");
			return null;
		} else {
			return temp;
		}
	}
	// return a boolean element
	public boolean getBooleanValue(String parameter, String configArea){
		String temp = null;
		
		try {
			temp = getValueByName(parameter, getConfigFilePath(configArea));
		} catch (SAXException | ParserConfigurationException e) {
            logger.error("EXCEPTION :: could not parse the config file while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (NullPointerException e){ 
        	logger.error("EXCEPTION :: element or file not found while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea));
        	e.printStackTrace();
        	System.exit(-1);
		} catch (IOException e){
	        	logger.error("EXCEPTION :: IO Error while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (Exception e){
        	logger.error("EXCEPTION :: Somethig went wrong while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
        }
		
		//logger.trace("received " + temp + " for " + parameter + " in section " +configArea+ " of file " + getConfigFilePath(configArea));
		
		if (temp == null) {
			logger.warn("Did not receive any information for "+parameter+" in area "+configArea+" - returning true");
			return true;
		} else {
			if ("true".equals(temp))
				return true;
			else
				return false;
		}
	}
	// return an int element
	public int getIntValue(String parameter, String configArea){
		String temp =null;
		
		try {
			temp = getValueByName(parameter, getConfigFilePath(configArea));
		} catch (SAXException | ParserConfigurationException e) {
            logger.error("EXCEPTION :: could not parse the config file while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (NullPointerException e){ 
        	logger.error("EXCEPTION :: element or file not found while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea));
        	e.printStackTrace();
        	System.exit(-1);
		} catch (IOException e){
	        	logger.error("EXCEPTION :: IO Error while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
		} catch (Exception e){
        	logger.error("EXCEPTION :: Somethig went wrong while checking for "+parameter+" in section "+configArea+" of file "+getConfigFilePath(configArea)+" the exception is -- " + e.getMessage());
        }
		
		//logger.trace("received " + temp + " for " + parameter + " in section " +configArea+ " of file " + getConfigFilePath(configArea));
		
		if (temp == null) {
			logger.warn("Did not receive any information for "+parameter+" in area "+configArea+" - returning -666");
			return -666;
		} else {
			return (Integer.parseInt(temp));
		}
	}
	
	private String getValueByName(String parameter, String configFile) throws SAXException, IOException, ParserConfigurationException, NullPointerException {
		final RuntimeOptionsParser handler = new RuntimeOptionsParser();
		
		SAXParserFactory.newInstance().newSAXParser().parse(configFile, handler);
		
		Map<String, RuntimeOption> result = handler.getResultAsMap();
		Collection<RuntimeOption> values = result.values();
		
		for (RuntimeOption option : values) {
			//logger.trace("checking OPTION: " + option.getName() + " TYPE: " + option.getType()+ " VALUE: " + option.getValue());
			if (option.getName().equalsIgnoreCase(parameter)) {
				//logger.trace("PARAMETER "+parameter+" matches OPTION: " + option.getName() + " of TYPE: " + option.getType()+ " with VALUE: " + option.getValue());
				return option.getValue();
			}
		}
		
		return null;
    }
	
	// get the full qualified path to the given configuration file
	public String returnQualifiedConfigPath(String inputPath){
		return ContextLoaderListener.getServletContext().getRealPath("/WEB-INF/"+ inputPath);
	}
	
	
	// the constructor is NOT to be executed externally, but only via getInstance()
	private RuntimeConfiguration(){
		logger.debug("initializing runtime configuration");
		setRuntimeConfigFilePath(	returnQualifiedConfigPath(RTCF));
		
		//logger.debug("setting path to configuration files");
		setXmlLayoutFilePath(		returnQualifiedConfigPath(	getStringValue("XmlLayoutFilePath", "runtime")));
		setCrawlerConfigFilePath(	returnQualifiedConfigPath(	getStringValue("CrawlerConfigurationFilePath", "runtime")));
		setDataConfigFilePath(		returnQualifiedConfigPath(	getStringValue("DataConfigurationFilePath", "runtime")));
		setThreadingConfigFilePath(	returnQualifiedConfigPath(	getStringValue("ThreadingConfigurationFilePath", "runtime")));
		setHanaConfigFilePath(		returnQualifiedConfigPath(	getStringValue("HanaConfigurationFilePath", "runtime")));
		setHanaConfigFilePath(		returnQualifiedConfigPath(	getStringValue("Neo4JConfigurationFilePath", "runtime")));
		setWebParserListFilePath(	returnQualifiedConfigPath(	getStringValue("ParserListFilePath", "runtime")));
		setSocialNetworkFilePath(	returnQualifiedConfigPath(	getStringValue("SocialNetworkFilePath", "runtime")));
	}
	
	
	
	// getter for the configuration file path
	public String 	getRuntimeConfigFilePath()	{ return runtimeConfigFilePath; }
	public String 	getHanaConfigFilePath() 	{ return hanaConfigFilePath; }
	public String	getNeo4JConfigFilePath()	{ return neo4JConfigFilePath; }
	public String 	getSocialNetworkFilePath()	{ return socialNetworkFilePath; }
	public String 	getWebParserListFilePath()	{ return webParserListFilePath; }
	public String 	getCrawlerConfigFilePath()	{ return crawlerConfigFilePath; }
	public String 	getDataConfigFilePath()		{ return dataConfigFilePath; }
	public String	getThreadingConfigFilePath(){ return threadingConfigFilePath; }
	public String	getXmlLayoutFilePath()		{ return xmlLayoutFilePath; }
	

	// setter for the configuration file path
	private void 	setRuntimeConfigFilePath(String runtimeConfigFile) 		{ runtimeConfigFilePath = runtimeConfigFile;}
	private void 	setSocialNetworkFilePath(String socialNetworkFile) 		{ socialNetworkFilePath = socialNetworkFile;}
	private void 	setWebParserListFilePath(String parserListFile) 		{ webParserListFilePath = parserListFile;}
	private void	setHanaConfigFilePath(String hanaConfigFile) 			{ hanaConfigFilePath = hanaConfigFile;}
	private void	setNeo4JConfigFilePath(String neo4JConfigFile) 			{ neo4JConfigFilePath = neo4JConfigFile;}
	private void	setCrawlerConfigFilePath(String crawlerConfigFile)		{ crawlerConfigFilePath = crawlerConfigFile;}
	private void	setDataConfigFilePath(String dataConfigFile)			{ dataConfigFilePath = dataConfigFile; }
	private void 	setThreadingConfigFilePath(String threadingConfigFile)	{ threadingConfigFilePath = threadingConfigFile;}
	private void 	setXmlLayoutFilePath(String xmlLayoutFile)				{ xmlLayoutFilePath = xmlLayoutFile;}

	
}