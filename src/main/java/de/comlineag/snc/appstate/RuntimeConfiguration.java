package de.comlineag.snc.appstate;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.comlineag.snc.constants.SocialNetworks;


/**
 * 
 * @author 		Christina Guenther
 * @category	handler
 * @revision	0.6				- 26.09.2014
 * @status		productive with minor limitations
 * 
 * @description	this class is used to setup the overall configuration of the SNC.
 * 				All runtime configuration options, like whether or not to warn on 
 * 				weak encryption or simple configuration options, are configured via 
 * 				this class. 
 * 				It is instantiated by the job control from applicationContext.xml 
 * 				
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
 *
 * TODO 1. get the xml layout structure elements from RuntimeConfiguration.xml
 * TODO 2. use nodelist instead of single expressions for each node
 * 
 */
@DisallowConcurrentExecution
public final class RuntimeConfiguration implements Job {
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private static String configFile = "webapp/WEB-INF/SNC_Runtime_Configuration.xml";
	
	// some important and static runtime informations
	// whether or not to warn in the log in case a "simple" configuration option was chosen
	private static boolean 	WARN_ON_SIMPLE_CONFIG 				= true;
	private static boolean 	WARN_ON_SIMPLE_XML_CONFIG 			= true;
	private static boolean 	WARN_ON_REJECTED_ACTIONS			= false;
	
	// how to react in case storing in the sap hana db, or any other, failed to save a post or user (or maybe create a json even on success)
	private static boolean 	CREATE_POST_JSON_ON_ERROR 			= true;
	private static boolean 	CREATE_USER_JSON_ON_ERROR 			= true;
	private static boolean 	CREATE_POST_JSON_ON_SUCCESS 		= false;
	private static boolean 	CREATE_USER_JSON_ON_SUCCESS 		= false;
	private static boolean 	STOP_SNC_ON_PERSISTENCE_FAILURE 	= false;
	
	// where to store and how to process json files
	private static String 	STORAGE_PATH 						= "storage";
	private static String 	JSON_BACKUP_STORAGE_PATH 			= "json";
	private static String 	PROCESSED_JSON_BACKUP_STORAGE_PATH 	= "processedJson";
	private static String 	INVALID_JSON_BACKUP_STORAGE_PATH 	= "invalidJson";
	private static String 	MOVE_OR_DELETE_PROCESSED_JSON_FILES = "move";
	
	// text length limitations and constraints on markup usage 
	private static boolean 	TEASER_WITH_MARKUP 					= false;
	private static int 		TEASER_MAX_LENGTH 					= 256;
	private static int 		TEASER_MIN_LENGTH 					= 20;
	private static boolean 	SUBJECT_WITH_MARKUP 				= false;
	private static int 		SUBJECT_MAX_LENGTH 					= 20;
	private static int 		SUBJECT_MIN_LENGTH 					= 7;
	private static boolean 	TEXT_WITH_MARKUP 					= false;
	private static boolean 	RAW_TEXT_WITH_MARKUP 				= true;
	
	// some constants for the simple web crawler
	private static int		WC_SEARCH_LIMIT 					= 100;		// Absolute max number of pages to download
	private static int		WC_MAX_DEPTH						= 10;		// maximum number of links to go down from toplevel
	private static String	WC_ROBOT_DISALLOW_TEXT 				= "Disallow:";
	private static int		WC_CRAWLER_MAX_DOWNLOAD_SIZE 		= 2000000;	// Max size of download per page in kb
	private static boolean	WC_STAY_ON_DOMAIN					= true;
	private static boolean	WC_STAY_BELOW_GIVEN_PATH			= false;
	private static int		WC_WORD_DISTANCE_CUTOFF_MARGIN		= 30;
	private static int		WC_THREAD_POOL_SIZE					= 10;		// number of threads for parallel downloading of pages
	
	// Threading options
	private static int		PARSER_THREADING_POOL_SIZE			= 1;
	private static int		CRAWLER_THREADING_POOL_SIZE			= 1;
	private static int		PERSISTENCE_THREADING_POOL_SIZE		= 1;
	private static boolean	PARSER_THREADING_ENABLED			= false;
	private static boolean	CRAWLER_THREADING_ENABLED			= false;
	private static boolean	PERSISTENCE_THREADING_ENABLED		= false;
	private static String	PARSER_THREADING_POOL_TYPE			= "fixed";
	private static String	CRAWLER_THREADING_POOL_TYPE			= "fixed";
	private static String	PERSISTENCE_THREADING_POOL_TYPE		= "fixed";
	
	// these values are section names within the configuration db 
	private static String 	CONSTRAINT_TERM_TEXT				= "term";
	private static String 	CONSTRAINT_USER_TEXT				= "user";
	private static String 	CONSTRAINT_LANGUAGE_TEXT			= "language";
	private static String 	CONSTRAINT_SITE_TEXT				= "site";
	private static String 	CONSTRAINT_BOARD_TEXT				= "board";
	private static String 	CONSTRAINT_BLOG_TEXT				= "blog";
	private static String 	CONSTRAINT_LOCATION_TEXT			= "geoLocation";
	
