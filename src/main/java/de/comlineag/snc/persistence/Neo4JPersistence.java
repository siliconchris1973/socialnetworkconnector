package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.HttpErrorMessages;
import de.comlineag.snc.constants.HttpStatusCodes;
import de.comlineag.snc.constants.GraphRelationshipTypes;
import de.comlineag.snc.constants.LithiumConstants;
import de.comlineag.snc.constants.LithiumStatusCode;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.data.GraphCustomerData;
import de.comlineag.snc.data.GraphDomainData;
import de.comlineag.snc.data.GraphKeywordData;
import de.comlineag.snc.data.GraphPostingData;
import de.comlineag.snc.data.GraphSocialNetworkData;
import de.comlineag.snc.data.GraphUserData;
import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;


/**
 *
 * @author 		Christian Guenther
 * @category 	Connector Class
 * @version 	0.8
 * @status		in development
 *
 * @description handles the connectivity to the Neo4J Graph Database and saves posts, 
 * 				users and connections in the graph. Implements IPersistenceManager
 *
 * @changelog	0.1 (Chris)		initial version as copy from HANAPersistence
 * 				0.2 			insert of post
 * 				0.3				insert of user
 * 				0.4				query for location
 * 				0.5				create relationship between nodes
 * 				0.6 			bugfixing and wrap up
 * 				0.7 			skeleton for graph traversal
 * 				0.7a			removed Base64Encryption (now in its own class)
 * 				0.7b			added support for different encryption provider, the actual one is set in applicationContext.xml
 * 				0.7c			changed id from Long to String 
 * 				0.8				rewrote class from scratch
 * 
 * 
 * the workflow is as follows:
 * 	1. if necessary, create social network
 * 		store reference to social network
 *  2. if necessary, create domain of interest
 *  	store reference to domain
 *  3.if necessary, create customer
 *  	store reference to customer
 *  4. check if the post is already in the graph, if not create it
 *  	store reference to post
 *  5. check if the user is already in the graph, if not create it
 *  	store reference to user
 *  6 if necessary, create keywords
 *  	store reference to keywords
 *  
 *  7. if necessary, create relationship from customer to domain
 *  	customer -BELONGS_TO-> domain
 *  8. create relationship from user to post
 *  	user -WROTE-> post
 *  9. if necessary, create relationship from post to user
 *  	post -MENTIONEND-> user
 *  10.create relationship from post to social network
 *  	post -POSTED_ID-> network
 *  11.create relationship from post to keyword
 *  	post -CONTAINS-> keyword
 * 
 */
public class Neo4JPersistence implements IGraphPersistenceManager {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	// this is a reference to the Neo4J configuration settings
	private final Neo4JConfiguration nco = Neo4JConfiguration.getInstance();
	
	private String dbServerUrl;
	private String nodePointUrl;
	// path to the configuration xml file
	public String configDb;
	
	
	
