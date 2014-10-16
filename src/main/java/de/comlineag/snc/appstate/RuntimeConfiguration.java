package de.comlineag.snc.appstate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.comlineag.snc.constants.SNCStatusCodes;


/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @revision	0.9a			- 16.10.2014
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
 *
 * TODO 1 use nodelist instead of single expressions for each node
 * 
 */
public final class RuntimeConfiguration { 
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// singleton design pattern using Initialization-on-demand holder idiom, 
	private static class Holder { static final RuntimeConfiguration instance = new RuntimeConfiguration(); }
    public static RuntimeConfiguration getInstance() { return Holder.instance; }
    
    // in case you want to change the name of the runtime configuration file - you need to change this variable and recompile
	private static final String RTCF = "SNC_Runtime_Configuration.xml";
	
	
	// Every variable below is usually defined in the file SNC_Runtime_Configuration.xml. Although it is possible to
	// put some of them can also be moved to other configuration files. For an explanation of what each variable is
	// used for, see the configuration XML file.
	
	// configuration file path variables - these variables are used internally by the RuntimeConfiguration class in case
	// any of these sections are moved out of the runtime configuration xml in other files.
	private String 		runtimeConfigFilePath;	// this is the standard runtime configuration file (usually SNC_Runtime_Configuration.xml) 
	private String 		hanaConfigFilePath;		// this is the hana data definition file (usually SNC_HANA_Configuration.xml)
	private String 		socialNetworkFilePath;	// this is file containing all social network and web page definitions (usually SocialNetworkDefinition.xml)
	private String 		webParserListFilePath; 	// this file contains all available web parser (usually properties/webparser.xml)
	private String		crawlerConfigFilePath;	// this file contains definitions for the crawler (usually in SNC_Runtime_Configuration.xml)
	private String		dataConfigFilePath;		// this file contains global data definitions (usually in SNC_Runtime_Configuration.xml)
	private String		threadingConfigFilePath;// this file contains the threading configuration (usually in SNC_Runtime_Configuration.xml)
	
	// Runtime configuration
	// whether or not to warn in the log in case a "simple" configuration option was chosen
	private boolean 	WARN_ON_SIMPLE_CONFIG 				= true;
	private boolean 	WARN_ON_SIMPLE_XML_CONFIG 			= true;
	private boolean 	WARN_ON_REJECTED_ACTIONS			= false;
	
	// how to react in case storing in the sap hana db, or any other, failed to save a post or user (or maybe create a json even on success)
	private boolean 	CREATE_POST_JSON_ON_ERROR 			= true;
	private boolean 	CREATE_USER_JSON_ON_ERROR 			= true;
	private boolean 	CREATE_POST_JSON_ON_SUCCESS 		= false;
	private boolean 	CREATE_USER_JSON_ON_SUCCESS 		= false;
	private boolean 	STOP_SNC_ON_PERSISTENCE_FAILURE 	= false;
	
	// where to store and how to process json files
	private String 		STORAGE_PATH 						= "storage";
	private String 		JSON_BACKUP_STORAGE_PATH 			= "json";
	private String 		PROCESSED_JSON_BACKUP_STORAGE_PATH 	= "processedJson";
	private String 		INVALID_JSON_BACKUP_STORAGE_PATH 	= "invalidJson";
	private String 		MOVE_OR_DELETE_PROCESSED_JSON_FILES = "move";
	
	// Data definitions
	// text length limitations and constraints on markup usage 
	private boolean 	TEASER_WITH_MARKUP 					= false;
	private int 		TEASER_MAX_LENGTH 					= 256;
	private int 		TEASER_MIN_LENGTH 					= 20;
	private boolean 	SUBJECT_WITH_MARKUP 				= false;
	private int 		SUBJECT_MAX_LENGTH 					= 20;
	private int 		SUBJECT_MIN_LENGTH 					= 7;
	private boolean 	TEXT_WITH_MARKUP 					= false;
	private boolean 	RAW_TEXT_WITH_MARKUP 				= true;
	
	// Crawler configuration
	// some constants for the simple web crawler
	private int			WC_SEARCH_LIMIT 					= 100;		// Absolute max number of pages to download
	private int			WC_MAX_DEPTH						= 10;		// maximum number of links to go down from toplevel
	private String		WC_ROBOT_DISALLOW_TEXT 				= "Disallow:";
	private int			WC_CRAWLER_MAX_DOWNLOAD_SIZE 		= 2000000;	// Max size of download per page in kb
	private boolean		WC_STAY_ON_DOMAIN					= true;
	private boolean		WC_STAY_BELOW_GIVEN_PATH			= false;
	private int			WC_WORD_DISTANCE_CUTOFF_MARGIN		= 30;
	
	// Threading options
	private int			PARSER_THREADING_POOL_SIZE			= 1;
	private int			CRAWLER_THREADING_POOL_SIZE			= 1;
	private static int	PERSISTENCE_THREADING_POOL_SIZE		= 1;
	private boolean		PARSER_THREADING_ENABLED			= false;
	private boolean		CRAWLER_THREADING_ENABLED			= false;
	private boolean		PERSISTENCE_THREADING_ENABLED		= false;
	
	// XML Layout
	// these values are section names within the configuration db 
	private String 		CONSTRAINT_TERM_TEXT				= "term";
	private String 		CONSTRAINT_USER_TEXT				= "user";
	private String 		CONSTRAINT_LANGUAGE_TEXT			= "language";
	private String 		CONSTRAINT_SITE_TEXT				= "site";
	private String 		CONSTRAINT_BLOCKEDSITE_TEXT			= "blockedsite";
	private String 		CONSTRAINT_BOARD_TEXT				= "board";
	private String 		CONSTRAINT_BLOG_TEXT				= "blog";
	private String 		CONSTRAINT_LOCATION_TEXT			= "geoLocation";
	
