package de.comlineag.snc.constants;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.comlineag.snc.appstate.GeneralConfiguration;

/**
 * 
 * @author 		Christian Guenther
 * @category 	enum
 * @version 	0.7			- 28.07.2014
 * @status		productive
 * 
 * @description contains an enumeration with shortcuts referencing the social networks
 * 
 * @changelog	0.1 (Chris)		enum created with unknown, twitter, facebook, google+ and linkedin
 * 				0.2 			added xing
 * 				0.3 			added instagram, foursquare and streamwork
 * 				0.4 			added lithium
 * 				0.5 			added youtube and finanzforum
 * 				0.6 			added ALL as an indicator for xml configuration for all networks
 * 				0.7				moved definition of the networks to GeneralConfiguration.xml 
 * 
 * TODO 1 activate SN definition to come from GeneralConfiguration.xml
 */
public enum SocialNetworks {
	//enum		value	type
	ALL			("AL", "All networks"),
	UNKNOWN		("XY", "Unknown"),
	TWITTER		("TW", "Twitter"), 
	FACEBOOK	("FB", "Facebook"), 
	GOOGLE		("GP", "Google+"), 
	LINKEDIN	("LI", "Linkedin"), 
	XING		("XI", "XING"), 
	STREAMWORK	("SW", "Streamwork"), 
	INSTAGRAM	("IN", "Instagram"),
	FOURSQUARE	("FS", "Foursquare"),
	STACKOVERFLOW	("SO", "Stack Overflow"),
	LITHIUM		("CC", "Lithium"),
	YOUTUBE		("YT", "Youtube"),
	FINANZFORUM	("FF", "Finanzforum");
	
	private final String value;
	private final String type;
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private SocialNetworks(final String value, final String type) {
		this.value = value;
		this.type = type;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return getValue();
	}
	
	public static SocialNetworks getNetworkNameByValue(String value){
		for (SocialNetworks name : SocialNetworks.values()) {
			if(name.getValue() == value)
				return name;
		}
		return SocialNetworks.UNKNOWN;
	}
	
	public static String getNetworkTypeByValue(String value){
		for (SocialNetworks type : SocialNetworks.values()) {
			if(type.getValue().equals(value))
				return type.getType();
		}
		return SocialNetworks.UNKNOWN.getType();
	}
	
	// source social networks from XML Configuration file
	public void defineNetworks(){
		try {
			File file = new File(GeneralConfiguration.getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			// set text identifiers for the constraints from XML file 
			// TWITTER
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/" +
						GeneralConfiguration.getSingleconfigurationidentifier() +
						"[@"+GeneralConfiguration.getScopeidentifier()+"='socialNetworkDefinition']/" +
						"network[@name='TWITTER']/"+GeneralConfiguration.getCodeidentifier();
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.warn("Did not receive any information for TWITTER from " + GeneralConfiguration.getConfigFile() + " using expression " + expression);
			} else {
				// set the values
				//.(node.getTextContent());
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
}

