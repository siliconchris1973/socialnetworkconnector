package de.comlineag.snc.persistence;

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
import de.comlineag.snc.constants.SNCStatusCodes;


/**
 * 
 * @author 		Christina Guenther
 * @category	handler
 * @revision	0.1				- 08.11.2014
 * @status		in development
 * 
 * @description	this class is used to setup schema and db layout of the Neo4J Graph database
 * 
 * @changelog	0.1 (Chris) 	class created as copy from HANAConfiguration Version 0.3
 * 
 */
public final class Neo4JConfiguration {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// singleton design pattern using Initialization-on-demand holder idiom, 
	private static class Holder { static final Neo4JConfiguration instance = new Neo4JConfiguration(); }
    public static Neo4JConfiguration getInstance() { return Holder.instance; }
    
	
	private String neo4JConfigFile;
	
	private String PATH_TO_NODES = "nodes";
	private String SCHEMA_NAME = "CL_SAA";
	private String POSTS_LABEL = "posts";
	private String USERS_LABEL = "users";
	private String HASHTAG_LABEL = "hashtag";
	
	// extension to the URI of a node to store relationships
	private final String RELATIONSHIP_LOC = "/relationships";
	// extension of the URI of the graph db for cypher queries
	private final String CYPHERENDPOINT_LOC = "/cypher";
	// extension of the URI to a node
	private final String NODE_LOC = "/node";
	// extension of the URI for Properties
	private final String PROPERTY_LOC = "/properties";
	// extension to the URI for the label of a node
	private final String LABEL_LOC = "/labels";
	
	// if set SNC will automatically create connections between nodes
	// for example: if a new post and a new user is passed, SNC will automatically create the 
	// 				relationship of type authored between these two nodes.
	public static final boolean AUTO_CREATE_EDGE = true;
	
	// HANA column spaces and field length 
	private int POSTING_TEXT_SIZE = 5000;
	private int SUBJECT_TEXT_SIZE = 20;
	private int TEASER_TEXT_SIZE = 256;
	private int POSTLANG_TEXT_SIZE = 64;
	private int LONGITUDE_TEXT_SIZE = 40;
	private int LATITUDE_TEXT_SIZE = 40;
	private int CLIENT_TEXT_SIZE = 2048;
	private int INREPLYTO_TEXT_SIZE = 20;
	private int INREPLYTOSCREENNAME_TEXT_SIZE = 128;
	private int PLACEID_TEXT_SIZE = 16;
	private int PLNAME_TEXT_SIZE = 256;
	private int PLCOUNTRY_TEXT_SIZE = 128;
	
	private int SNID_TEXT_SIZE = 2;
	private int USERID_TEXT_SIZE = 20;
	private int USERNAME_TEXT_SIZE = 128;
	private int USERNICKNAME_TEXT_SIZE = 128;
	private int USERLANG_TEXT_SIZE = 64;
	private int USERLOCATION_TEXT_SIZE = 1024;
	
	
	// convenience variables to make the code easier to read and reduce number of calls to RuntimeConfiguration
	private final String configurationsKey = rtc.getStringValue("RootIdentifier", "XmlLayout");
	private final String configurationKey = rtc.getStringValue("SingleConfigurationIdentifier", "XmlLayout");
	private final String scopeKey = rtc.getStringValue("ScopeIdentifier", "XmlLayout");
	private final String paramKey = rtc.getStringValue("ParamIdentifier", "XmlLayout");
	private final String valueKey = rtc.getStringValue("ValueIdentifier", "XmlLayout");
	private final String nameKey = rtc.getStringValue("NameIdentifier", "XmlLayout");
	
	private final boolean isStopOnConfigurationError = rtc.getBooleanValue("StopOnConfigurationFailure", "runtime");
	
