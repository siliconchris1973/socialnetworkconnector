package de.comlineag.snc.persistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
 *  	post -BELONGS_TO-> network
 *  11.create relationship from post to keyword
 *  	post -CONTAINS-> keyword
 * 
 * 
 * The graph schema looks like this:
 * 
 *                            +---------------------------------+
 *                            |                                 |
 * 							  | +-[BELONGS_TO]->(SOCIALNETWORK) |
 * 							  | |                               |
 * 		(KEYWORD)<-[CONTAINS]-(POST)<-[WROTE]-(USER)<-+         |
 * 			|					|                     |         |
 *		[BELONGS_TO]		[MENTIONS]----------------+         |
 * 			|	                                                |
 *          +->(DOMAIN)<-[BELONGS_TO]-(CUSTOMER)<-[TRACKED_FOR]-+
 * 
 * 	a USER writes a POST
 *  the POST is from a SOCIALNETWORK
 * 	the POST possibly mentions 1-n USER
 * 	the POST contains 1-n KEYWORD
 *  the POST is tracked for a CUSTOMER
 * 	the KEYWORD belongs to 1-n DOMAIN
 * 	a CUsTOMER belongs to a DOMAIN
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
	public URI saveNode(JSONObject nodeObject, GraphNodeTypes label, URI transactLoc) {
		/* the cypher statement is part of a larger one that is executed via method 
		 *    sendTransactionalCypherStatement(). 
		 * Ultimately, the statement looks like this:
		 * 
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
	
	

	/**
	 * 
	 * @description	this is the public save method for the graph persistence
	 * 				it takes a node object in json format and creates the embedded
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
	public void createNodeObject(JSONObject nodeObject){
		logger.info("About to create {} node object(s) in the graph", SocialNetworks.getSocialNetworkConfigElementByCode("name", nodeObject.get("sn_id").toString()));
		//logger.trace("   >>> {}", nodeObject.toString());
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
		ArrayList<String> keywords = null;
		
		String postSnId = null;
		String postId = null;
		String userSnId = null;
		String userId = null;
		String domainName = null;
		String customerName = null;
		String socNetName = null;
		String sn_id = null;
		String id = null;
		String name = null;
		
		String jsonPayload;
		
		
		
		// fill the json object for post, user, domain and customer plus social network and keyword(s)
		postNodeObject = new JSONObject((JSONObject) nodeObject);
		if (postNodeObject.containsKey("sn_id"))
			postSnId = postNodeObject.get("sn_id").toString();
		if (postNodeObject.containsKey("id"))
			postId = postNodeObject.get("id").toString();
		logger.debug("POST object {}-{} found", postSnId, postId);
		logger.trace("   >>> {}", postNodeObject.toString());
		
		if (nodeObject.containsKey("USER")) {
			userNodeObject = new JSONObject((JSONObject) nodeObject.get("USER"));
			if (userNodeObject.containsKey("sn_id"))
				userSnId = userNodeObject.get("sn_id").toString();
			if (userNodeObject.containsKey("id"))
				userId = userNodeObject.get("id").toString();
			else if (userNodeObject.containsKey("user_id"))
				userId = userNodeObject.get("user_id").toString();
			
			logger.debug("USER object {}-{} found", userSnId, userId);
			logger.trace("   >>> {}", userNodeObject.toString());
		}
		if (nodeObject.containsKey("DOMAIN")) {
			domainNodeObject = new JSONObject((JSONObject) nodeObject.get("DOMAIN"));
			if (domainNodeObject.containsKey("name"))
				domainName = domainNodeObject.get("name").toString();  
			logger.debug("DOMAIN object {} found", domainName);
			logger.trace("   >>> {}", domainNodeObject.toString());
		}
		if (nodeObject.containsKey("CUSTOMER")) {
			customerNodeObject = new JSONObject((JSONObject) nodeObject.get("CUSTOMER"));
			if (customerNodeObject.containsKey("name"))
				customerName = customerNodeObject.get("name").toString();  
			logger.debug("CUSTOMER object {} found", customerName);
			logger.trace("   >>> {}", customerNodeObject.toString());
		}
		if (nodeObject.containsKey("SOCIALNETWORK")) {
			socialNetworkNodeObject = new JSONObject((JSONObject) nodeObject.get("SOCIALNETWORK"));
			if (socialNetworkNodeObject.containsKey("name"))
				socNetName = socialNetworkNodeObject.get("name").toString();  
			logger.debug("SOCIALNETWORK object {} found", socNetName);
			logger.trace("   >>> {}", socialNetworkNodeObject.toString());
		}
		if (nodeObject.containsKey("KEYWORD")) {
			keywords = ((ArrayList<String>) nodeObject.get("KEYWORD"));
			logger.debug("KEYWORD object found");
			logger.trace("   >>> {}", keywords.toString());
		}
		
		// start the transaction
		URI transactLoc = startTransaction();
		
		// POST
		if (postNodeObject != null) {
			sn_id = postSnId;
			id = postId;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (sn_id != null && id != null) {
				logger.debug("working on post-object {}-{}", sn_id, id);
				jsonPayload = "{sn_id: '"+sn_id+"', id: '"+id+"'}";
				
				/* new way
				GraphPostingData node = new GraphPostingData(postNodeObject);
				GraphNodeTypes label = GraphNodeTypes.POST;
				String matchPayload = jsonPayload;
				String createPayload = node.getJson().toString();
				
				postNodeLocation = matchAndCreateNodeTransactional(label, matchPayload, createPayload, transactLoc);
				*/
				
				// old way
				postNodeLocation = findNode(jsonPayload, GraphNodeTypes.POST, transactLoc);
				
				if (postNodeLocation == null) {
					logger.debug("creating the post {}-{}", sn_id, id);
					GraphPostingData gpd = new GraphPostingData(postNodeObject);
					//logger.trace("   POST : >>> {}", gpd.getJson().toString());
					postNodeLocation = saveNode(gpd.getJson(), GraphNodeTypes.POST, transactLoc);
				} 
				
				
				// after the node is created - check if the location is returned and if not, find the node
				if (postNodeLocation == null) {
					postNodeLocation = findNode(jsonPayload, GraphNodeTypes.POST, transactLoc);
				}
				if (postNodeLocation == null) {
					logger.warn("could not locate node for post {}-{} ", sn_id, id);
				} else {
					logger.info("POST node {}-{} is at location {}", sn_id, id, postNodeLocation);
				}
				
			}
		}
		
		// USER
		if (userNodeObject != null) {
			sn_id = userSnId;
			id = userId;
			name = null;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (sn_id != null && id != null) {
				logger.debug("working on user-object {}-{}", sn_id, id);
				jsonPayload = "{sn_id: '"+sn_id+"', id: '"+id+"'}";
								
				// OLD way
				userNodeLocation = findNode(jsonPayload, GraphNodeTypes.USER, transactLoc);
				
				if (userNodeLocation == null) {
					logger.debug("creating the user {}-{}", sn_id, id);
					GraphUserData gud = new GraphUserData(userNodeObject);
					//logger.trace("   USER : >>> {}", gud.getJson().toString());
					userNodeLocation = saveNode(gud.getJson(), GraphNodeTypes.USER, transactLoc);
				}
				
				// after the node is created - check if the location is returned and if not, find the node
				if (userNodeLocation == null) {
					userNodeLocation = findNode(jsonPayload, GraphNodeTypes.USER, transactLoc);
				}
				
				if (userNodeLocation == null) {
					logger.warn("could not locate node for user {}-{} ", sn_id, id);
				} else {
					logger.info("USER node {}-{} is at location {}", sn_id, id, userNodeLocation);
				}
			}
		}
		
		// DOMAIN
		if (domainNodeObject != null) {
			sn_id = null;
			id = null;
			name = domainName;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (name != null) {
				logger.debug("working on domain-object {}", name);
				jsonPayload = "{name: '"+name+"'}";
				
				// OLD way
				domainNodeLocation = findNode(jsonPayload, GraphNodeTypes.DOMAIN, transactLoc);
				
				if (domainNodeLocation == null) {
					logger.debug("creating the domain {}", name);
					DomainData gdd = new DomainData(domainNodeObject);
					//logger.trace("   DOMAIN : >>> {}", gdd.getJson().toString());
					domainNodeLocation = saveNode(gdd.getJson(), GraphNodeTypes.DOMAIN, transactLoc);
				}
				
				//  after the node is created - check if the location is returned and if not, find the node
				if (domainNodeLocation == null) {
					domainNodeLocation = findNode(jsonPayload, GraphNodeTypes.DOMAIN, transactLoc);
				}
				if (domainNodeLocation == null) {
					logger.warn("could not locate node for domain {} ", name);
				} else {
					logger.info("DOMAIN node {} is at location {}", name, domainNodeLocation);
				}
			}
		}
		
		
		// CUSTOMER
		if (customerNodeObject != null) {
			sn_id = null;
			id = null;
			name = customerName;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (name != null) {
				logger.debug("working on customer-object {}", name);
				jsonPayload = "{name: '"+name+"'}";
				
				// OLD way
				customerNodeLocation = findNode(jsonPayload, GraphNodeTypes.CUSTOMER, transactLoc);
				
				if (customerNodeLocation == null) {
					logger.debug("creating the customer {}", name);
					CustomerData gcd = new CustomerData(customerNodeObject);
					//logger.trace("   CUSTOMER : >>> {}", gcd.getJson().toString());
					customerNodeLocation = saveNode(gcd.getJson(), GraphNodeTypes.CUSTOMER, transactLoc);
				}
				
				//  after the node is created - check if the location is returned and if not, find the node
				if (customerNodeLocation == null) {
					customerNodeLocation = findNode(jsonPayload, GraphNodeTypes.CUSTOMER, transactLoc);
				}
				if (customerNodeLocation == null) {
					logger.warn("could not locate node for customer {} ", name);
				} else {
					logger.info("CUSTOMER node {} is at location {}", name, customerNodeLocation);
				}
			}
		}
		
		
		// SOCIAL NETWORK
		if (socialNetworkNodeObject != null) {
			sn_id = null;
			id = null;
			name = socNetName;
			
			// before we construct and call the create method, we check if the requested object does not already exist 
			if (name != null) {
				logger.debug("working on social network-object {}", name);
				jsonPayload = "{name: '"+name+"'}";
								
				// OLD way
				socialNetworkNodeLocation = findNode(jsonPayload, GraphNodeTypes.SOCIALNETWORK, transactLoc);
				
				if (socialNetworkNodeLocation == null) {
					logger.debug("creating the social network {}", name);
					SocialNetworkData gsd = new SocialNetworkData(socialNetworkNodeObject);
					//logger.trace("   SOCIALNETWORK : >>> {}", gsd.getJson().toString());
					socialNetworkNodeLocation = saveNode(gsd.getJson(), GraphNodeTypes.SOCIALNETWORK, transactLoc);
				}
				
				//  after the node is created - check if the location is returned and if not, find the node
				if (socialNetworkNodeLocation == null) {
					socialNetworkNodeLocation = findNode(jsonPayload, GraphNodeTypes.SOCIALNETWORK, transactLoc);
				}
				if (socialNetworkNodeLocation == null) {
					logger.warn("could not locate node for social network {} ", name);
				} else {
					logger.info("SOCIALNETWORK node {} is at location {}", name, socialNetworkNodeLocation);
				}
			}
		}
		
		// KEYWORD
		for (String keyword : keywords) {
			logger.debug("working on keyword-object {}", keyword);
			jsonPayload = "{keyword: '"+keyword+"'}";
			
			keywordNodeLocation = findNode(jsonPayload, GraphNodeTypes.KEYWORD, transactLoc);
			
			if (keywordNodeLocation == null) {
				KeywordData gkd = new KeywordData(keyword);
				keywordNodeLocation = saveNode(gkd.getJson(), GraphNodeTypes.KEYWORD, transactLoc);
			}
			
			//  after the node is created - check if the location is returned and if not, find the node
			if (keywordNodeLocation == null) {
				keywordNodeLocation = findNode(jsonPayload, GraphNodeTypes.KEYWORD, transactLoc);
			}
			if (keywordNodeLocation == null) {
				logger.warn("could not create node for keyword {} ", keyword);
			} else {
				logger.info("node for keyword {} created at location {}", keyword, keywordNodeLocation);
			}
		}
		
		
		logger.debug("=========>");
		logger.debug("post          {}\t-> {}: \t{}-{}", getNodeIdFromLocation(postNodeLocation), postNodeLocation, postSnId, postId);
		logger.debug("user          {}\t-> {}: \t{}-{}", getNodeIdFromLocation(userNodeLocation), userNodeLocation, userSnId, userId);
		logger.debug("domain        {}\t-> {}: \t{}", getNodeIdFromLocation(domainNodeLocation), domainNodeLocation, domainName);
		logger.debug("customer      {}\t-> {}: \t{}", getNodeIdFromLocation(customerNodeLocation), customerNodeLocation, customerName);
		logger.debug("socialnetwork {}\t-> {}: \t{}", getNodeIdFromLocation(socialNetworkNodeLocation), socialNetworkNodeLocation, socNetName);
		logger.debug("<========="); 
		
		// start a new transaction for the relationships
		//transactLoc = startTransaction();
		
		/*
		 * The graph schema looks like this:
		 *                            +---------------------------------+
		 *                            |                                 |
		 * 							  | +-[BELONGS_TO]->(SOCIALNETWORK) |
		 * 							  | |                               |
		 * 		(KEYWORD)<-[CONTAINS]-(POST)<-[WROTE]-(USER)<-+         |
		 * 			|					|                     |         |
		 *		[BELONGS_TO]		[MENTIONS]----------------+         |
		 * 			|	                                                |
		 *          +->(DOMAIN)<-[BELONGS_TO]-(CUSTOMER)<-[TRACKED_FOR]-+
		 * 
		 * 	a USER writes a POST
		 *  the POST is from a SOCIALNETWORK
		 * 	the POST possibly mentions 1-n USER
		 * 	the POST contains 1-n KEYWORD
		 * 	the KEYWORD belongs to 1-n DOMAIN
		 * 	a CUsTOMER belongs to a DOMAIN
		 * 
		 */
		//
		// create relationships
		//           sourceJsonMatch                       targetJsonMatch
		// MATCH  (a:USER {sn_id: "TW", id: "123456"}), (b:POST {sn_id: "TW", id: "654321"})
		//               relType
		// MERGE  (a)-[r:WROTE]->(b)
		URI relLoc = null;
		
		GraphNodeTypes sourceLabel = null;
		String sourceSnId = "";
		String sourceId = "";
		String sourceJsonMatch = "";
		
		GraphNodeTypes targetLabel = null;
		String targetSnId = "";
		String targetId = "";
		String targetJsonMatch = "";
		
		GraphRelationshipTypes relType = null;
		
		
		// rel: USER-[WROTE]->POST
		logger.debug("create relationship: (USER:"+userSnId+"-"+userId+")-[WROTE]->(POST:"+postSnId+"-"+postId+")");
		sourceLabel = GraphNodeTypes.USER;
		sourceSnId = userSnId;
		sourceId = userId;
		targetLabel = GraphNodeTypes.POST;
		targetSnId = postSnId;
		targetId = postId;
		relType = GraphRelationshipTypes.WROTE;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {sn_id: '"+sourceSnId+"', id: '"+sourceId+"'}";
		targetJsonMatch = targetLabel+" {sn_id: '"+targetSnId+"', id: '"+targetId+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
		
		// rel: POST-[BELONGS_TO]->SOCIALNETWORK
		logger.debug("create relationship: (POST:"+postSnId+"-"+postId+")-[FETCHED_FROM]->(SOCIALNETWORK:"+socNetName+")");
		sourceLabel = GraphNodeTypes.POST;
		sourceSnId = postSnId;
		sourceId = postId;
		targetLabel = GraphNodeTypes.SOCIALNETWORK;
		targetSnId = socNetName;
		relType = GraphRelationshipTypes.FETCHED_FROM;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {sn_id: '"+sourceSnId+"', id: '"+sourceId+"'}";
		targetJsonMatch = targetLabel+" {name: '"+targetSnId+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
		
		// rel: CUSTOMER-[BELONGS_TO]->DOMAIN
		logger.debug("create relationship: (CUSTOMER:"+customerName+")-[BELONGS_TO]->(DOMAIN:"+domainName+")");
		sourceLabel = GraphNodeTypes.CUSTOMER;
		sourceSnId = customerName;
		targetLabel = GraphNodeTypes.DOMAIN;
		targetSnId = domainName;
		relType = GraphRelationshipTypes.BELONGS_TO;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {name: '"+sourceSnId+"'}";
		targetJsonMatch = targetLabel+" {name: '"+targetSnId+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
		
		// rel: POST-[TRACKED_FOR]->CUSTOMER
		logger.debug("create relationship: (POST:"+postSnId+"-"+postId+")-[TRACKED_FOR]->(CUSTOMER:"+customerName+")");
		sourceLabel = GraphNodeTypes.POST;
		targetLabel = GraphNodeTypes.CUSTOMER;
		relType = GraphRelationshipTypes.TRACKED_FOR;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {sn_id: '"+postSnId+"', id: '"+postId+"'}";
		targetJsonMatch = targetLabel+" {name: '"+customerName+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
		
		// KEYWORD
		// rel: POST-[CONTAINS]->KEYWORD
		//		KEYWORD-[BELONGS_TO]->DOMAIN
		for (String keyword : keywords) {
			logger.debug("create relationship: (KEYWORD:"+keyword+")-[RELEVANT_FOR]->(DOMAIN:"+domainName+")");
			
			sourceLabel = GraphNodeTypes.KEYWORD;
			targetLabel = GraphNodeTypes.DOMAIN;
			relType = GraphRelationshipTypes.RELEVANT_FOR;
			
			// execute MATCH & MERGE
			sourceJsonMatch = sourceLabel+" {meyword: '"+keyword+"'}";
			targetJsonMatch = targetLabel+" {name: '"+domainName+"'}";
			relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
			
			logger.debug("create relationship: (POST:"+postSnId+"-"+postId+")-[CONTAINS]->(KEYWORD:"+keyword+")");
			sourceLabel = GraphNodeTypes.POST;
			targetLabel = GraphNodeTypes.KEYWORD;
			relType = GraphRelationshipTypes.CONTAINS;
			
			// execute MATCH & MERGE
			sourceJsonMatch = sourceLabel+" {sn_id: '"+postSnId+"', id: '"+postId+"'}";
			targetJsonMatch = targetLabel+" {keyword: '"+keyword+"'}";
			relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		}
		
		// commit the transaction
		commitTransaction(transactLoc);
	}
	
	
	
	private URI matchAndCreateNodeTransactional(GraphNodeTypes label, String matchPayload, String createPayload, URI transactLoc){
		String matchCypherStatement = "\"MATCH (p:"+ label.toString() + " " +matchPayload+" ) ";
		
		String createCypherStatement = " CREATE (n:"+ label.toString() +" {properties}) RETURN n\", "
		//String createCypherStatement = " CREATE (p {properties}) RETURN p\", "
				+ "\"parameters\": {"
				+ "\"properties\":" + createPayload
				+ "} ";
		String cypherStatement = matchCypherStatement + createCypherStatement;
		return (sendTransactionalCypherStatement(cypherStatement, transactLoc));
		
	}
	private URI findNode(String jsonPayload, GraphNodeTypes label, URI transactLoc){
		/* the cypher statement is part of a larger one that is executed via method 
		 *    sendTransactionalCypherStatement(). 
		 * Ultimately, the statement looks like this:
		 * 
		 * {
		 * 	"statements": [ { 
		 * 		"statement": 
		 * 			"MATCH (p:POST {sn_id: {parameters}}) RETURN p", 
		 * 				"parameters": {
		 * 					"sn_id":"TW"
		 * 					}
		 * 				} 
		 * 				, "resultDataContents":["REST"]
		 * 		} ] 
		 * } 
		 * 
		 */
		String cypherStatement = "\"MATCH (p:"+ label.toString() + " " +jsonPayload+" ) RETURN p\"";
		
		return (getNodeLocationTransactional(cypherStatement, transactLoc));
		
	}
	private JSONObject getNodeObject(String jsonPayload, GraphNodeTypes label, URI transactLoc){
		/* the cypher statement is part of a larger one that is executed via method 
		 *    sendTransactionalCypherStatement(). 
		 * Ultimately, the statement looks like this:
		 * 
		 * {
		 * 	"statements": [ { 
		 * 		"statement": 
		 * 			"MATCH (p:POST {sn_id: {parameters}}) RETURN p", 
		 * 				"parameters": {
		 * 					"sn_id":"TW"
		 * 					}
		 * 				} 
		 * 				, "resultDataContents":["REST"]
		 * 		} ] 
		 * } 
		 * 
		 */
		String cypherStatement = "\"MATCH (p:"+ label.toString() + " " +jsonPayload+" ) RETURN p\"";
		
		return (getNodeObjectTransactional(cypherStatement, transactLoc));
		
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
		URI nodeLocation = null;
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
											+ ", \"resultDataContents\":[\"REST\"]"
									+ "} "
								+ "] "
							+ "}";
			*/
			/* until it finally becomes this at execution time:
			 * {"statements": [ 
			 * 		{ "statement": 
			 * 			"CREATE (p:SOCIALNETWORK {properties}) ", 
			 * 				"parameters": {
			 * 					"properties": {
			 * 						"description":"Twitter",
			 * 						"name":"Twitter",
			 * 						"domain":"twitter.com",
			 * 						"sn_id":"TW"
			 * 					}
			 * 				}
			 * 				, "resultDataContents":["REST"] 
			 * 		} 
			 *  ] } 
			 * 
			 */
			String payload = "{\"statements\": "
								+ "[ "
									+ "{\"statement\": "
										+ cypherStatement
										+ ", \"resultDataContents\":[\"REST\"]"
									+ "} "
								+ "] "
							+ "}";
				    
			logger.debug("sending {} cypher {} ", payload.substring(32, 38), payload);
			logger.trace("    endpoint {}", finalUrl);
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
				
				//	CREATE statement for nodes
				//	{
				//		"commit":"http://localhost:7474/db/data/transaction/16/commit",
				//		"results":[
				//			{
				//				"columns":[],
				//				"data":[]			<-- contains a rest and a nested self object with the location
				//			}
				//		],
				//		"transaction":
				//			{
				//				"expires":"Wed, 03 Dec 2014 16:20:03 +0000"
				//			},
				//		"errors":[]
				//	}
/*
				try {
					// try for CREATE node
					nodeLocation = new URI(
											(String)(
													(JSONObject)(
															(JSONArray)(
																	(JSONObject)(
																			(JSONArray)(
																					(JSONObject)(
																							(JSONArray)jsonResponseObj.get("results")
																					).get(0)
																			).get("data")
																	).get(0)
															).get("rest")
													).get(0)
											).get("self")
										);
					logger.trace("nodeLocation is {}", nodeLocation);
				} catch (Exception e) {
					e.printStackTrace();
					try {
						// try for MATCH and CREATE relationship
						//								{
						//										"errors":[],
						//										"results":[
						//													{
						//														"data":[],
						//														"columns":["p"]		<-- contains the location
						//													}
						//										],
						//										"transaction":{
						//											"expires":"Wed, 03 Dec 2014 16:20:03 +0000"
						//										},
						//										"commit":"http:\/\/localhost:7474\/db\/data\/transaction\/16\/commit"
						//								}
						nodeLocation = new URI(
												(String)(
														(JSONObject)(
																(JSONArray)(
																		(JSONObject)(
																				(JSONArray)(
																						(JSONObject)(
																								(JSONArray)jsonResponseObj.values() //.get("results")
																						).get(0)
																				).get("p")
																		).get(0)
																).get("columns")
														).get(0)
												).get("results")
											);
						logger.trace("nodeLocation is {}", nodeLocation);
					} catch (Exception e1) {
						e1.printStackTrace();
						logger.warn("{} statement did not return a self object, returning null -- error was {}", payload.substring(32, 38), e1.getMessage());
						nodeLocation = null;
					}
				}
*/
				/*
				JSONArray errors = (JSONArray)jsonResponseObj.get("errors");
				logger.trace("errors is {}", errors.toString());
				if (errors.size() > 0) {
					JSONObject error = (JSONObject) errors.get(0);
					logger.debug("error-object: {}", error.toString());
				}
				
				// if the error array has only the [] brackets, it's ok
				if (errors.size() <= 2 && self != null) {
					logger.debug("{} cypher statement executed successfully at location {}, returning {}", payload.substring(32, 38), finalUrl, self);
					nodeLocation = new URI(self);
				} else {
					if (errors.size() > 2) {
						logger.error("ERROR :: {} - could not execute {} cypher statement at location {}", errors, payload.substring(32, 38), finalUrl);
					} else {
						logger.debug("{} returned no error, but also no node found, returning null", payload.substring(32, 38));
						nodeLocation = null;
					}
				}
				*/
			}
			response.close();
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node - {}", e.getMessage());
			e.printStackTrace();
		}
		return nodeLocation;
	}
	
	
	
	
	
	/**
	 * 
	 * @description checks if a given relationship between two node exists and if not, creates it
	 * 
	 * @param 		sourceJsonMatch - the json string fragment to identify the source node
	 * @param 		targetJsonMatch	- the json string fragment to identify the target node 
	 * @param 		relType			- the type of the relationship as provided in the enum
	 * @param 		targetUri		- the URI of an open transaction
	 * @return		URI				- the URI of a new relationship or null
	 * 
	 */
	public URI matchAndMergeRelationshipTransactional(String sourceJsonMatch, String targetJsonMatch, GraphRelationshipTypes relType, URI transactLoc){
		URI relationshipLocation = null;
		try {
			// MATCH (a:Person {name: "Bob"}), (b:Person {name: "Susan"}) 
			// MERGE (a)-[r:knows]->(b)
			
			// MATCH  (a:"+sourceJsonMatch+"), (b:"+targetJsonMatch+") 
			// MERGE (a)-[r:"+relType+"]->(b) 
			String cypherAscii = "MATCH (a: "+sourceJsonMatch+")-[r:"+relType+"]->(b: "+targetJsonMatch+")";
			String cypherStatement = "\""
									+ "MATCH (a:"+sourceJsonMatch+"), "
									+ "(b:"+targetJsonMatch+") "
									+ "MERGE (a)-[r:"+relType+"]->(b)"
									+ "\"";
			
			logger.trace("query if relationship {} exists and if not, create it", cypherAscii);
			relationshipLocation = sendTransactionalCypherStatement(cypherStatement, transactLoc);
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: failed to execute query - {}", e.getMessage());
			e.printStackTrace();
		}
		return relationshipLocation;
	}
	
	/**
	 * 
	 * @param cypherStatement
	 * @param endpointLoc
	 * @return
	 */
	public URI getNodeLocationTransactional(String cypherStatement, URI endpointLoc){
		URI nodeLocation = null;
		String self = null;
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
			
			
			logger.debug("sending {} cypher {} ", payload.substring(32, 38), payload);
			logger.trace("    endpoint {}", endpointLoc);
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
				
				logger.trace("returned response {}", jsonResponseObj.toString());
				
				try {
					nodeLocation = new URI((String)((JSONObject)((JSONArray)((JSONObject)((JSONArray)((JSONObject)((JSONArray)jsonResponseObj.get("results")).get(0)).get("data")).get(0)).get("rest")).get(0)).get("self"));
				} catch (Exception e) {
					logger.warn("{} statement did not return a self object, returning null -- error was {}", payload.substring(32, 38), e.getMessage());
					nodeLocation = null;
				}
				
				/*
				JSONArray errors = (JSONArray)jsonResponseObj.get("errors");
				logger.trace("errors is {}", errors.toString());
				if (errors.size() > 0) {
					JSONObject error = (JSONObject) errors.get(0);
					logger.debug("error-object: {}", error.toString());
				}
				
				
				// if the error array has only the [] brackets, it's ok
				if (errors.size() <= 2 && self != null) {
					logger.debug("{} cypher statement executed successfully at location {}, returning {}", payload.substring(32, 38), endpointLoc, self);
					nodeLocation = new URI(self);
				} else {
					if (errors.size() > 2) {
						logger.error("ERROR :: {} - could not execute {} cypher statement at location {}", errors, payload.substring(32, 38), endpointLoc);
					} else {
						logger.debug("{} returned no error, but also no node found, returning null", payload.substring(32, 38));
						nodeLocation = null;
					}
				}
				*/
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: failed to execute query - {}", e.getMessage());
			e.printStackTrace();
		}
		return nodeLocation;
	}
	
	
	
	/**
	 * 
	 * @param cypherStatement
	 * @param endpointLoc
	 * @return
	 */
	public JSONObject getNodeObjectTransactional(String cypherStatement, URI endpointLoc){
		JSONObject node = new JSONObject();
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
			
			
			logger.debug("sending {} cypher {} ", payload.substring(32, 38), payload);
			logger.trace("    endpoint {}", endpointLoc);
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
				
				logger.trace("returned response {}", jsonResponseObj.toString());
				
				node = jsonResponseObj;
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: failed to execute query - {}", e.getMessage());
			e.printStackTrace();
		}
		return node;
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
	private long getNodeIdFromLocation(URI nodeLocationUri) {
		//assert nodeLocationUri != null : "ERROR :: nodeLocationUri must not be null";
		if (nodeLocationUri != null){
			try { 
				String nodeIdAsString = "";
				int pos = (nodeLocationUri.toString().lastIndexOf('/') + 1);
				
				nodeIdAsString = nodeLocationUri.toString().substring(pos);
				return Long.parseLong(nodeIdAsString);
			} catch (Exception e) {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	
	// get/set the config file
	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}

	
	public URI createRelationship(GraphNodeTypes sourceType, URI sourceNode,
			GraphNodeTypes targetType, URI targetNode,
			GraphRelationshipTypes relationshipType, String[] jsonAttributes) {
		// Auto-generated method stub
		return null;
	}
}



// 
// OLD STUFF
//
/*


	
	@Override
	public URI createRelationship(GraphNodeTypes sourceType, URI sourceNode, 
								GraphNodeTypes targetType, URI targetNode,
			GraphRelationshipTypes relationshipType, String[] jsonAttributes) {
		URI relationShipLocation = null;
		
		String cypherArt = getNodeIdFromLocation(sourceNode)+"-[:"+relationshipType+"]->"+getNodeIdFromLocation(targetNode);
		
		logger.info("creating relationship ({}:{}) -[:{}]-> ({}:{})", 
										sourceType,
										getNodeIdFromLocation(sourceNode), 
										relationshipType,
										targetType,
										getNodeIdFromLocation(targetNode));
		
		try {
			URI finalUrl = new URI( sourceNode.toString() + "/relationships" );
			String cypherStatement = generateJsonRelationship( targetNode,
																relationshipType, 
																jsonAttributes );
			logger.debug("creating relationship {}", cypherArt);
			logger.trace("    using statement {}", cypherStatement);
			
			// direct call
			logger.debug("sending {} cypher {} ", payload.substring(32, 38), payload);
			logger.trace("    endpoint {}", finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( cypherStatement )
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
				JSONParser reponseParser = new JSONParser();
				Object responseObj = reponseParser.parse(responseEntity);
				JSONObject jsonResponseObj = responseObj instanceof JSONObject ?(JSONObject) responseObj : null;
				if(jsonResponseObj == null)
					throw new ParseException(0, "returned json object is null");
				
				try {
					//new URI((String)((JSONObject)((JSONArray)((JSONObject)((JSONArray)((JSONObject)((JSONArray)jsonResponseObj.get("results")).get(0)).get("data")).get(0)).get("rest")).get(0)).get("self"));
					relationShipLocation = new URI((String) jsonResponseObj.get("self"));
				} catch (Exception e) {
					logger.warn("CREATE RELATIONSHIP statement did not return a self object, returning null -- error was {}", e.getMessage());
					relationShipLocation = null;
				}
			}
		} catch (Exception e) {
			logger.error("could not create relationship ");
		}
        return relationShipLocation;
    }
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
	public URI haveRelationship(URI sourceNode, URI targetNode, GraphRelationshipTypes relType){
		URI relationshipLocation = null;
		try {
			//MATCH (martin { name:'Martin Sheen' })-[r]->(movie) RETURN r
			String cypherStatement = "MATCH (r {'"+sourceNode+"'})-["+relType+"]->('"+targetNode+"') RETURN r";
			
			logger.trace("querying if relationship of type {} exists between node {} and node {}", relType, sourceNode, targetNode);
			WebResource resource = Client.create().resource( sourceNode );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( cypherStatement )
			        .get(ClientResponse.class);
			
			String responseEntity = response.getEntity(String.class).toString();
			int responseStatus = response.getStatus();
			logger.trace("GET to {} returned status code {}, returned data: {}",
					sourceNode, responseStatus,
			        responseEntity);
			
			// first check if the http code was ok
			HttpStatusCodes httpStatusCodes = HttpStatusCodes.getHttpStatusCode(responseStatus);
			if (!httpStatusCodes.isOk()){
				if (httpStatusCodes == HttpStatusCodes.FORBIDDEN){
					logger.error(HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				} else {
					logger.error("Error {} sending data to {}: {} ", response.getStatus(), sourceNode, HttpErrorMessages.getHttpErrorText(httpStatusCodes.getErrorCode()));
				}
			} else {
				// now do the check on json details within the returned JSON object
				JSONParser reponseParser = new JSONParser();
				Object responseObj = reponseParser.parse(responseEntity);
				JSONObject jsonResponseObj = responseObj instanceof JSONObject ?(JSONObject) responseObj : null;
				if(jsonResponseObj == null)
					throw new ParseException(0, "returned json object is null");
				
				logger.trace("returned response {}", jsonResponseObj.toString());
				
				try {
					relationshipLocation = new URI((String)((JSONObject)((JSONArray)((JSONObject)((JSONArray)((JSONObject)((JSONArray)jsonResponseObj.get("results")).get(0)).get("data")).get(0)).get("rest")).get(0)).get("self"));
					logger.debug("found relationship at location {}", relationshipLocation);
				} catch (Exception e) {
					logger.warn("QUERY statement did not return a self object, returning null -- error was {}", e.getMessage());
					relationshipLocation = null;
				}
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: failed to execute query - {}", e.getMessage());
			e.printStackTrace();
		}
		return relationshipLocation;
	}
	
	

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
	
	
	private static String toJsonNameValuePairCollection( String name, String value ){
        return String.format( "{ \"%s\" : \"%s\" }", name, value );
    }
	

 */

// after all nodes have been created or, in case they were already there, their respective links
/* are retrieved, we create the relationship(s) between the nodes
String[] jsonAttributes = null;
URI relLoc = null;
if (userNodeLocation != null && postNodeLocation != null) {
	// check if relationship exists:
	relLoc = haveRelationship(userNodeLocation, postNodeLocation, GraphRelationshipTypes.WROTE);
	if (relLoc != null)
		createRelationship(GraphNodeTypes.USER, userNodeLocation, GraphNodeTypes.POST, postNodeLocation, GraphRelationshipTypes.WROTE, jsonAttributes);
}

if (postNodeLocation != null && socialNetworkNodeLocation != null) {
	// check if relationship exists:
	relLoc = haveRelationship(postNodeLocation, socialNetworkNodeLocation, GraphRelationshipTypes.BELONGS_TO);
	if (relLoc != null)
		createRelationship(GraphNodeTypes.POST, postNodeLocation, GraphNodeTypes.SOCIALNETWORK, socialNetworkNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
}

if (postNodeLocation != null && domainNodeLocation != null) { 
	// check if relationship exists:
	relLoc = haveRelationship(postNodeLocation, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO);
	if (relLoc != null)
		createRelationship(GraphNodeTypes.POST, postNodeLocation, GraphNodeTypes.DOMAIN, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
}

if (postNodeLocation != null && customerNodeLocation != null) {
	// check if relationship exists:
	relLoc = haveRelationship(postNodeLocation, customerNodeLocation, GraphRelationshipTypes.BELONGS_TO);
	if (relLoc != null)
		createRelationship(GraphNodeTypes.POST, postNodeLocation, GraphNodeTypes.CUSTOMER, customerNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
}

if (customerNodeLocation != null && domainNodeLocation != null) {
	// check if relationship exists:
	relLoc = haveRelationship(customerNodeLocation, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO);
	if (relLoc != null)
		createRelationship(GraphNodeTypes.CUSTOMER, customerNodeLocation, GraphNodeTypes.DOMAIN, domainNodeLocation, GraphRelationshipTypes.BELONGS_TO, jsonAttributes);
}

 		//rel: POST-[BELONGS_TO]->DOMAIN
		logger.debug("create relationship: POST-[BELONGS_TO]->DOMAIN");
		sourceLabel = GraphNodeTypes.POST;
		sourceSnId = postSnId;
		sourceId = postId;
		targetLabel = GraphNodeTypes.DOMAIN;
		targetSnId = domainName;
		relType = GraphRelationshipTypes.BELONGS_TO;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {sn_id: '"+sourceSnId+"', id: '"+sourceId+"'}";
		targetJsonMatch = targetLabel+" {name: '"+targetSnId+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
		
		
		// rel: POST-[BELONGS_TO]->CUSTOMER
		logger.debug("create relationship: POST-[BELONGS_TO]->CUSTOMER");
		sourceLabel = GraphNodeTypes.POST;
		sourceSnId = postSnId;
		sourceId = postId;
		targetLabel = GraphNodeTypes.CUSTOMER;
		targetSnId = customerName;
		relType = GraphRelationshipTypes.BELONGS_TO;
		
		// execute MATCH & MERGE
		sourceJsonMatch = sourceLabel+" {sn_id: '"+sourceSnId+"', id: '"+sourceId+"'}";
		targetJsonMatch = targetLabel+" {name: '"+targetSnId+"'}";
		relLoc = matchAndMergeRelationshipTransactional(sourceJsonMatch, targetJsonMatch, relType, transactLoc);
		
*/