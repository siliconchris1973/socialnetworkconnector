package de.comlineag.snc.handler;

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
 * @revision	0.3			- 21.07.2014
 * @status		productive but still with limitations
 * 
 * @description	this class is used to setup the overall configuration of the SNC.
 * 				In this fall the domain driven and/or customer driven configuration for 
 * 				crawler but also runtime configuration options, like whether or not 
 * 				to warn on weak encryption or simple configuration options. 
 * 				It is instantiated by the job control from applicationContext.xml and 
 * 				sets the currently used domain and customer. 
 * 				These values in turn are then accessed by the actual crawler
 * 				and passed to the crawler configuration handler to receive the correct 
 * 				constraints from the crawler configuration.
 * 
 * @limitation	xml structure is not fetched from GeneralConfiguration.xml but hard coded
 * 
 * @changelog	0.1 (Chris) 	class created
 * 				0.1a			renamed to GeneralConfiguration
 * 				0.2				added own configuration file GeneralConfiguration.xml
 * 				0.3				added xml structure of crawler configuration file
 * 								and create json on user or post creation error 
 *
 * TODO 1. get the xml layout structure elements from GeneralConfiguration.xml
 * TODO 2. use nodelist instead of single expressions for each node
 * 
 */
@DisallowConcurrentExecution
public final class GeneralConfiguration implements Job {
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	private static String configFile = "src/main/webapp/WEB-INF/GeneralConfiguration.xml";
	
	// holds domain specific information
	private static JSONObject crawlerConfigurationScope = new JSONObject();
	
	// information about the domain and customer setup
	/*
	private static String domain;
	private static String customer;
	private static boolean domainIsActive;
	private static boolean customerIsActive;
	private static int domainPriority;
	private static int customerPriority;
	*/
	
	// some publicly available runtime informations
	private static boolean WARN_ON_SIMPLE_CONFIG = true;
	private static boolean WARN_ON_SIMPLE_XML_CONFIG = true;
	private static boolean CREATE_POST_JSON_ON_ERROR = true;
	private static boolean CREATE_USER_JSON_ON_ERROR = true;
	private static boolean CREATE_POST_JSON_ON_SUCCESS = true;
	private static boolean CREATE_USER_JSON_ON_SUCCESS = true;
	
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
	private static String domainNameIdentifier				= "name";
	private static String domainNameForAllValue 			= "ALL";
	private static String constraintIdentifier 				= "constraints";
	private static String scopeIdentifier 					= "scope";
	private static String scopeOnAllValue 					= "ALL";
	private static String singleConstraintIdentifier 		= "constraint";
	private static String valueIdentifier 					= "value";
	
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		setConfigFile((String) arg0.getJobDetail().getJobDataMap().get("configFile"));
		logger.debug("using configuration file from job control " + arg0.getJobDetail().getJobDataMap().get("configFile"));
		
		// set the configuration scope in globally available variables
		setConfiguration();
		
