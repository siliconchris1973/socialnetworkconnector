package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;

import java.io.IOException;
import java.util.ArrayList;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.7		- 22.07.2014
 * @status		productive but some functions are missing
 * 
 * @description	A configuration manager for the crawler using structured xml files for the configuration
 *
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		copy of IniFileConfigurationPersistence
 * 				0.2 			added simple parsing of XML file with jsoup
 * 				0.3 			changed XML parsing to XPath
 * 				0.4 			added warnings to unimplemented methods
 * 				0.5 			added support for generic type conversion on T
 * 				0.5a 			scope identifiers are now taken from ConfigurationConstants 
 * 								Added method parameter customer. Has no effect here but needed
 * 								for XMLFileCustomerSpecificConfiguration
 * 				0.6				renamed to SimpleXmlConfigurationPersistence and changed signature
 * 								to use JSON Object instead of String for customer
 * 				0.7				added parts from RuntimeConfiguration (domain and customer)
 *  
 *  TODO 1. implement code for missing methods
 */
public class SimpleXmlConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	private JSONParser parser = new JSONParser();
	private String SN = null;
	private Object obj = null;
	
	private String domain = null;
	private String customer = null;
	private boolean domainIsActive = false;
	private boolean customerIsActive = false;
	private JSONObject crawlerConfigurationScope = new JSONObject();
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	

	@Override
	public Boolean getRunState(String socialNetwork) {
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = "/"+RuntimeConfiguration.getRootidentifier()+"/"
								+RuntimeConfiguration.getSingleconfigurationidentifier()
								+"[@"+RuntimeConfiguration.getScopeidentifier()+"='"+SN+"']/"
								+"CrawlerRun";
			Node node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			
			if (node == null) {
				return false;
			} else {
				logger.trace("the node containing infos about CrawlerRun contains " + node.getTextContent());
				
				// only and only if there actually is a false, we return false. 
				if ("false".equals(node.getTextContent()))
					return false;
			}
		} catch (Exception e) {
			logger.warn("WARNING :: could not parse configuration file or did not find CrawlerRun information - returning true.");
			logger.debug("just for your information, here is the exception message" + e.getLocalizedMessage());
		}
		// in any other circumstance, we assume the crawler shall run and return true
		return true;
	}
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		assert (!"term".equals(category) && !"site".equals(category) && !"language".equals(category) && !"geoLocation".equals(category) && !"user".equals(category))  : "ERROR :: can only accept term, site, user, language or geoLocation as category";
		
		if (!"term".equals(category) && !"site".equals(category) && !"language".equals(category) && !"geoLocation".equals(category) && !"user".equals(category)) 
			logger.warn("received "+category+" as category, but can only process term, site, user, language or geoLocation");
		
		// get configuration scope
		try {
			obj = parser.parse(configurationScope.toString());
			JSONObject jsonObject = (JSONObject) obj;
			SN = (String) jsonObject.get("SN_ID");
			
			//if ((RuntimeConfiguration.getCustomerIsActive() || RuntimeConfiguration.getDomainIsActive()) && RuntimeConfiguration.getWarnOnSimpleXmlConfig())
			if (RuntimeConfiguration.getWarnOnSimpleXmlConfig())
				logger.warn("no customer or domain specific configuration possible - consider using complex xml or db configuration manager\nyou can turn off this warning by setting WARN_ON_SIMPLE_XML_CONFIG to false in " + RuntimeConfiguration.getConfigFile().substring(RuntimeConfiguration.getConfigFile().lastIndexOf("/")+1));
		} catch (ParseException e1) {
			logger.error("ERROR :: could not parse configurationScope json " + e1.getLocalizedMessage());
		}
		
		// first check, if the correct configuration file type was specified and if not, bail out the hard way
		if (!isConfigFileCorrect()){
			System.exit(-1);
		}
		
		return (ArrayList<T>) getDataFromXml(category, SN);
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, String SN) {
		ArrayList<T> ar = new ArrayList<T>();
		logger.debug("reading " + section + "-constraints for network " + SocialNetworks.getSocialNetworkConfigElement("name", SN) + " from configuration file " + getConfigDbHandler().substring(getConfigDbHandler().lastIndexOf("/")+1));
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// first step is to get all general constraints 
			String expression = "/"+RuntimeConfiguration.getRootidentifier()+"/"+RuntimeConfiguration.getSingleconfigurationidentifier()+"[@"+RuntimeConfiguration.getScopeidentifier()+"='"+RuntimeConfiguration.getScopeonallvalue()+"']/"+RuntimeConfiguration.getSingleconstraintidentifier()+"/"+section+"/"+RuntimeConfiguration.getValueidentifier();
			NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for the specified social network 
			expression = "/"+RuntimeConfiguration.getRootidentifier()+"/"+RuntimeConfiguration.getSingleconfigurationidentifier()+"[@"+RuntimeConfiguration.getScopeidentifier()+"='"+SN+"']/"+RuntimeConfiguration.getSingleconstraintidentifier()+"/"+section+"/"+RuntimeConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			logger.trace("    " + ar.toString());
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}

        return (ArrayList<T>) ar;
	}
	
	@Override
	public String getConfigurationElement(String key, String path) {
		// TODO implement code to retrieve one single configuration element
		logger.warn("The method getConfigurationElement is currently not implemented for configuration type xml-file");
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String value, String path) {
		// TODO implement code to update a configuration element 
		logger.warn("The method setConfigurationElement is currently not implemented for configuration type xml-file");
	}
	
	@Override
	public void writeNewConfiguration(String xml) {
		// TODO implement writeNewConfiguration
		logger.warn("The method writeNewConfiguration is not yet implemented for configuration type xml-file");
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getCrawlerConfigurationScope() {
		crawlerConfigurationScope.put((String) RuntimeConfiguration.getDomainidentifier(), (String) "undefined");
		setDomain((String) "undefined");
		crawlerConfigurationScope.put((String) RuntimeConfiguration.getCustomeridentifier(), (String) "undefined");
		setCustomer((String) "undefined");
			
		/*
		try {
			File file = new File(getConfigDbHandler());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			String basicStructure = "/"+RuntimeConfiguration.getRootidentifier()+"/"+RuntimeConfiguration.getSingleconfigurationidentifier()+"[@"+RuntimeConfiguration.getScopeidentifier()+"='"+RuntimeConfiguration.getDomainstructureidentifier()+"']/"+RuntimeConfiguration.getDomainstructureidentifier();
			
			// first step is to get the domain
			expression = basicStructure + "/"+RuntimeConfiguration.getDomainidentifier()+"/"+RuntimeConfiguration.getValueidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) RuntimeConfiguration.getDomainidentifier(), (String) node.getTextContent());
				setDomain((String) node.getTextContent());
			}
			
			// whether or not it is active
			expression = basicStructure + RuntimeConfiguration.getDomainidentifier()+"[@"+RuntimeConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/isActive/"+RuntimeConfiguration.getValueidentifier();
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
			expression = basicStructure + RuntimeConfiguration.getDomainidentifier()+"[@"+RuntimeConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/priority/"+RuntimeConfiguration.getValueidentifier();
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
			expression = basicStructure + RuntimeConfiguration.getDomainidentifier()+"[@"+RuntimeConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+RuntimeConfiguration.getCustomeridentifier()+"/"+RuntimeConfiguration.getValueidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) RuntimeConfiguration.getCustomeridentifier(), (String) node.getTextContent());
				setCustomer((String) node.getTextContent());
			}
			// whether or not it is active
			expression = basicStructure + RuntimeConfiguration.getDomainidentifier()+"[@"+RuntimeConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+RuntimeConfiguration.getCustomeridentifier()+"[@"+RuntimeConfiguration.getCustomernameidentifier()+"='"+getCustomer()+"']/isActive/"+RuntimeConfiguration.getValueidentifier();
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
			expression = basicStructure + RuntimeConfiguration.getDomainidentifier()+"[@"+RuntimeConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+RuntimeConfiguration.getCustomeridentifier()+"[@"+RuntimeConfiguration.getCustomernameidentifier()+"='"+getCustomer()+"']/priority/"+RuntimeConfiguration.getValueidentifier();
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
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: could not get crawler configuration " + e.getLocalizedMessage());
		}
		*/
		return crawlerConfigurationScope;
	}
	// getter and setter for above method data
	@Override
	public String getDomain() {
		return domain;
	}
	@Override
	public String getCustomer() {
		return customer;
	}
	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}
	@Override
	public void setCustomer(String customer) {
		this.customer = customer;
	}
	public boolean getDomainIsActive(){
		return domainIsActive;
	}
	public void setDomainIsActive(boolean isActive){
		domainIsActive = isActive;
	}
	public boolean getCustomerIsActive(){
		return customerIsActive;
	}
	public void setCustomerIsActive(boolean isActive){
		customerIsActive = isActive;
	}
	public String getDomainIsActiveAsString(){
		if (domainIsActive)
			return "true";
		else
			return "false";
	}
	public void setDomainIsActive(String isActive){
		if ("isActive".equals("true"))
			domainIsActive = true;
		else
			domainIsActive = false;
	}
	public String getCustomerIsActiveAsString(){
		if (customerIsActive)
			return "true";
		else
			return "false";
	}
	public void setCustomerIsActive(String isActive){
		if ("isActive".equals("true"))
			customerIsActive = true;
		else
			customerIsActive = false;
	}
	
	// check to see, if provided configuration file is correct for chosen configuration manager
	public boolean isConfigFileCorrect(){
		try {
			File file = new File(getConfigDbHandler());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			expression = "/"+RuntimeConfiguration.getRootidentifier()+"/"
										+RuntimeConfiguration.getSingleconfigurationidentifier()
											+"[@"+RuntimeConfiguration.getScopeidentifier()+"='"
												+RuntimeConfiguration.getConfigFileTypeIdentifier()+"']/type";
										
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information using expression " + expression);
				return false;
			} else {
				if (this.getClass().getSimpleName().equals(node.getTextContent())) {
					logger.debug("provied xml configuration file is of type " + node.getTextContent());
					return true;
				} else {
					logger.error("ERROR :: wrong type of configuration file provided. I need "+this.getClass().getSimpleName()+" but got "+node.getTextContent()+". Check applicationContext.xml to see, if the configuration persistence manager is pointed to the right file");
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
	}
}
