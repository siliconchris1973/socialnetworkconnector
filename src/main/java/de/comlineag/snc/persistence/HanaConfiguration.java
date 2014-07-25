package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * 
 * @author 		Christina Guenther
 * @category	handler
 * @revision	0.1			- 21.07.2014
 * @status		productive but still with limitations
 * 
 * @description	this class is used to setup special configuration details for SAP HANA
 * 
 * @limitation	xml structure is not fetched from GeneralConfiguration.xml but hard coded
 * 
 * @changelog	0.1 (Chris) 	class created as copy from GeneralConfiguration Version 0.3
 *
 * TODO 1. get the xml layout structure elements from GeneralConfiguration.xml
 * TODO 2. use nodelist instead of single expressions for each node
 * 
 */
public final class HanaConfiguration {
	
	// Logger Instanz
	//private final Logger logger = Logger.getLogger(getClass().getName());
	private static String configFile = "src/main/webapp/WEB-INF/HANAConfiguration.xml";
	
	// some publicly available runtime informations
	private static boolean CREATE_POST_JSON_ON_ERROR = true;
	private static boolean CREATE_USER_JSON_ON_ERROR = true;
	private static boolean CREATE_POST_JSON_ON_SUCCESS = true;
	private static boolean CREATE_USER_JSON_ON_SUCCESS = true;
	private static boolean STOP_SNC_ON_PERSISTENCE_FAILURE = false;
	
	private static String PATH_TO_TABLES = "comline.saa.data.tables";
	private static String SCHEMA_NAME = "CL_SAA";
	private static String POSTS_TABLE = "posts";
	private static String USERS_TABLE = "users";
	
	private void setConfiguration(){
		// hard coded schema information
		/*
		try {
			
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			*/
			
			// set boolean values of runtime environment
			/*
			// CREATE_POST_JSON_ON_ERROR
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='CreatePostJsonOnError']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
				setSTOP_SNC_ON_PERSISTENCE_FAILURE(false);
			} else {
				if ("true".equals(node.getTextContent()))
					setSTOP_SNC_ON_PERSISTENCE_FAILURE(true);
				else
					setSTOP_SNC_ON_PERSISTENCE_FAILURE(false);
			}
			
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
		*/
	}
	
	// getter and setter for the configuration path
	public static String getConfigFile() {
		return HanaConfiguration.configFile;
	}
	public static void setConfigFile(String configFile) {
		HanaConfiguration.configFile = configFile;
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

	public static String getPATH_TO_TABLES() {
		return PATH_TO_TABLES;
	}

	public static void setPATH_TO_TABLES(String pATH_TO_TABLES) {
		PATH_TO_TABLES = pATH_TO_TABLES;
	}

	public static String getSCHEMA_NAME() {
		return SCHEMA_NAME;
	}

	public static void setSCHEMA_NAME(String sCHEMA_NAME) {
		SCHEMA_NAME = sCHEMA_NAME;
	}

	public static String getPOSTS_TABLE() {
		return POSTS_TABLE;
	}

	public static void setPOSTS_TABLE(String pOSTS_TABLE) {
		POSTS_TABLE = pOSTS_TABLE;
	}

	public static String getUSERS_TABLE() {
		return USERS_TABLE;
	}

	public static void setUSERS_TABLE(String uSERS_TABLE) {
		USERS_TABLE = uSERS_TABLE;
	}
}
