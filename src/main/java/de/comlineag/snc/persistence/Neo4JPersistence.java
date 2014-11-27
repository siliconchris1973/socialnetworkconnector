package de.comlineag.snc.persistence;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;





//import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.HttpErrorMessages;
import de.comlineag.snc.constants.HttpStatusCodes;
import de.comlineag.snc.constants.GraphRelationshipTypes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.data.CustomerData;
import de.comlineag.snc.data.DomainData;
import de.comlineag.snc.data.KeywordData;
import de.comlineag.snc.data.GraphPostingData;
import de.comlineag.snc.data.SocialNetworkData;
import de.comlineag.snc.data.GraphUserData;
import de.comlineag.snc.neo4j.Relation;
import de.comlineag.snc.neo4j.TraversalDefinition;


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
	//private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	// this is a reference to the Neo4J configuration settings
	private final Neo4JConfiguration nco = Neo4JConfiguration.getInstance();
	
	private String dbServerUrl;
	private String transactionUrl;
	// path to the configuration xml file
	public String configDb;
	
	
	
	public Neo4JPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
	}
	
	
	@Override
	/**
	 * 
	 * @description	this is the public save method for the graph persistence
	 * 				it takes a node object in json format creates the embedded
	 * 				objects in the graph.
	 * 				The method is quite strict as to how the json should look like:
	 * 				the outermost json structure must be a post with all needed elements
	 * 				Within the json object embedded jsons are expected for domain,
	 * 				customer, social network and a user. Each embedded json must have 
	 * 				it's name all in uppercase.
	 * 
	 *  @param		json object with post and embedded domain, customer, social network and user
	 *  
	 */
	public void saveNode(JSONObject nodeObject) {
		logger.info("creating {} node object(s) to store in the graph", SocialNetworks.getSocialNetworkConfigElementByCode("name", nodeObject.get("sn_id").toString()));
		logger.trace("    content: {}", nodeObject.toString());
		URI postNodeLocation = null;
		URI userNodeLocation = null;
		URI domainNodeLocation = null;
		URI customerNodeLocation = null;
		URI socialNetworkNodeLocation = null;
		URI keywordNodeLocation = null;
		
		JSONObject postNodeObject = null;
		JSONObject userNodeObject = null; 
		JSONObject domainNodeObject = null;
		JSONObject customerNodeObject = null;
		JSONObject socialNetworkNodeObject = null;
		JSONObject keywordNodeObject = null;
		
		String sn_id = null;
		String id = null;
		String name = null;
		String jsonPayload;
		
		
		// fill the json object for post, user, domain and customer plus social network and keyword(s)
		postNodeObject = new JSONObject((JSONObject) nodeObject);
		if (nodeObject.containsKey("USER")) {
			logger.debug("found a user-object");
			userNodeObject = new JSONObject((JSONObject) nodeObject.get("USER"));
			logger.trace("   >>> {}", userNodeObject.toString());
		}
		if (nodeObject.containsKey("DOMAIN")) {
			logger.debug("found a domain-object");
			domainNodeObject = new JSONObject((JSONObject) nodeObject.get("DOMAIN"));
			logger.trace("   >>> {}", domainNodeObject.toString());
		}
		if (nodeObject.containsKey("CUSTOMER")) {
			logger.debug("found a customer-object");
			customerNodeObject = new JSONObject((JSONObject) nodeObject.get("CUSTOMER"));
			logger.trace("   >>> {}", customerNodeObject.toString());
		}
		if (nodeObject.containsKey("SOCIALNETWORK")) {
			logger.debug("found a social-network-object");
			socialNetworkNodeObject = new JSONObject((JSONObject) nodeObject.get("SOCIALNETWORK"));
			logger.trace("   >>> {}", socialNetworkNodeObject.toString());
		}
		if (nodeObject.containsKey("KEYWORD")) {
			logger.debug("found a keyword-object");
			keywordNodeObject = new JSONObject((JSONObject) nodeObject.get("KEYWORD"));
			logger.trace("   >>> {}", keywordNodeObject.toString());
		}
		
		// start the transaction
		URI transactLoc = startTransaction();
		
		// POST
		if (postNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (postNodeObject.containsKey("sn_id"))
				sn_id = (String) nodeObject.get("sn_id");
			if (postNodeObject.containsKey("id"))
				id = (String) nodeObject.get("id");
			
			if (sn_id != null && id != null) {
				logger.debug("checking if the post {}-{} exist", sn_id, id);
				
				jsonPayload = "{sn_id: '"+sn_id+"', id: '"+id+"'}";
				postNodeLocation = findNode(jsonPayload, GraphNodeTypes.POST, transactLoc);
				
				if (postNodeLocation == null) {
					logger.debug("creating the node");
					GraphPostingData gpd = new GraphPostingData(postNodeObject);
					postNodeLocation = createNodeObject(gpd.getJson(), GraphNodeTypes.POST, transactLoc);
				} 
				
				// after the node is created - we KNOW that the location is null
				if (postNodeLocation == null) {
					logger.warn("no node location returned");
				
					postNodeLocation = findNode(jsonPayload, GraphNodeTypes.POST, transactLoc);
				}
				logger.debug("post node created at location {}", postNodeLocation);
			}
		}
		
		// USER
		if (userNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (userNodeObject.containsKey("sn_id"))
				sn_id = (String) nodeObject.get("sn_id");
			if (userNodeObject.containsKey("id"))
				id = (String) nodeObject.get("id");
			
			if (sn_id != null && id != null) {
				logger.debug("checking if the user {}-{} exist", sn_id, id);
				
				jsonPayload = "{sn_id: '"+sn_id+"', id: '"+id+"'}";
				userNodeLocation = findNode(jsonPayload, GraphNodeTypes.USER, transactLoc);
				
				if (userNodeLocation == null) {
					logger.debug("creating the user");
					GraphUserData gud = new GraphUserData(userNodeObject);
					userNodeLocation = createNodeObject(gud.getJson(), GraphNodeTypes.USER, transactLoc);
				}
				
				// after the node is created - we KNOW that the location is null
				if (userNodeLocation == null) {
					logger.warn("no node location returned");
				
					userNodeLocation = findNode(jsonPayload, GraphNodeTypes.USER, transactLoc);
				}
				logger.debug("user node created at location {}", userNodeLocation);
			}
		}
		
		// DOMAIN
		if (domainNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (domainNodeObject.containsKey("name"))
				name = (String) domainNodeObject.get("name");
			
			if (name != null) {
				logger.debug("checking if the domain of interest {} exist", name);
				
				jsonPayload = "{name: '"+name+"'}";
				domainNodeLocation = findNode(jsonPayload, GraphNodeTypes.DOMAIN, transactLoc);
				
				if (domainNodeLocation == null) {
					logger.debug("creating the domain {}", name);
					DomainData gdd = new DomainData(domainNodeObject);
					domainNodeLocation = createNodeObject(gdd.getJson(), GraphNodeTypes.DOMAIN, transactLoc);
				}
				
				//  after the node is created - we KNOW that the location is null
				if (domainNodeLocation == null) {
					logger.warn("no node location returned");
				
					domainNodeLocation = findNode(jsonPayload, GraphNodeTypes.DOMAIN, transactLoc);
				}
				logger.debug("node for domain {} is at location {}", name, domainNodeLocation);
			}
		}
		
		
		// CUSTOMER
		if (customerNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (customerNodeObject.containsKey("name"))
				name = (String) customerNodeObject.get("name");
			
			if (name != null) {
				logger.debug("checking if the customer {} exist", name);
			
				jsonPayload = "{name: '"+name+"'}";
				customerNodeLocation = findNode(jsonPayload, GraphNodeTypes.CUSTOMER, transactLoc);
				
				if (customerNodeLocation == null) {
					logger.debug("creating the customer {}", name);
					CustomerData gcd = new CustomerData(customerNodeObject);
					customerNodeLocation = createNodeObject(gcd.getJson(), GraphNodeTypes.CUSTOMER, transactLoc);
				}
				
				//  after the node is created - we KNOW that the location is null
				if (customerNodeLocation == null) {
					logger.warn("no node location returned");
				
					customerNodeLocation = findNode(jsonPayload, GraphNodeTypes.CUSTOMER, transactLoc);
				}
				logger.debug("node for customer {} is at location {}", name, customerNodeLocation);
			}
		}
		
		
		// SOCIAL NETWORK
		if (socialNetworkNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (socialNetworkNodeObject.containsKey("sn_id"))
				name = (String) socialNetworkNodeObject.get("sn_id");
			
			if (name != null) {
				logger.debug("checking if the social network {} exist", name);
				
				jsonPayload = "{sn_id: '"+name+"'}";
				socialNetworkNodeLocation = findNode(jsonPayload, GraphNodeTypes.SOCIALNETWORK, transactLoc);
				
				if (socialNetworkNodeLocation == null) {
					logger.debug("creating the social network {}", name);
					SocialNetworkData gsd = new SocialNetworkData(socialNetworkNodeObject);
					socialNetworkNodeLocation = createNodeObject(gsd.getJson(), GraphNodeTypes.SOCIALNETWORK, transactLoc);
				}
				
				//  after the node is created - we KNOW that the location is null
				if (socialNetworkNodeLocation == null) {
					logger.warn("no node location returned");
				
					socialNetworkNodeLocation = findNode(jsonPayload, GraphNodeTypes.SOCIALNETWORK, transactLoc);
				}
				logger.debug("node for social network {} is at location {}", name, socialNetworkNodeLocation);
			}
		}
		
		
		// KEYWORD
		if (keywordNodeObject != null) {
			sn_id = null;
			id = null;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (keywordNodeObject.containsKey("keyword"))
				name = (String) keywordNodeObject.get("keyword");
			
			if (name != null) {
				logger.debug("checking if the keyword {} exist", name);
				
				jsonPayload = "{keyword: '"+name+"'}";
				keywordNodeLocation = findNode(jsonPayload, GraphNodeTypes.KEYWORD, transactLoc);
				
				if (keywordNodeLocation == null) {
					logger.debug("creating the keyword {}", name);
					KeywordData gkd = new KeywordData(keywordNodeObject);
					keywordNodeLocation = createNodeObject(gkd.getJson(), GraphNodeTypes.KEYWORD, transactLoc);
				}
				
				//  after the node is created - we KNOW that the location is null
				if (keywordNodeLocation == null) {
					logger.warn("no node location returned");
				
					keywordNodeLocation = findNode(jsonPayload, GraphNodeTypes.KEYWORD, transactLoc);
				}
				logger.debug("node for keyword {} is at location {}", name, keywordNodeLocation);
			}
		}
		
		
		// after all nodes have been created or, in case they were already there, their respective links
		// are retrieved, we create the relationship(s) between the nodes
		String[] jsonAttributes = null;
		try {
			createRelationship(userNodeLocation, postNodeLocation, GraphRelationshipTypes.WROTE, jsonAttributes);
		} catch (Exception e) {
			logger.error("Could not create relationship of type {} between node {} and node {}", 
					GraphRelationshipTypes.WROTE, userNodeLocation, postNodeLocation);
		}
		try {
			createRelationship(postNodeLocation, socialNetworkNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
		} catch (Exception e) {
			logger.error("Could not create relationship of type {} between node {} and node {}", 
					GraphRelationshipTypes.BELONGS_TO, postNodeLocation, socialNetworkNodeLocation);
		}
		try {
			createRelationship(postNodeLocation, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
		} catch (Exception e) {
			logger.error("Could not create relationship of type {} between node {} and node {}", 
					GraphRelationshipTypes.BELONGS_TO, postNodeLocation, domainNodeLocation);
		}
		try {
			createRelationship(postNodeLocation, customerNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
		} catch (Exception e) {
			logger.error("Could not create relationship of type {} between node {} and node {}", 
					GraphRelationshipTypes.BELONGS_TO, postNodeLocation, customerNodeLocation);
		}
		try {
			createRelationship(customerNodeLocation, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
		} catch (Exception e) {
			logger.error("Could not create relationship of type {} between node {} and node {}", 
					GraphRelationshipTypes.BELONGS_TO, customerNodeLocation, domainNodeLocation);
		}
		
		// commit the transaction
		commitTransaction(transactLoc);
	}
	
	
	/**
	 * 
	 * @description wraps the json object and the label in a cypher statement to
	 * 				create a node. The statement is then passed on to the method 
	 * 				sendTransactionalCypherStatement(String statement, uri location)
	 * @param 		nodeObject
	 * @param 		label
	 * @param 		transactLoc
	 * 
	 * @return		URI to created object in the graph
	 * 
	 */
	private URI createNodeObject(JSONObject nodeObject, GraphNodeTypes label, URI transactLoc){
		/* the cypher statement is part of a larger one that is executed by the 
		 * next method. It finally becomes this at execution time:
		 * {
		 * 	"statements": [ { 
		 * 		"statement": 
		 * 			"CREATE (p:SOCIALNETWORK {properties}) ", 
		 * 				"parameters": {
		 * 					"properties": {
		 * 						"description":"Twitter",
		 * 						"name":"Twitter",
		 * 						"domain":"twitter.com",
		 * 						"sn_id":"TW"
		 * 					}
		 * 				} 
		 * 		} ] 
		 * } 
		 * 
		 */
		String cypherStatement = "\"CREATE (p:"+ label.toString() +" {properties}) \", "
				+ "\"parameters\": {"
				+ "\"properties\":" + nodeObject.toString() 
				+ "} ";
		return (sendTransactionalCypherStatement(cypherStatement, transactLoc));
		
	}
	
	private URI findNode(String jsonPayload, GraphNodeTypes label, URI transactLoc){
		/* the cypher statement is part of a larger one that is executed by the 
		 * next method. It finally becomes this at execution time:
		 * {
		 * 	"statements": [ { 
		 * 		"statement": 
		 * 			"MATCH (p:POST {sn_id: {parameters}}) RETURN p", 
		 * 				"parameters": {
		 * 					"sn_id":"TW"
		 * 					}
		 * 				} 
		 * 		} ] 
		 * } 
		 * 
		 */
		/*
		String qPart = "sn_id";
		String cypherStatement = "\"MATCH (p:"+ label.toString() +" {"+qPart+": {parameters}}) RETURN p\", "
				+ "\"parameters\": " + jsonPayload ;
		*/
		
		String cypherStatement = "\"MATCH (p:"+ label.toString() + " " +jsonPayload+" ) RETURN p\"";
		
		return (getNodeLocationTransactional(cypherStatement, transactLoc));
		
	}
	

	/**
	 * 
	 * @description	executes a cypher statement in the given transaction 
	 * @param 		cypher statement
	 * @param		URI to an open transaction
	 * 
	 * @returns		URI to object in graph
	 * 
	 */
	private URI sendTransactionalCypherStatement(String cypherStatement, URI transactLoc){
		//URI nodeLocation = null;
		try{
			URI finalUrl = transactLoc;
			
			/* the statement is expanded to this
			 String payload = "{\"statements\": "
								+ "[ "
									+ "{\"statement\": "
										+ "\"CREATE (p:"+ label.toString() +" {properties}) \", "
											+ "\"parameters\": {"
													+ "\"properties\":" + nodeObject.toString() 
											+ "} "
									+ "} "
								+ "] "
							+ "}";
			*/
			/* until it finally becomes this at execution time:
			 * {
			 * 	"statements": [ { 
			 * 		"statement": 
			 * 			"CREATE (p:SOCIALNETWORK {properties}) ", 
			 * 				"parameters": {
			 * 					"properties": {
			 * 						"description":"Twitter",
			 * 						"name":"Twitter",
			 * 						"domain":"twitter.com",
			 * 						"sn_id":"TW"
			 * 					}
			 * 				} 
			 * 		} ] 
			 * } 
			 * 
			 */
			String payload = "{\"statements\": "
								+ "[ "
									+ "{\"statement\": "
										+ cypherStatement
									+ "} "
								+ "] "
							+ "}";
				    
			logger.trace("sending cypher {} to endpoint {}", payload, finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( payload )
			        .post( ClientResponse.class );
			
			String responseEntity = response.getEntity(String.class).toString();
			int responseStatus = response.getStatus();
			
			logger.trace("POST to {} returned status code {}, returned data: {}",
					finalUrl, responseStatus,
			        responseEntity);
			
			// first check if the http code was ok
			HttpStatusCodes httpStatusCodes = HttpStatusCodes.getHttpStatusCode(responseStatus);
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error("Error {} sending data to {}: {} ", response.getStatus(), finalUrl, HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				}
			} else {
				/*
				 * Structure of the JSON response
				 	{
					  "commit" : "http://localhost:7474/db/data/transaction/1/commit",
					  "results" : [ {
					    "columns" : [ "n" ],
					    "data" : [ {
					      "rest" : [ {
					        "labels" : "http://localhost:7474/db/data/node/12/labels",
					        "outgoing_relationships" : "http://localhost:7474/db/data/node/12/relationships/out",
					        "all_typed_relationships" : "http://localhost:7474/db/data/node/12/relationships/all/{-list|&|types}",
					        "traverse" : "http://localhost:7474/db/data/node/12/traverse/{returnType}",
	--> location of node	"self" : "http://localhost:7474/db/data/node/12",
					        "property" : "http://localhost:7474/db/data/node/12/properties/{key}",
					        "outgoing_typed_relationships" : "http://localhost:7474/db/data/node/12/relationships/out/{-list|&|types}",
					        "properties" : "http://localhost:7474/db/data/node/12/properties",
					        "incoming_relationships" : "http://localhost:7474/db/data/node/12/relationships/in",
					        "create_relationship" : "http://localhost:7474/db/data/node/12/relationships",
					        "paged_traverse" : "http://localhost:7474/db/data/node/12/paged/traverse/{returnType}{?pageSize,leaseTime}",
					        "all_relationships" : "http://localhost:7474/db/data/node/12/relationships/all",
					        "incoming_typed_relationships" : "http://localhost:7474/db/data/node/12/relationships/in/{-list|&|types}",
					        "metadata" : {
	--> id of the node        "id" : 12,
					          "labels" : [ ]
					        },
					        "data" : {
					        }
					      } ]
					    } ]
					  } ],
					  "transaction" : {
					    "expires" : "Tue, 30 Sep 2014 06:16:54 +0000"
					  },
					  "errors" : [ ]
					}
				 */
				
				// now do the check on json details within the returned JSON object
				JSONParser reponseParser = new JSONParser();
				Object responseObj = reponseParser.parse(responseEntity);
				JSONObject jsonResponseObj = responseObj instanceof JSONObject ?(JSONObject) responseObj : null;
				if(jsonResponseObj == null)
					throw new ParseException(0, "returned json object is null");
				
				//String error = (String) jsonResponseObj.get("errors").toString();
				
				// first results
				JSONArray resultsArray = (JSONArray) jsonResponseObj.get("results");
				JSONArray errorArray = (JSONArray) jsonResponseObj.get("errors");
				String error = (String) errorArray.toString();
				
				// get the data array from results array
				JSONObject t = (JSONObject) resultsArray.get(0);// .get("data")[0];
				JSONArray dataArray = (JSONArray) t.get("data");
				
				// get the rest array from data array
				t = (JSONObject) dataArray.get(0);
				JSONArray restArray = (JSONArray) t.get("rest");
				
				// create the simple json objects each from first element in array, because we only have that
				JSONObject data = (JSONObject) dataArray.get(0);
				JSONObject rest = (JSONObject) restArray.get(0);
				
				logger.trace("\n>>>>>>>>>");
				logger.trace("results: {}", resultsArray.toString());
				logger.trace("data: {}", data.toString());
				logger.trace("rest: {}", rest.toString());
				logger.trace(">>>>>>>>>\n");
				
				// if the error array has only the [] brackets, it's ok
				if (error.length() == 2) {
					logger.debug("cypher statement executed successfully at location {}", finalUrl);
					return finalUrl;
				} else {
					logger.error("ERROR :: {} - could not execute cypher statement at location {}", error, finalUrl);
					return null;
				}
			}
			response.close();
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node - {}", e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	/**
	 * 
	 * @description	adds a relationship from source to target node with attributes
	 * 
	 * @param		URI to the source node
	 * @param		URI to the target node
	 * @param		GraphRelationshipType
	 * @param		String[] json attribute to add to the new relationship
	 * 
	 * @throws 		URISyntaxException
	 * 
	 */
	@Override
	public URI createRelationship(URI sourceNode, URI targetNode,
			GraphRelationshipTypes relationshipType, String[] jsonAttributes) 
					throws URISyntaxException {
		
        URI fromUri = new URI( sourceNode.toString() + "/relationships" );
        String relationshipJson = generateJsonRelationship( targetNode,
                relationshipType, jsonAttributes );

        WebResource resource = Client.create()
                .resource( fromUri );
        // POST JSON to the relationships URI
        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( relationshipJson )
                .post( ClientResponse.class );

        final URI location = response.getLocation();
        logger.info("POST to {}, status code [{}], location header [{}]",
                fromUri, response.getStatus(), location.toString() ) ;

        response.close();
        return location;
    }
	/**
	 * @description	used by createRelationship to generate a json structure for the cypher
	 * 				command to create the relationship between the two nodes
	 */
	private static String generateJsonRelationship( URI endNode,
            GraphRelationshipTypes relationshipType, String[] jsonAttributes ) {
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
            for ( int i = 0; i < jsonAttributes.length; i++ ) {
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
	
	
	public URI getNodeLocationTransactional(String cypherStatement, URI endpointLoc){
		URI nodeLocation = null;
		try {
			//+ "MATCH (n {" + field + " : " + id + ", type : \"" + type + "\" }) RETURN n"
			String payload = "{\"statements\": "
					+ "[ "
						+ "{\"statement\": "
							+ cypherStatement
							+ ", \"resultDataContents\":[\"REST\"]"
						+ "} "
					+ "] "
				+ "}";
			
			
			logger.trace("sending cypher {} to endpoint {}", payload, endpointLoc);
			WebResource resource = Client.create().resource( endpointLoc );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( payload )
			        .get(ClientResponse.class);
			        //.post( ClientResponse.class );
			
			String responseEntity = response.getEntity(String.class).toString();
			int responseStatus = response.getStatus();
			logger.trace("GET to {} returned status code {}, returned data: {}",
					endpointLoc, responseStatus,
			        responseEntity);
			
			// first check if the http code was ok
			HttpStatusCodes httpStatusCodes = HttpStatusCodes.getHttpStatusCode(responseStatus);
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error("Error {} sending data to {}: {} ", response.getStatus(), endpointLoc, HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				}
			} else {
				// now do the check on json details within the returned JSON object
				JSONParser reponseParser = new JSONParser();
				Object responseObj = reponseParser.parse(responseEntity);
				JSONObject jsonResponseObj = responseObj instanceof JSONObject ?(JSONObject) responseObj : null;
				if(jsonResponseObj == null)
					throw new ParseException(0, "returned json object is null");
				
				String result = (String) jsonResponseObj.get("results").toString();
				logger.trace("result is {} ", result);
				
				String error = (String) jsonResponseObj.get("errors").toString();
				
				// the url to a node is contained within the extensions in the key self 
				// "self" : "http://localhost:7474/db/data/node/357",
				
				// if the error array has only the [] brackets, it's ok
				if (error.length() == 2) {
					logger.debug("cypher statement executed successfully at location {}", endpointLoc);
					//nodeLocation = response.getLocation();
				} else {
					logger.error("ERROR :: {} - could not execute cypher statement at location {}", error, endpointLoc);
					nodeLocation = null;
				}
				
				//logger.trace("query for node returned {}", nodeLocation.toString());
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: failed to execute query - {}", e.getMessage());
			e.printStackTrace();
		}
		return nodeLocation;
	}
	
	
	
	/**
	 * 
	 * @description traverses the graph. Given a starting node, it follows all relationships off
	 * 				of this node that have the given type up to the maximum number of hops
	 * 				
	 * @param 		URI of the starting node
	 * @param 		GraphRelationshipType 
	 * @param		int maximum number of hops from each given node
	 * @returns		JSONObject with the urls of all nodes found
	 * 
	 * @throws 		URISyntaxException
	 * 
	 */
	public JSONObject traverseGraph(URI startNode, GraphRelationshipTypes relation, int maxTraverseDepth)
			throws URISyntaxException{
		JSONObject resultObj = new JSONObject();
	
		// TraversalDefinition turns into JSON to send to the Server
		TraversalDefinition t = new TraversalDefinition();
		t.setOrder(TraversalDefinition.DEPTH_FIRST);
		t.setUniqueness(TraversalDefinition.NODE);
		t.setMaxDepth(maxTraverseDepth);
		t.setReturnFilter(TraversalDefinition.ALL);
		t.setRelationships(new Relation(relation.toString(), Relation.OUT));
		
		URI traverserUri = new URI( startNode.toString() + "/traverse/node" );
		WebResource resource = Client.create()
								.resource(traverserUri);
		String jsonTraverserPayload = t.toJson();
		ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
				.type( MediaType.APPLICATION_JSON )
				.entity( jsonTraverserPayload )
				.post( ClientResponse.class );
		
		//String temp = response.getEntityInputStream().toString();
		
		logger.debug("POST [{}] to [{}], status code [{}], returned data: {}",
					jsonTraverserPayload, 
					traverserUri, 
					response.getStatus(),
					response.getEntity( String.class)) ;
		response.close();
	
		return resultObj;
    }
	
	
	/**
	 * 
	 * @description	adds a property to a given node
	 * @param nodeUri
	 * @param propertyName
	 * @param propertyValue
	 * 
	 */
	private void addProperty(URI nodeUri, String propertyName,
            String propertyValue ){
        // START SNIPPET: addProp
        String propertyUri = nodeUri.toString() + "/properties/" + propertyName;
        // http://localhost:7474/db/data/node/{node_id}/properties/{property_name}

        WebResource resource = Client.create()
                .resource( propertyUri );
        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( "\"" + propertyValue + "\"" )
                .put( ClientResponse.class );

        logger.debug("PUT to [{}], status code [{}]",
                propertyUri, response.getStatus());
        response.close();
        // END SNIPPET: addProp
    }
	
	
	/**
	 * 
	 * @description	adds a property to a relation between two nodes
	 * @param relationshipUri
	 * @param name
	 * @param value
	 * @throws URISyntaxException
	 * 
	 */
	private void addMetadataToProperty( URI relationshipUri,
            String name, String value ) throws URISyntaxException{
        URI propertyUri = new URI( relationshipUri.toString() + "/properties" );
        String entity = toJsonNameValuePairCollection( name, value );
        WebResource resource = Client.create()
                .resource( propertyUri );
        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( entity )
                .put( ClientResponse.class );

        logger.debug("PUT [%s] to [%s], status code [%d]", entity, propertyUri,
                response.getStatus() );
        response.close();
    }
	
	
	/**
	 * 
	 * @description	starts a new transaction and returns the end point to it
	 * 				this method must be executed first when interacting with the neo4j 
	 * 				graph database in a transactional way
	 * 
	 * @return		URI new transaction endpoint
	 * 
	 */
	private URI startTransaction(){
		URI transactionLocation=null;
		
		try{
			dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
			transactionUrl = dbServerUrl + "/db/data" + "/transaction";
			String finalUrl = transactionUrl;
			
			//String payload = "";
				    
			logger.debug("opening transaction at endpoint {}", finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        //.entity( payload )
			        .post( ClientResponse.class );
			
			transactionLocation = response.getLocation();
			logger.trace("opened transaction is at location {}", transactionLocation);
			
			response.close();
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to open transaction - {}", e.getMessage());
			e.printStackTrace();
		}
		
		return transactionLocation;
	}
	
	
	/**
	 * 
	 * @description commits the transaction at the given endpoint
	 * @param 		URI transaction location
	 * 
	 */
	private void commitTransaction(URI transactLoc) {
		try{
			String finalUrl = transactLoc + "/commit";
				    
			logger.debug("committing transaction at endpoint {}", finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        //.entity( payload )
			        .post( ClientResponse.class );
			
			response.close();
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to commit transaction - {}", e.getMessage());
			e.printStackTrace();
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
		return Long.parseLong(nodeIdAsString);
	}
	
	private static String toJsonNameValuePairCollection( String name, String value ){
        return String.format( "{ \"%s\" : \"%s\" }", name, value );
    }
	
	// get/set the config file
	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}
}
