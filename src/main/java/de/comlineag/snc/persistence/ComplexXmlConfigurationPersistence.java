package de.comlineag.snc.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;

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
 * @version		0.4				- 31.10.2014
 * @status		productive but some functions are missing
 * 
 * @description	A configuration manager for the crawler using structured xml files for the configuration
 * 				and with the ability to configure individual crawler for different domains
 *
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		copy of XMLFileCustomerSpecificConfigurationPersistence - Revision 0.2
 * 				0.2				added parts from RuntimeConfiguration (domain and customer)
 * 				0.3				Added support for getRunState
 * 				0.4				adapted to the changes made to RuntimeConfiguration Version 1.0
 *  
 */
public class ComplexXmlConfigurationPersistence<T> implements IConfigurationManager<T>  {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// the path to the configuration file
	private String configDbHandler;
	private JSONParser parser = new JSONParser();
	private String SN = null;
	// the configuration file to use. This can either be the one passed from applicationContext.xml 
	// from section configuration manager or one that was provided (overridden) when calling the
	// configuration manager
	private String CONF_FILE = null; 
	private Object obj = null;
	
	private String domain = null;
	private String customer = null;
	private boolean domainIsActive = false;
	private boolean customerIsActive = false;
	private int domainPriority = 0;
	private int customerPriority = 0;
	private JSONObject crawlerConfigurationScope = new JSONObject();
	

	// convenience variables to make the code easier to read and reduce number of calls to RuntimeConfiguration
	private final String configurationsKey = rtc.getStringValue("RootIdentifier", "XmlLayout");
	private final String configurationKey = rtc.getStringValue("SingleConfigurationIdentifier", "XmlLayout");
	private final String scopeKey = rtc.getStringValue("ScopeIdentifier", "XmlLayout");
	private final String domainKey = rtc.getStringValue("DomainIdentifier", "XmlLayout");
	private final String domainNameKey = rtc.getStringValue("DomainNameIdentifier", "XmlLayout");
	private final String domainStructureKey = rtc.getStringValue("DomainStructureIdentifier", "XmlLayout");
	private final String allDomainsKey = rtc.getStringValue("DomainNameForAllValue", "XmlLayout");
	private final String customerKey = rtc.getStringValue("CustomerIdentifier", "XmlLayout");
	private final String customerNameKey = rtc.getStringValue("CustomerNameIdentifier", "XmlLayout");
	private final String allCustomerKey = rtc.getStringValue("CustomerNameForAllValue", "XmlLayout");
	private final String constraintsKey = rtc.getStringValue("ConstraintIdentifier", "XmlLayout");
	private final String scopeOnAllKey = rtc.getStringValue("ScopeOnAllvalue", "XmlLayout");
	private final String singleConstraintKey = rtc.getStringValue("SingleConstraintIdentifier", "XmlLayout");
	private final String valueKey = rtc.getStringValue("ValueIdentifier", "XmlLayout");
	private final String configFileTypeKey = rtc.getStringValue("ConfigFileTypeIdentifier", "XmlLayout");
	private final String crawlerRunKey = rtc.getStringValue("CrawlerRunIdentifier", "XmlLayout");
	
