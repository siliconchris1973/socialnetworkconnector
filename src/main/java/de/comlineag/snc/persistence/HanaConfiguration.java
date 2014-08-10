package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * 
 * @author 		Christina Guenther
 * @category	handler
 * @revision	0.1			- 21.07.2014
 * @status		productive
 * 
 * @description	this class is used to setup schema and db layout of the SAP HANA DB
 * 
 * @changelog	0.1 (Chris) 	class created as copy from GeneralConfiguration Version 0.3
 *
 */
public final class HanaConfiguration {
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private static String configFile = "src/main/webapp/WEB-INF/HANA_Configuration.xml";
	
	private static String PATH_TO_TABLES = "comline.saa.data.tables";
	private static String SCHEMA_NAME = "CL_SAA";
	private static String POSTS_TABLE = "posts";
	private static String USERS_TABLE = "users";
	
	// HANA column spaces and field length 
	private static int POSTING_TEXT_SIZE = 5000;
	private static int SUBJECT_TEXT_SIZE = 20;
	private static int TEASER_TEXT_SIZE = 256;
	private static int POSTLANG_TEXT_SIZE = 64;
	private static int LONGITUDE_TEXT_SIZE = 40;
	private static int LATITUDE_TEXT_SIZE = 40;
	private static int CLIENT_TEXT_SIZE = 2048;
	private static int INREPLYTO_TEXT_SIZE = 20;
	private static int INREPLYTOSCREENNAME_TEXT_SIZE = 128;
	private static int PLACEID_TEXT_SIZE = 16;
	private static int PLNAME_TEXT_SIZE = 256;
	private static int PLCOUNTRY_TEXT_SIZE = 128;
	
