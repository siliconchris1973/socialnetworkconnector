package de.comlineag.snc.neo4j;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class TwitterPostingNeo4JNode extends Neo4JNodeObject {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
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

	@Override
	public int getDegree() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDegree(RelationshipType arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDegree(Direction arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDegree(RelationshipType arg0, Direction arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterable<RelationshipType> getRelationshipTypes() {
		// TODO Auto-generated method stub
		return null;
	}
}