	private final boolean rtcStopOnConfigurationFailure = rtc.getBooleanValue("StopOnConfigurationFailure", "runtime");
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getCrawlerConfigurationScope() {
		logger.info("setting up crawler configuration scope");
		
		try {
			File file = new File(getConfigDbHandler());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			String basicStructure = "/"+configurationsKey+"/"
										+configurationKey
											+"[@"+scopeKey+"='"+domainStructureKey+"']/"
										+domainStructureKey;
			
			// first step is to get the domain
			expression = basicStructure + "/"+domainKey+"/"+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive information on the domain using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) domainKey, (String) node.getTextContent());
				setDomain((String) node.getTextContent());
			}
			
			
			// whether or not it is active
			expression = basicStructure + "/"+domainKey
											+"[@"+domainNameKey+"='"+getDomain()+"']"
										+"/isActive/"
										+valueKey;
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
			expression = basicStructure + "/"+domainKey
												+"[@"+domainNameKey+"='"+getDomain()+"']"
											+"/priority/"
											+valueKey;
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
			expression = basicStructure + 
							"/"+domainKey
								+"[@"+domainNameKey+"='"+getDomain()+"']/"
							+customerKey+"/"
							+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive information on customer using expression " + expression);
			} else {
				crawlerConfigurationScope.put((String) customerKey, (String) node.getTextContent());
				setCustomer((String) node.getTextContent());
			}
			
			// whether or not it is active
			expression = basicStructure + "/"+domainKey
											+"[@"+domainNameKey+"='"+getDomain()+"']/"
										+customerKey
											+"[@"+customerNameKey+"='"+getCustomer()+"']"
										+ "/isActive/"
										+valueKey;
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
			expression = basicStructure + "/"+domainKey
												+"[@"+domainNameKey+"='"+getDomain()+"']/"
											+customerKey
												+"[@"+customerNameKey+"='"+getCustomer()+"']"
											+ "/priority/"
											+valueKey;
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
			
		} catch (java.io.FileNotFoundException e) {
			logger.error("ERROR :: file "+getConfigDbHandler()+" not found.");
			e.printStackTrace();
			if (rtcStopOnConfigurationFailure)
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		} catch (Exception e) {
			logger.error("ERROR :: could not get crawler configuration " + e.getLocalizedMessage());
		}
		return crawlerConfigurationScope;
	}
	
	
	@Override
	public boolean getRunState(String socialNetwork) {
		String expression = null;
		Node node = null;
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			expression = "/"+configurationsKey+"/"
							+configurationKey
								+"[@"+scopeKey+"='"+crawlerRunKey+"']/"
							+"crawler"
								+"[@name='"+socialNetwork+"']";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.trace("CrawlerRun for network "+socialNetwork+" could not be found - returning true");
				return true;
			} else {
				logger.trace("CrawlerRun for network "+socialNetwork+" is set to " + node.getTextContent());
				
				// only and only if there actually is a false, we return false. 
				if ("false".equals(node.getTextContent()))
					return false;
			}
		} catch (java.io.FileNotFoundException e) {
			logger.error("ERROR :: file "+getConfigDbHandler()+" not found.");
			e.printStackTrace();
			if (rtcStopOnConfigurationFailure)
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		} catch (Exception e) {
			logger.warn("WARNING :: could not parse configuration file "+getConfigDbHandler()+" using expression "+expression+" - returning true.");
			e.printStackTrace();
			
			return true;
		}
		
		// in any other circumstance, we assume the crawler shall run and return true
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, String SN) {
		
		ArrayList<T> ar = new ArrayList<T>();
		logger.debug("reading " + section + "-constraints for customer "+customer+" in domain "+domain+" for network " + SocialNetworks.getSocialNetworkConfigElement("name", SN) + " from configuration file " + getConfigDbHandler().substring(getConfigDbHandler().lastIndexOf("/")+1));
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			NodeList nodeList = null;
			
			// first step is to get all constraints for all domains without a specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
								+"[@"+scopeKey+"='"+domainKey+"']/"
							+domainKey
								+"[@"+domainNameKey+"='"+allDomainsKey+"']/"
							+constraintsKey
								+"[@"+scopeKey+"='"+scopeOnAllKey+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for all domains within a specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+domainKey+"']/"
							+domainKey
							+"[@"+domainNameKey+"='"+allDomainsKey+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+SN+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// third step is to get all general constraints 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+domainKey+"']/"
							+domainKey
							+"[@"+domainNameKey+"='"+domain+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+scopeOnAllKey+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// fourth step is to get all constraints for the specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+domainKey+"']/"
							+domainKey
							+"[@"+domainNameKey+"='"+domain+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+SN+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			
			
			
			// AND NOW FOR CUSTOMER
			// first step is to get all constraints for all domains within a specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+customerKey+"']/"
							+customerKey
							+"[@"+customerNameKey+"='"+allCustomerKey+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+scopeOnAllKey+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for all customers within a specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+customerKey+"']/"
							+customerKey
							+"[@"+customerNameKey+"='"+allCustomerKey+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+SN+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// third step is to get all general constraints 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+customerKey+"']/"
							+customerKey
							+"[@"+customerNameKey+"='"+customer+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+scopeOnAllKey+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// fourth step is to get all constraints for the specified social network 
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+customerKey+"']/"
							+customerKey
							+"[@"+customerNameKey+"='"+customer+"']/"
							+constraintsKey
							+"[@"+scopeKey+"='"+SN+"']/"
							+singleConstraintKey+"/"
							+section+"/"
							+valueKey;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			//logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			logger.debug("found " + ar.size() + " constraints on " + section);
			logger.trace("    " + ar.toString());
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious!");
			if (rtc.getBooleanValue("StopOnConfigurationFailure", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious!");
			e.printStackTrace();
			if (rtc.getBooleanValue("StopOnConfigurationFailure", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		}

        return (ArrayList<T>) ar;
	}
	
	
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		assert (   !"term".equals(category) 	 && !"blockedterm".equals(category) 
				&& !"site".equals(category) 	 && !"blockedsite".equals(category) 
				&& !"language".equals(category)  && !"blockedlanguage".equals(category)
				&& !"location".equals(category)  && !"blockedlocation".equals(category)
				&& !"user".equals(category) 	 && !"blockeduser".equals(category) 
				&& !"dnsdomain".equals(category) && !"blockeddnsdomain".equals(category)
				&& !"board".equals(category) 	 && !"blockedboard".equals(category)
				&& !"blog".equals(category) 	 && !"blockedblog".equals(category)
				)  : "ERROR :: can only accept term, site, user, language, location, blog, board or the equivalent blocked as category";
		
		if (	   !"term".equals(category) 	 && !"blockedterm".equals(category) 
				&& !"site".equals(category) 	 && !"blockedsite".equals(category) 
				&& !"language".equals(category)  && !"blockedlanguage".equals(category)
				&& !"location".equals(category)  && !"blockedlocation".equals(category)
				&& !"user".equals(category) 	 && !"blockeduser".equals(category) 
				&& !"dnsdomain".equals(category) && !"blockeddnsdomain".equals(category)
				&& !"board".equals(category) 	 && !"blockedboard".equals(category)
				&& !"blog".equals(category) 	 && !"blockedblog".equals(category)
				) 
			logger.warn("received "+category+" as category, but can only process term, site, user, language, location, blog, board or the equivalent blocked");
		
		// get configuration scope
		try {
			obj = parser.parse(configurationScope.toString());
			JSONObject jsonObject = (JSONObject) obj;
			SN = (String) jsonObject.get("SN_ID");
			CONF_FILE = (String) jsonObject.get("configDbHandler");
		} catch (ParseException e1) {
			logger.error("ERROR :: could not parse configurationScope json " + e1.getLocalizedMessage());
		}
		
		// in case a specific configuration file was given within the configurationScope object, set it here
		if ("___CRAWLER_CONFIGURATION___".equals(CONF_FILE)) {
			logger.debug("configuration file from configDbHandler overriden with " + CONF_FILE);
			setConfigDbHandler(CONF_FILE);
		}
		// first check, if the correct configuration file type was specified and if not, bail out the hard way
		if (!isConfigFileCorrect()){
			if (rtc.getBooleanValue("StopOnConfigurationFailure", "runtime"))
				System.exit(SNCStatusCodes.ERROR.getErrorCode());
		}
		
		return (ArrayList<T>) getDataFromXml(category, SN);
	}
	
	@Override
	public String getConfigurationElement(String key, String path) {
		// TODO implement code to retrieve one single configuration element
		logger.warn("The method getConfigurationElement is currently not implemented for configuration type xml-file");
		return null;
	}
	@Override
	public void setConfigurationElement(String key, String value, String path) {
		// TODO implement code to update a single configuration element 
		logger.warn("The method setConfigurationElement is currently not implemented for configuration type xml-file");
	}
	@Override
	public void writeNewConfiguration(String xml) {
		// TODO implement method to create new configuration-file
		logger.warn("The method writeNewConfiguration is not yet implemented for configuration type xml-file");
	}
	
	
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {return rtc.returnQualifiedConfigPath(this.configDbHandler);}
	public void setConfigDbHandler(String configDb) {this.configDbHandler = configDb;}
	
	// getter and setter for domain and customer
	@Override
	public String getDomain() {	return domain;}
	@Override
	public String getCustomer() {return customer;}
	@Override
	public void setDomain(String domain) {this.domain = domain;}
	@Override
	public void setCustomer(String customer) {this.customer = customer;}
	
	public int getCustomerPriority() {return customerPriority;}
	private void setCustomerPriority(int i) {customerPriority = i;}
	public int getDomainPriority() {return domainPriority;}
	private void setDomainPriority(int i) {domainPriority = i;}
	public boolean getDomainIsActive(){return domainIsActive;}
	private void setDomainIsActive(boolean isActive){domainIsActive = isActive;}
	public boolean getCustomerIsActive(){return customerIsActive;}
	private void setCustomerIsActive(boolean isActive){customerIsActive = isActive;}
	public String getDomainIsActiveAsString(){if (domainIsActive) return "true"; else return "false"; }
	//private void setDomainIsActive(String isActive){if ("isActive".equals("true")) domainIsActive = true; else domainIsActive = false; }
	public String getCustomerIsActiveAsString(){if (customerIsActive) return "true"; else return "false";}
	//private void setCustomerIsActive(String isActive){if ("isActive".equals("true"))	customerIsActive = true; else customerIsActive = false;}
	
	
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
			expression = "/"+configurationsKey+"/"
							+configurationKey
							+"[@"+scopeKey+"='"+configFileTypeKey+"']/"
							+"type";
										
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information using expression " + expression);
				return false;
			} else {
				if (this.getClass().getSimpleName().equals(node.getTextContent())) {
					logger.trace("provided xml configuration file is of type " + node.getTextContent());
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
