package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;

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
 * @version		0.1		- 14.07.2014
 * @status		productive but some functions are missing
 * 
 * @description	A configuration manager for the crawler using structured xml files for the configuration
 * 				and with the ability to configure individual crawler for different customers
 *
 * @param <T>
 * 
 * @changelog	0.1 (Chris)		copy of XMLFileConfigurationPersistence - Revision 0.5
 *  
 *  TODO 1. implement code for missing methods - see below
 */
public class XMLFileCustomerSpecificConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, SocialNetworks SN, String customer) {
		assert (category != "term" && category != "site" && category != "user" && category != "language" && category != "geoLocation")  : "ERROR :: can only accept term, site, user, language or geoLocation as category";
		assert (customer != "CortalConsors" && customer != "COMLINE") : "ERROR :: while in development, can only accept CortalConsors or COMLINE as customer identifier";
		
		return (ArrayList<T>) getDataFromXml(category, SN, customer);
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, SocialNetworks SN, String customer) {
		ArrayList<T> ar = new ArrayList<T>();
		logger.debug("reading constraints for "+customer+" on " + section + " for network " + SN.toString() + " from configuration file " + getConfigDbHandler());
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			NodeList nodeList = null;
			
			// first step is to get all constraints for all customers within a specified social network 
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"
					+ConfigurationConstants.singleConfigurationIdentifier+"/"
					+ConfigurationConstants.customerIdentifier
											+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+ConfigurationConstants.customerNameForAllIdentifier+"']/"
					+ConfigurationConstants.constraintIdentifier
											+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.scopeOnAllIdentifier+"']/"
					+ConfigurationConstants.singleConstraintIdentifier+"/"
					+section+"/"
					+ConfigurationConstants.valueIdentifier;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for all customers within a specified social network 
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"
					+ConfigurationConstants.singleConfigurationIdentifier+"/"
					+ConfigurationConstants.customerIdentifier
											+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+ConfigurationConstants.customerNameForAllIdentifier+"']/"
					+ConfigurationConstants.constraintIdentifier
											+"[@"+ConfigurationConstants.scopeIdentifier+"='"+SN+"']/"
					+ConfigurationConstants.singleConstraintIdentifier+"/"
					+section+"/"
					+ConfigurationConstants.valueIdentifier;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " ");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			// third step is to get all general constraints 
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"
								+ConfigurationConstants.singleConfigurationIdentifier+"/"
								+ConfigurationConstants.customerIdentifier
														+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+customer+"']/"
								+ConfigurationConstants.constraintIdentifier
														+"[@"+ConfigurationConstants.scopeIdentifier+"='"+ConfigurationConstants.scopeOnAllIdentifier+"']/"
								+ConfigurationConstants.singleConstraintIdentifier+"/"
								+section+"/"
								+ConfigurationConstants.valueIdentifier;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// fourth step is to get all constraints for the specified social network 
			expression = "/"+ConfigurationConstants.rootIdentifier+"/"
					+ConfigurationConstants.singleConfigurationIdentifier+"/"
					+ConfigurationConstants.customerIdentifier
											+"[@"+ConfigurationConstants.customerNameIdentifier+"='"+customer+"']/"
					+ConfigurationConstants.constraintIdentifier
											+"[@"+ConfigurationConstants.scopeIdentifier+"='"+SN+"']/"
					+ConfigurationConstants.singleConstraintIdentifier+"/"
					+section+"/"
					+ConfigurationConstants.valueIdentifier;
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