		logger.info("retrieved domain ("+ crawlerConfigurationScope.get(domainIdentifier) +" with priority "+crawlerConfigurationScope.get("domainPriority")+") and customer ("+ crawlerConfigurationScope.get(customerIdentifier)+" with priority "+crawlerConfigurationScope.get("customerPriority")+")");
	}
	
	@SuppressWarnings("unchecked")
	private void setConfiguration(){
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			/*
			// first step is to get the domain
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) domainIdentifier, (String) node.getTextContent());
				setDomain((String) node.getTextContent());
			}
			// whether or not it is active
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"[@"+domainNameIdentifier+"='"+getDomain()+"']/isActive/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a domain activation - setting to false");
				setDomainIsActive(false);
			} else {
				if ("true".equals(node.getTextContent()))
					setDomainIsActive(true);
				else
					setDomainIsActive(false);
			}
			// and the corresponding priority
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"[@"+domainNameIdentifier+"='"+getDomain()+"']/priority/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a domain priority - setting to 0");
				setDomainPriority((int) 0);
			} else {
				setDomainPriority(Integer.parseInt(node.getTextContent()));
			}
			crawlerConfigurationScope.put((String) "domainIsActive", (boolean) getDomainIsActive());
			crawlerConfigurationScope.put((String) "domainPriority", (int) getDomainPriority());
			logger.debug("the domain "+getDomain()+" is active " + getDomainIsActiveAsString() + "and has priority "+ getDomainPriority());
			
			
			// second step is to get the customer
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"[@"+domainNameIdentifier+"='"+getDomain()+"']/"+customerIdentifier+"/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) customerIdentifier, (String) node.getTextContent());
				setCustomer((String) node.getTextContent());
			}
			// whether or not it is active
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"[@"+domainNameIdentifier+"='"+getDomain()+"']/"+customerIdentifier+"[@"+customerNameIdentifier+"='"+getCustomer()+"']/isActive/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a customer activation - setting to false");
				setCustomerIsActive(false);
			} else {
				if ("true".equals(node.getTextContent()))
					setCustomerIsActive(true);
				else
					setCustomerIsActive(false);
			}
			// and the corresponding priority
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+domainIdentifier+"']/"+domainIdentifier+"[@"+domainNameIdentifier+"='"+getDomain()+"']/"+customerIdentifier+"[@"+customerNameIdentifier+"='"+getCustomer()+"']/priority/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a customer priority - setting to 0");
				setCustomerPriority((int) 0);
			} else {
				setCustomerPriority(Integer.parseInt(node.getTextContent()));
			}
			crawlerConfigurationScope.put((String) "customerIsActive", (boolean) getCustomerIsActive());
			crawlerConfigurationScope.put((String) "customerPriority", (int) getCustomerPriority());
			logger.debug("the customer "+getCustomer()+" is active " + getCustomerIsActiveAsString() + " and has priority " + getCustomerPriority());
			*/
			
			
			// third set boolean values of runtime environment
			// WarnOnSimpleConfig
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='runtime']/param[@name='WarnOnSimpleConfigOption']/"+valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
				logger.warn("Did not receive any information from " + configFile + " using expression " + expression);
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
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/*
	private int getCustomerPriority() {
		return GeneralConfiguration.customerPriority;
	}
	private void setCustomerPriority(int i) {
		GeneralConfiguration.customerPriority = i;
	}

	private int getDomainPriority() {
		return GeneralConfiguration.domainPriority;
	}

	private void setDomainPriority(int i) {
		GeneralConfiguration.domainPriority = i;
	}
	*/
	
	// return the json with the configured domains and customers
	public static JSONObject getDomainSetup() {
		return crawlerConfigurationScope;
	}
	
	
	
	
	// getter and setter for the configuration path
	public static String getConfigFile() {
		return GeneralConfiguration.configFile;
	}
	public static void setConfigFile(String configFile) {
		GeneralConfiguration.configFile = configFile;
	}
	
	// for domain and customer data
	/*
	public static String getDomain(){
		return GeneralConfiguration.domain;
	}
	public static void setDomain(String domain){
		GeneralConfiguration.domain = domain;
	}
	public static String getCustomer(){
		return GeneralConfiguration.customer;
	}
	public static void setCustomer(String customer){
		GeneralConfiguration.customer = customer;
	}
	public static boolean getDomainIsActive(){
		return GeneralConfiguration.domainIsActive;
	}
	public static void setDomainIsActive(boolean isActive){
		GeneralConfiguration.domainIsActive = isActive;
	}
	public static boolean getCustomerIsActive(){
		return GeneralConfiguration.customerIsActive;
	}
	public static void setCustomerIsActive(boolean isActive){
		GeneralConfiguration.customerIsActive = isActive;
	}
	public static String getDomainIsActiveAsString(){
		if (GeneralConfiguration.domainIsActive)
			return "true";
		else
			return "false";
	}
	public static void setDomainIsActive(String isActive){
		if ("isActive".equals("true"))
			GeneralConfiguration.domainIsActive = true;
		else
			GeneralConfiguration.domainIsActive = false;
	}
	public static String getCustomerIsActiveAsString(){
		if (GeneralConfiguration.customerIsActive)
			return "true";
		else
			return "false";
	}
	public static void setCustomerIsActive(String isActive){
		if ("isActive".equals("true"))
			GeneralConfiguration.customerIsActive = true;
		else
			GeneralConfiguration.customerIsActive = false;
	}
	*/
	
	// for configuration xml structure
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
	public static String getDomainnameidentifier() {
		return domainNameIdentifier;
	}
	public static String getDomainnameforallvalue() {
		return domainNameForAllValue;
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
	
	
	// for runtime state 
	public static boolean getWarnOnSimpleConfig() {
		return GeneralConfiguration.WARN_ON_SIMPLE_CONFIG;
	}
	public static void setWarnOnSimpleConfig(boolean wARN_ON_SIMPLE_CONFIG) {
		GeneralConfiguration.WARN_ON_SIMPLE_CONFIG = wARN_ON_SIMPLE_CONFIG;
	}

	public static boolean getWarnOnSimpleXmlConfig() {
		return GeneralConfiguration.WARN_ON_SIMPLE_XML_CONFIG;
	}
	public static void setWarnOnSimpleXmlConfig(boolean wARN_ON_SIMPLE_XML_CONFIG) {
		GeneralConfiguration.WARN_ON_SIMPLE_XML_CONFIG = wARN_ON_SIMPLE_XML_CONFIG;
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
}
