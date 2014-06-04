package de.comlineag.sbm.Neo4J;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import de.comlineag.sbm.data.UserData;

public class TwitterUserNeo4JNode extends Neo4JNodeObject {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private UserData userData = new UserData();
	private JSONObject u = new JSONObject();
	
	public TwitterUserNeo4JNode(JSONObject json, GraphDatabaseService graphdb) {
		super(json, graphdb);
	}

	private JSONObject createUserData(UserData userData){
		
		// set json payload 
		u.put("type", "User");
		u.put("sn_id", userData.getSnId());									// {name = "sn_id"; sqlType = NVARCHAR; nullable = false; length = 2;},
		u.put("user_id", userData.getId());									// {name = "user_id"; sqlType = NVARCHAR; nullable = false; length = 20;},
		u.put("userName", userData.getUsername());							// {name = "userName"; sqlType = NVARCHAR; nullable = true; length = 128;},
		u.put("nickName", userData.getScreenName());						// {name = "nickName"; sqlType = NVARCHAR; nullable = true; length = 128;},
		u.put("userLang", userData.getLang());								// {name = "userLang"; sqlType = NVARCHAR; nullable = true; length = 64;},
		u.put("location", userData.getLocation());							// {name = "location"; sqlType = NVARCHAR; nullable = true; length = 1024;},
		u.put("follower", userData.getFollowersCount());					// {name = "follower"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("friends", userData.getFriendsCount());						// {name = "friends"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("postingsCount", userData.getPostingsCount());				// {name = "postingsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("favoritesCount", userData.getFavoritesCount());				// {name = "favoritesCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
		u.put("listsAndGroupsCount", userData.getListsAndGrooupsCount());	// {name = "listsAndGroupsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";}
		// TODO add field db_entry with timestamp on creation time
		//u.put("db_entry", )

		logger.debug("about to insert the following data in the graph: " + u.toString());
		return u;
	}
	
	// find a user in the graph
	private void findUserNodeByName(String nodeLabel, String name, GraphDatabaseService graphdb) {
		Label label = DynamicLabel.label( nodeLabel );
			
		try ( Transaction tx = graphdb.beginTx() )
		{
		    try ( ResourceIterator<Node> users =
		            graphdb.findNodesByLabelAndProperty( label, "username", name ).iterator() ) {
		        ArrayList<Node> userNodes = new ArrayList<>();
		        while ( users.hasNext() ) {
		            userNodes.add( users.next() );
		        }

		        for ( Node node : userNodes ) {
		            logger.info( "found node object matching username " + name + " with " + node.getProperty( "username" ) );
		        }
		    }
		}
	}
}
