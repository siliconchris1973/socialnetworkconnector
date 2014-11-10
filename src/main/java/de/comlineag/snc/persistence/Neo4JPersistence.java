package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.HttpErrorMessages;
import de.comlineag.snc.constants.HttpStatusCodes;
import de.comlineag.snc.constants.Neo4JConstants;
import de.comlineag.snc.constants.GraphRelationshipTypes;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.neo4j.Relation;
import de.comlineag.snc.neo4j.TraversalDefinition;
import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;


/**
 *
 * @author 		Christian Guenther
 * @category 	Connector Class
 * @version 	0.7c
 * @status		in development
 *
 * @description handles the connectivity to the Neo4J Graph Database and saves posts, 
 * 				users and connections in the graph. Implements IPersistenceManager
 *
 * @changelog	0.1 (Chris)		initial version as copy from HANAPersistence
 * 				0.2 			insert of post
 * 				0.3				insert of user
 * 				0.4				query for geoLocation
 * 				0.5				create relationship between nodes
 * 				0.6 			bugfixing and wrap up
 * 				0.7 			skeleton for graph traversal
 * 				0.7a			removed Base64Encryption (now in its own class)
 * 				0.7b			added support for different encryption provider, the actual one is set in applicationContext.xml
 * 				0.7c			changed id from Long to String 
 * 
 * TODO implement code to check if a node already exists prior inserting one
 * TODO implement code for graph traversal
 * TODO check implementation of geo geoLocation
 */
public class Neo4JPersistence implements IGraphPersistenceManager {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	// this is a reference to the Neo4J configuration settings
	private final Neo4JConfiguration nco = Neo4JConfiguration.getInstance();
	
	private String dbServerUrl;
	private String nodePointUrl;
	
	// contains ID and position of a node within the graph - used as the target of a relationship (edge between nodes)
	private static String toNodeLocationUri;
	private static Long toNodeId;

	// contains ID and position of a node within the graph - used as the origin of a relationship (edge between nodes)
	private static String fromNodeLocationUri;
	private static Long fromNodeId;
	
	public String configDb;
		