	private static int SNID_TEXT_SIZE = 2;
	private static int USERID_TEXT_SIZE = 20;
	private static int USERNAME_TEXT_SIZE = 128;
	private static int USERNICKNAME_TEXT_SIZE = 128;
	private static int USERLANG_TEXT_SIZE = 64;
	private static int USERLOCATION_TEXT_SIZE = 1024;
	
	
	public HanaConfiguration(){
		try {
			
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			
			// set boolean values of runtime environment
			expression = "/configurations/configuration[@scope=\"DbLayout\"]/param[@name=\"PathToTables\"]";
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
				System.exit(-1);
			} else {
				setPATH_TO_TABLES(node.getTextContent());
			}
			expression = "/configurations/configuration[@scope=\"DbLayout\"]/param[@name=\"SchemaName\"]";
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
				System.exit(-1);
			} else {
				setSCHEMA_NAME(node.getTextContent());
			}
			expression = "/configurations/configuration[@scope=\"DbLayout\"]/param[@name=\"PostsTable\"]";
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
				System.exit(-1);
			} else {
				setPOSTS_TABLE(node.getTextContent());
			}
			expression = "/configurations/configuration[@scope=\"DbLayout\"]/param[@name=\"UsersTable\"]";
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + configFile + " using expression " + expression);
				System.exit(-1);
			} else {
				setUSERS_TABLE(node.getTextContent());
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
	
	// getter and setter for the configuration path
	public static String getConfigFile() {
		return HanaConfiguration.configFile;
	}
	public static void setConfigFile(String configFile) {
		HanaConfiguration.configFile = configFile;
	}
	
	public static String getPATH_TO_TABLES() {
		return PATH_TO_TABLES;
	}
	public static void setPATH_TO_TABLES(String pATH_TO_TABLES) {
		PATH_TO_TABLES = pATH_TO_TABLES;
	}
	public static String getSCHEMA_NAME() {
		return SCHEMA_NAME;
	}
	public static void setSCHEMA_NAME(String sCHEMA_NAME) {
		SCHEMA_NAME = sCHEMA_NAME;
	}
	public static String getPOSTS_TABLE() {
		return POSTS_TABLE;
	}
	public static void setPOSTS_TABLE(String pOSTS_TABLE) {
		POSTS_TABLE = pOSTS_TABLE;
	}
	public static String getUSERS_TABLE() {
		return USERS_TABLE;
	}
	public static void setUSERS_TABLE(String uSERS_TABLE) {
		USERS_TABLE = uSERS_TABLE;
	}

	public static int getPOSTING_TEXT_SIZE() {
		return POSTING_TEXT_SIZE;
	}

	public static void setPOSTING_TEXT_SIZE(int pOSTING_TEXT_SIZE) {
		POSTING_TEXT_SIZE = pOSTING_TEXT_SIZE;
	}

	public static int getSUBJECT_TEXT_SIZE() {
		return SUBJECT_TEXT_SIZE;
	}

	public static void setSUBJECT_TEXT_SIZE(int sUBJECT_TEXT_SIZE) {
		SUBJECT_TEXT_SIZE = sUBJECT_TEXT_SIZE;
	}

	public static int getTEASER_TEXT_SIZE() {
		return TEASER_TEXT_SIZE;
	}

	public static void setTEASER_TEXT_SIZE(int tEASER_TEXT_SIZE) {
		TEASER_TEXT_SIZE = tEASER_TEXT_SIZE;
	}

	public static int getPOSTLANG_TEXT_SIZE() {
		return POSTLANG_TEXT_SIZE;
	}

	public static void setPOSTLANG_TEXT_SIZE(int pOSTLANG_TEXT_SIZE) {
		POSTLANG_TEXT_SIZE = pOSTLANG_TEXT_SIZE;
	}

	public static int getLONGITUDE_TEXT_SIZE() {
		return LONGITUDE_TEXT_SIZE;
	}

	public static void setLONGITUDE_TEXT_SIZE(int lONGITUDE_TEXT_SIZE) {
		LONGITUDE_TEXT_SIZE = lONGITUDE_TEXT_SIZE;
	}

	public static int getLATITUDE_TEXT_SIZE() {
		return LATITUDE_TEXT_SIZE;
	}

	public static void setLATITUDE_TEXT_SIZE(int lATITUDE_TEXT_SIZE) {
		LATITUDE_TEXT_SIZE = lATITUDE_TEXT_SIZE;
	}

	public static int getCLIENT_TEXT_SIZE() {
		return CLIENT_TEXT_SIZE;
	}

	public static void setCLIENT_TEXT_SIZE(int cLIENT_TEXT_SIZE) {
		CLIENT_TEXT_SIZE = cLIENT_TEXT_SIZE;
	}

	public static int getINREPLYTO_TEXT_SIZE() {
		return INREPLYTO_TEXT_SIZE;
	}

	public static void setINREPLYTO_TEXT_SIZE(int iNREPLYTO_TEXT_SIZE) {
		INREPLYTO_TEXT_SIZE = iNREPLYTO_TEXT_SIZE;
	}

	public static int getINREPLYTOSCREENNAME_TEXT_SIZE() {
		return INREPLYTOSCREENNAME_TEXT_SIZE;
	}

	public static void setINREPLYTOSCREENNAME_TEXT_SIZE(
			int iNREPLYTOSCREENNAME_TEXT_SIZE) {
		INREPLYTOSCREENNAME_TEXT_SIZE = iNREPLYTOSCREENNAME_TEXT_SIZE;
	}

	public static int getPLACEID_TEXT_SIZE() {
		return PLACEID_TEXT_SIZE;
	}

	public static void setPLACEID_TEXT_SIZE(int pLACEID_TEXT_SIZE) {
		PLACEID_TEXT_SIZE = pLACEID_TEXT_SIZE;
	}

	public static int getPLNAME_TEXT_SIZE() {
		return PLNAME_TEXT_SIZE;
	}

	public static void setPLNAME_TEXT_SIZE(int pLNAME_TEXT_SIZE) {
		PLNAME_TEXT_SIZE = pLNAME_TEXT_SIZE;
	}

	public static int getPLCOUNTRY_TEXT_SIZE() {
		return PLCOUNTRY_TEXT_SIZE;
	}

	public static void setPLCOUNTRY_TEXT_SIZE(int pLCOUNTRY_TEXT_SIZE) {
		PLCOUNTRY_TEXT_SIZE = pLCOUNTRY_TEXT_SIZE;
	}

	public static int getUSERID_TEXT_SIZE() {
		return USERID_TEXT_SIZE;
	}

	public static void setUSERID_TEXT_SIZE(int uSERID_TEXT_SIZE) {
		USERID_TEXT_SIZE = uSERID_TEXT_SIZE;
	}

	public static int getSNID_TEXT_SIZE() {
		return SNID_TEXT_SIZE;
	}

	public static void setSNID_TEXT_SIZE(int sNID_TEXT_SIZE) {
		SNID_TEXT_SIZE = sNID_TEXT_SIZE;
	}

	public static int getUSERNAME_TEXT_SIZE() {
		return USERNAME_TEXT_SIZE;
	}

	public static void setUSERNAME_TEXT_SIZE(int uSERNAME_TEXT_SIZE) {
		USERNAME_TEXT_SIZE = uSERNAME_TEXT_SIZE;
	}

	public static int getUSERNICKNAME_TEXT_SIZE() {
		return USERNICKNAME_TEXT_SIZE;
	}

	public static void setUSERNICKNAME_TEXT_SIZE(int uSERNICKNAME_TEXT_SIZE) {
		USERNICKNAME_TEXT_SIZE = uSERNICKNAME_TEXT_SIZE;
	}

	public static int getUSERLANG_TEXT_SIZE() {
		return USERLANG_TEXT_SIZE;
	}

	public static void setUSERLANG_TEXT_SIZE(int uSERLANG_TEXT_SIZE) {
		USERLANG_TEXT_SIZE = uSERLANG_TEXT_SIZE;
	}

	public static int getUSERLOCATION_TEXT_SIZE() {
		return USERLOCATION_TEXT_SIZE;
	}

	public static void setUSERLOCATION_TEXT_SIZE(int uSERLOCATION_TEXT_SIZE) {
		USERLOCATION_TEXT_SIZE = uSERLOCATION_TEXT_SIZE;
	}
}
