package de.comlineag.snc.constants;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.comlineag.snc.appstate.RuntimeConfiguration;

/**
 * 
 * @author 		Christian Guenther
 * @category 	data type
 * @version 	0.8b			- 14.10.2014
 * @status		productive
 * 
 * @description provides access to all defined social networks.
 * 				SocialNetworks acts like a dynamic constant in such that it can be extended by 
 * 				appending new social networks to the file SocialNetworkDefinitions.xml. As a 
 * 				side effect, there is no real reason to instantiate the class but instead you 
 * 				can just call the access method to get information on a social network and it 
 * 				will fetch the requested data from the definition file.
 * 
 * @changelog	0.1 (Chris)		enum created with unknown, twitter, facebook, google+ and linkedin
 * 				0.2 			added xing
 * 				0.3 			added instagram, foursquare and streamwork
 * 				0.4 			added lithium
 * 				0.5 			added youtube and finanzforum
 * 				0.6 			added ALL as an indicator for xml configuration for all networks
 * 				0.7				moved definition of the networks to RuntimeConfiguration.xml and made enum a class
 * 				0.8				moved SocialNetwork definitions to their own file and adapted query for file
 * 								and deleted deprecated enum
 * 				0.8a			changed exit code to SNCStatusCodes
 * 				0.8b			changed access to runtime configuration to non-static
 * 
 */
public final class SocialNetworks {
	// this holds a reference to the runtime configuration
	private static final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private static final Logger logger = LoggerFactory.getLogger(SocialNetworks.class);
	
	private static final String configurationsKey = rtc.getStringValue("RootIdentifier", "XmlLayout");
	private static final String configurationKey = rtc.getStringValue("SingleConfigurationIdentifier", "XmlLayout");
	private static final String scopeKey = rtc.getStringValue("ScopeIdentifier", "XmlLayout");
	private static final String socNetConfKey = rtc.getStringValue("SocialNetworkConfiguration", "XmlLayout");
	private static final String socNetKey = rtc.getStringValue("SocialNetworkIdentifier", "XmlLayout");
	private static final String socNetNameKey = rtc.getStringValue("SocialNetworkNameIdentifier", "XmlLayout");
	private static final String nameKey = rtc.getStringValue("NameIdentifier", "XmlLayout");
	
	// make the constructor private so that it can only be called from inside the class itself, thus preventing 
	// the initialization from someplace (more important unmanaged place) else
	private SocialNetworks() {}
	
	/**
	 * @description	retrieves a configuration element for a given social network referenced by it's name
	 * 
	 * @param 		key   	xml-element to retrieve 
	 * 						can be  code, name, description, domain or supported
	 * 				snname	name of the social network in uppper case
	 * @return		element (e.g. spoken name of the social network) or null in case of error
	 * 
	 * @TODO check if we can eliminate the XML parsing for every call of the method
	 * 		 and instead retrieve the name from an internal structure which is 
	 * 		 setup for every social network during first instantiation of this class - maybe
	 * 		 during startup of the snc via a call from RuntimeConfiguration class  
	 */
	public static String getSocialNetworkConfigElement(String key, String snname){
		assert ("localePattern".equals(key) && "locale".equals(key) && "code".equals(key) && "name".equals(key) && "description".equals(key) && "domain".equals(key) && "supported".equals(key)) : "ERROR :: can only accept code, name, description, domain, locale and localePattern or supported as key";
		
		try {
			File file = new File(getConfigFile());
			
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
			expression = "/"+configurationsKey+"/" 
							+configurationKey 
								+"[@"+scopeKey+"='"+socNetConfKey+"']/" 
							+socNetKey 
								+"[@"+socNetNameKey+"='"+snname+"']/"
							+key;
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any node information on "+key+" for social network "+snname+" from " + rtc.getSocialNetworkFilePath() + " using expression " + expression);
				return null;
			} else {
				return node.getTextContent();
			}
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious!");
			if (rtc.getBooleanValue("STOP_SNC_ON_CONFIGURATION_FAILURE", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious!");
			e.printStackTrace();
			if (rtc.getBooleanValue("STOP_SNC_ON_CONFIGURATION_FAILURE", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		}
		return null;
	}
	
	/**
	 * @description	retrieves a configuration element for a given social network reference by it's id
	 * 				internally this method retrievs the name of the social network via it's id and then
	 * 				calls getSocialNetworkConfigElement(key, snname)
	 * 
	 * @param 		key   	xml-element to retrieve 
	 * 						can be  code, name, description, domain or supported
	 * 				snname	name of the social network in uppper case
	 * @return		element (e.g. spoken name of the social network) or null in case of error
	 * 
	 */
	public static String getSocialNetworkConfigElementByCode(String key, String _snId) {
		try {
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null;
			
			// get an element from the xml file
			// structure of xml
			// 	<network name="AL">
			//		<name>ALL</name>
			//  </network>
			expression = "/"+configurationsKey+"/" 
							+configurationKey 
								+"[@"+scopeKey+"='"+socNetConfKey+"']/" 
							+socNetKey 
								+"[@"+socNetNameKey+"='"+_snId+"']/"
							+nameKey;
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any node information on "+key+" for social network "+_snId+" from " + rtc.getSocialNetworkFilePath() + " using expression " + expression);
				return null;
			} else {
				return getSocialNetworkConfigElement(key, node.getTextContent());
			}
			
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage() + ". This is serious!");
			if (rtc.getBooleanValue("STOP_SNC_ON_CONFIGURATION_FAILURE", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage() + ". This is serious!");
			e.printStackTrace();
			if (rtc.getBooleanValue("STOP_SNC_ON_CONFIGURATION_FAILURE", "runtime"))
				System.exit(SNCStatusCodes.CRITICAL.getErrorCode());
		}
		return null;
	}
	
	// the path to the xml file containing the social network definitions 
	private static String getConfigFile() {return rtc.getSocialNetworkFilePath();}
}

