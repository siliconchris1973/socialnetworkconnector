package de.comlineag.snc.appstate;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
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

import de.comlineag.snc.constants.SocialNetworks;


/**
 * 
 * @author 		Christina Guenther
 * @category	handler
 * @revision	0.3			- 21.07.2014
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
	
	private static String configFile = "src/main/webapp/WEB-INF/SNC_Runtime_Configuration.xml";
	
	// some publicly available runtime informations
	private static boolean WARN_ON_SIMPLE_CONFIG = true;
	private static boolean WARN_ON_SIMPLE_XML_CONFIG = true;
	private static boolean CREATE_POST_JSON_ON_ERROR = true;
	private static boolean CREATE_USER_JSON_ON_ERROR = true;
	private static boolean CREATE_POST_JSON_ON_SUCCESS = true;
	private static boolean CREATE_USER_JSON_ON_SUCCESS = true;
	private static boolean STOP_SNC_ON_PERSISTENCE_FAILURE = false;
	
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
		
		// set the configuration scope in globally available variables
		setXmlLayout();
		setRuntimeConfiguration();
	}
	
	private void setRuntimeConfiguration(){
		logger.trace("setting runtime definitions ...");
		
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			// set boolean values of runtime environment
			// WarnOnSimpleConfig
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='WarnOnSimpleConfigOption']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for WarnOnSimpleConfig from " + configFile + " using expression " + expression);
				setWarnOnSimpleConfig(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setWarnOnSimpleConfig(true);
				else
					setWarnOnSimpleConfig(false);
			}
			// WarnOnSimpleXmlConfig
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='WarnOnSimpleXmlConfigOption']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for WarnOnSimpleXmlConfig from " + configFile + " using expression " + expression);
				setWarnOnSimpleXmlConfig(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setWarnOnSimpleXmlConfig(true);
				else
					setWarnOnSimpleXmlConfig(false);
			}
			// CREATE_POST_JSON_ON_ERROR
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='CreatePostJsonOnError']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CREATE_POST_JSON_ON_ERROR from " + configFile + " using expression " + expression);
				setCREATE_POST_JSON_ON_ERROR(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setCREATE_POST_JSON_ON_ERROR(true);
				else
					setCREATE_POST_JSON_ON_ERROR(false);
			}
			// CREATE_USER_JSON_ON_ERROR
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='CreateUserJsonOnError']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CREATE_USER_JSON_ON_ERROR from " + configFile + " using expression " + expression);
				setCREATE_USER_JSON_ON_ERROR(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setCREATE_USER_JSON_ON_ERROR(true);
				else
					setCREATE_USER_JSON_ON_ERROR(false);
			}
			// CREATE_POST_JSON_ON_SUCCESS
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='CreatePostJsonOnSuccess']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CREATE_POST_JSON_ON_SUCCESS from " + configFile + " using expression " + expression);
				setCREATE_POST_JSON_ON_SUCCESS(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setCREATE_POST_JSON_ON_SUCCESS(true);
				else
					setCREATE_POST_JSON_ON_SUCCESS(false);
			}
			// CREATE_USER_JSON_ON_SUCCESS
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='CreateUserJsonOnSuccess']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CREATE_USER_JSON_ON_SUCCESS from " + configFile + " using expression " + expression);
				setCREATE_USER_JSON_ON_SUCCESS(true);
			} else {
				if ("true".equals(node.getTextContent()))
					setCREATE_USER_JSON_ON_SUCCESS(true);
				else
					setCREATE_USER_JSON_ON_SUCCESS(false);
			}
			// STOP_SNC_ON_PERSISTENCE_FAILURE
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='ExitOnPersistenceFailure']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for STOP_SNC_ON_PERSISTENCE_FAILURE from " + configFile + " using expression " + expression);
				setSTOP_SNC_ON_PERSISTENCE_FAILURE(false);
			} else {
				if ("true".equals(node.getTextContent()))
					setSTOP_SNC_ON_PERSISTENCE_FAILURE(true);
				else
					setSTOP_SNC_ON_PERSISTENCE_FAILURE(false);
			}
			
			logger.trace("    WarnOnSimpleConfig is " + getWarnOnSimpleConfig() + 
						" / WarnOnSimpleXmlConfig " + getWarnOnSimpleXmlConfig() + 
						" / CREATE_POST_JSON_ON_ERROR is " + isCREATE_POST_JSON_ON_ERROR() + 
						" / CREATE_USER_JSON_ON_ERROR is " + isCREATE_USER_JSON_ON_ERROR() +
						" / CREATE_POST_JSON_ON_SUCCESS is " + isCREATE_POST_JSON_ON_SUCCESS() + 
						" / CREATE_USER_JSON_ON_SUCCESS is " + isCREATE_USER_JSON_ON_SUCCESS());
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
		
		
		// instantiate the social networks class
		logger.trace("initializing social network defintitions");
		SocialNetworks sn = SocialNetworks.getInstance();
	}
	
	private void setXmlLayout(){
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			// set text identifiers for the constraints from XML file 
			// CONSTRAINT_TERM_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_TERM_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_TERM_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_TERM_TEXT(node.getTextContent());
			}
			// CONSTRAINT_USER_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_USER_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_USER_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_USER_TEXT(node.getTextContent());
			}
			// CONSTRAINT_SITE_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_SITE_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_SITE_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_SITE_TEXT(node.getTextContent());
			}
			// CONSTRAINT_BOARD_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_BOARD_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_BOARD_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_BOARD_TEXT(node.getTextContent());
			}
			// CONSTRAINT_BLOG_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_BLOG_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_BLOG_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_BLOG_TEXT(node.getTextContent());
			}
			// CONSTRAINT_LOCATION_TEXT
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='XmlLayout']/"
					+ "param[@name='CONSTRAINT_LOCATION_TEXT']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for CONSTRAINT_LOCATION_TEXT from " + configFile + " using expression " + expression);
			} else {
				setCONSTRAINT_LOCATION_TEXT(node.getTextContent());
			}
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
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
}