	private Neo4JConfiguration(){
		String baseExpression = "/"+configurationsKey+"/"+configurationKey+"[@"+scopeKey+"=\"DbLayout\"]/"+paramKey;
		
		// first set the location of the HANA Configuration file from RuntimeConfiguration
		setConfigFile(rtc.getNeo4JConfigFilePath());
		
		try {
			//File file = new File(hanaConfigFile);
			File file = new File(getConfigFile());
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			String expression = null;
			Node node = null; 
			
			
			// set the path to the nodes
			expression = baseExpression + "[@"+nameKey+"=\"PathToNodes\"]/"+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + neo4JConfigFile + " using expression " + expression);
				if (isStopOnConfigurationError)
					System.exit(SNCStatusCodes.ERROR.getErrorCode());
			} else {
				setPathToNodes(node.getTextContent());
			}
			
			// set the path to the nodes
			expression = baseExpression + "[@"+nameKey+"=\"SchemaName\"]/"+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + neo4JConfigFile + " using expression " + expression);
				if (isStopOnConfigurationError)
					System.exit(SNCStatusCodes.ERROR.getErrorCode());
			} else {
				setSchemaName(node.getTextContent());
			}
			
			// set the path to the nodes
			expression = baseExpression + "[@"+nameKey+"=\"PostsLabel\"]/"+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + neo4JConfigFile + " using expression " + expression);
				if (isStopOnConfigurationError)
					System.exit(SNCStatusCodes.ERROR.getErrorCode());
			} else {
				setPostsLabel(node.getTextContent());
			}

			// set the path to the nodes
			expression = baseExpression + "[@"+nameKey+"=\"UsersLabel\"]/"+valueKey;
			node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
			if (node == null) {
				logger.error("Did not receive any information from " + neo4JConfigFile + " using expression " + expression);
				if (isStopOnConfigurationError)
					System.exit(SNCStatusCodes.ERROR.getErrorCode());
			} else {
				setUsersLabel(node.getTextContent());
			}
		} catch (IOException e) {
			logger.error("EXCEPTION :: error reading configuration file " + e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error("EXCEPTION :: unforseen error " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	// getter and setter
	public String getConfigFile() {return neo4JConfigFile;}
	public void setConfigFile(String configFile) {neo4JConfigFile = configFile;}
	public String getPathToNodes() {return PATH_TO_NODES;}
	public void setPathToNodes(String path_to_nodes) {PATH_TO_NODES = path_to_nodes;}
	public String getSchemaName() {return SCHEMA_NAME;}
	public void setSchemaName(String schema_name) {SCHEMA_NAME = schema_name;}
	public String getPostsLabel() {return POSTS_LABEL;}
	public void setPostsLabel(String posts_label) {POSTS_LABEL = posts_label;}
	public String getUsersLabel() {return USERS_LABEL;}
	public void setUsersLabel(String users_label) {USERS_LABEL = users_label;}
	public String getHashtagLabel() {return HASHTAG_LABEL;}
	public void setHashtagLabel(String hashtag_label) {HASHTAG_LABEL = hashtag_label;}
	
	
	
	public int getPOSTING_TEXT_SIZE() {return POSTING_TEXT_SIZE;}
	public void setPOSTING_TEXT_SIZE(int pOSTING_TEXT_SIZE) {POSTING_TEXT_SIZE = pOSTING_TEXT_SIZE;}
	public int getSUBJECT_TEXT_SIZE() {return SUBJECT_TEXT_SIZE;}
	public void setSUBJECT_TEXT_SIZE(int sUBJECT_TEXT_SIZE) {SUBJECT_TEXT_SIZE = sUBJECT_TEXT_SIZE;}
	public int getTEASER_TEXT_SIZE() {return TEASER_TEXT_SIZE;}
	public void setTEASER_TEXT_SIZE(int tEASER_TEXT_SIZE) {TEASER_TEXT_SIZE = tEASER_TEXT_SIZE;}
	public int getPOSTLANG_TEXT_SIZE() {return POSTLANG_TEXT_SIZE;}
	public void setPOSTLANG_TEXT_SIZE(int pOSTLANG_TEXT_SIZE) {POSTLANG_TEXT_SIZE = pOSTLANG_TEXT_SIZE;}
	public int getLONGITUDE_TEXT_SIZE() {return LONGITUDE_TEXT_SIZE;}
	public void setLONGITUDE_TEXT_SIZE(int lONGITUDE_TEXT_SIZE) {LONGITUDE_TEXT_SIZE = lONGITUDE_TEXT_SIZE;}
	public int getLATITUDE_TEXT_SIZE() {return LATITUDE_TEXT_SIZE;}
	public void setLATITUDE_TEXT_SIZE(int lATITUDE_TEXT_SIZE) {LATITUDE_TEXT_SIZE = lATITUDE_TEXT_SIZE;}
	public int getCLIENT_TEXT_SIZE() {return CLIENT_TEXT_SIZE;}
	public void setCLIENT_TEXT_SIZE(int cLIENT_TEXT_SIZE) {CLIENT_TEXT_SIZE = cLIENT_TEXT_SIZE;}
	public int getINREPLYTO_TEXT_SIZE() {return INREPLYTO_TEXT_SIZE;}
	public void setINREPLYTO_TEXT_SIZE(int iNREPLYTO_TEXT_SIZE) {INREPLYTO_TEXT_SIZE = iNREPLYTO_TEXT_SIZE;}
	public int getINREPLYTOSCREENNAME_TEXT_SIZE() {return INREPLYTOSCREENNAME_TEXT_SIZE;}
	public void setINREPLYTOSCREENNAME_TEXT_SIZE(int iNREPLYTOSCREENNAME_TEXT_SIZE) {INREPLYTOSCREENNAME_TEXT_SIZE = iNREPLYTOSCREENNAME_TEXT_SIZE;}
	public int getPLACEID_TEXT_SIZE() {return PLACEID_TEXT_SIZE;}
	public void setPLACEID_TEXT_SIZE(int pLACEID_TEXT_SIZE) {PLACEID_TEXT_SIZE = pLACEID_TEXT_SIZE;}
	public int getPLNAME_TEXT_SIZE() {return PLNAME_TEXT_SIZE;}
	public void setPLNAME_TEXT_SIZE(int pLNAME_TEXT_SIZE) {PLNAME_TEXT_SIZE = pLNAME_TEXT_SIZE;}
	public int getPLCOUNTRY_TEXT_SIZE() {return PLCOUNTRY_TEXT_SIZE;}
	public void setPLCOUNTRY_TEXT_SIZE(int pLCOUNTRY_TEXT_SIZE) {PLCOUNTRY_TEXT_SIZE = pLCOUNTRY_TEXT_SIZE;}
	public int getUSERID_TEXT_SIZE() {return USERID_TEXT_SIZE;}
	public void setUSERID_TEXT_SIZE(int uSERID_TEXT_SIZE) {USERID_TEXT_SIZE = uSERID_TEXT_SIZE;}
	public int getSNID_TEXT_SIZE() {return SNID_TEXT_SIZE;}
	public void setSNID_TEXT_SIZE(int sNID_TEXT_SIZE) {SNID_TEXT_SIZE = sNID_TEXT_SIZE;}
	public int getUSERNAME_TEXT_SIZE() {return USERNAME_TEXT_SIZE;}
	public void setUSERNAME_TEXT_SIZE(int uSERNAME_TEXT_SIZE) {USERNAME_TEXT_SIZE = uSERNAME_TEXT_SIZE;}
	public int getUSERNICKNAME_TEXT_SIZE() {return USERNICKNAME_TEXT_SIZE;}
	public void setUSERNICKNAME_TEXT_SIZE(int uSERNICKNAME_TEXT_SIZE) {USERNICKNAME_TEXT_SIZE = uSERNICKNAME_TEXT_SIZE;}
	public int getUSERLANG_TEXT_SIZE() {return USERLANG_TEXT_SIZE;}
	public void setUSERLANG_TEXT_SIZE(int uSERLANG_TEXT_SIZE) {USERLANG_TEXT_SIZE = uSERLANG_TEXT_SIZE;}
	public int getUSERLOCATION_TEXT_SIZE() {return USERLOCATION_TEXT_SIZE;}
	public void setUSERLOCATION_TEXT_SIZE(int uSERLOCATION_TEXT_SIZE) {USERLOCATION_TEXT_SIZE = uSERLOCATION_TEXT_SIZE;}
}
