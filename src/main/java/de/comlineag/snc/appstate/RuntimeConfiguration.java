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
 * @revision	0.3a		- 21.07.2014
 * @status		productive but still with limitations
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
	private static boolean WARN_ON_SIMPLE_CONFIG 			= true;
	private static boolean WARN_ON_SIMPLE_XML_CONFIG 		= true;
	private static boolean CREATE_POST_JSON_ON_ERROR 		= true;
	private static boolean CREATE_USER_JSON_ON_ERROR 		= true;
	private static boolean CREATE_POST_JSON_ON_SUCCESS 		= false;
	private static boolean CREATE_USER_JSON_ON_SUCCESS 		= false;
	private static boolean STOP_SNC_ON_PERSISTENCE_FAILURE 	= false;
	// where to store and how to process json files
	private static String STORAGE_PATH 							= "storage";
	private static String JSON_BACKUP_STORAGE_PATH 				= "json";
	private static String PROCESSED_JSON_BACKUP_STORAGE_PATH 	= "processedJson";
	private static String INVALID_JSON_BACKUP_STORAGE_PATH 		= "invalidJson";
	private static String MOVE_OR_DELETE_PROCESSED_JSON_FILES 	= "move";
	
	// these values are section names within the configuration db 
	private static String CONSTRAINT_TERM_TEXT				= "term";
	private static String CONSTRAINT_USER_TEXT				= "user";
	private static String CONSTRAINT_LANGUAGE_TEXT			= "language";
	private static String CONSTRAINT_SITE_TEXT				= "site";
	private static String CONSTRAINT_BOARD_TEXT				= "board";
	private static String CONSTRAINT_BLOG_TEXT				= "blog";
	private static String CONSTRAINT_LOCATION_TEXT			= "geoLocation";
	
	// XML Schema identifiers
	private static String rootIdentifier 					= "configurations";
	private static String singleConfigurationIdentifier 	= "configuration";
	private static String customerIdentifier 				= "customer";
	private static String customerNameIdentifier			= "name";
	private static String customerNameForAllValue 			= "ALL";
	private static String domainIdentifier 					= "domain";
	private static String domainStructureIdentifier 		= "domainStructure";
	private static String domainNameIdentifier				= "name";
	private static String domainNameForAllValue 			= "ALL";
	private static String constraintIdentifier 				= "constraints";
	private static String scopeIdentifier 					= "scope";
	private static String scopeOnAllValue 					= "ALL";
	private static String singleConstraintIdentifier 		= "constraint";
	private static String valueIdentifier 					= "value";
	private static String codeIdentifier 					= "code";
	private static String configFileTypeIdentifier			= "configFileType";
	
	private static String socialNetworkConfiguration		= "socialNetworkDefinition";
	private static String socialNetworkIdentifier			= "network";
	private static String socialNetworkName					= "name";
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		setConfigFile((String) arg0.getJobDetail().getJobDataMap().get("configFile"));
		logger.debug("setting global configuration parameters using configuration file " + arg0.getJobDetail().getJobDataMap().get("configFile") + " from job control");
		
		// set the xml layout
		setXmlLayout();
		// set the runtime configuration 
		setRuntimeConfiguration();
		// instantiate the social networks class
		SocialNetworks sn = SocialNetworks.getInstance();
	}
	
	
	private void setRuntimeConfiguration(){
		String debugMsg = null;
		
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			
			// set boolean values of runtime environment
			// WarnOnSimpleConfig
			setWarnOnSimpleConfig(getBooleanElement("runtime", "WarnOnSimpleConfig", xpath, doc));
			debugMsg += "    WarnOnSimpleConfig is " + getWarnOnSimpleConfig(); 
			
			// WarnOnSimpleXmlConfig
			setWarnOnSimpleXmlConfig(getBooleanElement("runtime", "WarnOnSimpleXmlConfig", xpath, doc));
			debugMsg += " / WarnOnSimpleXmlConfig " + getWarnOnSimpleXmlConfig();
			
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
			
			logger.trace(debugMsg);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logger.error("EXCEPTION :: error reading configuration " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (XPathExpressionException e) {
			logger.error("EXCEPTION :: error parsing xml " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		}
	}
	
	private void setXmlLayout(){
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
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
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
	
	// getter and setter for the configuration path
	public static String getConfigFile() {
		return RuntimeConfiguration.configFile;
	}
	public static void setConfigFile(String configFile) {
		RuntimeConfiguration.configFile = configFile;
	}
	
	// for configuration xml structure
	private void setCONSTRAINT_TERM_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_TERM_TEXT = s;
	}
	private void setCONSTRAINT_USER_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_USER_TEXT = s;
	}
	private void setCONSTRAINT_SITE_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_SITE_TEXT = s;
	}
	private void setCONSTRAINT_BOARD_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_BOARD_TEXT = s;
	}
	private void setCONSTRAINT_BLOG_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_BLOG_TEXT = s;
	}
	private void setCONSTRAINT_LOCATION_TEXT(final String s){
		RuntimeConfiguration.CONSTRAINT_LOCATION_TEXT = s;
	}
	
	// getter for the xml structure
	public static String getRootidentifier() {
		return rootIdentifier;
	}
	public static String getSingleconfigurationidentifier() {
		return singleConfigurationIdentifier;
	}
	public static String getCustomeridentifier() {
		return customerIdentifier;
	}
	public static String getCustomernameidentifier() {
		return customerNameIdentifier;
	}
	public static String getCustomernameforallvalue() {
		return customerNameForAllValue;
	}
	public static String getDomainidentifier() {
		return domainIdentifier;
	}
	public static String getDomainstructureidentifier() {
		return domainStructureIdentifier;
	}
	public static String getDomainnameidentifier() {
		return domainNameIdentifier;
	}
	public static String getDomainnameforallvalue() {
		return domainNameForAllValue;
	}
	public static String getCodeidentifier() {
		return codeIdentifier;
	}
	public static String getConstraintidentifier() {
		return constraintIdentifier;
	}
	public static String getScopeidentifier() {
		return scopeIdentifier;
	}
	public static String getScopeonallvalue() {
		return scopeOnAllValue;
	}
	public static String getSingleconstraintidentifier() {
		return singleConstraintIdentifier;
	}
	public static String getValueidentifier() {
		return valueIdentifier;
	}
	public static String getConstraintTermText() {
		return CONSTRAINT_TERM_TEXT;
	}
	public static String getConstraintUserText() {
		return CONSTRAINT_USER_TEXT;
	}
	public static String getConstraintLanguageText() {
		return CONSTRAINT_LANGUAGE_TEXT;
	}
	public static String getConstraintSiteText() {
		return CONSTRAINT_SITE_TEXT;
	}
	public static String getConstraintBoardText() {
		return CONSTRAINT_BOARD_TEXT;
	}
	public static String getConstraintBlogText() {
		return CONSTRAINT_BLOG_TEXT;
	}
	public static String getConstraintLocationText() {
		return CONSTRAINT_LOCATION_TEXT;
	}
	public static String getConfigFileTypeIdentifier() {
		return configFileTypeIdentifier;
	}
	public static void setConfigFileTypeIdentifier(
			String configFileTypeIdentifier) {
		RuntimeConfiguration.configFileTypeIdentifier = configFileTypeIdentifier;
	}
	
	// for runtime state 
	public static boolean getWarnOnSimpleConfig() {
		return RuntimeConfiguration.WARN_ON_SIMPLE_CONFIG;
	}
	public static void setWarnOnSimpleConfig(boolean wARN_ON_SIMPLE_CONFIG) {
		RuntimeConfiguration.WARN_ON_SIMPLE_CONFIG = wARN_ON_SIMPLE_CONFIG;
	}

	public static boolean getWarnOnSimpleXmlConfig() {
		return RuntimeConfiguration.WARN_ON_SIMPLE_XML_CONFIG;
	}
	public static void setWarnOnSimpleXmlConfig(boolean wARN_ON_SIMPLE_XML_CONFIG) {
		RuntimeConfiguration.WARN_ON_SIMPLE_XML_CONFIG = wARN_ON_SIMPLE_XML_CONFIG;
	}
	public static boolean isCREATE_POST_JSON_ON_ERROR() {
		return CREATE_POST_JSON_ON_ERROR;
	}
	public static void setCREATE_POST_JSON_ON_ERROR(
			boolean cREATE_POST_JSON_ON_ERROR) {
		CREATE_POST_JSON_ON_ERROR = cREATE_POST_JSON_ON_ERROR;
	}
	public static boolean isCREATE_USER_JSON_ON_ERROR() {
		return CREATE_USER_JSON_ON_ERROR;
	}
	public static void setCREATE_USER_JSON_ON_ERROR(
			boolean cREATE_USER_JSON_ON_ERROR) {
		CREATE_USER_JSON_ON_ERROR = cREATE_USER_JSON_ON_ERROR;
	}
	public static boolean isCREATE_POST_JSON_ON_SUCCESS() {
		return CREATE_POST_JSON_ON_SUCCESS;
	}
	public static void setCREATE_POST_JSON_ON_SUCCESS(
			boolean cREATE_POST_JSON_ON_SUCCESS) {
		CREATE_POST_JSON_ON_SUCCESS = cREATE_POST_JSON_ON_SUCCESS;
	}
	public static boolean isCREATE_USER_JSON_ON_SUCCESS() {
		return CREATE_USER_JSON_ON_SUCCESS;
	}
	public static void setCREATE_USER_JSON_ON_SUCCESS(
			boolean cREATE_USER_JSON_ON_SUCCESS) {
		CREATE_USER_JSON_ON_SUCCESS = cREATE_USER_JSON_ON_SUCCESS;
	}
	public static boolean isSTOP_SNC_ON_PERSISTENCE_FAILURE() {
		return STOP_SNC_ON_PERSISTENCE_FAILURE;
	}
	public static void setSTOP_SNC_ON_PERSISTENCE_FAILURE(
			boolean sTOP_SNC_ON_PERSISTENCE_FAILURE) {
		STOP_SNC_ON_PERSISTENCE_FAILURE = sTOP_SNC_ON_PERSISTENCE_FAILURE;
	}

	public static String getSocialNetworkConfiguration() {
		return socialNetworkConfiguration;
	}

	public static void setSocialNetworkConfiguration(
			String socialNetworkConfiguration) {
		RuntimeConfiguration.socialNetworkConfiguration = socialNetworkConfiguration;
	}

	public static String getSocialNetworkIdentifier() {
		return socialNetworkIdentifier;
	}

	public static void setSocialNetworkIdentifier(
			String socialNetworkIdentifier) {
		RuntimeConfiguration.socialNetworkIdentifier = socialNetworkIdentifier;
	}

	public static String getSocialNetworkName() {
		return socialNetworkName;
	}

	public static void setSocialNetworkName(String socialNetworkName) {
		RuntimeConfiguration.socialNetworkName = socialNetworkName;
	}

	public static String getJSON_BACKUP_STORAGE_PATH() {
		return JSON_BACKUP_STORAGE_PATH;
	}
	public static void setJSON_BACKUP_STORAGE_PATH(
			String jSON_BACKUP_STORAGE_PATH) {
		JSON_BACKUP_STORAGE_PATH = jSON_BACKUP_STORAGE_PATH;
	}
	public static String getPROCESSED_JSON_BACKUP_STORAGE_PATH() {
		return PROCESSED_JSON_BACKUP_STORAGE_PATH;
	}
	public static void setPROCESSED_JSON_BACKUP_STORAGE_PATH(
			String pROCESSED_JSON_BACKUP_STORAGE_PATH) {
		PROCESSED_JSON_BACKUP_STORAGE_PATH = pROCESSED_JSON_BACKUP_STORAGE_PATH;
	}

	public static String getMOVE_OR_DELETE_PROCESSED_JSON_FILES() {
		return MOVE_OR_DELETE_PROCESSED_JSON_FILES;
	}

	public static void setMOVE_OR_DELETE_PROCESSED_JSON_FILES(
			String mOVE_OR_DELETE_PROCESSED_JSON_FILES) {
		MOVE_OR_DELETE_PROCESSED_JSON_FILES = mOVE_OR_DELETE_PROCESSED_JSON_FILES;
	}

	public static String getINVALID_JSON_BACKUP_STORAGE_PATH() {
		return INVALID_JSON_BACKUP_STORAGE_PATH;
	}

	public static void setINVALID_JSON_BACKUP_STORAGE_PATH(
			String iNVALID_JSON_BACKUP_STORAGE_PATH) {
		INVALID_JSON_BACKUP_STORAGE_PATH = iNVALID_JSON_BACKUP_STORAGE_PATH;
	}

	public static String getSTORAGE_PATH() {
		return STORAGE_PATH;
	}

	public static void setSTORAGE_PATH(String sTORAGE_PATH) {
		STORAGE_PATH = sTORAGE_PATH;
	}
}