	// XML Schema identifiers
	private String 		rootIdentifier 						= "configurations";
	private String 		singleConfigurationIdentifier 		= "configuration";
	private String 		customerIdentifier 					= "customer";
	private String 		customerNameIdentifier				= "name";
	private String 		customerNameForAllValue 			= "ALL";
	private String 		domainIdentifier 					= "domain";
	private String 		domainStructureIdentifier 			= "domainStructure";
	private String 		domainNameIdentifier				= "name";
	private String 		domainNameForAllValue 				= "ALL";
	private String 		constraintIdentifier 				= "constraints";
	private String 		scopeIdentifier 					= "scope";
	private String 		scopeOnAllValue 					= "ALL";
	private String 		singleConstraintIdentifier 			= "constraint";
	private String 		valueIdentifier 					= "value";
	private String 		codeIdentifier 						= "code";
	private String 		configFileTypeIdentifier			= "configFileType";
	private String 		crawlerRunIdentifier				= "CrawlerRun";
	
	private String 		socialNetworkConfiguration			= "socialNetworkDefinition";
	private String 		socialNetworkIdentifier				= "network";
	private String 		socialNetworkName					= "name";
	
	private String 		threadingIdentifier 				= "Threading";
	private String 		parserIdentifier 					= "Parser";
	private String 		crawlerIdentifier 					= "Crawler";
	private String 		persistenceIdentifier 				= "Persistence";
	
	
	// from this point on the methods to setup the global runtime environment are defined 
	
