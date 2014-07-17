package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.constants.ConfigurationConstants;
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
import org.w3c.dom.NodeList;

/**
 * @author		Christian Guenther
 * @category	Persistence manager
 * @version		0.6		- 17.07.2014
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
 *  
 *  TODO 1. implement code for missing methods
 */
public class SimpleXmlConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	private JSONParser parser = new JSONParser();
	private String SN = null;
	private Object obj = null;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, JSONObject configurationScope) {
		assert (category != "term" && category != "site" && category != "user" && category != "language" && category != "geoLocation")  : "ERROR :: can only accept term, site, user, language or geoLocation as category";
		
		// get configuration scope - that is doman and customer
		try {
			obj = parser.parse(configurationScope.toString());
			JSONObject jsonObject = (JSONObject) obj;
			SN = (String) jsonObject.get("SN_ID");
		} catch (ParseException e1) {
			logger.error("ERROR :: could not parse configurationScope jason " + e1.getLocalizedMessage());
		}
		
		logger.warn("no customer or domain specific configuration - consider using complex xml or db configuration manager");
		return (ArrayList<T>) getDataFromXml(category, SN);
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, String SN) {
		ArrayList<T> ar = new ArrayList<T>();
		logger.debug("reading constraints on " + section + " for network " + SN.toString() + " from configuration file " + getConfigDbHandler().substring(getConfigDbHandler().lastIndexOf("/")+1));
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// first step is to get all general constraints 
			String expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.scopeOnAllValue+"']/"+ConfigurationConstants.constraintIdentifier+"/"+section+"/"+ConfigurationConstants.valueIdentifier;
			NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for the specified social network 
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"+ConfigurationConstants.singleConfigurationIdentifier+"[@"+ConfigurationConstants.scopeIdentifier+"='"+SN+"']/"+ConfigurationConstants.constraintIdentifier+"/"+section+"/"+ConfigurationConstants.valueIdentifier;
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
}
