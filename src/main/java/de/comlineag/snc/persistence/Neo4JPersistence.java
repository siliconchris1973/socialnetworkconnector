package de.comlineag.snc.persistence;

import java.net.URI;
import java.net.URL;

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
	public void saveNode(JSONObject nodeObject) {
		logger.info("creating {} node object(s) to store in the graph", SocialNetworks.getSocialNetworkConfigElementByCode("name", nodeObject.get("sn_id").toString()));
		logger.trace("    content: {}", nodeObject.toString());
		String postNodeLocation;
		String userNodeLocation;
		String domainNodeLocation;
		String customerNodeLocation;
		String socialNetworkNodeLocation;
		String keywordNodeLocation;
		
		JSONObject postNodeObject = null;
		JSONObject userNodeObject = null; 
		JSONObject domainNodeObject = null;
		JSONObject customerNodeObject = null;
		JSONObject socialNetworkNodeObject = null;
		JSONObject keywordNodeObject = null;
		
		// fill the json object for post, user, domain and customer plus social network and keyword(s)
		postNodeObject = new JSONObject((JSONObject) nodeObject);
		if (nodeObject.containsKey("USER"))
			userNodeObject = new JSONObject((JSONObject) nodeObject.get("USER"));;
		if (nodeObject.containsKey("DOMAIN"))
			domainNodeObject = new JSONObject((JSONObject) nodeObject.get("DOMAIN"));;
		if (nodeObject.containsKey("CUSTOMER"))
			customerNodeObject = new JSONObject((JSONObject) nodeObject.get("CUSTOMER"));;
		if (nodeObject.containsKey("SOCIALNETWORK"))
			socialNetworkNodeObject = new JSONObject((JSONObject) nodeObject.get("SOCIALNETWORK"));;
		if (nodeObject.containsKey("KEYWORD"))
			keywordNodeObject = new JSONObject((JSONObject) nodeObject.get("KEYWORD"));;
		
		
		if (postNodeObject != null) {
			logger.debug("creating the post");
			GraphPostingData gpd = new GraphPostingData(postNodeObject);
			postNodeLocation = createNodeObject(gpd.getJson(), GraphNodeTypes.POST);
			if (postNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		if (userNodeObject != null) {
			logger.debug("creating the user");
			GraphUserData gud = new GraphUserData(userNodeObject);
			userNodeLocation = createNodeObject(gud.getJson(), GraphNodeTypes.USER);
			if (userNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		if (domainNodeObject != null) {
			logger.debug("creating the domain");
			DomainData gdd = new DomainData(domainNodeObject);
			domainNodeLocation = createNodeObject(gdd.getJson(), GraphNodeTypes.DOMAIN);
			if (domainNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		if (customerNodeObject != null) {
			logger.debug("creating the customer");
			CustomerData gcd = new CustomerData(customerNodeObject);
			customerNodeLocation = createNodeObject(gcd.getJson(), GraphNodeTypes.CUSTOMER);
			if (customerNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		if (socialNetworkNodeObject != null) {
			logger.debug("creating the social network");
			SocialNetworkData gsd = new SocialNetworkData(socialNetworkNodeObject);
			socialNetworkNodeLocation = createNodeObject(gsd.getJson(), GraphNodeTypes.SOCIALNETWORK);
			if (socialNetworkNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		if (keywordNodeObject != null) {
			logger.debug("creating the keyword");
			KeywordData gkd = new KeywordData(keywordNodeObject);
			keywordNodeLocation = createNodeObject(gkd.getJson(), GraphNodeTypes.KEYWORD);
			if (keywordNodeLocation == null)
				logger.error("node was NOT created :-(");
		}
		
		
		// after all nodes have been created or, in case they were already there, their respective links
		// retrieved, lets create the relationship(s) between the nodes
	}
	
	
	private String createNodeObject(JSONObject nodeObject, GraphNodeTypes label){
		return (createNodeObjectTransactional(nodeObject, label));
	}
	
	/**
	 * 
	 * @description	creates a node in the graph 
	 * @param 		json object
	 * @param		label
	 * 
	 */
	private String createNodeObjectTransactional(JSONObject nodeObject, GraphNodeTypes label){
		String nodeLocation=null;
		try{
			dbServerUrl = nco.getProtocol() + "://" + nco.getHost() + ":" + nco.getPort();
			transactionUrl = dbServerUrl + "/db/data" + "/transaction";
			String finalUrl = transactionUrl;
			
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
				    
			logger.trace("sending cypher {} to endpoint {}", payload, finalUrl);
			WebResource resource = Client.create().resource( finalUrl );
			
			ClientResponse response = resource
					.accept( MediaType.APPLICATION_JSON )
	                .type( MediaType.APPLICATION_JSON )
			        .entity( payload )
			        .post( ClientResponse.class );
			
			nodeLocation = response.getLocation().toString();
			
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
				
				// this is the location to commit the transaction if node creation was successful
				String commit = (String) jsonResponseObj.get("commit").toString();
				// this contains an error object (actually an array) in case the creation was NOT successful
				String error = (String) jsonResponseObj.get("errors").toString();
				
				logger.trace("returned commit json is {}", commit.toString());
				logger.trace("returned error json is {}", error.toString());
				
				final URI location = response.getLocation();
				
				// if the error array has only the [] brackets, it's ok
				if (error.length() == 2) {
					//logger.info("new node created at location {}", location);
					logger.debug("committing transaction at location {}", commit);
					resource = Client.create().resource( commit );
					response = resource
							.accept( MediaType.APPLICATION_JSON )
			                .type( MediaType.APPLICATION_JSON )
					        .post( ClientResponse.class );
					
					String response2String = response.getEntity(String.class);
					
					logger.debug("COMMIT returned status code {}, returned data: {}",
							response.getStatus(),
					        response.getEntity(String.class));
					
					// now do the check on json details within the returned JSON object
					JSONParser reponse2Parser = new JSONParser();
					Object response2Obj = reponse2Parser.parse(response2String);
					JSONObject jsonResponse2Obj = response2Obj instanceof JSONObject ?(JSONObject) response2Obj : null;
					if(jsonResponse2Obj == null)
						throw new ParseException(0, "returned json object is null");
					
					// contains the created node information 
					String result = (String) jsonResponse2Obj.get("results").toString();
					logger.trace("returned result json is {}", result.toString());
					
				} else {
					logger.error("ERROR :: {} - could not create node at location {}", error, location);
				}
			}
			response.close();
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node - {}", e.getMessage());
			e.printStackTrace();
		}
		
		return nodeLocation;
	}
	
	
	
	
	
	/**
	 * @description	adds a relationship from source to target node with attributes
	 */
	@Override
	public URI createRelationship(URI sourceNode, URI targetNode,
			GraphRelationshipTypes relationshipType, String[] additionalData) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * @description find a node and return the url to it
	 */
	@Override
	public URL findNode(String key, String value, String label) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	// get/set the config file
	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}
}
