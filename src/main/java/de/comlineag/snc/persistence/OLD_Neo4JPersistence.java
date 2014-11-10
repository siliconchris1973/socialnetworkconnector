package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
import de.comlineag.snc.data.PostingData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
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
public class OLD_Neo4JPersistence implements IPersistenceManager {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	// this is a reference to the Neo4J configuration settings
	private final Neo4JConfiguration nco = Neo4JConfiguration.getInstance();
	
	
	// Servicelocation
	private static String host;
	private static String port;
	private static String protocol;
	private static String location;
	
	private static String dbServerUrl;
	private static String nodePointUrl;
	
	// file system path to the db
	private static String db_path = null;
	
	private static String serviceUserEndpoint;
	private static String servicePostEndpoint;
	// Credentials
	private static String user;
	private static String pass;

	// contains ID and position of a node within the graph - used as the target of a relationship (edge between nodes)
	private static String toNodeLocationUri;
	private static Long toNodeId;

	// contains ID and position of a node within the graph - used as the origin of a relationship (edge between nodes)
	private static String fromNodeLocationUri;
	private static Long fromNodeId;
	
		
	public OLD_Neo4JPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		dbServerUrl = OLD_Neo4JPersistence.protocol + "://" + OLD_Neo4JPersistence.host + ":" + OLD_Neo4JPersistence.port;
		nodePointUrl = dbServerUrl + OLD_Neo4JPersistence.location + "/node";
	}
	
	
	
	@Override
	public void savePosts(PostingData postingData) {
		// we only store posts in german and english at the moment
		if (postingData.getLang().equalsIgnoreCase("de") || postingData.getLang().equalsIgnoreCase("en")) {
			dbServerUrl = OLD_Neo4JPersistence.protocol + "://" + OLD_Neo4JPersistence.host + ":" + OLD_Neo4JPersistence.port;
			nodePointUrl = dbServerUrl + OLD_Neo4JPersistence.location + Neo4JConstants.NODE_LOC;

			logger.trace("savePosts for post-id " + postingData.getId() + " called - working with " + nodePointUrl);
			
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
			logger.debug("Creating node for post (ID " + postingData.getId() + " / text (first 20 chars) " + postingData.getText().substring(0, 20) + ") at " + nodePointUrl);
			
			// set json payload
			JSONObject p = new JSONObject();
			
			//p.put("type", SocialNetworkEntryTypes.POSTING.toString());	// --> Fixed value Posting -- will be done with a label
			p.put("sn_id", postingData.getSnId());								// Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"		--> Fixed value TW
			p.put("post_id", postingData.getId());								// Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"
			p.put("timestamp", "\"" + postingData.getTimestamp() + "\""); 		// Property Name="timestamp" Type="Edm.DateTime"
			p.put("postLang", postingData.getLang());							// Property Name="postLang" Type="Edm.String" MaxLength="64"
			p.put("text", postingData.getText().toString());					// Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="1024"
			p.put("truncated", postingData.getTruncated());						// Property Name="truncated" Type="Edm.Byte" DefaultValue="0"					--> true or false
			//p.put("client", postingData.getClient()); 							// OBSOLETE Name="client" Type="Edm.String" MaxLength="2048"
			
			// TODO check implementation of geo geoLocation
			if (postingData.getGeoLongitude() != null) 
				p.put("geoLocation_longitude", postingData.getGeoLongitude());	// Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"
			if (postingData.getGeoLatitude() != null)
				p.put("geoLocation_latitude", postingData.getGeoLatitude());	// Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"
			if (postingData.getGeoPlaceId() != null)
				p.put("placeID", postingData.getGeoPlaceId());					// Property Name="placeID" Type="Edm.String" MaxLength="16"
			if (postingData.getGeoPlaceName() != null)
				p.put("plName", postingData.getGeoPlaceName());				// Property Name="plName" Type="Edm.String" MaxLength="256"
			if (postingData.getGeoPlaceCountry() != null)
				p.put("plCountry", postingData.getGeoPlaceCountry());			// Property Name="plCountry" Type="Edm.String" MaxLength="128"
			if (postingData.getGeoAroundLatitude() != null)
				p.put("plAround_latitude", postingData.getGeoAroundLatitude());	// Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"
			if (postingData.getGeoAroundLongitude() != null)
				p.put("plAround_longitude", postingData.getGeoAroundLongitude());	// Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"
			
			
			//p.put("user_id", postingData.getUserId()); 						// CONNECTION Name="user_id" Type="AUTHORED"
			// the following data is NOT stored directly but rathe as a relationship between nodes
			p.put("inReplyTo", postingData.getInReplyTo()); 					// CONNECTION Name="inReplyTo" Type="REPLIED_ON"
			/*
			if (postData.getInReplyTo() <= 0) {
				// create a relationship between User   and Post           of type AUTHORED            with no additional data
				createRelationship(fromNodeLocationUri, toNodeLocationUri, GraphRelationshipTypes.IN_REPLY_TO_STATUS, null);
			}
			*/
			p.put("inReplyToUserID", postingData.getInReplyToUser()); 				// CONNECTION Name="inReplyToUserID" Type="REPLIED_TO"
			/*
			 if (postData.getInReplyToUser() <= 0) {
				// create a relationship between User   and Post           of type AUTHORED            with no additional data
				createRelationship(fromNodeLocationUri, toNodeLocationUri, GraphRelationshipTypes.IN_REPLY_TO_USER, null);
			}
			*/
			logger.trace("about to insert the following data in the graph: " + p.toString());
			
			try{
				StringRequestEntity requestEntity = new StringRequestEntity(p.toString(),
	                                                                        "application/json",
	                                                                        "UTF-8");
				
				mPost.setRequestEntity(requestEntity);
				int status = client.executeMethod(mPost);
				HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
				
				// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
				if (!statusCode.isOk()){
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.info("SUCCESS :: node for post " + postingData.getId() + " successfully created");
					
					output = mPost.getResponseBodyAsString();
					Header locationHeader =  mPost.getResponseHeader("geoLocation");
					
					toNodeLocationUri = locationHeader.getValue();
					toNodeId = getNodeIdFromLocation(toNodeLocationUri);
					logger.trace("locationHeader = " + locationHeader + " \n output = " + output);

					mPost.releaseConnection();
					addLabelToNode(toNodeLocationUri, "User");
				}
			} catch(Exception e) {
				logger.error("EXCEPTION :: failed to create node for post (ID " + postingData.getId() + ") " + e.getMessage());
			}
		} // end if - check on languages
	}
	
	
	@Override
	public void saveUsers(UserData userData) {
		dbServerUrl = OLD_Neo4JPersistence.protocol + "://" + OLD_Neo4JPersistence.host + ":" + OLD_Neo4JPersistence.port;
		nodePointUrl = dbServerUrl + OLD_Neo4JPersistence.location + Neo4JConstants.NODE_LOC;

		logger.trace("saveUsers called for user-id " + userData.getId() + " - working with " + nodePointUrl);
		
		// check if the user already exists and if so, DO NOT add him/her a second time, but only create a relationship
		fromNodeLocationUri = findNodeByIdAndLabel("user_id", userData.getId(), "User");
		logger.trace("the search for the user returned " + fromNodeLocationUri);
		fromNodeLocationUri = nodePointUrl + "1";
		fromNodeId = getNodeIdFromLocation(fromNodeLocationUri);
		
		if (fromNodeLocationUri == null) { 
			// user does NOT exist, create it
			//prepareConnection();
			String output = null;
			HttpClient client = new HttpClient();
			PostMethod mPost = new PostMethod(nodePointUrl);
	
			// set headers
			Header mtHeader = new Header();
			mtHeader.setName("content-type");
			mtHeader.setValue("application/json");
			mtHeader.setName("accept");
			mtHeader.setValue("application/json");
			mPost.addRequestHeader(mtHeader);
					
			logger.debug("Creating node for user (ID " + userData.getId() + " / screenname " + userData.getScreenName() + ") at " + nodePointUrl);
			
			// set json payload 
			JSONObject u = new JSONObject();
			
			//u.put("type", SocialNetworkEntryTypes.USER.toString());			// -- will be done with a label  
			u.put("sn_id", userData.getSnId());									// {name = "sn_id"; sqlType = NVARCHAR; nullable = false; length = 2;},
			u.put("user_id", userData.getId());									// {name = "user_id"; sqlType = NVARCHAR; nullable = false; length = 20;},
			u.put("userName", userData.getUsername());							// {name = "userName"; sqlType = NVARCHAR; nullable = true; length = 128;},
			u.put("nickName", userData.getScreenName());						// {name = "nickName"; sqlType = NVARCHAR; nullable = true; length = 128;},
			u.put("userLang", userData.getLang());								// {name = "userLang"; sqlType = NVARCHAR; nullable = true; length = 64;},
			u.put("geoLocation", userData.getGeoLocation());							// {name = "geoLocation"; sqlType = NVARCHAR; nullable = true; length = 1024;},
			u.put("follower", userData.getFollowersCount());					// {name = "follower"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			u.put("friends", userData.getFriendsCount());						// {name = "friends"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			u.put("postingsCount", userData.getPostingsCount());				// {name = "postingsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			u.put("favoritesCount", userData.getFavoritesCount());				// {name = "favoritesCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			u.put("listsAndGroupsCount", userData.getListsAndGroupsCount());	// {name = "listsAndGroupsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";}
			
			logger.trace("about to insert the following data in the graph: " + u.toString());
			
			try {
				StringRequestEntity requestEntity = new StringRequestEntity(u.toString(),
	                    													"application/json",
	                    													"UTF-8");
	
				mPost.setRequestEntity(requestEntity);
				int status = client.executeMethod(mPost);
				HttpStatusCodes statusCode = HttpStatusCodes.getHttpStatusCode(status);
				
				// in case everything is fine, neo4j should return 200. any other case needs to be investigated
				if (!statusCode.isOk()){
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.info("SUCCESS :: node for user " + userData.getScreenName() + " successfully created");
					
					output = mPost.getResponseBodyAsString( );
					Header locationHeader = mPost.getResponseHeader("geoLocation");
					
					fromNodeLocationUri = locationHeader.getValue();
					fromNodeId = getNodeIdFromLocation(fromNodeLocationUri);
					logger.trace("locationHeader = " + locationHeader + " \n output = " + output);
					
					mPost.releaseConnection();
					
					addLabelToNode(fromNodeLocationUri, "User");
				}
			} catch (Exception e) {
				logger.error("EXCEPTION :: failed to create node for user (ID " + userData.getId() + " / name " + userData.getScreenName() + ") " + e.getMessage());
			}
		}
		
		// now that we have a post and a user (either new or already stored) in the graph-db lets create the relationship
		if (Neo4JConstants.AUTO_CREATE_EDGE) 
			// create a relationship between User   and Post           of type AUTHORED            with no additional data
			createRelationship(fromNodeLocationUri, toNodeLocationUri, GraphRelationshipTypes.WROTE, null);
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
	private void createRelationship(String fromNodeUri, String toNodeUri, GraphRelationshipTypes relationshipType, String additionalData){ //, UserData userData, PostingData postData){
		dbServerUrl = OLD_Neo4JPersistence.protocol + "://" + OLD_Neo4JPersistence.host + ":" + OLD_Neo4JPersistence.port;
		
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
				Header locationHeader =  mPost.getResponseHeader("geoLocation");
				mPost.releaseConnection( );
				
				logger.trace("status = " + status + " / geoLocation = " + locationHeader.getValue() + " \n output = " + output);
			}
		} catch(Exception e) {
			logger.error("EXCEPTION :: Creating connection of type " + relationshipType + " between node ( " + fromNodeUri + ") and node (" + toNodeUri + ")  " + e.getMessage());
		}
	}
	
	/**
	 * @description receives a geoLocation URL and returns the ID
	 * 				the geoLocation URL MUST be of the form <protocol>://<servername>:<port>/<path>/<id>
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
	 * @description accepts a field name and an id (as long) and returns the nodeLocationUri (if this object exists) or null
	 * @param string
	 * @param id
	 * @return
	 */
	private String findNodeByIdAndLabel(String field, String id, String label) {
		
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
				+ "MATCH (n:"+label+" {" + field + " : " + id + "}) RETURN n"
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
				Header locationHeader =  mPost.getResponseHeader("geoLocation");
				mPost.releaseConnection( );
				
				logger.debug("SUCCESS :: found the node at geoLocation " + locationHeader.getValue());
				logger.trace("status = " + status + " / geoLocation = " + locationHeader.getValue() + " \n output = " + output);
				
				return locationHeader.getValue();
			}
		} catch(Exception e) {
			logger.error("EXCEPTION :: searching the graph for node " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * @description add label to a node
	 * 
	 */
	public void addLabelToNode(String nodeUri, String label){

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
    
    
    /**
     * @description adds property to a created relationship
     * @param relationshipUri
     * @param propertyName
     * @param propertyValue
     
    private void addPropertyToRelation( String relationshipUri,
                                        String propertyName,
                                        String propertyValue ){

        String output = null;

        try{
            String relPropUrl = relationshipUri + PROPERTY_LOC;
            HttpClient client = new HttpClient();
            PutMethod mPut = new PutMethod(relPropUrl);
            
            // set headers
            Header mtHeader = new Header();
            mtHeader.setName("content-type");
            mtHeader.setValue("application/json");
            mtHeader.setName("accept");
            mtHeader.setValue("application/json");
            mPut.addRequestHeader(mtHeader);
            
            // set json payload
            String jsonString = toJsonNameValuePairCollection(propertyName,propertyValue );
            StringRequestEntity requestEntity = new StringRequestEntity(jsonString,
                                                                        "application/json",
                                                                        "UTF-8");
            mPut.setRequestEntity(requestEntity);
            int status = client.executeMethod(mPut);
            output = mPut.getResponseBodyAsString( );

            mPut.releaseConnection( );
            logger.trace("status = " + status + " / output = " + output);
        }catch(Exception e){
             logger.error("EXCEPTION :: adding the property " + propertyName + " to the relationship: " + e);
        }
    }
    */
    
    /**
     * @description generates json payload to be passed to relationship property web service
     * @param name
     * @param value
     * @return
     
    private String toJsonNameValuePairCollection(String name, String value) {
        return String.format("{ \"%s\" : \"%s\" }", name, value);
    }
    */
    
    /**
     * @description Performs traversal from a source node
     * @param nodeURI
     * @param relationShip - relationship used for traversal
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
		dbServerUrl = OLD_Neo4JPersistence.protocol + "://" + OLD_Neo4JPersistence.host + ":" + OLD_Neo4JPersistence.port;
		nodePointUrl = dbServerUrl + OLD_Neo4JPersistence.location + Neo4JConstants.NODE_LOC;

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
        try { deleteRecursively( new File( db_path) ); }
        catch ( IOException e ) { throw new RuntimeException( e ); }
    }
	
	
	// standard getter and setter
	public String getHost() {return host;}
	public void setHost(String host) {OLD_Neo4JPersistence.host = host;}
	public String getLocation() {return location;}
	public void setLocation(String location) {OLD_Neo4JPersistence.location = location;}
	public String getServiceUserEndpoint() {return serviceUserEndpoint;}
	public void setServiceUserEndpoint(String serviceUserEndpoint) {OLD_Neo4JPersistence.serviceUserEndpoint = serviceUserEndpoint;}
	public String getServicePostEndpoint() {return servicePostEndpoint;}
	public void setServicePostEndpoint(String servicePostEndpoint) {OLD_Neo4JPersistence.servicePostEndpoint = servicePostEndpoint;}
	public String getUser() {return user;}
	public void setUser(String user) {OLD_Neo4JPersistence.user = user;}
	public String getPass() {return pass;}
	public void setPass(String pass) {OLD_Neo4JPersistence.pass = pass;}
	public String getPort() {return port;}
	public void setPort(String port) {OLD_Neo4JPersistence.port = port;}
	public String getProtocol() {return protocol;}
	public void setProtocol(String protocol) {OLD_Neo4JPersistence.protocol = protocol;}
	public String getDbPath() {return db_path;}
	public void setDbPath(String dB_PATH) {OLD_Neo4JPersistence.db_path = dB_PATH;}
}
