package de.comlineag.snc.constants;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.comlineag.snc.appstate.GeneralConfiguration;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
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
 * 				0.7				moved definition of the networks to GeneralConfiguration.xml and made enum a class
 * 
 */
public final class SocialNetworks {
	
	// we use simple org.apache.log4j.Logger for logging
	private static Logger logger = Logger.getLogger("de.comlineag.snc.SocialNetworks");
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());

	private static String configFile = "src/main/webapp/WEB-INF/GeneralConfiguration.xml";
	
	
	// a (hidden) class variable whos type is of its own class - used to see, if we are already instantiated
	private static SocialNetworks instance;
	// make the constructor private so that it can only be called from inside the class itself, thus preventing 
	// the initialization from someplace (more important unmanaged place) else
	private SocialNetworks() {
		ParseSocialNetworkDefinition();
	}
	
	/**
	 * @description	a caller method on class level which instantiates the 
	 * 				object exactly ones and then returns it.
	 * 				by 'synchronized' we make sure that this method can 
	 * 				only be executed by one thread at a time so that the 
	 * 				first thread really creates the object while a possible 
	 * 				second thread always uses a completely instantiated object
	 * 
	 * @return		instance of SocialNetworks
	 */
	public static synchronized SocialNetworks getInstance () {
		if (SocialNetworks.instance == null) {
			SocialNetworks.instance = new SocialNetworks ();
		}
		return SocialNetworks.instance;
	}
	
	
	
	/**
	 * @description	retrieves the corresponding name for a given social network
	 * 
	 * @param 		key   	xml-element to retrieve 
	 * 						can be  code, name, description, domain or supported
	 * 				snname	name of the social network in uppper case
	 * @return		element (e.g. spoken name of the social network) or null in case of error
	 * 
	 * @TODO check if we can eliminate the XML parsing for every call of the method
	 * 		 and instead retrieve the name from an internal structure which is 
	 * 		 setup for every social network during first instantiation of this class - maybe
	 * 		 during startup of the snc via a call from GeneralConfiguration class  
	 */
	public static String getSocialNetworkConfigElement(String key, String snname){
		assert ("code".equals(key) && "name".equals(key) && "description".equals(key) && "domain".equals(key) && "supported".equals(key)) : "ERROR :: can only accept code, name, description, domain or supported as key";
		
		try {
			File file = new File(GeneralConfiguration.getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null;
			
			// get an element from the xml file
			// structure of xml
			//	<network name="TWITTER">
			//		<code>TW</code>
			//		<name>Twitter</name>
			//		<description>Twitter</description>
			//		<domain>twitter.com</domain>
			//		<supported>YES</supported>
			//	</network>
			// /configurations/configuration[@scope=socialNetworkDefinition]/network[@name='TWITTER']/code
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/" +
						GeneralConfiguration.getSingleconfigurationidentifier() +
							"[@"+GeneralConfiguration.getScopeidentifier()+"='"+GeneralConfiguration.getSocialNetworkConfiguration()+"']/" + 
						GeneralConfiguration.getSocialNetworkIdentifier() + 
							"[@"+GeneralConfiguration.getSocialNetworkName()+"='"+snname+"']/"+
						key;
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any node information on "+key+" for social network "+snname+" from " + GeneralConfiguration.getConfigFile() + " using expression " + expression);
				return null;
			} else {
				return node.getTextContent();
			}
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	
	
	
	/**
	 * @description	parses the xml configuration file and sets up the internal structure of SocialNetworks
	 * 				so that it can be used like an enum for the class
	 * 
	 * @TODO write the function code :-) 
	 */
	public static void ParseSocialNetworkDefinition(){
		try {
			logger.debug("retrieving all social network definitions from configuration file " + GeneralConfiguration.getConfigFile());
			
			File file = new File(GeneralConfiguration.getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null;
			NodeList nodeList = null;
			
			// get a list of all social networks from XML file 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/" +
						GeneralConfiguration.getSingleconfigurationidentifier() +
						"[@"+GeneralConfiguration.getScopeidentifier()+"="+GeneralConfiguration.getSocialNetworkConfiguration()+"]"; 
						//"network[@name='TWITTER']/"+GeneralConfiguration.getCodeidentifier();
			
			// work with the nodeset containing the social networks 
			nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			if (nodeList == null) {
				logger.error("Did not receive any nodeset information for social networks from " + GeneralConfiguration.getConfigFile() + " using expression " + expression);
			} else {
				logger.trace("working on nodeset with length "+nodeList.getLength()+"::");
				for (int i = 0; i < nodeList.getLength(); i++){
					// do something with the node item at position
					
					logger.trace("   my current childNode is " + nodeList.item(i).getNodeName() + " with " + nodeList.item(i).getNodeValue());
					// TODO make this xml parsing for social networks work
					for(Node childNode = node.getFirstChild(); childNode!=null;){
						Node nextChild = childNode.getNextSibling();
					    // Do something with childNode, including move or delete...
						logger.trace("   my current childNode is " + childNode.getNodeName() + " with " + childNode.getNodeValue());
						
					    childNode = nextChild;
					}
					
					// set the values
					//.(node.getTextContent());
				}
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
	
	/**
	 * @description	retrieves the corresponding 2-digit code for a given social network name
	 * 
	 * @param 		name (name of the social network in uppper case)
	 * @return		2-digit code of the social network or null in case of error
	 * 
	 * @TODO check if we can eliminate the XML parsing for every call of the method
	 * 		 and instead retrieve the 2-digit code from an internal structure which is 
	 * 		 setup for every social network during first instantiation of this class - maybe
	 * 		 during startup of the snc via a call from GeneralConfiguration class  
	
	public static String returnSNCode(String name){
		try {
			logger.trace("retrieving 2-digit code fro social network " + name);
			
			File file = new File(GeneralConfiguration.getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null;
			
			// get a list of all social networks from XML file 
			expression = "/"+GeneralConfiguration.getRootidentifier()+"/" +
						GeneralConfiguration.getSingleconfigurationidentifier() +
							"[@"+GeneralConfiguration.getScopeidentifier()+"="+GeneralConfiguration.getSocialNetworkConfiguration()+"]" +
						GeneralConfiguration.getSocialNetworkIdentifier() + 
							"[@"+GeneralConfiguration.getSocialNetworkName()+"='"+name+"']/"+
						GeneralConfiguration.getCodeidentifier();
			
			// work with the node containing the social networks 
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any node information for social network from " + GeneralConfiguration.getConfigFile() + " using expression " + expression);
				return null;
			} else {
				return node.getTextContent();
			}
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			System.exit(-1);
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious, I'm giving up!");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	 */
	
	
	/**
	 * 
	 * @author 		Christian Guenther
	 * @category 	enum
	 * @version 	0.7			- 28.07.2014
	 * @status		Deprecated
	 * 
	 * @description	contains an enumeration with shortcuts referencing the social networks
	 * 
	 * @Deprecated	Instead of using the enum, please make use of the class methods and
	 * 				setup all relevant social networks in General>Configuration.xml
	 */
	@Deprecated
	private static enum SocialNetworkDefs{
		//enum			value	Description
		TWITTER			("TW", "Twitter"), 
		FACEBOOK		("FB", "Facebook"), 
		GOOGLE			("GP", "Google+"), 
		LINKEDIN		("LI", "Linkedin"), 
		XING			("XI", "XING"), 
		STREAMWORK		("SW", "Streamwork"), 
		INSTAGRAM		("IN", "Instagram"),
		FOURSQUARE		("FS", "Foursquare"),
		STACKOVERFLOW	("SO", "Stack Overflow"),
		YOUTUBE			("YT", "Youtube"),
		ALL				("AL", "All networks"),
		UNKNOWN			("XY", "Unknown"),
		LITHIUM			("CC", "Lithium"),
		FINANZFORUM		("FF", "Finanzforum");
		
		private final String value;
		private final String type;
		
		// we use simple org.apache.log4j.Logger for lgging
		private final Logger logger = Logger.getLogger(getClass().getName());
		// in case you want a log-manager use this line and change the import above
		//private final Logger logger = LogManager.getLogger(getClass().getName());
		
		private SocialNetworkDefs(final String value, final String type) {
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
		
		public static SocialNetworkDefs getNetworkNameByValue(String value){
			for (SocialNetworkDefs name : SocialNetworkDefs.values()) {
				if(name.getValue() == value)
					return name;
			}
			return SocialNetworkDefs.UNKNOWN;
		}
		
		public static String getNetworkTypeByValue(String value){
			for (SocialNetworkDefs type : SocialNetworkDefs.values()) {
				if(type.getValue().equals(value))
					return type.getType();
			}
			return SocialNetworkDefs.UNKNOWN.getType();
		}
	}
}

