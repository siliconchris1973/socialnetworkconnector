package de.comlineag.snc.crawler;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





import de.comlineag.snc.appstate.RuntimeConfiguration;

/**
 * 
 * @author 		Magnus Leinemann, Christian Guenther
 * @category 	Job
 * @version		0.2
 * @status		productive but new method whatAmI not usable
 * 
 * @description abstract definition for a crawler to be executed by the job control
 * 
 * @changelog	0.1 (Magnus)	class created
 * 				0.2	(Chris)		added method whatAmI returning json object with information about the crawler
 * 
 */
public abstract class GenericCrawler implements Job {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private static String CRAWLER_NAME;
	private long postsTracked;
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// convenience variables to make the code easier to read and reduce number of calls to RuntimeConfiguration
	private final String configurationsKey = rtc.getStringValue("RootIdentifier", "XmlLayout");
	private final String configurationKey = rtc.getStringValue("SingleConfigurationIdentifier", "XmlLayout");
	private final String scopeKey = rtc.getStringValue("ScopeIdentifier", "XmlLayout");
	private final String socialNetworkKey = rtc.getStringValue("SocialNetworkConfiguration", "XmlLayout");
	private final String socialNetworkNameKey = rtc.getStringValue("SocialNetworkNameIdentifier", "XmlLayout");
	
	
	
	
	public GenericCrawler() {
		super();
	}

	// every crawler must implement a method execute
	public abstract void execute(JobExecutionContext arg0) throws JobExecutionException;
	
	
	/**
	 * 
	 * @description	returns a JSON Object containing information about the network this crawler is used for
	 * @return		JSONObject
	 * 					{	
	 * 						"networkNameUpperCase":"TWITTER",
	 * 						"networkName":"Twitter",
	 * 						"networkCode":"TW",
	 * 						"description":"Twitter - a short message service",
	 * 						"domain":"https://www.twitter.com",
	 * 						"supported":"YES"
	 * 					}
	 * 
	 * XML Structure from SocialNetworkDefinition.xml
	 *					<configuration scope="socialNetworkDefinition">
	 *						<network name="TW"><name>TWITTER</name></network>
	 *						<network name="TWITTER">
	 *							<code>TW</code>
	 *							<name>Twitter</name>
	 *							<description>Twitter - a short message service</description>
	 *							<domain>https://www.twitter.com</domain>
	 *							<supported>YES</supported>
	 *						</network>
	 *					</configuration>
	 *
	 */
	@SuppressWarnings("unchecked")
	public JSONObject whatAmI() throws ParseException{
		JSONObject obj = new JSONObject();
		
		obj.put((String)"networkNameUpperCase", (String)CRAWLER_NAME);
		
		try {
			File file = new File(rtc.getRuntimeConfigFilePath());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			 
			//String expressionStatic = "/configurations/configuration[@scope='socialNetworkDefinition']/network[@name='"+CRAWLER_NAME+"']/";
			expression = "/"+configurationsKey
						+"/"+configurationKey
						+"[@"+scopeKey+"='"+socialNetworkKey+"']"
						+"/network"
						+"[@"+socialNetworkNameKey + "='"+CRAWLER_NAME+"']"
						+"/name";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			obj.put((String)"networkName", (String)node.getTextContent());
			
			expression = "/"+configurationsKey
						+"/"+configurationKey
						+"[@"+scopeKey+"='"+socialNetworkKey+"']"
						+"/network"
						+"[@"+socialNetworkNameKey + "='"+CRAWLER_NAME+"']"
						+"/code";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			obj.put((String)"networkCode", (String)node.getTextContent());
			
			expression = "/"+configurationsKey
						+"/"+configurationKey
						+"[@"+scopeKey+"='"+socialNetworkKey+"']"
						+"/network"
						+"[@"+socialNetworkNameKey + "='"+CRAWLER_NAME+"']"
						+"/description";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			obj.put((String)"description", (String)node.getTextContent());
			
			expression = "/"+configurationsKey
						+"/"+configurationKey
						+"[@"+scopeKey+"='"+socialNetworkKey+"']"
						+"/network"
						+"[@"+socialNetworkNameKey + "='"+CRAWLER_NAME+"']"
						+"/domain";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			obj.put((String)"domain", (String)node.getTextContent());
			
			expression = "/"+configurationsKey
						+"/"+configurationKey
						+"[@"+scopeKey+"='"+socialNetworkKey+"']"
						+"/network"
						+"[@"+socialNetworkNameKey + "='"+CRAWLER_NAME+"']"
						+"/supported";
			
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			obj.put((String)"supported", (String)node.getTextContent());
			
			
		} catch (Exception e) {
			logger.error("unforseen error");
		}
		
		logger.trace("constructed json object is " + obj.toString());
		return obj;
	}
	
	public static String getCrawlerName() {return CRAWLER_NAME;}
	
	// exposes the number of messages, posts or pages tracked by each crawler
	public long getPostsTracked() {	return postsTracked;}
	protected void setPostsTracked(long postsTracked) {this.postsTracked = postsTracked;}
}