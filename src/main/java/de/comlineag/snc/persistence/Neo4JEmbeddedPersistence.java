package de.comlineag.snc.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.GraphNodeTypes;
import de.comlineag.snc.constants.GraphRelationshipTypes;
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
 * @version 	0.1
 * @status		in development
 *
 * @description handles the connectivity to an embedded Neo4J Graph Database and saves nodes and 
 * 				connections in the graph. Implements IGraphPersistenceManager
 *
 * @changelog	0.1 (Chris)		initial version as copy from Neo4JPersistence Version 0.8
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
public class Neo4JEmbeddedPersistence implements IGraphPersistenceManager {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	// this is a reference to the Neo4J configuration settings
	private final Neo4JConfiguration nco = Neo4JConfiguration.getInstance();
	
	// singleton design pattern using Initialization-on-demand holder idiom, 
	private static class Holder { static final Neo4JEmbeddedPersistence instance = new Neo4JEmbeddedPersistence(); }
    public static Neo4JEmbeddedPersistence getInstance() { return Holder.instance; }
    
    
	// path to the configuration xml file
	public String configDb;
	
	private static final String DB_PATH = "storage/neo4j_db";
	
	GraphDatabaseService graphDb;
	Node firstNode, secondNode;
	Relationship relation;
	
	private Neo4JEmbeddedPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(getDbPath());
		registerShutdownHook(graphDb);
	}
	private String getDbPath() {
		return DB_PATH;
	}


	@Override
	public void saveNode(JSONObject nodeObject, GraphNodeTypes label) {
		logger.info("creating a {} node object to store in the graph", SocialNetworks.getSocialNetworkConfigElementByCode("name", nodeObject.get("sn_id").toString()));
		//GraphNodeTypes label = GraphNodeTypes.POST;
		
		if ("POST".equalsIgnoreCase(label.getValue())){
			GraphPostingData gpd = new GraphPostingData(nodeObject);
			createPostNodeObject(gpd.getJson(), label);
		} else if ("USER".equalsIgnoreCase(label.getValue())){
			GraphUserData gud = new GraphUserData(nodeObject);
			createUserNodeObject(gud.getJson(), label);
		} else if ("DOMAIN".equalsIgnoreCase(label.getValue())){
			GraphDomainData gdd = new GraphDomainData(nodeObject);
			createOtherNodeObject(gdd.getJson(), label);
		} else if ("CUSTOMER".equalsIgnoreCase(label.getValue())){
			GraphCustomerData gcd = new GraphCustomerData(nodeObject);
			createOtherNodeObject(gcd.getJson(), label);
		} else if ("KEYWORD".equalsIgnoreCase(label.getValue())){
			GraphKeywordData gkd = new GraphKeywordData(nodeObject);
			createOtherNodeObject(gkd.getJson(), label);
		} else if ("SOCIALNETWORK".equalsIgnoreCase(label.getValue())){
			GraphSocialNetworkData gsd = new GraphSocialNetworkData(nodeObject);
			createOtherNodeObject(gsd.getJson(), label);
		} else {
			logger.warn("warning can't create an object with label {} in the graph, beacause I do not know what it is is", label.getValue());
		}
	}
	
	
	
	
	
	
	
	/**
	 * 
	 * @description	creates a node in the graph 
	 * @param 		JSONObject attributes of the node
	 * @param		GraphNodeTypes for label
	 * 
	 */
	private void createOtherNodeObject(JSONObject jsonNode, GraphNodeTypes label){
		try ( Transaction tx = graphDb.beginTx() ) {
			firstNode = graphDb.createNode(label);
			logger.info("{} node created, setting properties", label.getValue());
			logger.debug("attributes...{}",jsonNode.keySet());
			Set<String> keys = jsonNode.keySet();
			
			Iterator<String> iterator = keys.iterator();
		    while(iterator.hasNext()) {
		    	logger.trace("working on key {} with value {}", iterator.toString(), jsonNode.get(iterator));
			    firstNode.setProperty(iterator.toString(), jsonNode.get(iterator));
		        String setElement = iterator.next();
		    }
			
		    /*
			if(jsonNode.containsKey("id"))
				firstNode.setProperty("id", jsonNode.get("id"));
			if(jsonNode.containsKey("name"))
				firstNode.setProperty("name", jsonNode.get("name"));
			if(jsonNode.containsKey("lang"))
				firstNode.setProperty("lang", jsonNode.get("lang"));
			*/
            tx.success();
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node with {} - {}",  jsonNode.toJSONString(), e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	
	private void createPostNodeObject(JSONObject jsonNode, GraphNodeTypes label){
		try ( Transaction tx = graphDb.beginTx() ) {
			firstNode = graphDb.createNode(label);
			logger.info("{} node created, setting properties", label.getValue());
			logger.debug("setting attributes...{}",jsonNode.keySet());
			
			if(jsonNode.containsKey("id"))
				firstNode.setProperty("id", jsonNode.get("id"));
			if(jsonNode.containsKey("text"))
				firstNode.setProperty("text", jsonNode.get("text"));
			if(jsonNode.containsKey("lang"))
				firstNode.setProperty("lang", jsonNode.get("lang"));
			if(jsonNode.containsKey("teaser"))
				firstNode.setProperty("teaser", jsonNode.get("teaser"));
			if(jsonNode.containsKey("subject"))
				firstNode.setProperty("subject", jsonNode.get("subject"));
			if(jsonNode.containsKey("timestamp"))
				firstNode.setProperty("timestamp", jsonNode.get("timestamp"));
			
            tx.success();
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node with {} - {}",  jsonNode.toJSONString(), e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void createUserNodeObject(JSONObject jsonNode, GraphNodeTypes label){
		try ( Transaction tx = graphDb.beginTx() ) {
			firstNode = graphDb.createNode(label);
			logger.info("{} node created, setting properties", label.getValue());
			logger.debug("setting attributes...{}",jsonNode.keySet());
			
			if(jsonNode.containsKey("id"))
				firstNode.setProperty("id", jsonNode.get("id"));
			if(jsonNode.containsKey("username"))
				firstNode.setProperty("username", jsonNode.get("username"));
			if(jsonNode.containsKey("screen_name"))
				firstNode.setProperty("screen_name", jsonNode.get("screen_name"));
			if(jsonNode.containsKey("lang"))
				firstNode.setProperty("lang", jsonNode.get("lang"));
            tx.success();
		} catch(Exception e) {
			logger.error("EXCEPTION :: failed to create node with {} - {}",  jsonNode.toJSONString(), e.getMessage());
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
			
			
		} catch (Exception e) {
			logger.error("could not create relationship between {} and {} - {}", startNode, endNode, e.getLocalizedMessage());
		}
		return null;
	}
	
	

	/**
	 * @description deletes all graph-db files on file system, thus essentially wiping out the entire DB
	 */
	@SuppressWarnings("unused")
	private void clearDbPath() {
        try { deleteRecursively( new File( nco.getDbPath() ) ); }
        catch ( IOException e ) { throw new RuntimeException( e ); }
    }
	
	/**
	 * 
	 * @param firstNode
	 * @param secondNode
	 * @param relationshipType
	 * @param direction
	 */
	private void removeData(Node firstNode, Node secondNode, GraphRelationshipTypes relationshipType, Direction direction){
		try ( Transaction tx = graphDb.beginTx() ){
            // let's remove the data
            firstNode.getSingleRelationship( relationshipType, direction ).delete();
            firstNode.delete();
            secondNode.delete();

            tx.success();
        }
    }
	
	/**
	 * @checks if the database is already running
	 * @param dbServerUrl
	 */
	private void checkDatabaseIsRunning(String dbServerUrl){
		logger.debug("opening connection to server {}", dbServerUrl);
		
    }
	
	
	/**
	 * @description
	 */
	private void shutDown(){
		logger.info("Shutting down Neo4J graph database ..." );
        
        graphDb.shutdown();
    }
	
	/**
	 * @description	registers a shutdown hook for the Neo4j instance so that it
	 *				shuts down nicely when the VM exits (even if you "Ctrl-C" the
	 *				running application).
	 */
	private static void registerShutdownHook( final GraphDatabaseService graphDb ){
		Runtime.getRuntime().addShutdownHook( new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		} );
	}
	
	public String getConfigDb() {return configDb;}
	public void setConfigDb(String configDb) {this.configDb = configDb;}
	
	
	/**
	 * @description create the database
	 */
	private void createDb(){
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(nco.getDbPath());
		registerShutdownHook(graphDb);
		
		try ( Transaction tx = graphDb.beginTx() ){
            // Database operations go here
            firstNode = graphDb.createNode();
            firstNode.setProperty( "message", "Hello, " );
            secondNode = graphDb.createNode();
            secondNode.setProperty( "message", "World!" );

            relation = firstNode.createRelationshipTo( secondNode, GraphRelationshipTypes.BELONGS_TO);
            relation.setProperty( "message", "brave Neo4j " );
            
            System.out.print( firstNode.getProperty( "message" ) );
            System.out.print( relation.getProperty( "message" ) );
            System.out.print( secondNode.getProperty( "message" ) );
            
            String greeting = ( (String) firstNode.getProperty( "message" ) )
                       + ( (String) relation.getProperty( "message" ) )
                       + ( (String) secondNode.getProperty( "message" ) );
            
            tx.success();
        }
	}
}
