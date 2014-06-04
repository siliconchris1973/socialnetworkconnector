package de.comlineag.sbm.Neo4J;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;

/**
 * 
 * @author Christian Guenther
 * @description abstract class representing a leaf in the graph - that is to say a node
 * 				the specific classes for TwitterPostingNeo4JNode and TwitterUserNeo4JNode 
 * 				inherit their constructor and the StringRequestEntity from Neo4JNodeObject
 */
abstract class Neo4JNodeObject implements Node {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	
	// these are used to find a single node in the graph
	private String locationUri;
	private Long nodeId;
	
	// this represents the graph db itself
	private GraphDatabaseService graphdb;
	
	// will hold the data of the node object as a JSON object
	private JSONObject j = new JSONObject();
	
	// constructor - every node is to be created with it's json payload and the appropriate grapdb-service
	public Neo4JNodeObject(JSONObject json, GraphDatabaseService graphdb){
		this.j = json;
		this.graphdb = graphdb; 
	}
	
	/**
	 * 
	 * @return StringRequestEntity
	 * @description returns the node object as a string entity with json payload and encoding parameter
	 */
	public StringRequestEntity getStringRequestEntity(){
		StringRequestEntity requestEntity = null;
		try {
			requestEntity = new StringRequestEntity(j.toString(),
			        								"application/json",
			        								"UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("EXCEPTION :: could not create StringRequestEntity from JSON-Object due to malformed data " + e.getStackTrace().toString());
		}
		return requestEntity;
	}
	
	
	// from this point on, all the methods from the superclass node are implemented
	@Override
	public long getId() {
		return nodeId;
	}
	
	@Override
	public void delete() {
		// TODO Auto-generated method stub
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.debug("ATTENTION :: delete code not yet implemented.");
		    
		    tx.success();
		}
	}

	@Override
	public Iterable<Relationship> getRelationships() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasRelationship() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<Relationship> getRelationships(RelationshipType... types) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction,
			RelationshipType... types) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasRelationship(RelationshipType... types) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasRelationship(Direction direction,
			RelationshipType... types) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasRelationship(Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<Relationship> getRelationships(RelationshipType type,
			Direction dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasRelationship(RelationshipType type, Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Relationship getSingleRelationship(RelationshipType type,
			Direction dir) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Relationship createRelationshipTo(Node otherNode,
			RelationshipType type) {
		// TODO Auto-generated method stub
		
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.debug("ATTENTION :: create relationship code not yet implemented.");
		    
		    tx.success();
		}
		return null;
	}

	@Override
	public Traverser traverse(Order traversalOrder,
			StopEvaluator stopEvaluator,
			ReturnableEvaluator returnableEvaluator,
			RelationshipType relationshipType, Direction direction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Traverser traverse(Order traversalOrder,
			StopEvaluator stopEvaluator,
			ReturnableEvaluator returnableEvaluator,
			RelationshipType firstRelationshipType, Direction firstDirection,
			RelationshipType secondRelationshipType, Direction secondDirection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Traverser traverse(Order traversalOrder,
			StopEvaluator stopEvaluator,
			ReturnableEvaluator returnableEvaluator,
			Object... relationshipTypesAndDirections) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addLabel(Label label) {
		// TODO Auto-generated method stub
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.debug("ATTENTION :: add label code not yet implemented.");
		    
		    tx.success();
		}
	}

	@Override
	public void removeLabel(Label label) {
		// TODO Auto-generated method stub
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.debug("ATTENTION :: remove label code not yet implemented.");
		    
		    tx.success();
		}
	}

	@Override
	public boolean hasLabel(Label label) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<Label> getLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphDatabaseService getGraphDatabase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasProperty(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getProperty(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperty(String key, Object value) {
		// TODO Auto-generated method stub
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.error("ATTENTION :: set property code not yet implemented.");
		    
		    tx.success();
		}
	}

	@Override
	public Object removeProperty(String key) {
		// TODO Auto-generated method stub
		try ( Transaction tx = graphdb.beginTx() ) {
		    logger.error("ATTENTION :: remove property code not yet implemented.");
		    
		    tx.success();
		}
		return null;
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	// these are the standard getter and setter for the class vars
	public String getLocationUri() {
		return locationUri;
	}
	public void setLocationUri(String locationUri) {
		this.locationUri = locationUri;
	}
	// same as getId
	public Long getNodeId() {
		return nodeId;
	}
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
}
