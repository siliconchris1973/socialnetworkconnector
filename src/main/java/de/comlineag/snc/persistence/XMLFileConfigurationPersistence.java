package de.comlineag.snc.persistence;

import org.apache.log4j.Logger;

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
 * @version		0.5
 * @param <T>
 * 
 * @description	A configuration manager for the crawler using structured xml files for the configuration
 * 
 * @changelog	0.1 copy of IniFileConfigurationPersistence
 * 				0.2 added simple parsing of XML file with jsoup
 * 				0.3 changed XML parsing to XPath
 * 				0.4 added warnings to unimplemented methods
 * 				0.5 added support for generic type conversion on T
 *  
 */
public class XMLFileConfigurationPersistence<T> implements IConfigurationManager<T>  {
	
	// the path to the configuration file
	private String configDbHandler;
	final String scopeIdentifier = "scope";
	final String constraintIdentifier = "constraint";
	final String valueIdentifier = "value";
	final String rootIdentifier = "configurations";
	final String singleConfigurationIdentifier = "configuration";
	final String scopeOnAllIdentifier = "ALL";
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// general invocation for every constraint
	@Override
	public ArrayList<T> getConstraint(String category, SocialNetworks SN) {
		assert (category != "term" && category != "site" && category != "user" && category != "language" && category != "location")  : "ERROR :: can only accept term, site, user, language or location as category";
		
		logger.debug("reading constraints on " + category + " for network " + SN.toString());
		return (ArrayList<T>) getDataFromXml(category, SN);
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<T> getDataFromXml(String section, SocialNetworks SN) {
		ArrayList<T> ar = new ArrayList<T>();
		logger.trace("using configuration file " + getConfigDbHandler());
		
		
		try {
			File file = new File(getConfigDbHandler());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			// first step is to get all general constraints 
			String expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+scopeOnAllIdentifier+"']/"+constraintIdentifier+"/"+section+"/"+valueIdentifier;
			NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + ": \r");
			for (int i = 0 ; i < nodeList.getLength() ; i++) 
				ar.add((T) nodeList.item(i).getTextContent());
			
			// second step is to get all constraints for the specified social network 
			expression = "/"+rootIdentifier+"/"+singleConfigurationIdentifier+"[@"+scopeIdentifier+"='"+SN+"']/"+constraintIdentifier+"/"+section+"/"+valueIdentifier;
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			logger.trace("found " + nodeList.getLength() + " elements using expression " + expression + " - now iterating over them");
			for (int i = 0 ; i < nodeList.getLength() ; i++)
				ar.add((T) nodeList.item(i).getTextContent());
			
			logger.debug("    " + ar.toString());
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
		logger.warn("The method getConfigurationElement is not supported on configuration type xml-file");
		return null;
	}

	@Override
	public void setConfigurationElement(String key, String value, String path) {
		logger.warn("The method setConfigurationElement is not supported on configuration type xml-file");
	}
	
	@Override
	public void writeNewConfiguration(String xml) {
		// TODO implement writeNewConfiguration
		logger.warn("The method writeNewConfiguration is not yet implemented");
	}
	
	// getter and setter for the configuration path
	public String getConfigDbHandler() {
		return this.configDbHandler;
	}
	public void setConfigDbHandler(String configDbHandler) {
		this.configDbHandler = configDbHandler;
	}
}