	public Neo4JPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
		nodePointUrl = dbServerUrl + nco.getNodeLoc();
	}
	
	
	@Override
	public void saveNode(JSONObject nodeObject, GraphNodeTypes label) {
		logger.info("creating a {} node object to store in the graph", SocialNetworks.getSocialNetworkConfigElementByCode("name", nodeObject.get("sn_id").toString()));
		logger.trace("    content: {}", nodeObject.toString());
		
		//GraphNodeTypes label = GraphNodeTypes.POST;
		
		if ("POST".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a post");
			GraphPostingData gpd = new GraphPostingData(nodeObject);
			createNodeObject(gpd.createCypher());
		} else if ("USER".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a user");
			GraphUserData gud = new GraphUserData(nodeObject);
			createNodeObject(gud.createCypher());
		} else if ("DOMAIN".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a domain");
			GraphDomainData gdd = new GraphDomainData(nodeObject);
			createNodeObject(gdd.createCypher());
		} else if ("CUSTOMER".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a customer");
			GraphCustomerData gcd = new GraphCustomerData(nodeObject);
			createNodeObject(gcd.createCypher());
		} else if ("KEYWORD".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a keyword");
			GraphKeywordData gkd = new GraphKeywordData(nodeObject);
			createNodeObject(gkd.createCypher());
		} else if ("SOCIALNETWORK".equalsIgnoreCase(label.getValue())){
			logger.debug("lets create a social network");
			GraphSocialNetworkData gsd = new GraphSocialNetworkData(nodeObject);
			createNodeObject(gsd.createCypher());
		} else {
			logger.warn("warning can't create an object with label {} in the graph, beacause I do not know what it is is", label.getValue());
		}
	}
	
	
	
	
	
	/**
	 * 
	 * @description	creates a node in the graph 
	 * @param 		cypherString
	 * 
	 */
	private void createNodeObject(String cypherString){
		try{
			dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
			nodePointUrl = dbServerUrl + "/db/data" + nco.getNodeLoc();
			String transactionalUrl = dbServerUrl + "/db/data" + "/transaction";
			String finalUrl = transactionalUrl;
			
			//String payload = "{\"CREATE\": [ {\"s\":" +cypherString + "}]}";
			String payload = "{\"statements\": [ {\"statement\": \"CREATE\" (p:" +cypherString + ") }]}";
			
			logger.trace("sending cypher {} to endpoint {}", payload, finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( cypherString )
			        .post( ClientResponse.class );
			
			String responseString = response.getEntity(String.class);
			
			logger.debug("POST to {} returned status code {}, returned data: {}",
					finalUrl, response.getStatus(),
			        responseString);
			
			// first check if the http code was ok
			HttpStatusCodes httpStatusCodes = HttpStatusCodes.getHttpStatusCode(response.getStatus());
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error("Error {} sending data to {}: {} ", response.getStatus(), finalUrl, HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				}
			} else {
				// now do the check on json details within the returned JSON object
				JSONParser reponseParser = new JSONParser();
				Object responseObj = reponseParser.parse(responseString);
				JSONObject jsonResponseObj = responseObj instanceof JSONObject ?(JSONObject) responseObj : null;
				if(jsonResponseObj == null)
					throw new ParseException(0, "returned json object is null");
				JSONArray jsonCommitArray = (JSONArray)jsonResponseObj.get("commit");
				JSONArray jsonErrorArray = (JSONArray)jsonResponseObj.get("errors");
				JSONArray jsonResultArray = (JSONArray)jsonResponseObj.get("results");
				logger.trace("returned commit json is {}", jsonCommitArray.toString());
				logger.trace("returned error json is {}", jsonErrorArray.toString());
				logger.trace("returned result json is {}", jsonResultArray.toString());
				
				JSONObject singleErrObject = new JSONObject();
				JSONObject singleCommitObject = new JSONObject();
				JSONObject singleResultObject = new JSONObject();
				
				
				final URI location = response.getLocation();
				logger.info("new node created at location {}", location);
				
				logger.debug("committing transaction at location {}", location);
				resource = Client.create().resource( location +"/commit" );
				response = resource
						.accept( MediaType.APPLICATION_JSON )
		                .type( MediaType.APPLICATION_JSON )
				        .post( ClientResponse.class );
				logger.debug("COMMIT returned status code {}, returned data: {}",
						response.getStatus(),
				        response.getEntity(String.class));
			}
			response.close();
			
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node with {} - {}",  cypherString, e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	@Override
	public URL findNode(String key, String value, String label) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI createRelationship(URI startNode, URI endNode,
			GraphRelationshipTypes relationshipType, String[] jsonAttributes) {
		try {
			URI location = null;
			URI fromUri = new URI( startNode.toString() + "/relationships" );
			String relationshipJson = generateJsonRelationship(endNode,
					relationshipType, jsonAttributes );
			
			WebResource resource = Client.create()
					.resource( fromUri );
			// POST JSON to the relationships URI
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
					.type( MediaType.APPLICATION_JSON )
					.entity( relationshipJson )
					.post( ClientResponse.class );
			
			logger.debug("POST to {} returned status code {}, returned data: {}",
					fromUri, response.getStatus(),
					response.getEntity(String.class));
			
			HttpStatusCodes httpStatusCodes = HttpStatusCodes.getHttpStatusCode(response.getStatus());
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error("Error {} sending data to {}: {} ", response.getStatus(), fromUri, HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				}
			} else {
				location = response.getLocation();
				logger.info("new relation created between {} and {} at location {}", startNode,  endNode, location);
			}
			response.close();
			return location;
		} catch (Exception e) {
			logger.error("could not create relationship between {} and {} - {}", startNode, endNode, e.getLocalizedMessage());
		}
		return null;
	}
	private static String generateJsonRelationship(URI endNode,
							GraphRelationshipTypes relationshipType, String[] jsonAttributes){
		StringBuilder sb = new StringBuilder();
		sb.append( "{ \"to\" : \"" );
		sb.append( endNode.toString() );
		sb.append( "\", " );
		sb.append( "\"type\" : \"" );
		sb.append( relationshipType.toString() );
		if ( jsonAttributes == null || jsonAttributes.length < 1 ){
			sb.append( "\"" );
		} else {
			sb.append( "\", \"data\" : " );
			for ( int i = 0; i < jsonAttributes.length; i++ ){
				sb.append( jsonAttributes[i] );
				if ( i < jsonAttributes.length - 1 ){
                	// Miss off the final comma
					sb.append( ", " );
				}
			}
		}
		sb.append( " }" );
		return sb.toString();
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
		logger.trace("The URL {} contains the node id {} from position {} till the end of the string"
						, nodeLocationUri, nodeIdAsString, pos);
		
		return Long.parseLong(nodeIdAsString);
	}
	

	/**
	 * @description deletes all graph-db files on file system, thus essentially wiping out the entire DB
	 */
	@SuppressWarnings("unused")
	private void clearDbPath() {
        try { deleteRecursively( new File( nco.getDbPath() ) ); }
        catch ( IOException e ) { throw new RuntimeException( e ); }
    }
	
	private void checkDatabaseIsRunning(String dbServerUrl){
		logger.debug("opening connection to server {}", dbServerUrl);
		WebResource resource = Client.create().resource(dbServerUrl);
		ClientResponse response = resource.get(ClientResponse.class);
		logger.debug("GET on {}, status code {}",dbServerUrl, response.getStatus());
		response.close();
    }
	
	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}
	
	// OLD METHODS
	/**
	 * @description create an edge between two nodes in the graph
	 * 				with the given attribute (relationshipType) 
	 *
	 * @param fromNodeId
	 * @param toNodeId
	 * @param relationshipType
	 * 
	 
	public void createRelationship(URI fromNodeUri, URI toNodeUri, GraphRelationshipTypes relationshipType, String additionalData){ 
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
	*/
	
	
	/**
	 * @description add label to a node
	 * 
	 
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
	*/
	
	/**
     * @description Adds property to a node whose url is passed
     * 
     * @param 		nodeURI 		- URI of node to which the property is to be added
     * @param 		propertyName 	- name of property which we want to add
     * @param 		propertyValue 	- Value of above property
    
    public void addProperty(String nodeURI,
                            String propertyName,
                            String propertyValue){
        String output = null;

        try{
            String nodePointUrl = nodeURI + Neo4JConstants.PROPERTY_LOC + propertyName;
            HttpClient client = new HttpClient();
            PutMethod mPut = new PutMethod(nodePointUrl);

            //set headers
            Header mtHeader = new Header();
            mtHeader.setName("content-type");
            mtHeader.setValue("application/json");
            mtHeader.setName("accept");
            mtHeader.setValue("application/json");
            mPut.addRequestHeader(mtHeader);

            // set json payload
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
    */
    
    /*
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
	*/
	
	
	/**
     * @description Performs a traversal from a source node
     * 
     * @param 		nodeURI
     * @param 		relationShip - relationship used for traversal
     * @return		
     
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


            // set headers
            Header mtHeader = new Header();
            mtHeader.setName("content-type");
            mtHeader.setValue("application/json");
            mtHeader.setName("accept");
            mtHeader.setValue("application/json");
            mPost.addRequestHeader(mtHeader);

            // set json payload
            StringRequestEntity requestEntity = new StringRequestEntity(t.toJson(),
                                                                        "application/json",
                                                                        "UTF-8");
            mPost.setRequestEntity(requestEntity);
            int status = client.executeMethod(mPost);
            output = mPost.getResponseBodyAsString( );
            mPost.releaseConnection( );
            logger.debug("status : " + status + " / output : " + output);
        }catch(Exception e){
             logger.error("Exception in creating node in neo4j : {}" + e);
        }

        return output;
    }
    */
    
	/**
	 * @description returns the server status
	 
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
*/
}
