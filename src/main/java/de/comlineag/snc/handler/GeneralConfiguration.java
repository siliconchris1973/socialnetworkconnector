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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.comlineag.snc.constants.ConfigurationConstants;


/**
 * 
 * @author 		Christina Guenther
 * @category	job control handler
 * @revision	0.2
 * @status		in development
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
 * @changelog	0.1 (Chris) 	class created
 * 				0.1a			renamed to GeneralConfiguration
 * 				0.2				added own configuration file GeneralConfiguration.xml
 *
 */

public final class GeneralConfiguration implements Job {
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	private static String configFile = "src/main/webapp/WEB-INF/GeneralConfiguration.xml";
	
	// holds domain specific information
	private static JSONObject crawlerConfigurationScope = new JSONObject();
	
	private static String domain;
	private static String customer;
	private static boolean domainIsActive;
	private static boolean customerIsActive;
	private static int domainPriority;
	private static int customerPriority;
	
	// some publicly available runtime informations
	private static boolean WARN_ON_SIMPLE_CONFIG = true;
	private static boolean WARN_ON_SIMPLE_XML_CONFIG = true;
	
	// this is used if triggered by job control 
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		setConfigFile((String) arg0.getJobDetail().getJobDataMap().get("configFile"));
		logger.debug("using configuration file from job control " + arg0.getJobDetail().getJobDataMap().get("configFile"));
		
		// set the configuration scope in globally available variables
		setConfiguration();
		
		logger.info("retrieved domain ("+ crawlerConfigurationScope.get(ConfigurationConstants.domainIdentifier) +" with priority "+crawlerConfigurationScope.get("domainPriority")+") and customer ("+ crawlerConfigurationScope.get(ConfigurationConstants.customerIdentifier)+" with priority "+crawlerConfigurationScope.get("customerPriority")+")");
	}
	
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
			
			// first step is to get the domain
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"/"+ConfigurationConstants.valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) ConfigurationConstants.domainIdentifier, (String) node.getTextContent());
				setDomain((String) node.getTextContent());
			}
			// and the corresponding is active status
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"[@"+ConfigurationConstants.domainNameIdentifier+"='"+getDomain()+"']/isActive/"+ConfigurationConstants.valueIdentifier;
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
			crawlerConfigurationScope.put((String) "domainIsActive", (boolean) getDomainIsActive());
			logger.trace("the domain is active " + getDomainIsActiveAsString());
			
			// and the corresponding priority
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"[@"+ConfigurationConstants.domainNameIdentifier+"='"+getDomain()+"']/priority/"+ConfigurationConstants.valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a domain priority - setting to 0");
				setDomainPriority((int) 0);
			} else {
				setDomainPriority(Integer.parseInt(node.getTextContent()));
			}
			crawlerConfigurationScope.put((String) "domainPriority", (int) getDomainPriority());
			logger.trace("the domain has priority " + getDomainPriority());
			
			// second step is to get the customer
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"[@"+ConfigurationConstants.domainNameIdentifier+"='"+getDomain()+"']/"+ConfigurationConstants.customerIdentifier+"/"+ConfigurationConstants.valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) ConfigurationConstants.customerIdentifier, (String) node.getTextContent());
				setCustomer((String) node.getTextContent());
			}
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"[@"+ConfigurationConstants.domainNameIdentifier+"='"+getDomain()+"']/"+ConfigurationConstants.customerIdentifier+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+getCustomer()+"']/isActive/"+ConfigurationConstants.valueIdentifier;
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
			crawlerConfigurationScope.put((String) "customerIsActive", (boolean) getCustomerIsActive());
			logger.trace("the customer is active " + getCustomerIsActiveAsString());
			
			// and the corresponding priority
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.domainIdentifier+"']/"+ConfigurationConstants.domainIdentifier+"[@"+ConfigurationConstants.domainNameIdentifier+"='"+getDomain()+"']/"+ConfigurationConstants.customerIdentifier+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+getCustomer()+"']/priority/"+ConfigurationConstants.valueIdentifier;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a customer priority - setting to 0");
				setCustomerPriority((int) 0);
			} else {
				setCustomerPriority(Integer.parseInt(node.getTextContent()));
			}
			crawlerConfigurationScope.put((String) "customerPriority", (int) getCustomerPriority());
			logger.trace("the customer has priority " + getCustomerPriority());
			
			
			
			// third set boolean values for warnings
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='runtime']/param[@name='WarnOnSimpleConfigOption']/"+ConfigurationConstants.valueIdentifier;
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
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='runtime']/param[@name='WarnOnSimpleXmlConfigOption']/"+ConfigurationConstants.valueIdentifier;
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
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
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
}
