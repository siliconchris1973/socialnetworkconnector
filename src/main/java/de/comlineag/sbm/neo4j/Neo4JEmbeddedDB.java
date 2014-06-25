package de.comlineag.sbm.neo4j;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.management.NotCompliantMBeanException;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.jmx.impl.ManagementData;
import org.neo4j.jmx.impl.Neo4jMBean;

import de.comlineag.sbm.data.RelationshipTypes;
import de.comlineag.sbm.data.UserData;

/**
 * 
 * @author Christian Guenther
 * @description	This is only a demo class with different code examples on how to work with the graph 
 *
 */
public class Neo4JEmbeddedDB extends Neo4jMBean {
	
	private static String DB_PATH = "/usr/local/var/neo4jembedded";
	
	protected Neo4JEmbeddedDB(ManagementData management, String[] extraNaming)
			throws NotCompliantMBeanException {
		super(management, extraNaming);
	}

	private void createDB(){
		GraphDatabaseService graphDb = (new GraphDatabaseFactory()
		.newEmbeddedDatabase( DB_PATH ));
		/*
		.setConfig( GraphDatabaseSettings.nodestore_mapped_memory_size, "10M" )
		.setConfig( GraphDatabaseSettings.string_block_size, "60" )
		.setConfig( GraphDatabaseSettings.array_block_size, "300" )
		.newGraphDatabase());
		*/
	}
	
	private void createIndexOnDB(String where, String what, GraphDatabaseService graphdb){
		IndexDefinition indexDefinition;
		
		try ( Transaction tx = graphdb.beginTx() ) {
		    Schema schema = graphdb.schema();
		    indexDefinition = schema.indexFor( DynamicLabel.label( what ) )
		            .on( where )
		            .create();
		    tx.success();
		}
		
		try ( Transaction tx = graphdb.beginTx() ) {
		    Schema schema = graphdb.schema();
		    schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
		}
	}
	
	private void saveUser(UserData userData, GraphDatabaseService graphdb) {
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( "User" );

		    
		    
		    tx.success();
		}
	}
	
	
	private void deleteUser(String nodeLabel, String userName, GraphDatabaseService graphdb){
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( nodeLabel );
		    
		    for ( Node node : graphdb.findNodesByLabelAndProperty( label, "username", userName ) ) {
		        node.delete();
		    }
		    tx.success();
		}
	}
	
	private void dropIndex(String nodeLabel, GraphDatabaseService graphdb){
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( nodeLabel );
		    
		    for ( IndexDefinition indexDefinition : graphdb.schema()
		            .getIndexes( label ) ) {
		        
		    	// There is only one index
		        indexDefinition.drop();
		    }

		    tx.success();
		}
	}
	
	private void createRelationship(String postNodeUri, String userNodeUri, RelationshipTypes relationshipType, GraphDatabaseService graphdb){
		
		String relationAttributes = "{ \"" + relationshipType.toString() + "\" : \"at\" : \"2014\" }";
        
		/*
		// TODO make registering a relationship work
		String relationShipURI = graphdb.addRelationship(userNodeUri,
                                                            postNodeUri,
                                                            //relationshipType,
                                                            relationAttributes);
		*/
		try ( Transaction tx = graphdb.beginTx() ) {
			
		
			tx.success();
		}
		
	}
}