	public Neo4JPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
		nodePointUrl = dbServerUrl + nco.getNodeLoc();
	}
	
	
	
	@Override
	public void saveNode(JSONObject nodeObject) {
		// we only store posts in german and english at the moment
		if (((String) nodeObject.get("Lang")).equalsIgnoreCase("de") || ((String) nodeObject.get("Lang")).equalsIgnoreCase("en")) {
			dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
			nodePointUrl = dbServerUrl + nco.getNodeLoc();
			
			logger.trace("saveNodefor id " + (String) nodeObject.get("Id") + " called - working with " + nodePointUrl);
			
			//prepareConnection();
			String output = null;
			HttpClient client = new HttpClient();
			PostMethod mPost = new PostMethod(nodePointUrl);
			
			// set header
			Header mtHeader = new Header();
			mtHeader.setName("content-type");
			mtHeader.setValue("application/json");
			mtHeader.setName("accept");
			mtHeader.setValue("application/json");
			mPost.addRequestHeader(mtHeader);
			
			
			// first step, save post itself
			logger.debug("Creating node for post (ID " + (String) nodeObject.get("Id") + " / text (first 20 chars) " + (String) nodeObject.get("text").toString().substring(0, 20) + ") at " + nodePointUrl);
			
			logger.trace("about to insert the following data in the graph: " + nodeObject.toString());
			
			try{
				StringRequestEntity requestEntity = new StringRequestEntity(nodeObject.toString(),
	                                                                        "application/json",
	                                                                        "UTF-8");
				
				mPost.setRequestEntity(requestEntity);
				int status = client.executeMethod(mPost);
				HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
				
				// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
				if (!statusCode.isOk()){
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.info("SUCCESS :: node for post " +  (String) nodeObject.get("Id") + " successfully created");
					
					output = mPost.getResponseBodyAsString();
					Header locationHeader =  mPost.getResponseHeader("geoLocation");
					
					toNodeLocationUri = locationHeader.getValue();
					toNodeId = getNodeIdFromLocation(toNodeLocationUri);
					logger.trace("locationHeader = " + locationHeader + " \n output = " + output);

					mPost.releaseConnection();
					addLabelToNode(toNodeLocationUri, "User");
				}
			} catch(Exception e) {
				logger.error("EXCEPTION :: failed to create node for post (ID " +  (String) nodeObject.get("Id")+ ") " + e.getMessage());
			}
		} // end if - check on languages
	}
	
	
	
	/**
	 * @description create an edge between two nodes in the graph
	 * 				with the given attribute (relationshipType) 
	 *
	 * @param fromNodeId
	 * @param toNodeId
	 * @param relationshipType
	 * 
	 */
	public void createRelationship(URI fromNodeUri, URI toNodeUri, GraphRelationshipTypes relationshipType, String additionalData){ //, UserData userData, PostingData postData){
		dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
		nodePointUrl = dbServerUrl + nco.getNodeLoc();
		
		logger.debug("Creating relationship from node " + fromNodeUri + " to node " + toNodeUri + " with relationship type " + relationshipType);
		
		//prepareConnection();
		String output = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(fromNodeUri + Neo4JConstants.RELATIONSHIP_LOC);

		// set header
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);
		
		// this cryptic string is passed along as part of a StringRequestEntity 
		String r = "{ "
				+ "\"to\" : \"" + toNodeUri + "\", "
				+ "\"type\" : \"" + relationshipType.toString() + "\" ";
				// some extra data, if needed 
				
				if (additionalData != null)
					r = r + "\"data\" : { \"since\" : \"" + additionalData + "\" }";
				
				r = r +" }";
		
		logger.trace("About to use the following string to create the relationship: " + r);
				
		StringRequestEntity requestEntity = null;
		try {
			requestEntity = new StringRequestEntity(r.toString(),
													"application/json",
													"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.error("EXCEPTION :: malformed StringRequestEntity " + e1.getMessage());
			e1.printStackTrace();
		}
		
		try {	
			mPost.setRequestEntity(requestEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
			
			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (!statusCode.isOk()){
				logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				logger.info("SUCCESS :: connection between node " + fromNodeId + " and node " + toNodeId + " created");
				
				output = mPost.getResponseBodyAsString();
				Header locationHeader =  mPost.getResponseHeader("location");
				mPost.releaseConnection( );
				
				logger.trace("status = " + status + " / location = " + locationHeader.getValue() + " \n output = " + output);
			}
		} catch(Exception e) {
			logger.error("EXCEPTION :: Creating connection of type " + relationshipType + " between node ( " + fromNodeUri + ") and node (" + toNodeUri + ")  " + e.getMessage());
		}
	}
	
	
	
	/**
	 * @description add label to a node
	 * 
	 */
	private void addLabelToNode(String nodeUri, String label){

		String output = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(nodeUri + Neo4JConstants.LABEL_LOC);

		// set header
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);
		
		// this cryptic string is passed along as part of a StringRequestEntity 
		String r = "{ Label : " + label + " }";
		
		logger.trace("About to use the following string to add the label " + label + " to the node " + nodeUri + Neo4JConstants.LABEL_LOC + ": " + r);
				
		StringRequestEntity requestEntity = null;
		try {
			requestEntity = new StringRequestEntity(r.toString(),
													"application/json",
													"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.error("EXCEPTION :: malformed StringRequestEntity " + e1.getMessage());
			e1.printStackTrace();
		}
		
		try {	
			mPost.setRequestEntity(requestEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
			
			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (!statusCode.isOk()){
				logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				output = mPost.getResponseBodyAsString();
				Header locationHeader =  mPost.getResponseHeader("geoLocation");
				mPost.releaseConnection( );
				
				logger.info("SUCCESS :: added label " + label + " to the node " + nodeUri + Neo4JConstants.LABEL_LOC);
				logger.trace("status = " + status + " / geoLocation = " + locationHeader.getValue() + " \n output = " + output);
			}
		} catch(Exception e) {
			logger.error("EXCEPTION :: adding label to the node: " + e.getMessage());
		}
	}
	
	/**
     * @description Adds property to a node whose url is passed
     * 
     * @param 		nodeURI 		- URI of node to which the property is to be added
     * @param 		propertyName 	- name of property which we want to add
     * @param 		propertyValue 	- Value of above property
     */
    public void addProperty(String nodeURI,
                            String propertyName,
                            String propertyValue){
        String output = null;

        try{
            String nodePointUrl = nodeURI + Neo4JConstants.PROPERTY_LOC + propertyName;
            HttpClient client = new HttpClient();
            PutMethod mPut = new PutMethod(nodePointUrl);

            /**
             * set headers
             */
            Header mtHeader = new Header();
            mtHeader.setName("content-type");
            mtHeader.setValue("application/json");
            mtHeader.setName("accept");
            mtHeader.setValue("application/json");
            mPut.addRequestHeader(mtHeader);

            /**
             * set json payload
             */
            String jsonString = "\"" + propertyValue + "\"";
            StringRequestEntity requestEntity = new StringRequestEntity(jsonString,
                                                                        "application/json",
                                                                        "UTF-8");
            mPut.setRequestEntity(requestEntity);
            int status = client.executeMethod(mPut);
            output = mPut.getResponseBodyAsString( );

            mPut.releaseConnection( );
            logger.trace("status = " + status + " / output = " + output);
			
        }catch(Exception e){
             logger.error("EXVCEPTION :: adding the property " + propertyName + " to the node: " + e);
        }
    }
    
    
    
    @Override
	public URL findNode(String key, String value, String label) {

		String output = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(nodePointUrl); // + CYPHERENDPOINT_LOC);

		// set header
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);
		
		// this cryptic string is passed along as part of a StringRequestEntity 
		String r = "{ \"query\" : "
				//+ "MATCH (n {" + field + " : " + id + ", type : \"" + type + "\" }) RETURN n"
				+ "MATCH (n:"+label+" {" + key + " : " + value + "}) RETURN n"
				//+ "\"params\" : {}"
				+" }";
		
		logger.trace("About to use the following string to query for the node: " + r);
				
		StringRequestEntity requestEntity = null;
		try {
			requestEntity = new StringRequestEntity(r.toString(),
													"application/json",
													"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.error("EXCEPTION :: malformed StringRequestEntity " + e1.getMessage());
			e1.printStackTrace();
		}
		
		try {	
			mPost.setRequestEntity(requestEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
			
			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (!statusCode.isOk()){
				logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				output = mPost.getResponseBodyAsString();
				Header locationHeader =  mPost.getResponseHeader("location");
				mPost.releaseConnection( );
				
				logger.debug("SUCCESS :: found the node at location " + locationHeader.getValue());
				logger.trace("status = " + status + " / location = " + locationHeader.getValue() + " \n output = " + output);
				
				return new URL(locationHeader.getValue().toString());
			}
		} catch(Exception e) {
			logger.error("EXCEPTION :: searching the graph for node " + e.getMessage());
		}
		
		return null;
	}
	
	
	/**
	 * @description receives a location URL and returns the ID
	 * 				the location URL MUST be of the form <protocol>://<servername>:<port>/<path>/<id>
	 * 				IMPORTTANT: the node-id must be the last part of the URL, otherwise an exception is thrown 
	 * @param toNodeLocationUri
	 * @return toNodeId
	 */
	private long getNodeIdFromLocation(String nodeLocationUri) {
		assert nodeLocationUri != null : "ERROR :: toNodeLocationUri must not be null";
		
		String nodeIdAsString = "";
		int pos = (nodeLocationUri.toString().lastIndexOf('/') + 1);
		
		nodeIdAsString = nodeLocationUri.toString().substring(pos);
		logger.trace("The URL " + nodeLocationUri + 
				" contains the node id " + nodeIdAsString + 
				" from position " + pos + " till the end of the string"
				);
		
		return Long.parseLong(nodeIdAsString);
	}
	
	
	
	/**
     * @description Performs a traversal from a source node
     * 
     * @param 		nodeURI
     * @param 		relationShip - relationship used for traversal
     * @return		
     */
    public String searchDatabase(String nodeURI, String relationShip){
        String output = null;

        try{
            TraversalDefinition t = new TraversalDefinition();
            t.setOrder( TraversalDefinition.DEPTH_FIRST );
            t.setUniqueness( TraversalDefinition.NODE );
            t.setMaxDepth( 10 );
            t.setReturnFilter( TraversalDefinition.ALL );
            t.setRelationships( new Relation( relationShip, Relation.OUT ) );
            
            logger.debug("Traversal is " + t.toString());
            HttpClient client = new HttpClient();
            PostMethod mPost = new PostMethod(nodeURI+"/traverse/node");


            /**
             * set headers
             */
            Header mtHeader = new Header();
            mtHeader.setName("content-type");
            mtHeader.setValue("application/json");
            mtHeader.setName("accept");
            mtHeader.setValue("application/json");
            mPost.addRequestHeader(mtHeader);

            /**
             * set json payload
             */
            StringRequestEntity requestEntity = new StringRequestEntity(t.toJson(),
                                                                        "application/json",
                                                                        "UTF-8");
            mPost.setRequestEntity(requestEntity);
            int status = client.executeMethod(mPost);
            output = mPost.getResponseBodyAsString( );
            mPost.releaseConnection( );
            logger.debug("status : " + status + " / output : " + output);
        }catch(Exception e){
             System.out.println("Exception in creating node in neo4j : " + e);
        }

        return output;
    }
    
    
	/**
	 * @description returns the server status
	 */
	public HttpStatusCodes getServerStatus(){
		dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
		nodePointUrl = dbServerUrl + nco.getNodeLoc();
		
		// initialize the return value and whether this is acceptable or not.
		HttpStatusCodes status = HttpStatusCodes.UNKNOWN;

        try{
	        HttpClient client = new HttpClient();
	        GetMethod mGet =   new GetMethod(dbServerUrl);
	        status = HttpStatusCodes.getHttpStatusCode(client.executeMethod(mGet));
	        
	        if (!status.isOk()){
	        	logger.warn(HttpErrorMessages.getHttpErrorText(status.getErrorCode()));
	        } else {
	        	logger.debug("Return code " + status + " (" + status.getErrorCode() + ") everything should be fine");
	        }
	        
	        mGet.releaseConnection();
	    }catch(Exception e){
	    	logger.error("EXCEPTION :: " + e.getLocalizedMessage());
	    }
        
	    return status;
	}

	
	/**
	 * @description deletes all graph-db files on file system, thus essentially wiping out the entire DB
	 */
	@SuppressWarnings("unused")
	private void clearDbPath() {
        try { deleteRecursively( new File( nco.getDbPath() ) ); }
        catch ( IOException e ) { throw new RuntimeException( e ); }
    }



	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}
}
