package de.comlineag.snc.neo4j;

import java.util.ArrayList;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class TwitterPostingNeo4JNode extends Neo4JNodeObject {
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public TwitterPostingNeo4JNode(JSONObject json, GraphDatabaseService graphdb) {
		super(json, graphdb);
	}
	
	// find a user in the graph
	private void findPostingNodeByUserId(String nodeLabel, Long userid, GraphDatabaseService graphdb) {
		Label label = DynamicLabel.label( nodeLabel );
			
		try ( Transaction tx = graphdb.beginTx() )
		{
		    try ( ResourceIterator<Node> users =
		            graphdb.findNodesByLabelAndProperty( label, "user-id", userid ).iterator() ) {
		        ArrayList<Node> userNodes = new ArrayList<>();
		        while ( users.hasNext() ) {
		            userNodes.add( users.next() );
		        }

		        for ( Node node : userNodes ) {
		            logger.info( "found node object matching user-id " + userid + " with " + node.getProperty( "post-id" ) );
		        }
		    }
		}
	}
}
