package de.comlineag.sbm.persistence;

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

/**
 * 
 * @author Christian Guenther
 * @description	This is only a demo class with different code examples on how to work with the graph 
 *
 */
public class Neo4JEmbeddedDB extends Neo4jMBean {
	
	private static String DB_PATH = "/usr/local/var/neo4jembedded";
	private static String EMAIL_DOMAIN = "@comlineag.de";
	
	protected Neo4JEmbeddedDB(ManagementData management, String[] extraNaming)
			throws NotCompliantMBeanException {
		super(management, extraNaming);
		// TODO Auto-generated constructor stub
		
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
	
	private void createTestUsers(GraphDatabaseService graphdb) {
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( "User" );

		    // Create some users
		    for ( int id = 0; id < 100; id++ ) {
		        Node userNode = graphdb.createNode( label );
		        userNode.setProperty( "username", "user" + id + EMAIL_DOMAIN );
		    }
		    System.out.println( "Users created" );
		    tx.success();
		}
	}
	
	// find a user in the graph:  User      e.g. 45
	private void findUser(String nodeLabel, int who, GraphDatabaseService graphdb) {
		Label label = DynamicLabel.label( nodeLabel );
		
		int idToFind = who;
		String nameToFind = "user" + idToFind + EMAIL_DOMAIN;
		
		try ( Transaction tx = graphdb.beginTx() )
		{
		    try ( ResourceIterator<Node> users =
		            graphdb.findNodesByLabelAndProperty( label, "username", nameToFind ).iterator() ) {
		        ArrayList<Node> userNodes = new ArrayList<>();
		        while ( users.hasNext() ) {
		            userNodes.add( users.next() );
		        }

		        for ( Node node : userNodes ) {
		            System.out.println( "The username of user " + idToFind + " is " + node.getProperty( "username" ) );
		        }
		    }
		}
	}
	
	// update username
	private void updateUsername(String nodeLabel, int who, GraphDatabaseService graphdb){
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( nodeLabel );
		    int idToFind = who;
		    String nameToFind = "user" + idToFind + EMAIL_DOMAIN;

		    for ( Node node : graphdb.findNodesByLabelAndProperty( label, "username", nameToFind ) ) {
		        node.setProperty( "username", "user" + ( idToFind + 1 ) + EMAIL_DOMAIN );
		    }
		    tx.success();
		}
	}
	
	private void deleteUser(String nodeLabel, int who, GraphDatabaseService graphdb){
		try ( Transaction tx = graphdb.beginTx() ) {
		    Label label = DynamicLabel.label( nodeLabel );
		    int idToFind = who;
		    String nameToFind = "user" + idToFind + EMAIL_DOMAIN;

		    for ( Node node : graphdb.findNodesByLabelAndProperty( label, "username", nameToFind ) ) {
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
}
