package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.appstate.GeneralConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.2		- 22.07.2014
 * @status		productive but some functions are missing
 * 
 * @description	A configuration manager for the crawler using structured xml files for the configuration
 * 				and with the ability to configure individual crawler for different domains
 *
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		copy of XMLFileCustomerSpecificConfigurationPersistence - Revision 0.2
 * 				0.2				added parts from GeneralConfiguration (domain and customer)
 *  
 *  TODO 1. implement code for missing methods - see below
 */
public class ComplexXmlConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	private JSONParser parser = new JSONParser();
	private String SN = null;
	private Object obj = null;
	
	private String domain = null;
	private String customer = null;
	private boolean domainIsActive = false;
	private boolean customerIsActive = false;
	private int domainPriority = 0;
	private int customerPriority = 0;
	private JSONObject crawlerConfigurationScope = new JSONObject();
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		assert (!"term".equals(category) && !"site".equals(category) && !"language".equals(category) && !"geoLocation".equals(category) && !"user".equals(category))  : "ERROR :: can only accept term, site, user, language or geoLocation as category";
		
		if (!"term".equals(category) && !"site".equals(category) && !"language".equals(category) && !"geoLocation".equals(category) && !"user".equals(category)) 
			logger.warn("received "+category+" as category, but can only process term, site, user, language or geoLocation");
		
		// first check, if the correct configuration file type was specified and if not, bail out the hard way
		if (!isConfigFileCorrect()){
			System.exit(-1);
		}
		
		// get configuration scope
		try {
			obj = parser.parse(configurationScope.toString());
			JSONObject jsonObject = (JSONObject) obj;
			SN = (String) jsonObject.get("SN_ID");
		} catch (ParseException e1) {
			logger.error("ERROR :: could not parse configurationScope json " + e1.getLocalizedMessage());
		}
		
		return (ArrayList<T>) getDataFromXml(category, SN);
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, String SN) {
		
		ArrayList<T> ar = new ArrayList<T>();
		logger.debug("reading constraints for "+customer+" in "+domain+" on " + section + " for network " + SN.toString() + " from configuration file " + getConfigDbHandler().substring(getConfigDbHandler().lastIndexOf("/")+1));
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			NodeList nodeList = null;
			
			// first step is to get all constraints for all domains within a specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getDomainidentifier()+"']/"
					+GeneralConfiguration.getDomainidentifier()
											+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+GeneralConfiguration.getDomainnameforallvalue()+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getScopeonallvalue()+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for all domains within a specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getDomainidentifier()+"']/"
					+GeneralConfiguration.getDomainidentifier()
											+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+GeneralConfiguration.getDomainnameforallvalue()+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+SN+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// third step is to get all general constraints 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getDomainidentifier()+"']/"
					+GeneralConfiguration.getDomainidentifier()
											+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+domain+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getScopeonallvalue()+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// fourth step is to get all constraints for the specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getDomainidentifier()+"']/"
					+GeneralConfiguration.getDomainidentifier()
											+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+domain+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+SN+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			
			
			
			// AND NOW FOR CUSTOMER
			// first step is to get all constraints for all domains within a specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getCustomeridentifier()+"']/"
					+GeneralConfiguration.getCustomeridentifier()
											+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+GeneralConfiguration.getCustomernameforallvalue()+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getScopeonallvalue()+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for all customers within a specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getCustomeridentifier()+"']/"
					+GeneralConfiguration.getCustomeridentifier()
											+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+GeneralConfiguration.getCustomernameforallvalue()+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+SN+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// third step is to get all general constraints 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
								+GeneralConfiguration.getSingleconfigurationidentifier()
														+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getCustomeridentifier()+"']/"
								+GeneralConfiguration.getCustomeridentifier()
														+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+customer+"']/"
								+GeneralConfiguration.getConstraintidentifier()
														+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getScopeonallvalue()+"']/"
								+GeneralConfiguration.getSingleconstraintidentifier()+"/"
								+section+"/"
								+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// fourth step is to get all constraints for the specified social network 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
					+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getCustomeridentifier()+"']/"
					+GeneralConfiguration.getCustomeridentifier()
											+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+customer+"']/"
					+GeneralConfiguration.getConstraintidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"+SN+"']/"
					+GeneralConfiguration.getSingleconstraintidentifier()+"/"
					+section+"/"
					+GeneralConfiguration.getValueidentifier();
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			logger.debug("found " + ar.size() + " constraints on " + section);
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
	
	
	@Override
	public JSONObject getCrawlerConfigurationScope() {
		try {
			File file = new File(getConfigDbHandler());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			String basicStructure = "/"+GeneralConfiguration.getRootidentifier()+"/"
										+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"
												+GeneralConfiguration.getDomainstructureidentifier()+"']/"
										+GeneralConfiguration.getDomainstructureidentifier();
			
			// first step is to get the domain
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()+"/"+GeneralConfiguration.getValueidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive information on the domain using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) GeneralConfiguration.getDomainidentifier(), (String) node.getTextContent());
				setDomain((String) node.getTextContent());
			}
			
			// whether or not it is active
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/isActive/"+GeneralConfiguration.getValueidentifier();
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
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()
													+"[@"+GeneralConfiguration.getDomainnameidentifier()
															+"='"+getDomain()+"']"
												+ "/priority/"+GeneralConfiguration.getValueidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("did not receive a domain priority - setting to 0");
				setDomainPriority((int) 0);
			} else {
				setDomainPriority(Integer.parseInt(node.getTextContent()));
			}
			crawlerConfigurationScope.put((String) "domainIsActive", (boolean) getDomainIsActive());
			crawlerConfigurationScope.put((String) "domainPriority", (int) getDomainPriority());
			logger.debug("the domain "+getDomain()+" is active " + getDomainIsActiveAsString() + " and has priority "+ getDomainPriority());
			
			
			// second step is to get the customer
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+GeneralConfiguration.getCustomeridentifier()+"/"+GeneralConfiguration.getValueidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive information on customer using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) GeneralConfiguration.getCustomeridentifier(), (String) node.getTextContent());
				setCustomer((String) node.getTextContent());
			}
			// whether or not it is active
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+GeneralConfiguration.getCustomeridentifier()+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+getCustomer()+"']/isActive/"+GeneralConfiguration.getValueidentifier();
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
			expression = basicStructure + "/"+GeneralConfiguration.getDomainidentifier()+"[@"+GeneralConfiguration.getDomainnameidentifier()+"='"+getDomain()+"']/"+GeneralConfiguration.getCustomeridentifier()+"[@"+GeneralConfiguration.getCustomernameidentifier()+"='"+getCustomer()+"']/priority/"+GeneralConfiguration.getValueidentifier();
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
	private int getCustomerPriority() {
		return customerPriority;
	}
	private void setCustomerPriority(int i) {
		customerPriority = i;
	}

	private int getDomainPriority() {
		return domainPriority;
	}

	private void setDomainPriority(int i) {
		domainPriority = i;
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
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/"
										+GeneralConfiguration.getSingleconfigurationidentifier()
											+"[@"+GeneralConfiguration.getScopeidentifier()+"='"
												+GeneralConfiguration.getConfigFileTypeIdentifier()+"']/type";
										
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
