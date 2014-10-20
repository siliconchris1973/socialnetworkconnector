package de.comlineag.snc.parser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.parser.IWebParser;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1				- 06.10.2014
 * @status		in production
 * 
 * @description ParserControl is the generic caller class for each web parser. Whenever a web page
 * 				is crawled it is handed over to ParserControl. PC queries each registered parser
 * 				listed in properties/webparser.xml below WEB-INF directory) to determine if it can 
 * 				parse the page. Tpo achieve this, every registered parser must provide the method
 * 				canExecute() and return true (can be parsed) or false (can not be parsed) on calling.
 * 
 * 				To register a new web parser you have to create the class and enter it's details in 
 * 				the properties file (SNC_WebParser.properties). 
 * 				
 * 				PC is designed as a singleton class. It maintains a static reference to the lone 
 * 				singleton instance and returns that reference from the static getInstance() method. 
 * 				To achieve this we employ a technique known as lazy instantiation to create the 
 * 				singleton; as a result, the singleton instance is not created until the getInstance() 
 * 				method is called for the first time. This technique ensures that singleton instances 
 * 				are created only when needed.
 * 				 
 * 				Upon first call, PC will get all available parser from the webparser.xml file and create 
 * 				an ordered list of them. Every subsequent call will loop through this list querying the 
 * 				parser whether it can parse the page and, upon receiving true from the parser, hand the 
 * 				page, the original url and the list of track terms over to it. The loop works as a first-
 * 				match-wins decision - th efirst parser to return true on a page, will get the page.
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 * 
 */
public class ParserControl {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// we use simple org.apache.log4j.Logger for lgging
	private static final Logger logger = Logger.getLogger("de.comlineag.snc.parser.ParserControl");
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
		
	// the list of operational web parser as taken from the properties file is stored within this structure
	private List<IWebParser> webParser; 
	// the ParserControl instance - used during instantiation of the class and later to retrieve the list 
	private static ParserControl pc = null;
	
	// ParserControl is not to be directly instantiated by other classes
	private ParserControl() {
		try {
			webParser = getAllParser();
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parser execution " + e.getMessage());
			e.printStackTrace();
		}
	};
	
	
	// Static 'instance' method - this method is called every time
	// the submit method is called but can also be called implicitely to get
	// an instance of ParserControl
	public static ParserControl getInstance() 
			throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		if (pc == null) {pc = new ParserControl();}
		return pc;
	}
	
	
	/**
	 * 
	 * @description	submits the page to the actual parser - which parser is taken depends on the page
	 * 
	 * @param 		page 	- the web page as a String
	 * @param 		url		- the url the page is coming from 
	 * @param 		tTerms	- a list of track terms 
	 * @return 		List of simple web postings (a json structure with 1-n postings (or pages)
	 * 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws XPathExpressionException 
	 * @throws IOException 
	 * 
	 */
	public static List<SimpleWebPosting> submit(String page, URL url, ArrayList<String> tTerms) 
			throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
		pc = getInstance();
		
		Iterator<IWebParser> it = pc.webParser.iterator();
		while (it.hasNext()) {
		    IWebParser parser = it.next();
		    logger.trace("querying if parser " + parser.getClass().getSimpleName().toString() + " can operate on site " + url.toString());
		    if (parser.canExecute(page, url)) {
		    	logger.debug("executing parser " + parser.getClass().getSimpleName().toString());
		        return parser.parse(page, url, tTerms);
		    }
		}
		
		return null;
	}
	
	
	// retrieves all configured parser from the properties file and creates the parser list 
	private List<IWebParser> getAllParser()
			throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, 
					InstantiationException, IllegalAccessException, ClassNotFoundException, DOMException {
		logger.debug("building list of available web parser");
		
		String fileName = rtc.getWebParserListFilePath();
		List<IWebParser> ar = new ArrayList<IWebParser>();
		
		File file = new File(fileName);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		
		String expression = "//parser[@type='webparser']/value";
		NodeList nodeList= (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		for (int i = 0 ; i < nodeList.getLength() ; i++) {
			logger.trace("adding parser " + nodeList.item(i).getTextContent().toString() + " from configuration file " + fileName + " to list of avilable parser");
			ar.add((IWebParser)Class.forName(nodeList.item(i).getTextContent()).newInstance());
		}
		
		return ar;
	}
}