	// XML Schema identifiers
	private static String 	rootIdentifier 						= "configurations";
	private static String 	singleConfigurationIdentifier 		= "configuration";
	private static String 	customerIdentifier 					= "customer";
	private static String 	customerNameIdentifier				= "name";
	private static String 	customerNameForAllValue 			= "ALL";
	private static String 	domainIdentifier 					= "domain";
	private static String 	domainStructureIdentifier 			= "domainStructure";
	private static String 	domainNameIdentifier				= "name";
	private static String 	domainNameForAllValue 				= "ALL";
	private static String 	constraintIdentifier 				= "constraints";
	private static String 	scopeIdentifier 					= "scope";
	private static String 	scopeOnAllValue 					= "ALL";
	private static String 	singleConstraintIdentifier 			= "constraint";
	private static String 	valueIdentifier 					= "value";
	private static String 	codeIdentifier 						= "code";
	private static String 	configFileTypeIdentifier			= "configFileType";
	private static String 	crawlerRunIdentifier				= "CrawlerRun";
	
	private static String 	socialNetworkConfiguration			= "socialNetworkDefinition";
	private static String 	socialNetworkIdentifier				= "network";
	private static String 	socialNetworkName					= "name";
	
	private static String 	threadingIdentifier 				= "Threading";
	private static String 	parserIdentifier 					= "Parser";
	private static String 	crawlerIdentifier 					= "Crawler";
	private static String 	persistenceIdentifier 				= "Persistence";
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		setConfigFile((String) arg0.getJobDetail().getJobDataMap().get("configFile"));
		logger.debug("setting global configuration parameters using configuration file " + arg0.getJobDetail().getJobDataMap().get("configFile") + " from job control");
		