	// this gets the absolute file path to the runtime configuration XML-file 
	// FIXME get rid of this bloody hack
	@Deprecated
	private String getWebInfDirectory(){
		String result = null;
		try{
			FileInputStream fs= new FileInputStream("/opt/tomcat/runtimeconfighackfile.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fs));
			result = br.readLine();
			br.close();
			fs.close();																						
		}catch(Exception e){
			logger.error("EXCEPTION :: can't access /opt/tomcat/runtimeconfighackfile.txt " + e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
		return result;
	}
	
	public String returnQualifiedConfigPath(String inputPath){
		if ("//".equals(inputPath.substring(0,1))
				|| "/".equals(inputPath.substring(0,0)) 
				|| ":".equals(inputPath.substring(1,1))){
			return inputPath;
		} else {
			// TODO change the bloody hack to correct resource handler
			//return ResourcePathHolder.getResourcePath() + File.separator + inputPath;
			return getWebInfDirectory()+ File.separator + inputPath;
		}
	}
	
	
	// the constructor is NOT to be executed externally, but only via getInstance()
	private RuntimeConfiguration(){
		logger.debug("initializing runtime configuration");
		//setRuntimeConfigFilePath(	returnQualifiedConfigPath(RTCF));
		setRuntimeConfigFilePath(RTCF);
		
		// set the xml layout
		setXmlLayout(				getRuntimeConfigFilePath());
		
		// set the runtime configuration 
		setRuntimeConfiguration(	getRuntimeConfigFilePath());
		
		setCrawlerConfigFilePath(	returnQualifiedConfigPath(	getCrawlerConfigFilePath()));
		setDataConfigFilePath(		returnQualifiedConfigPath(	getDataConfigFilePath()));
		setThreadingConfigFilePath(	returnQualifiedConfigPath(	getThreadingConfigFilePath()));
		setHanaConfigFilePath(		returnQualifiedConfigPath(	getHanaConfigFilePath()));
		setWebParserListFilePath(	returnQualifiedConfigPath(	getWebParserListFilePath()));
		setSocialNetworkFilePath(	returnQualifiedConfigPath(	getSocialNetworkFilePath()));
		
		/*
		setCrawlerConfigFilePath(	ResourcePathHolder.getResourcePath()+File.separator+getCrawlerConfigFilePath());
		setDataConfigFilePath(		ResourcePathHolder.getResourcePath()+File.separator+getDataConfigFilePath());
		setThreadingConfigFilePath(	ResourcePathHolder.getResourcePath()+File.separator+getThreadingConfigFilePath());
		setHanaConfigFilePath(		ResourcePathHolder.getResourcePath()+File.separator+getHanaConfigFilePath());
		setWebParserListFilePath(	ResourcePathHolder.getResourcePath()+File.separator+getWebParserListFilePath());
		setSocialNetworkFilePath(	ResourcePathHolder.getResourcePath()+File.separator+getSocialNetworkFilePath());
		*/
		// set the threading model
		setThreadingModel(			getThreadingConfigFilePath());
		
		// set basic crawler
		setCrawlerConfiguration(	getCrawlerConfigFilePath());
		
		// set the data definitions
		setDataDefinitions(			getDataConfigFilePath());
	}
	
	
	private void setRuntimeConfiguration(String configFile){
		logger.debug(">>> setting runtime configuration");
		String debugMsg = "";
		
		try {
			File file = new File(configFile);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			
			// set boolean values of runtime environment
			// WarnOnSimpleConfig
			setWarnOnSimpleConfig(getBooleanElement("runtime", "WarnOnSimpleConfigOption", xpath, doc, configFile));
			debugMsg += "    WarnOnSimpleConfigOption is " + getWarnOnSimpleConfig(); 
			
			// WarnOnSimpleXmlConfig
			setWarnOnSimpleXmlConfig(getBooleanElement("runtime", "WarnOnSimpleXmlConfigOption", xpath, doc, configFile));
			debugMsg += " / WarnOnSimpleXmlConfigOption is " + getWarnOnSimpleXmlConfig();
			
			// WarnOnSimpleXmlConfig
			setWARN_ON_REJECTED_ACTIONS(getBooleanElement("runtime", "WarnOnRejectedActions", xpath, doc, configFile));
			debugMsg += " / WarnRejectedActions is " + isWARN_ON_REJECTED_ACTIONS();
			
			// CREATE_POST_JSON_ON_ERROR
			setCREATE_POST_JSON_ON_ERROR(getBooleanElement("runtime", "CreatePostJsonOnError", xpath, doc, configFile));
			debugMsg += " / CREATE_POST_JSON_ON_ERROR is " + isCREATE_POST_JSON_ON_ERROR(); 
			
			// CREATE_USER_JSON_ON_ERROR
			setCREATE_USER_JSON_ON_ERROR(getBooleanElement("runtime", "CreateUserJsonOnError", xpath, doc, configFile));
			debugMsg += " / CREATE_USER_JSON_ON_ERROR is " + isCREATE_USER_JSON_ON_ERROR();
					
			// CREATE_POST_JSON_ON_SUCCESS
			setCREATE_POST_JSON_ON_SUCCESS(getBooleanElement("runtime", "CreatePostJsonOnSuccess", xpath, doc, configFile));
			debugMsg += " / CREATE_POST_JSON_ON_SUCCESS is " + isCREATE_POST_JSON_ON_SUCCESS(); 
			
			// CREATE_USER_JSON_ON_SUCCESS
			setCREATE_USER_JSON_ON_SUCCESS(getBooleanElement("runtime", "CreateUserJsonOnSuccess", xpath, doc, configFile));
			debugMsg += " / CREATE_USER_JSON_ON_SUCCESS is " + isCREATE_USER_JSON_ON_SUCCESS();
			
			// STORAGE_PATH
			setSTORAGE_PATH(getStringElement("runtime", "StoragePath", xpath, doc, configFile));
			debugMsg += " / STORAGE_PATH is " + getSTORAGE_PATH();
			
			// JSON_BACKUP_STORAGE_PATH
			setJSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "JsonBackupStoragePath", xpath, doc, configFile));
			debugMsg += " / JSON_BACKUP_STORAGE_PATH is " + getJSON_BACKUP_STORAGE_PATH();
			
			// PROCESSED_JSON_BACKUP_STORAGE_PATH
			setPROCESSED_JSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "ProcessedJsonBackupStoragePath", xpath, doc, configFile));
			debugMsg += " / PROCESSED_JSON_BACKUP_STORAGE_PATH is " + getPROCESSED_JSON_BACKUP_STORAGE_PATH();
			
			// INVALID_JSON_BACKUP_STORAGE_PATH
			setINVALID_JSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "InvalidJsonBackupStoragePath", xpath, doc, configFile));
			debugMsg += " / INVALID_JSON_BACKUP_STORAGE_PATH is " + getINVALID_JSON_BACKUP_STORAGE_PATH();
			
			// MOVE_OR_DELETE_PROCESSED_JSON_FILES 
			setMOVE_OR_DELETE_PROCESSED_JSON_FILES(getStringElement("runtime", "MoveOrDeleteProcessedJsonFiles", xpath, doc, configFile));
			debugMsg += " / MOVE_OR_DELETE_PROCESSED_JSON_FILES is " + getMOVE_OR_DELETE_PROCESSED_JSON_FILES();
			
			// STOP_SNC_ON_PERSISTENCE_FAILURE
			setSTOP_SNC_ON_PERSISTENCE_FAILURE(getBooleanElement("runtime", "ExitOnPersistenceFailure", xpath, doc, configFile));
			debugMsg += " / STOP_SNC_ON_PERSISTENCE_FAILURE is " + isSTOP_SNC_ON_PERSISTENCE_FAILURE();
			
			
			// ParserListFilePath
			setWebParserListFilePath(getStringElement("runtime", "ParserListFilePath", xpath, doc, configFile));
			debugMsg += " / ParserListFilePath is " + getWebParserListFilePath();
			
			// HanaConfigurationFilePath
			setHanaConfigFilePath(getStringElement("runtime", "HanaConfigurationFilePath", xpath, doc, configFile));
			debugMsg += " / HanaConfigurationFilePath is " + getHanaConfigFilePath();
			
			// SocialNetworkDefinitionFile
			setSocialNetworkFilePath(getStringElement("runtime", "SocialNetworkFilePath", xpath, doc, configFile));
			debugMsg += " / SocialNetworkFilePath is " + getSocialNetworkFilePath();
			
			// CrawlerConfigFile
			setCrawlerConfigFilePath(getStringElement("runtime", "CrawlerConfigFilePath", xpath, doc, configFile));
			debugMsg += " / CrawlerConfigFilePath is " + getCrawlerConfigFilePath();
			
			// DataConfigFile
			setDataConfigFilePath(getStringElement("runtime", "DataConfigFilePath", xpath, doc, configFile));
			debugMsg += " / DataConfigFilePath is " + getDataConfigFilePath();
			
			// ThreadingConfigFile
			setThreadingConfigFilePath(getStringElement("runtime", "ThreadingConfigFilePath", xpath, doc, configFile));
			debugMsg += " / ThreadingConfigFilePath is " + getThreadingConfigFilePath();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading runtime configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
	
	private void setCrawlerConfiguration(String configFile){
		logger.debug(">>> setting basic crawler configuration");
		String debugMsg = "";
		
		try {
			File file = new File(configFile);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			
			// wcSearchLimit
			setWC_SEARCH_LIMIT(getIntElement("crawler", "wcSearchLimit", xpath, doc, configFile));
			debugMsg += "    WC_SEARCH_LIMIT is " + getWC_SEARCH_LIMIT();
			
			// wcMaxDepth
			setWC_MAX_DEPTH(getIntElement("crawler", "wcMaxDepth", xpath, doc, configFile));
			debugMsg += " / WC_MAX_DEPTH is " + getWC_MAX_DEPTH();
			
			// wcRobotDisallowText 
			setWC_ROBOT_DISALLOW_TEXT(getStringElement("crawler", "wcRobotDisallowText", xpath, doc, configFile));
			debugMsg += " / WC_ROBOT_DISALLOW_TEXT is " + getWC_ROBOT_DISALLOW_TEXT();
						
			// wcCrawlerMaxDownloadSize
			setWC_CRAWLER_MAX_DOWNLOAD_SIZE(getIntElement("crawler", "wcCrawlerMaxDownloadSize", xpath, doc, configFile));
			debugMsg += " / WC_CRAWLER_MAX_DOWNLOAD_SIZE is " + getWC_CRAWLER_MAX_DOWNLOAD_SIZE();
			
			// wcStayOnDomain
			setWC_STAY_ON_DOMAIN(getBooleanElement("crawler", "wcStayOnDomain", xpath, doc, configFile));
			debugMsg += " / WC_STAY_ON_DOMAIN is " + isWC_STAY_ON_DOMAIN();
			
			// wcStayBelowGivenPath
			setWC_STAY_BELOW_GIVEN_PATH(getBooleanElement("crawler", "wcStayBelowGivenPath", xpath, doc, configFile));
			debugMsg += " / WC_STAY_BELOW_GIVEN_PATH is " + isWC_STAY_BELOW_GIVEN_PATH();
			
			// wcWordDistanceCutoffMargin
			setWC_WORD_DISTANCE_CUTOFF_MARGIN(getIntElement("crawler", "wcWordDistanceCutoffMargin", xpath, doc, configFile));
			debugMsg += " / WC_WORD_DISTANCE_CUTOFF_MARGIN is " + getWC_WORD_DISTANCE_CUTOFF_MARGIN();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading runtime configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
	
	private void setThreadingModel(String configFile){
		logger.debug(">>> setting threading configuration");
		String debugMsg = "";
		
		try {
			File file = new File(configFile);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// CrawlerThreadingPoolSize
			setCRAWLER_THREADING_POOL_SIZE(getIntElement("threading", "CrawlerThreadingPoolSize", xpath, doc, configFile));
			debugMsg += "    CRAWLER_THREADING_POOL_SIZE is " + getCRAWLER_THREADING_POOL_SIZE();
			// ParserThreadingPoolSize
			setPARSER_THREADING_POOL_SIZE(getIntElement("threading", "ParserThreadingPoolSize", xpath, doc, configFile));
			debugMsg += " / PARSER_THREADING_POOL_SIZE is " + getPARSER_THREADING_POOL_SIZE();
			// PersistenceThreadingPoolSize
			setPERSISTENCE_THREADING_POOL_SIZE(getIntElement("threading", "PersistenceThreadingPoolSize", xpath, doc, configFile));
			debugMsg += " / PERSISTENCE_THREADING_POOL_SIZE is " + getPERSISTENCE_THREADING_POOL_SIZE();
			
			// CrawlerThreadingEnabled
			setCRAWLER_THREADING_ENABLED(getBooleanElement("threading", "CrawlerThreadingEnabled", xpath, doc, configFile));
			debugMsg += " / CRAWLER_THREADING_ENABLED is " + isCRAWLER_THREADING_ENABLED();
			// ParserThreadingEnabled
			setPARSER_THREADING_ENABLED(getBooleanElement("threading", "ParserThreadingEnabled", xpath, doc, configFile));
			debugMsg += " / PARSER_THREADING_ENABLED is " + isPARSER_THREADING_ENABLED();
			// PersistenceThreadingEnabled
			setPERSISTENCE_THREADING_ENABLED(getBooleanElement("threading", "PersistenceThreadingEnabled", xpath, doc, configFile));
			debugMsg += " / PERSISTENCE_THREADING_ENABLED is " + isPERSISTENCE_THREADING_ENABLED();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading threading configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
		
	
	// DataDefinitions
	private void setDataDefinitions(String configFile){
		logger.debug(">>> setting data definitions");
		String debugMsg = "";
		
		try {
			File file = new File(configFile);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// teaserWithMarkup
			setTEASER_WITH_MARKUP(getBooleanElement("DataDefinitions", "teaserWithMarkup", xpath, doc, configFile));
			debugMsg += "    TEASER_WITH_MARKUP is " + isTEASER_WITH_MARKUP();
			
			// teaserMaxLength
			setTEASER_MAX_LENGTH(getIntElement("DataDefinitions", "teaserMaxLength", xpath, doc, configFile));
			debugMsg += " / TEASER_MAX_LENGTH is " + getTEASER_MAX_LENGTH();
			// teaserMinLength
			setTEASER_MIN_LENGTH(getIntElement("DataDefinitions", "teaserMinLength", xpath, doc, configFile));
			debugMsg += " / TEASER_MIN_LENGTH is " + getTEASER_MIN_LENGTH();
			
			// subjectWithMarkup
			setSUBJECT_WITH_MARKUP(getBooleanElement("DataDefinitions", "subjectWithMarkup", xpath, doc, configFile));
			debugMsg += " / SUBJECT_WITH_MARKUP is " + isSUBJECT_WITH_MARKUP();
			// subjectMaxLength
			setSUBJECT_MAX_LENGTH(getIntElement("DataDefinitions", "subjectMaxLength", xpath, doc, configFile));
			debugMsg += " / SUBJECT_MAX_LENGTH is " + getSUBJECT_MAX_LENGTH();
			// subjectMinLength
			setSUBJECT_MIN_LENGTH(getIntElement("DataDefinitions", "subjectMinLength", xpath, doc, configFile));
			debugMsg += " / SUBJECT_MIN_LENGTH is " + getSUBJECT_MIN_LENGTH();
			
			// textWithMarkup
			setTEXT_WITH_MARKUP(getBooleanElement("DataDefinitions", "textWithMarkup", xpath, doc, configFile));
			debugMsg += " / TEXT_WITH_MARKUP is " + isTEXT_WITH_MARKUP();
			// rawTextWithMarkup
			setRAW_TEXT_WITH_MARKUP(getBooleanElement("DataDefinitions", "rawTextWithMarkup", xpath, doc, configFile));
			debugMsg += " / RAW_TEXT_WITH_MARKUP is " + isRAW_TEXT_WITH_MARKUP();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading data definition configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
	
	
	private void setXmlLayout(String configFile){
		logger.debug (">>> setting XML layout");
		String debugMsg = "";
		try {
			File file = new File(configFile);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// set text identifiers for the constraints from XML file 
			// CONSTRAINT_TERM_TEXT
			setCONSTRAINT_TERM_TEXT(getStringElement("XmlLayout", "CONSTRAINT_TERM_TEXT", xpath, doc, configFile));
			debugMsg += "    CONSTRAINT_TERM_TEXT is " + getConstraintTermText();
			
			// CONSTRAINT_USER_TEXT
			setCONSTRAINT_USER_TEXT(getStringElement("XmlLayout", "CONSTRAINT_USER_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_USER_TEXT is " + getConstraintUserText();
			
			// CONSTRAINT_SITE_TEXT
			setCONSTRAINT_SITE_TEXT(getStringElement("XmlLayout", "CONSTRAINT_SITE_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_SITE_TEXT is " + getConstraintSiteText();
			
			// CONSTRAINT_BOARD_TEXT
			setCONSTRAINT_BOARD_TEXT(getStringElement("XmlLayout", "CONSTRAINT_BOARD_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_BOARD_TEXT is " + getConstraintBoardText();
			
			// CONSTRAINT_BLOG_TEXT
			setCONSTRAINT_BLOG_TEXT(getStringElement("XmlLayout", "CONSTRAINT_BLOG_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_BLOG_TEXT is " + getConstraintBlogText();
			
			// CONSTRAINT_LOCATION_TEXT
			setCONSTRAINT_LOCATION_TEXT(getStringElement("XmlLayout", "CONSTRAINT_LOCATION_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_LOCATION_TEXT is " + getConstraintLocationText();
			
			// CONSTRAINT_BLOCKED_SITE_TEXT
			setCONSTRAINT_BLOCKEDSITE_TEXT(getStringElement("XmlLayout", "CONSTRAINT_BLOCKED_SITE_TEXT", xpath, doc, configFile));
			debugMsg += " / CONSTRAINT_BLOCKED_SITE_TEXT is " + getConstraintBlockedSiteText();
			
			// CrawlerRunIdentifier
			setCrawlerRunIdentifier(getStringElement("XmlLayout", "crawlerRunIdentifier", xpath, doc, configFile));
			debugMsg += " / CRAWLER_RUN_IDENTIFIER is " + getCrawlerRunIdentifier();
			
			
			// ConfigFiletypeIdentifier
			setConfigFileTypeIdentifier(getStringElement("XmlLayout", "configFileTypeIdentifier", xpath, doc, configFile));
			debugMsg += " / ConfigFileTypeIdentifier is " + getConfigFileTypeIdentifier();

			setThreadingIdentifier(getStringElement("XmlLayout", "THREADING_NAME", xpath, doc, configFile));
			debugMsg += " / ConfigFileTypeIdentifier is " + getConfigFileTypeIdentifier();
			setParserIdentifier(getStringElement("XmlLayout", "PARSER_NAME", xpath, doc, configFile));
			debugMsg += " / ParserIdentifier is " + getParserIdentifier();
			setCrawlerIdentifier(getStringElement("XmlLayout", "CRAWLER_NAME", xpath, doc, configFile));
			debugMsg += " / CrawlerIdentifier is " + getCrawlerIdentifier();
			setPersistenceIdentifier(getStringElement("XmlLayout", "PERSISTENCE_NAME", xpath, doc, configFile));
			debugMsg += " / PersistenceIdentifier is " + getPersistenceIdentifier();
			
			setSocialNetworkConfiguration(getStringElement("XmlLayout", "socialNetworkConfiguration", xpath, doc, configFile));
			debugMsg += " / SocialNetworkConfiguration is " + getSocialNetworkConfiguration();
			setSocialNetworkIdentifier(getStringElement("XmlLayout", "socialNetworkIdentifier", xpath, doc, configFile));
			debugMsg += " / SocialNetworkIdentifier is " + getSocialNetworkIdentifier();
			setSocialNetworkName(getStringElement("XmlLayout", "socialNetworkNameIdentifier", xpath, doc, configFile));
			debugMsg += " / SocialNetworkName is " + getSocialNetworkName();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading xml-layout configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
	
	

	private Boolean getBooleanElement(String configArea, String pathContent, XPath xpath, Document doc, String configFile) throws XPathExpressionException{
		Node node = null; 
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for "+pathContent+" in area "+configArea+" from " + configFile + " using expression " + expression);
			return true;
		} else {
			if ("true".equals(node.getTextContent()))
				return true;
			else
				return false;
		}
	}
	private String getStringElement(String configArea, String pathContent, XPath xpath, Document doc, String configFile) throws XPathExpressionException{
		Node node = null;
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for "+pathContent+" in area "+configArea+" from " + configFile + " using expression " + expression);
			return null;
		} else {
			return (node.getTextContent());
		}
	}
	private int getIntElement(String configArea, String pathContent, XPath xpath, Document doc, String configFile) throws XPathExpressionException{
		Node node = null;
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for "+pathContent+" in area "+configArea+" from " + configFile + " using expression " + expression);
			return -666;
		} else {
			return (Integer.parseInt(node.getTextContent()));
		}
	}
	
	// getter for the configuration path
	public String 	getRuntimeConfigFilePath()	{ return runtimeConfigFilePath; }
	public String 	getHanaConfigFilePath() 	{ return hanaConfigFilePath; }
	public String 	getSocialNetworkFilePath()	{ return socialNetworkFilePath; }
	public String 	getWebParserListFilePath()	{ return webParserListFilePath; }
	public String 	getCrawlerConfigFilePath()	{ return crawlerConfigFilePath; }
	public String 	getDataConfigFilePath()		{ return dataConfigFilePath; }
	public String	getThreadingConfigFilePath(){ return threadingConfigFilePath; }
	
	// JSON Backup storage path
	public String 	getSTORAGE_PATH() {return STORAGE_PATH;}
	public String 	getJSON_BACKUP_STORAGE_PATH() {return JSON_BACKUP_STORAGE_PATH;}
	public String 	getPROCESSED_JSON_BACKUP_STORAGE_PATH() {return PROCESSED_JSON_BACKUP_STORAGE_PATH; }
	public String 	getMOVE_OR_DELETE_PROCESSED_JSON_FILES() { return MOVE_OR_DELETE_PROCESSED_JSON_FILES; }
	public String 	getINVALID_JSON_BACKUP_STORAGE_PATH() {return INVALID_JSON_BACKUP_STORAGE_PATH;}
	
	// getter for the xml structure
	public String 	getRootidentifier() { return rootIdentifier; }
	public String 	getSingleconfigurationidentifier() { return singleConfigurationIdentifier; }
	public String 	getCustomeridentifier() { return customerIdentifier; }
	public String 	getCustomernameidentifier() { return customerNameIdentifier; }
	public String 	getCustomernameforallvalue() { return customerNameForAllValue; }
	public String 	getDomainidentifier() { return domainIdentifier; }
	public String 	getDomainstructureidentifier() { return domainStructureIdentifier; }
	public String 	getDomainnameidentifier() { return domainNameIdentifier; }
	public String 	getDomainnameforallvalue() { return domainNameForAllValue;	}
	public String 	getCodeidentifier() { return codeIdentifier; }
	public String 	getConstraintidentifier() { return constraintIdentifier; }
	public String 	getScopeidentifier() { return scopeIdentifier; }
	public String 	getScopeonallvalue() { return scopeOnAllValue; }
	public String 	getSingleconstraintidentifier() { return singleConstraintIdentifier; }
	public String 	getValueidentifier() {	return valueIdentifier;	}
	public String 	getConfigFileTypeIdentifier() {return configFileTypeIdentifier;}
	public String 	getCrawlerRunIdentifier() { return crawlerRunIdentifier; }
	public String 	getThreadingIdentifier() {return threadingIdentifier;}
	public String 	getParserIdentifier() {return parserIdentifier;}
	public String 	getCrawlerIdentifier() {return crawlerIdentifier;}
	public String 	getPersistenceIdentifier() {return persistenceIdentifier;}
	public String 	getSocialNetworkConfiguration() {return socialNetworkConfiguration;}
	public String 	getSocialNetworkIdentifier() {return socialNetworkIdentifier;}
	public String 	getSocialNetworkName() {return socialNetworkName;}
	
	public String 	getConstraintTermText() { return CONSTRAINT_TERM_TEXT;	}
	public String 	getConstraintUserText() { return CONSTRAINT_USER_TEXT; }
	public String 	getConstraintLanguageText() { return CONSTRAINT_LANGUAGE_TEXT; }
	public String 	getConstraintSiteText() { return CONSTRAINT_SITE_TEXT; }
	public String 	getConstraintBlockedSiteText() { return CONSTRAINT_BLOCKEDSITE_TEXT; }
	public String 	getConstraintBoardText() { return CONSTRAINT_BOARD_TEXT; }
	public String 	getConstraintBlogText() { return CONSTRAINT_BLOG_TEXT;}
	public String 	getConstraintLocationText() {return CONSTRAINT_LOCATION_TEXT;}
	
	// for runtime state 
	public boolean 	getWarnOnSimpleConfig() {return WARN_ON_SIMPLE_CONFIG;}
	public boolean 	getWarnOnSimpleXmlConfig() {return WARN_ON_SIMPLE_XML_CONFIG;}
	public boolean 	isCREATE_POST_JSON_ON_ERROR() {return CREATE_POST_JSON_ON_ERROR;}
	public boolean 	isCREATE_USER_JSON_ON_ERROR() {return CREATE_USER_JSON_ON_ERROR;}
	public boolean 	isCREATE_POST_JSON_ON_SUCCESS() {return CREATE_POST_JSON_ON_SUCCESS;}
	public boolean 	isCREATE_USER_JSON_ON_SUCCESS() {return CREATE_USER_JSON_ON_SUCCESS;}
	public boolean 	isSTOP_SNC_ON_PERSISTENCE_FAILURE() {return STOP_SNC_ON_PERSISTENCE_FAILURE;}
	public boolean 	isWARN_ON_REJECTED_ACTIONS() {return WARN_ON_REJECTED_ACTIONS;}
	
	// DataDefinitions
	public boolean 	isTEASER_WITH_MARKUP() { return TEASER_WITH_MARKUP;}
	public int 		getTEASER_MAX_LENGTH() { return TEASER_MAX_LENGTH;}
	public int 		getTEASER_MIN_LENGTH() { return TEASER_MIN_LENGTH;}
	public boolean 	isSUBJECT_WITH_MARKUP() { return SUBJECT_WITH_MARKUP;}
	public int 		getSUBJECT_MAX_LENGTH() { return SUBJECT_MAX_LENGTH;}
	public int 		getSUBJECT_MIN_LENGTH() { return SUBJECT_MIN_LENGTH;}
	public boolean 	isTEXT_WITH_MARKUP() { return TEXT_WITH_MARKUP;}
	public boolean 	isRAW_TEXT_WITH_MARKUP() { return RAW_TEXT_WITH_MARKUP;}
	
	// static configuration options for the web crawler
	public int 		getWC_SEARCH_LIMIT() {return WC_SEARCH_LIMIT;}
	public String 	getWC_ROBOT_DISALLOW_TEXT() { return WC_ROBOT_DISALLOW_TEXT;}
	public int 		getWC_CRAWLER_MAX_DOWNLOAD_SIZE() {return WC_CRAWLER_MAX_DOWNLOAD_SIZE;}
	
	public boolean 	isWC_STAY_ON_DOMAIN() {return WC_STAY_ON_DOMAIN;}
	public boolean 	isWC_STAY_BELOW_GIVEN_PATH() {return WC_STAY_BELOW_GIVEN_PATH;}
	public int 		getWC_MAX_DEPTH() {return WC_MAX_DEPTH;}
	public int		getWC_WORD_DISTANCE_CUTOFF_MARGIN() {return WC_WORD_DISTANCE_CUTOFF_MARGIN;}
	
	// Threading options
	public int 		getPARSER_THREADING_POOL_SIZE() {return PARSER_THREADING_POOL_SIZE;	}
	public int 		getCRAWLER_THREADING_POOL_SIZE() {return CRAWLER_THREADING_POOL_SIZE;	}
	public int 		getPERSISTENCE_THREADING_POOL_SIZE() {return PERSISTENCE_THREADING_POOL_SIZE;}
	public boolean 	isPARSER_THREADING_ENABLED() {return PARSER_THREADING_ENABLED;}
	public boolean 	isCRAWLER_THREADING_ENABLED() {return CRAWLER_THREADING_ENABLED;}
	public boolean 	isPERSISTENCE_THREADING_ENABLED() {return PERSISTENCE_THREADING_ENABLED;}
	
	
	
	// private setter
	private void 		setRuntimeConfigFilePath(String runtimeConfigFile) 	{ runtimeConfigFilePath = runtimeConfigFile;}
	private void 		setSocialNetworkFilePath(String socialNetworkFile) 	{ socialNetworkFilePath = socialNetworkFile;}
	private void 		setWebParserListFilePath(String parserListFile) 	{ webParserListFilePath = parserListFile;}
	private void		setHanaConfigFilePath(String hanaConfigFile) 		{ hanaConfigFilePath = hanaConfigFile;}
	private void		setCrawlerConfigFilePath(String crawlerConfigFile)	{ crawlerConfigFilePath = crawlerConfigFile;}
	private void		setDataConfigFilePath(String dataConfigFile)		{ dataConfigFilePath = dataConfigFile; }
	private void 		setThreadingConfigFilePath(String threadingConfigFile) {threadingConfigFilePath = threadingConfigFile;}
	
	private void 		setINVALID_JSON_BACKUP_STORAGE_PATH(String iNVALID_JSON_BACKUP_STORAGE_PATH) {INVALID_JSON_BACKUP_STORAGE_PATH = iNVALID_JSON_BACKUP_STORAGE_PATH;}
	private void 		setSTORAGE_PATH(String sTORAGE_PATH) {STORAGE_PATH = sTORAGE_PATH;}
	private void 		setJSON_BACKUP_STORAGE_PATH(String jSON_BACKUP_STORAGE_PATH) {JSON_BACKUP_STORAGE_PATH = jSON_BACKUP_STORAGE_PATH;}
	private void 		setPROCESSED_JSON_BACKUP_STORAGE_PATH( String pROCESSED_JSON_BACKUP_STORAGE_PATH) { PROCESSED_JSON_BACKUP_STORAGE_PATH = pROCESSED_JSON_BACKUP_STORAGE_PATH; }
	private void 		setMOVE_OR_DELETE_PROCESSED_JSON_FILES(String mOVE_OR_DELETE_PROCESSED_JSON_FILES) {MOVE_OR_DELETE_PROCESSED_JSON_FILES = mOVE_OR_DELETE_PROCESSED_JSON_FILES;}
	
	private void 		setCONSTRAINT_TERM_TEXT(String s) { CONSTRAINT_TERM_TEXT = s; }
	private void 		setCONSTRAINT_USER_TEXT(String s) { CONSTRAINT_USER_TEXT = s; }
	private void 		setCONSTRAINT_SITE_TEXT(String s) { CONSTRAINT_SITE_TEXT = s; }
	private void 		setCONSTRAINT_BLOCKEDSITE_TEXT(String s) { CONSTRAINT_BLOCKEDSITE_TEXT = s; }
	private void 		setCONSTRAINT_BOARD_TEXT(String s) { CONSTRAINT_BOARD_TEXT = s; }
	private void 		setCONSTRAINT_BLOG_TEXT(String s) { CONSTRAINT_BLOG_TEXT = s; }
	private void 		setCONSTRAINT_LOCATION_TEXT(String s) { CONSTRAINT_LOCATION_TEXT = s; }

	private void 		setCrawlerRunIdentifier(String crawlerRunIdent) { crawlerRunIdentifier = crawlerRunIdent;	}
	private void 		setConfigFileTypeIdentifier(String configFileTypeIdent) { configFileTypeIdentifier = configFileTypeIdent;}
	private void 		setThreadingIdentifier(String threadingIdent) { threadingIdentifier = threadingIdent;}
	private void 		setParserIdentifier(String parserIdent) { parserIdentifier = parserIdent;}
	private void 		setCrawlerIdentifier(String crawlerIdent) { crawlerIdentifier = crawlerIdent;}
	private void 		setPersistenceIdentifier(String persistenceIdent) { persistenceIdentifier = persistenceIdent;	}
	private void 		setSocialNetworkConfiguration(String socialNetworkConfig) { socialNetworkConfiguration = socialNetworkConfig;}
	private void 		setSocialNetworkIdentifier(String socialNetworkIdent) { socialNetworkIdentifier = socialNetworkIdent;}
	private void 		setSocialNetworkName(String socialNetworkNam) { socialNetworkName = socialNetworkNam;}
	
	private void 		setPARSER_THREADING_POOL_SIZE(int pARSER_THREADING_POOL_SIZE) {PARSER_THREADING_POOL_SIZE = pARSER_THREADING_POOL_SIZE;	}
	private void		setCRAWLER_THREADING_POOL_SIZE(int cRAWLER_THREADING_POOL_SIZE) {CRAWLER_THREADING_POOL_SIZE = cRAWLER_THREADING_POOL_SIZE;	}
	private void 		setPERSISTENCE_THREADING_POOL_SIZE(int pERSISTENCE_THREADING_POOL_SIZE) {PERSISTENCE_THREADING_POOL_SIZE = pERSISTENCE_THREADING_POOL_SIZE;	}
	private void 		setPARSER_THREADING_ENABLED(boolean pARSER_THREADING_ENABLED) {PARSER_THREADING_ENABLED = pARSER_THREADING_ENABLED;}
	private void 		setCRAWLER_THREADING_ENABLED(boolean cRAWLER_THREADING_ENABLED) {CRAWLER_THREADING_ENABLED = cRAWLER_THREADING_ENABLED;}
	private void 		setPERSISTENCE_THREADING_ENABLED(boolean pERSISTENCE_THREADING_ENABLED) {PERSISTENCE_THREADING_ENABLED = pERSISTENCE_THREADING_ENABLED;}
	
	private void 		setTEXT_WITH_MARKUP(boolean tEXT_WITH_MARKUP) { TEXT_WITH_MARKUP = tEXT_WITH_MARKUP;}
	private void 		setRAW_TEXT_WITH_MARKUP(boolean rAW_TEXT_WITH_MARKUP) { RAW_TEXT_WITH_MARKUP = rAW_TEXT_WITH_MARKUP;}
	private void		setSUBJECT_MIN_LENGTH(int sUBJECT_MIN_LENGTH) { SUBJECT_MIN_LENGTH = sUBJECT_MIN_LENGTH;}
	private void 		setSUBJECT_MAX_LENGTH(int sUBJECT_MAX_LENGTH) { SUBJECT_MAX_LENGTH = sUBJECT_MAX_LENGTH;}
	private void 		setSUBJECT_WITH_MARKUP(boolean sUBJECT_WITH_MARKUP) { SUBJECT_WITH_MARKUP = sUBJECT_WITH_MARKUP;}
	private void 		setTEASER_MIN_LENGTH(int tEASER_MIN_LENGTH) { TEASER_MIN_LENGTH = tEASER_MIN_LENGTH;}
	private void 		setTEASER_MAX_LENGTH(int tEASER_MAX_LENGTH) { TEASER_MAX_LENGTH = tEASER_MAX_LENGTH;}
	private void 		setTEASER_WITH_MARKUP(boolean tEASER_WITH_MARKUP) { TEASER_WITH_MARKUP = tEASER_WITH_MARKUP;}
	
	private void 		setWC_SEARCH_LIMIT(int sEARCH_LIMIT) {WC_SEARCH_LIMIT = sEARCH_LIMIT;}
	private void 		setWC_ROBOT_DISALLOW_TEXT(String dISALLOW) {WC_ROBOT_DISALLOW_TEXT = dISALLOW;}
	private void 		setWC_STAY_ON_DOMAIN(boolean sTAY_ON_DOMAIN) { WC_STAY_ON_DOMAIN = sTAY_ON_DOMAIN;}
	private void 		setWC_STAY_BELOW_GIVEN_PATH(boolean wC_STAY_BELOW_GIVEN_PATH) {WC_STAY_BELOW_GIVEN_PATH = wC_STAY_BELOW_GIVEN_PATH;}
	private void 		setWC_MAX_DEPTH(int wC_MAX_DEPTH) {WC_MAX_DEPTH = wC_MAX_DEPTH;}
	private void		setWC_WORD_DISTANCE_CUTOFF_MARGIN(int wC_WORD_DISTANCE_CUTOFF_MARGIN) {WC_WORD_DISTANCE_CUTOFF_MARGIN = wC_WORD_DISTANCE_CUTOFF_MARGIN;}
	private void 		setWC_CRAWLER_MAX_DOWNLOAD_SIZE(int mAXSIZE) {WC_CRAWLER_MAX_DOWNLOAD_SIZE = mAXSIZE;}
	
	private void 		setWarnOnSimpleConfig(boolean wARN_ON_SIMPLE_CONFIG) { WARN_ON_SIMPLE_CONFIG = wARN_ON_SIMPLE_CONFIG;}
	private void 		setWarnOnSimpleXmlConfig(boolean wARN_ON_SIMPLE_XML_CONFIG) { WARN_ON_SIMPLE_XML_CONFIG = wARN_ON_SIMPLE_XML_CONFIG;}
	private void 		setCREATE_POST_JSON_ON_ERROR(boolean cREATE_POST_JSON_ON_ERROR) {CREATE_POST_JSON_ON_ERROR = cREATE_POST_JSON_ON_ERROR;}
	private void 		setCREATE_USER_JSON_ON_ERROR(boolean cREATE_USER_JSON_ON_ERROR) {CREATE_USER_JSON_ON_ERROR = cREATE_USER_JSON_ON_ERROR;}
	private void 		setCREATE_POST_JSON_ON_SUCCESS(boolean cREATE_POST_JSON_ON_SUCCESS) {CREATE_POST_JSON_ON_SUCCESS = cREATE_POST_JSON_ON_SUCCESS;}
	private void 		setCREATE_USER_JSON_ON_SUCCESS(boolean cREATE_USER_JSON_ON_SUCCESS) {CREATE_USER_JSON_ON_SUCCESS = cREATE_USER_JSON_ON_SUCCESS;}
	private void 		setSTOP_SNC_ON_PERSISTENCE_FAILURE(boolean sTOP_SNC_ON_PERSISTENCE_FAILURE) {STOP_SNC_ON_PERSISTENCE_FAILURE = sTOP_SNC_ON_PERSISTENCE_FAILURE;}
	private void 		setWARN_ON_REJECTED_ACTIONS(boolean wARN_ON_REJECTED_ACTIONS) {WARN_ON_REJECTED_ACTIONS = wARN_ON_REJECTED_ACTIONS;}
}