		// set the xml layout
		setXmlLayout();
		// set the runtime configuration 
		setRuntimeConfiguration();
		// set the data definitions
		setDataDefinitions();
		// set the data definitions
		setThreadingModel();
		// instantiate the social networks class
		@SuppressWarnings("unused")
		SocialNetworks sn = SocialNetworks.getInstance();
	}
	
	
	private void setRuntimeConfiguration(){
		logger.trace("   setting runtime configuration");
		String debugMsg = "";
		
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			
			// set boolean values of runtime environment
			// WarnOnSimpleConfig
			setWarnOnSimpleConfig(getBooleanElement("runtime", "WarnOnSimpleConfigOption", xpath, doc));
			debugMsg += "    WarnOnSimpleConfigOption is " + getWarnOnSimpleConfig(); 
			
			// WarnOnSimpleXmlConfig
			setWarnOnSimpleXmlConfig(getBooleanElement("runtime", "WarnOnSimpleXmlConfigOption", xpath, doc));
			debugMsg += " / WarnOnSimpleXmlConfigOption is " + getWarnOnSimpleXmlConfig();
			
			// WarnOnSimpleXmlConfig
			setWARN_ON_REJECTED_ACTIONS(getBooleanElement("runtime", "WarnOnRejectedActions", xpath, doc));
			debugMsg += " / WarnRejectedActions is " + isWARN_ON_REJECTED_ACTIONS();
			
			// CREATE_POST_JSON_ON_ERROR
			setCREATE_POST_JSON_ON_ERROR(getBooleanElement("runtime", "CreatePostJsonOnError", xpath, doc));
			debugMsg += " / CREATE_POST_JSON_ON_ERROR is " + isCREATE_POST_JSON_ON_ERROR(); 
			
			// CREATE_USER_JSON_ON_ERROR
			setCREATE_USER_JSON_ON_ERROR(getBooleanElement("runtime", "CreateUserJsonOnError", xpath, doc));
			debugMsg += " / CREATE_USER_JSON_ON_ERROR is " + isCREATE_USER_JSON_ON_ERROR();
					
			// CREATE_POST_JSON_ON_SUCCESS
			setCREATE_POST_JSON_ON_SUCCESS(getBooleanElement("runtime", "CreatePostJsonOnSuccess", xpath, doc));
			debugMsg += " / CREATE_POST_JSON_ON_SUCCESS is " + isCREATE_POST_JSON_ON_SUCCESS(); 
			
			// CREATE_USER_JSON_ON_SUCCESS
			setCREATE_USER_JSON_ON_SUCCESS(getBooleanElement("runtime", "CreateUserJsonOnSuccess", xpath, doc));
			debugMsg += " / CREATE_USER_JSON_ON_SUCCESS is " + isCREATE_USER_JSON_ON_SUCCESS();
			
			// STORAGE_PATH
			setSTORAGE_PATH(getStringElement("runtime", "StoragePath", xpath, doc));
			debugMsg += " / STORAGE_PATH is " + getSTORAGE_PATH();
			
			// JSON_BACKUP_STORAGE_PATH
			setJSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "JsonBackupStoragePath", xpath, doc));
			debugMsg += " / JSON_BACKUP_STORAGE_PATH is " + getJSON_BACKUP_STORAGE_PATH();
			
			// PROCESSED_JSON_BACKUP_STORAGE_PATH
			setPROCESSED_JSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "ProcessedJsonBackupStoragePath", xpath, doc));
			debugMsg += " / PROCESSED_JSON_BACKUP_STORAGE_PATH is " + getPROCESSED_JSON_BACKUP_STORAGE_PATH();
			
			// INVALID_JSON_BACKUP_STORAGE_PATH
			setINVALID_JSON_BACKUP_STORAGE_PATH(getStringElement("runtime", "InvalidJsonBackupStoragePath", xpath, doc));
			debugMsg += " / INVALID_JSON_BACKUP_STORAGE_PATH is " + getINVALID_JSON_BACKUP_STORAGE_PATH();
			
			// MOVE_OR_DELETE_PROCESSED_JSON_FILES 
			setMOVE_OR_DELETE_PROCESSED_JSON_FILES(getStringElement("runtime", "MoveOrDeleteProcessedJsonFiles", xpath, doc));
			debugMsg += " / MOVE_OR_DELETE_PROCESSED_JSON_FILES is " + getMOVE_OR_DELETE_PROCESSED_JSON_FILES();
			
			// STOP_SNC_ON_PERSISTENCE_FAILURE
			setSTOP_SNC_ON_PERSISTENCE_FAILURE(getBooleanElement("runtime", "ExitOnPersistenceFailure", xpath, doc));
			debugMsg += " / STOP_SNC_ON_PERSISTENCE_FAILURE is " + isSTOP_SNC_ON_PERSISTENCE_FAILURE();
			
			
			// wcSearchLimit
			setWC_SEARCH_LIMIT(getIntElement("runtime", "wcSearchLimit", xpath, doc));
			debugMsg += " / WC_SEARCH_LIMIT is " + getWC_SEARCH_LIMIT();
			
			// wcMaxDepth
			setWC_MAX_DEPTH(getIntElement("runtime", "wcMaxDepth", xpath, doc));
			debugMsg += " / WC_MAX_DEPTH is " + getWC_MAX_DEPTH();
			
			// wcRobotDisallowText 
			setWC_ROBOT_DISALLOW_TEXT(getStringElement("runtime", "wcRobotDisallowText", xpath, doc));
			debugMsg += " / WC_ROBOT_DISALLOW_TEXT is " + getWC_ROBOT_DISALLOW_TEXT();
						
			// wcCrawlerMaxDownloadSize
			setWC_CRAWLER_MAX_DOWNLOAD_SIZE(getIntElement("runtime", "wcCrawlerMaxDownloadSize", xpath, doc));
			debugMsg += " / WC_CRAWLER_MAX_DOWNLOAD_SIZE is " + getWC_CRAWLER_MAX_DOWNLOAD_SIZE();
			
			// wcStayOnDomain
			setWC_STAY_ON_DOMAIN(getBooleanElement("runtime", "wcStayOnDomain", xpath, doc));
			debugMsg += " / WC_STAY_ON_DOMAIN is " + isWC_STAY_ON_DOMAIN();
			
			// wcStayBelowGivenPath
			setWC_STAY_BELOW_GIVEN_PATH(getBooleanElement("runtime", "wcStayBelowGivenPath", xpath, doc));
			debugMsg += " / WC_STAY_BELOW_GIVEN_PATH is " + isWC_STAY_BELOW_GIVEN_PATH();
			
			// wcWordDistanceCutoffMargin
			setWC_WORD_DISTANCE_CUTOFF_MARGIN(getIntElement("runtime", "wcWordDistanceCutoffMargin", xpath, doc));
			debugMsg += " / WC_WORD_DISTANCE_CUTOFF_MARGIN is " + getWC_WORD_DISTANCE_CUTOFF_MARGIN();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading runtime configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}
	}
	
	private void setThreadingModel(){
		logger.trace("   setting threading configuration");
		String debugMsg = "";
		
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// CrawlerThreading
			setCRAWLER_THREADING_POOL_SIZE(getIntElement("threading", "CrawlerThreadingPoolSize", xpath, doc));
			debugMsg += " / CRAWLER_THREADING_POOL_SIZE is " + getCRAWLER_THREADING_POOL_SIZE();
			// ParserThreading
			setPARSER_THREADING_POOL_SIZE(getIntElement("threading", "ParserThreadingPoolSize", xpath, doc));
			debugMsg += " / PARSER_THREADING_POOL_SIZE is " + getPARSER_THREADING_POOL_SIZE();
			// PersistenceThreading
			setPERSISTENCE_THREADING_POOL_SIZE(getIntElement("threading", "PersistenceThreadingPoolSize", xpath, doc));
			debugMsg += " / PERSISTENCE_THREADING_POOL_SIZE is " + getPERSISTENCE_THREADING_POOL_SIZE();
			// CrawlerThreading
			setCRAWLER_THREADING_POOL_TYPE(getStringElement("threading", "CrawlerThreadingPoolType", xpath, doc));
			debugMsg += " / CRAWLER_THREADING_POOL_TYPE is " + getCRAWLER_THREADING_POOL_TYPE();
			// ParserThreading
			setPARSER_THREADING_POOL_TYPE(getStringElement("threading", "ParserThreadingPoolType", xpath, doc));
			debugMsg += " / PARSER_THREADING_POOL_TYPE is " + getPARSER_THREADING_POOL_TYPE();
			// PersistenceThreading
			setPERSISTENCE_THREADING_POOL_TYPE(getStringElement("threading", "PersistenceThreadingPoolType", xpath, doc));
			debugMsg += " / PERSISTENCE_THREADING_POOL_TYPE is " + getPERSISTENCE_THREADING_POOL_TYPE();
			// CrawlerThreading
			setCRAWLER_THREADING_ENABLED(getBooleanElement("threading", "CrawlerThreadingEnabled", xpath, doc));
			debugMsg += " / CRAWLER_THREADING_ENABLED is " + isCRAWLER_THREADING_ENABLED();
			// ParserThreading
			setPARSER_THREADING_ENABLED(getBooleanElement("threading", "ParserThreadingEnabled", xpath, doc));
			debugMsg += " / PARSER_THREADING_ENABLED is " + isPARSER_THREADING_ENABLED();
			// PersistenceThreading
			setPERSISTENCE_THREADING_ENABLED(getBooleanElement("threading", "PersistenceThreadingEnabled", xpath, doc));
			debugMsg += " / PERSISTENCE_THREADING_ENABLED is " + isPERSISTENCE_THREADING_ENABLED();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading threading configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}
	}
		
	
	// DataDefinitions
	private void setDataDefinitions(){
		logger.trace("   setting data definitions");
		String debugMsg = "";
		
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// teaserWithMarkup
			setTEASER_WITH_MARKUP(getBooleanElement("DataDefinitions", "teaserWithMarkup", xpath, doc));
			debugMsg += " TEASER_WITH_MARKUP is " + isTEASER_WITH_MARKUP();
			
			// teaserMaxLength
			setTEASER_MAX_LENGTH(getIntElement("DataDefinitions", "teaserMaxLength", xpath, doc));
			debugMsg += " / TEASER_MAX_LENGTH is " + getTEASER_MAX_LENGTH();
			// teaserMinLength
			setTEASER_MIN_LENGTH(getIntElement("DataDefinitions", "teaserMinLength", xpath, doc));
			debugMsg += " / TEASER_MIN_LENGTH is " + getTEASER_MIN_LENGTH();
			
			// subjectWithMarkup
			setSUBJECT_WITH_MARKUP(getBooleanElement("DataDefinitions", "subjectWithMarkup", xpath, doc));
			debugMsg += " / SUBJECT_WITH_MARKUP is " + isSUBJECT_WITH_MARKUP();
			// subjectMaxLength
			setSUBJECT_MAX_LENGTH(getIntElement("DataDefinitions", "subjectMaxLength", xpath, doc));
			debugMsg += " / SUBJECT_MAX_LENGTH is " + getSUBJECT_MAX_LENGTH();
			// subjectMinLength
			setSUBJECT_MIN_LENGTH(getIntElement("DataDefinitions", "subjectMinLength", xpath, doc));
			debugMsg += " / SUBJECT_MIN_LENGTH is " + getSUBJECT_MIN_LENGTH();
			
			// textWithMarkup
			setTEXT_WITH_MARKUP(getBooleanElement("DataDefinitions", "textWithMarkup", xpath, doc));
			debugMsg += " / TEXT_WITH_MARKUP is " + isTEXT_WITH_MARKUP();
			// rawTextWithMarkup
			setRAW_TEXT_WITH_MARKUP(getBooleanElement("DataDefinitions", "rawTextWithMarkup", xpath, doc));
			debugMsg += " / RAW_TEXT_WITH_MARKUP is " + isRAW_TEXT_WITH_MARKUP();
			
			logger.trace(debugMsg);
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading data definition configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}
	}
	
	
	private void setXmlLayout(){
		logger.trace ("   setting XML layout");
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// set text identifiers for the constraints from XML file 
			// CONSTRAINT_TERM_TEXT
			setCONSTRAINT_TERM_TEXT(getStringElement("XmlLayout", "CONSTRAINT_TERM_TEXT", xpath, doc));
			
			// CONSTRAINT_USER_TEXT
			setCONSTRAINT_USER_TEXT(getStringElement("XmlLayout", "CONSTRAINT_USER_TEXT", xpath, doc));
			
			// CONSTRAINT_SITE_TEXT
			setCONSTRAINT_SITE_TEXT(getStringElement("XmlLayout", "CONSTRAINT_SITE_TEXT", xpath, doc));
			
			// CONSTRAINT_BOARD_TEXT
			setCONSTRAINT_BOARD_TEXT(getStringElement("XmlLayout", "CONSTRAINT_BOARD_TEXT", xpath, doc));
			
			// CONSTRAINT_BLOG_TEXT
			setCONSTRAINT_BLOG_TEXT(getStringElement("XmlLayout", "CONSTRAINT_BLOG_TEXT", xpath, doc));
			
			// CONSTRAINT_LOCATION_TEXT
			setCONSTRAINT_LOCATION_TEXT(getStringElement("XmlLayout", "CONSTRAINT_LOCATION_TEXT", xpath, doc));
			
			// CrawlerRunIdentifier
			setCrawlerRunIdentifier(getStringElement("XmlLayout", "crawlerRunIdentifier", xpath, doc));
						
		} catch (Exception e) {
			logger.error("EXCEPTION :: error reading xml-layout configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}
	}
	
	

	private Boolean getBooleanElement(String configArea, String pathContent, XPath xpath, Document doc) throws XPathExpressionException{
		Node node = null; 
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for "+pathContent+" from " + configFile + " using expression " + expression);
			return true;
		} else {
			if ("true".equals(node.getTextContent()))
				return true;
			else
				return false;
		}
	}
	private String getStringElement(String configArea, String pathContent, XPath xpath, Document doc) throws XPathExpressionException{
		Node node = null;
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for CONSTRAINT_TERM_TEXT from " + configFile + " using expression " + expression);
			return null;
		} else {
			return (node.getTextContent());
		}
	}
	private int getIntElement(String configArea, String pathContent, XPath xpath, Document doc) throws XPathExpressionException{
		Node node = null;
		String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+configArea+"']/"
				+ "param[@name='"+pathContent+"']/"+valueIdentifier;
		node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
		if (node == null) {
			logger.warn("Did not receive any information for CONSTRAINT_TERM_TEXT from " + configFile + " using expression " + expression);
			return -666;
		} else {
			return (Integer.parseInt(node.getTextContent()));
		}
	}
	
	// getter and setter for the configuration path
	public static String 	getConfigFile() { return RuntimeConfiguration.configFile;	}
	public static void 		setConfigFile(String configFile) { RuntimeConfiguration.configFile = configFile;	}
	
	// for configuration xml structure
	private void 			setCONSTRAINT_TERM_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_TERM_TEXT = s; }
	private void 			setCONSTRAINT_USER_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_USER_TEXT = s; }
	private void 			setCONSTRAINT_SITE_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_SITE_TEXT = s; }
	private void 			setCONSTRAINT_BOARD_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_BOARD_TEXT = s; }
	private void 			setCONSTRAINT_BLOG_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_BLOG_TEXT = s; }
	private void 			setCONSTRAINT_LOCATION_TEXT(final String s) { RuntimeConfiguration.CONSTRAINT_LOCATION_TEXT = s; }
	
	// getter for the xml structure
	public static String 	getRootidentifier() { return rootIdentifier; }
	public static String 	getSingleconfigurationidentifier() { return singleConfigurationIdentifier; }
	public static String 	getCustomeridentifier() { return customerIdentifier; }
	public static String 	getCustomernameidentifier() { return customerNameIdentifier; }
	public static String 	getCustomernameforallvalue() { return customerNameForAllValue; }
	public static String 	getDomainidentifier() { return domainIdentifier; }
	public static String 	getDomainstructureidentifier() { return domainStructureIdentifier; }
	public static String 	getDomainnameidentifier() { return domainNameIdentifier; }
	public static String 	getDomainnameforallvalue() { return domainNameForAllValue;	}
	public static String 	getCodeidentifier() { return codeIdentifier; }
	public static String 	getConstraintidentifier() { return constraintIdentifier; }
	public static String 	getScopeidentifier() { return scopeIdentifier; }
	public static String 	getScopeonallvalue() { return scopeOnAllValue; }
	public static String 	getSingleconstraintidentifier() { return singleConstraintIdentifier; }
	public static String 	getValueidentifier() {	return valueIdentifier;	}
	public static String 	getConstraintTermText() { return CONSTRAINT_TERM_TEXT;	}
	public static String 	getConstraintUserText() { return CONSTRAINT_USER_TEXT; }
	public static String 	getConstraintLanguageText() { return CONSTRAINT_LANGUAGE_TEXT; }
	public static String 	getConstraintSiteText() { return CONSTRAINT_SITE_TEXT; }
	public static String 	getConstraintBoardText() { return CONSTRAINT_BOARD_TEXT; }
	public static String 	getConstraintBlogText() { return CONSTRAINT_BLOG_TEXT;}
	public static String 	getConstraintLocationText() {return CONSTRAINT_LOCATION_TEXT;}
	public static String 	getConfigFileTypeIdentifier() {return configFileTypeIdentifier;}
	public static void 		setConfigFileTypeIdentifier(String configFileTypeIdentifier) {RuntimeConfiguration.configFileTypeIdentifier = configFileTypeIdentifier;}
	public static String 	getCrawlerRunIdentifier() { return crawlerRunIdentifier; }
	public static void 		setCrawlerRunIdentifier(String crawlerRunIdentifier) { RuntimeConfiguration.crawlerRunIdentifier = crawlerRunIdentifier;	}
	public static String 	getThreadingIdentifier() {return threadingIdentifier;}
	public static void 		setThreadingIdentifier(String threadingIdentifier) {RuntimeConfiguration.threadingIdentifier = threadingIdentifier;}
	public static String 	getParserIdentifier() {return parserIdentifier;}
	public static void 		setParserIdentifier(String parserIdentifier) {RuntimeConfiguration.parserIdentifier = parserIdentifier;}
	public static String 	getCrawlerIdentifier() {return crawlerIdentifier;}
	public static void 		setCrawlerIdentifier(String crawlerIdentifier) {RuntimeConfiguration.crawlerIdentifier = crawlerIdentifier;}
	public static String 	getPersistenceIdentifier() {return persistenceIdentifier;}
	public static void 		setPersistenceIdentifier(String persistenceIdentifier) {RuntimeConfiguration.persistenceIdentifier = persistenceIdentifier;	}
	
	// for runtime state 
	public static boolean 	getWarnOnSimpleConfig() {return RuntimeConfiguration.WARN_ON_SIMPLE_CONFIG;}
	public static void 		setWarnOnSimpleConfig(boolean wARN_ON_SIMPLE_CONFIG) {RuntimeConfiguration.WARN_ON_SIMPLE_CONFIG = wARN_ON_SIMPLE_CONFIG;}
	public static boolean 	getWarnOnSimpleXmlConfig() {return RuntimeConfiguration.WARN_ON_SIMPLE_XML_CONFIG;}
	public static void 		setWarnOnSimpleXmlConfig(boolean wARN_ON_SIMPLE_XML_CONFIG) {RuntimeConfiguration.WARN_ON_SIMPLE_XML_CONFIG = wARN_ON_SIMPLE_XML_CONFIG;}
	public static boolean 	isCREATE_POST_JSON_ON_ERROR() {return CREATE_POST_JSON_ON_ERROR;}
	public static void 		setCREATE_POST_JSON_ON_ERROR(boolean cREATE_POST_JSON_ON_ERROR) {CREATE_POST_JSON_ON_ERROR = cREATE_POST_JSON_ON_ERROR;}
	public static boolean 	isCREATE_USER_JSON_ON_ERROR() {return CREATE_USER_JSON_ON_ERROR;}
	public static void 		setCREATE_USER_JSON_ON_ERROR(boolean cREATE_USER_JSON_ON_ERROR) {CREATE_USER_JSON_ON_ERROR = cREATE_USER_JSON_ON_ERROR;}
	public static boolean 	isCREATE_POST_JSON_ON_SUCCESS() {return CREATE_POST_JSON_ON_SUCCESS;}
	public static void 		setCREATE_POST_JSON_ON_SUCCESS(boolean cREATE_POST_JSON_ON_SUCCESS) {CREATE_POST_JSON_ON_SUCCESS = cREATE_POST_JSON_ON_SUCCESS;}
	public static boolean 	isCREATE_USER_JSON_ON_SUCCESS() {return CREATE_USER_JSON_ON_SUCCESS;}
	public static void 		setCREATE_USER_JSON_ON_SUCCESS(boolean cREATE_USER_JSON_ON_SUCCESS) {CREATE_USER_JSON_ON_SUCCESS = cREATE_USER_JSON_ON_SUCCESS;}
	public static boolean 	isSTOP_SNC_ON_PERSISTENCE_FAILURE() {return STOP_SNC_ON_PERSISTENCE_FAILURE;}
	public static void 		setSTOP_SNC_ON_PERSISTENCE_FAILURE(boolean sTOP_SNC_ON_PERSISTENCE_FAILURE) {STOP_SNC_ON_PERSISTENCE_FAILURE = sTOP_SNC_ON_PERSISTENCE_FAILURE;}
	public static boolean 	isWARN_ON_REJECTED_ACTIONS() {return WARN_ON_REJECTED_ACTIONS;}
	public static void 		setWARN_ON_REJECTED_ACTIONS(boolean wARN_ON_REJECTED_ACTIONS) {WARN_ON_REJECTED_ACTIONS = wARN_ON_REJECTED_ACTIONS;}
	
	// XML layout
	public static String 	getSocialNetworkConfiguration() {return socialNetworkConfiguration;}
	public static void 		setSocialNetworkConfiguration(String socialNetworkConfiguration) {RuntimeConfiguration.socialNetworkConfiguration = socialNetworkConfiguration;}
	public static String 	getSocialNetworkIdentifier() {return socialNetworkIdentifier;}
	public static void 		setSocialNetworkIdentifier(String socialNetworkIdentifier) {RuntimeConfiguration.socialNetworkIdentifier = socialNetworkIdentifier;}
	public static String 	getSocialNetworkName() {return socialNetworkName;}
	public static void 		setSocialNetworkName(String socialNetworkName) {RuntimeConfiguration.socialNetworkName = socialNetworkName;}
	
	// JSON Backup storage path
	public static String 	getJSON_BACKUP_STORAGE_PATH() {return JSON_BACKUP_STORAGE_PATH;}
	public static void 		setJSON_BACKUP_STORAGE_PATH(String jSON_BACKUP_STORAGE_PATH) {JSON_BACKUP_STORAGE_PATH = jSON_BACKUP_STORAGE_PATH;}
	public static String 	getPROCESSED_JSON_BACKUP_STORAGE_PATH() {return PROCESSED_JSON_BACKUP_STORAGE_PATH; }
	public static void 		setPROCESSED_JSON_BACKUP_STORAGE_PATH( String pROCESSED_JSON_BACKUP_STORAGE_PATH) { PROCESSED_JSON_BACKUP_STORAGE_PATH = pROCESSED_JSON_BACKUP_STORAGE_PATH; }
	public static String 	getMOVE_OR_DELETE_PROCESSED_JSON_FILES() { return MOVE_OR_DELETE_PROCESSED_JSON_FILES; }
	public static void 		setMOVE_OR_DELETE_PROCESSED_JSON_FILES(String mOVE_OR_DELETE_PROCESSED_JSON_FILES) {MOVE_OR_DELETE_PROCESSED_JSON_FILES = mOVE_OR_DELETE_PROCESSED_JSON_FILES;}
	public static String 	getINVALID_JSON_BACKUP_STORAGE_PATH() {return INVALID_JSON_BACKUP_STORAGE_PATH;}
	public static void 		setINVALID_JSON_BACKUP_STORAGE_PATH(	String iNVALID_JSON_BACKUP_STORAGE_PATH) {INVALID_JSON_BACKUP_STORAGE_PATH = iNVALID_JSON_BACKUP_STORAGE_PATH;}
	public static String 	getSTORAGE_PATH() {return STORAGE_PATH;}
	public static void 		setSTORAGE_PATH(String sTORAGE_PATH) {STORAGE_PATH = sTORAGE_PATH;}
	
	// DataDefinitions
	public static boolean 	isTEASER_WITH_MARKUP() { return TEASER_WITH_MARKUP;}
	public static void 		setTEASER_WITH_MARKUP(boolean tEASER_WITH_MARKUP) { TEASER_WITH_MARKUP = tEASER_WITH_MARKUP;}
	public static int 		getTEASER_MAX_LENGTH() { return TEASER_MAX_LENGTH;}
	public static void 		setTEASER_MAX_LENGTH(int tEASER_MAX_LENGTH) { TEASER_MAX_LENGTH = tEASER_MAX_LENGTH;}
	public static int 		getTEASER_MIN_LENGTH() { return TEASER_MIN_LENGTH;}
	public static void 		setTEASER_MIN_LENGTH(int tEASER_MIN_LENGTH) { TEASER_MIN_LENGTH = tEASER_MIN_LENGTH;}
	public static boolean 	isSUBJECT_WITH_MARKUP() { return SUBJECT_WITH_MARKUP;}
	public static void 		setSUBJECT_WITH_MARKUP(boolean sUBJECT_WITH_MARKUP) { SUBJECT_WITH_MARKUP = sUBJECT_WITH_MARKUP;}
	public static int 		getSUBJECT_MAX_LENGTH() { return SUBJECT_MAX_LENGTH;}
	public static void 		setSUBJECT_MAX_LENGTH(int sUBJECT_MAX_LENGTH) { SUBJECT_MAX_LENGTH = sUBJECT_MAX_LENGTH;}
	public static int 		getSUBJECT_MIN_LENGTH() { return SUBJECT_MIN_LENGTH;}
	public static void		setSUBJECT_MIN_LENGTH(int sUBJECT_MIN_LENGTH) { SUBJECT_MIN_LENGTH = sUBJECT_MIN_LENGTH;}
	public static boolean 	isTEXT_WITH_MARKUP() { return TEXT_WITH_MARKUP;}
	public static void 		setTEXT_WITH_MARKUP(boolean tEXT_WITH_MARKUP) { TEXT_WITH_MARKUP = tEXT_WITH_MARKUP;}
	public static boolean 	isRAW_TEXT_WITH_MARKUP() { return RAW_TEXT_WITH_MARKUP;}
	public static void 		setRAW_TEXT_WITH_MARKUP(boolean rAW_TEXT_WITH_MARKUP) { RAW_TEXT_WITH_MARKUP = rAW_TEXT_WITH_MARKUP;}

	// static configuration options for the web crawler
	public static int 		getWC_SEARCH_LIMIT() {return WC_SEARCH_LIMIT;}
	public static void 		setWC_SEARCH_LIMIT(int sEARCH_LIMIT) {WC_SEARCH_LIMIT = sEARCH_LIMIT;}
	public static String 	getWC_ROBOT_DISALLOW_TEXT() { return WC_ROBOT_DISALLOW_TEXT;}
	public static void 		setWC_ROBOT_DISALLOW_TEXT(String dISALLOW) {WC_ROBOT_DISALLOW_TEXT = dISALLOW;}
	public static int 		getWC_CRAWLER_MAX_DOWNLOAD_SIZE() {return WC_CRAWLER_MAX_DOWNLOAD_SIZE;}
	public static void 		setWC_CRAWLER_MAX_DOWNLOAD_SIZE(int mAXSIZE) {WC_CRAWLER_MAX_DOWNLOAD_SIZE = mAXSIZE;}
	
	public static boolean 	isWC_STAY_ON_DOMAIN() {return WC_STAY_ON_DOMAIN;}
	public static void 		setWC_STAY_ON_DOMAIN(boolean sTAY_ON_DOMAIN) { WC_STAY_ON_DOMAIN = sTAY_ON_DOMAIN;}
	public static boolean 	isWC_STAY_BELOW_GIVEN_PATH() {return WC_STAY_BELOW_GIVEN_PATH;}
	public static void 		setWC_STAY_BELOW_GIVEN_PATH(boolean wC_STAY_BELOW_GIVEN_PATH) {WC_STAY_BELOW_GIVEN_PATH = wC_STAY_BELOW_GIVEN_PATH;}
	public static int 		getWC_THREAD_POOL_SIZE() {return WC_THREAD_POOL_SIZE;}
	public static void 		setWC_THREAD_POOL_SIZE(int wC_THREAD_POOL_SIZE) {WC_THREAD_POOL_SIZE = wC_THREAD_POOL_SIZE;}
	public static int 		getWC_MAX_DEPTH() {return WC_MAX_DEPTH;}
	public static void 		setWC_MAX_DEPTH(int wC_MAX_DEPTH) {WC_MAX_DEPTH = wC_MAX_DEPTH;}
	public static int		getWC_WORD_DISTANCE_CUTOFF_MARGIN() {return WC_WORD_DISTANCE_CUTOFF_MARGIN;}
	public static void		setWC_WORD_DISTANCE_CUTOFF_MARGIN(int wC_WORD_DISTANCE_CUTOFF_MARGIN) {WC_WORD_DISTANCE_CUTOFF_MARGIN = wC_WORD_DISTANCE_CUTOFF_MARGIN;}
	
	// Threading options
	public static int 		getPARSER_THREADING_POOL_SIZE() {	return PARSER_THREADING_POOL_SIZE;	}
	public static void 		setPARSER_THREADING_POOL_SIZE(int pARSER_THREADING_POOL_SIZE) {PARSER_THREADING_POOL_SIZE = pARSER_THREADING_POOL_SIZE;	}
	public static int 		getCRAWLER_THREADING_POOL_SIZE() {return CRAWLER_THREADING_POOL_SIZE;	}
	public static void		setCRAWLER_THREADING_POOL_SIZE(int cRAWLER_THREADING_POOL_SIZE) {CRAWLER_THREADING_POOL_SIZE = cRAWLER_THREADING_POOL_SIZE;	}
	public static int 		getPERSISTENCE_THREADING_POOL_SIZE() {return PERSISTENCE_THREADING_POOL_SIZE;}
	public static void 		setPERSISTENCE_THREADING_POOL_SIZE(int pERSISTENCE_THREADING_POOL_SIZE) {PERSISTENCE_THREADING_POOL_SIZE = pERSISTENCE_THREADING_POOL_SIZE;	}
	public static String 	getPARSER_THREADING_POOL_TYPE() {return PARSER_THREADING_POOL_TYPE;}
	public static void 		setPARSER_THREADING_POOL_TYPE(String pARSER_THREADING_POOL_TYPE) {PARSER_THREADING_POOL_TYPE = pARSER_THREADING_POOL_TYPE;}
	public static String 	getCRAWLER_THREADING_POOL_TYPE() {return CRAWLER_THREADING_POOL_TYPE;}
	public static void 		setCRAWLER_THREADING_POOL_TYPE(String cRAWLER_THREADING_POOL_TYPE) {CRAWLER_THREADING_POOL_TYPE = cRAWLER_THREADING_POOL_TYPE;	}
	public static String 	getPERSISTENCE_THREADING_POOL_TYPE() {return PERSISTENCE_THREADING_POOL_TYPE;}
	public static void 		setPERSISTENCE_THREADING_POOL_TYPE(String pERSISTENCE_THREADING_POOL_TYPE) {PERSISTENCE_THREADING_POOL_TYPE = pERSISTENCE_THREADING_POOL_TYPE;	}
	public static boolean isPARSER_THREADING_ENABLED() {return PARSER_THREADING_ENABLED;}
	public static void setPARSER_THREADING_ENABLED(boolean pARSER_THREADING_ENABLED) {PARSER_THREADING_ENABLED = pARSER_THREADING_ENABLED;}
	public static boolean isCRAWLER_THREADING_ENABLED() {return CRAWLER_THREADING_ENABLED;}
	public static void setCRAWLER_THREADING_ENABLED(boolean cRAWLER_THREADING_ENABLED) {CRAWLER_THREADING_ENABLED = cRAWLER_THREADING_ENABLED;}
	public static boolean isPERSISTENCE_THREADING_ENABLED() {return PERSISTENCE_THREADING_ENABLED;}
	public static void setPERSISTENCE_THREADING_ENABLED(boolean pERSISTENCE_THREADING_ENABLED) {PERSISTENCE_THREADING_ENABLED = pERSISTENCE_THREADING_ENABLED;}
}
