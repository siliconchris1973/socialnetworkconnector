package de.comlineag.sbm.persistence;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

/**
 *
 * @author Christian Guenther
 * @category Connector Class
 *
 * @description handles the connectivity to the Neo4J Graph Database
 * @version 1.0
 *
 */
public class Neo4JPersistence implements IPersistenceManager {

	// Servicelocation
	private String host;
	private String port;
	private String protocol;
	private String location;

	private String dbServerUrl;
	private String nodePointUrl;

	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Credentials
	private String user;
	private String pass;

	private final Logger logger = Logger.getLogger(getClass().getName());
//	private final Neo4JErrorHandling error = new Neo4JErrorHandling();

	public Neo4JPersistence() {
		// initialize the necessary variables from applicationContext.xml for server connection
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";

	}


	/**
	 * Speichern der Post im Greaphen mit folgenden Servicedaten:
	 *
	 * <Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
	 * <Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
	 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="timestamp" Type="Edm.DateTime"/>
	 * <Property Name="postLang" Type="Edm.String" MaxLength="64"/>
	 * <Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="1024"/>
	 * <Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="client" Type="Edm.String" MaxLength="2048"/>
	 * <Property Name="truncated" Type="Edm.Byte" DefaultValue="0"/>
	 * <Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
	 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 *
	 */
	@Override
	public void savePosts(PostData postData) {
		// we only store posts in german and english at the moment
		if (postData.getLang().equalsIgnoreCase("de") || postData.getLang().equalsIgnoreCase("en")) {
			dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
			nodePointUrl = dbServerUrl + this.location + "/node";

			logger.debug("savePosts for post-id " + postData.getId() + " called - working with " + nodePointUrl);

			//prepareConnection();
			String output = null;
			String locationHead = null;
			HttpClient client = new HttpClient();
			PostMethod mPost = new PostMethod(nodePointUrl);

			// set header
			Header mtHeader = new Header();
			mtHeader.setName("content-type");
			mtHeader.setValue("application/json");
			mtHeader.setName("accept");
			mtHeader.setValue("application/json");
			mPost.addRequestHeader(mtHeader);

			logger.debug("this is the header for savePosts() " + mtHeader.toString());


			// first step, save post itself
			try {
				logger.info("Creating node at " + nodePointUrl + " for post (ID " + postData.getId() + " / text (first 20 chars) " + postData.getText().substring(0, 20) + ")");

				/**
				 * set json payload
				 */
				JSONObject p = new JSONObject();

				p.put("sn_id", postData.getSnId());
				p.put("post_id", postData.getId());
				//	p.put("user_id", postData.getUserId()); // will be realized via graph connection
				p.put("timestamp", postData.getTimestamp());
				p.put("postLang", postData.getLang());
				p.put("text", postData.getText().toString());
				p.put("client", postData.getClient());
				p.put("truncated", postData.getTruncated());
				// p.put("inReplyTo", postData.getInReplyTo()); // will be realized via graph connection
				// p.put("inReplyToUserID", postData.getInReplyToUser()); // will be realized via graph connection
				// p.put("inReplyToScreenName", postData.getInReplyToUserScreenName());

				// TODO implement geo location
				//p.put("geoLocation_longitude", postData.getGeoLongitude());
				//p.put("geoLocation_latitude", postData.getGeoLatitude());
				//p.put("placeID", postData.getGeoPlaceId());
				//p.put("plName", postData.getGeoPlaceName());
				//p.put("plCountry", postData.getGeoPlaceCountry());
				//p.put("plAround_longitude", "00 00 00 00 00");
				//p.put("plAround_latitude", "00 00 00 00 00");

				logger.trace("about to insert the following data in the graph: " + p.toString());

				StringRequestEntity requestEntity = new StringRequestEntity(p.toString(),
		                                                                        "application/json",
		                                                                        "UTF-8");
				mPost.setRequestEntity(requestEntity);
				int status = client.executeMethod(mPost);
				HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(status);

				logger.debug("Status is " + statusCode + "/ Ok is " + statusCode.isOk());

				output = mPost.getResponseBodyAsString( );
				Header locationHeader =  mPost.getResponseHeader("location");
				locationHead = locationHeader.getValue();
				mPost.releaseConnection( );

				// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
				if (!statusCode.isOk()){
					logger.error(Neo4JErrorHandling.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.info("node for post " + postData.getId() + " created at location " + location + " in the graph");
				}

				logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);

			} catch(Exception e) {
				logger.error("Creating node for post (ID " + postData.getId() + " / text (first 20 chars) " + postData.getText().substring(0, 20) + ") " + e.getMessage());
			}

		/* second step establish connections between nodes
		try {
			logger.debug("Creating connection between nodes at " + nodePointUrl + " for post (ID " + postData.getId() + ") and user (ID " + postData.getUserId() + ") in neo4j");

			StringRequestEntity connectEntity = new StringRequestEntity("",
	                                                                        "application/json",
	                                                                        "UTF-8");
			mPost.setRequestEntity(connectEntity);
			status = client.executeMethod(mPost);
			statusText = HttpStatus.getStatusText(status).replaceAll(" ", "_").toUpperCase();
			httpErrorText = error.returnHttpErrorText(statusText);
	        okOrNotOk = HttpStatusCode.valueOf(statusText).toString();

	        logger.debug("Status is " + status + " / statusText is " + statusText + " / okOrNotOk is " + okOrNotOk);

			output = mPost.getResponseBodyAsString( );
			Header locationHeader =  mPost.getResponseHeader("location");
			locationHead = locationHeader.getValue();
			mPost.releaseConnection( );

			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (okOrNotOk == "NOK"){
				logger.error(httpErrorText);
			} else {
				logger.info("connection between nodes for post " + postData.getId() + " and user " + postData.getUserId() + " created");
			}

			logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);

		} catch(Exception e) {
			logger.error(httpErrorText + " ==> creating connection between nodes for post (ID " + postData.getId() + ") and user (" + postData.getUserId() + ") in neo4j " + e.getMessage());
		}
		*/

		} // end if - check on languages
	}

	@Override
	public void saveUsers(UserData userData) {
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";

		logger.debug("saveUsers called for user-id " + userData.getId() + " - working with " + nodePointUrl);

		//prepareConnection();
		String output = null;
		String locationHead = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(nodePointUrl);

		/**
		 * set headers
		 */
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);

		logger.debug("this is the header for saveUsers() " + mtHeader.toString());

		try {
			logger.info("Creating node at " + nodePointUrl + " for user (ID " + userData.getId() + " / screenname " + userData.getScreenName() + ")");

			/**
			 * set json payload
			 */
			/* values to store and corresponding keys
			 * {name = "sn_id"; sqlType = NVARCHAR; nullable = false; length = 2;},
			 * {name = "user_id"; sqlType = NVARCHAR; nullable = false; length = 20;},
			 * {name = "userName"; sqlType = NVARCHAR; nullable = true; length = 128;},
			 * {name = "nickName"; sqlType = NVARCHAR; nullable = true; length = 128;},
			 * {name = "userLang"; sqlType = NVARCHAR; nullable = true; length = 64;},
			 * {name = "location"; sqlType = NVARCHAR; nullable = true; length = 1024;},
			 * {name = "follower"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			 * {name = "friends"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			 * {name = "postingsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			 * {name = "favoritesCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";},
			 * {name = "listsAndGroupsCount"; sqlType = INTEGER; nullable = false; defaultValue ="0";}
			 */
			JSONObject u = new JSONObject();

			//TODO check if there is a more elegant way of creating the payload for the json object
			u.put("sn_id", userData.getSnId());
			u.put("user_id", userData.getId());
			u.put("userName", userData.getUsername());
			u.put("nickName", userData.getScreenName());
			u.put("userLang", userData.getLang());
			u.put("location", userData.getLocation());
			u.put("follower", userData.getFollowersCount());
			u.put("friends", userData.getFriendsCount());
			u.put("postingsCount", userData.getPostingsCount());
			u.put("favoritesCount", userData.getFavoritesCount());
			u.put("listsAndGroupsCount", userData.getListsAndGrooupsCount());

			logger.debug("about to insert the following data in the graph: " + u.toString());

			StringRequestEntity requestEntity = new StringRequestEntity(u.toString(),
	                                                                        "application/json",
	                                                                        "UTF-8");

			mPost.setRequestEntity(requestEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(status);

			logger.debug("Status is " + statusCode + "/ Ok is " + statusCode.isOk());

			output = mPost.getResponseBodyAsString( );
			Header locationHeader = mPost.getResponseHeader("location");
			locationHead = locationHeader.getValue();
			mPost.releaseConnection( );

			// in case everything is fine, neo4j should return 200. any other case needs to be investigated
			if (!statusCode.isOk()){
				logger.error(Neo4JErrorHandling.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				logger.info("node for user " + userData.getScreenName() + " created at location " + location + " in the graph");
			}

			logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);


		} catch (Exception e) {
			logger.error("Creating node for user (ID " + userData.getId() + " / name " + userData.getScreenName() + ") " + e.getMessage());
		}
	}

	/**
	 * @description create an edge between two nodes in the graph
	 * 				with the given attribute (connectType) and the direction
	 * 				being either from left to right (that is node1 --> node2)
	 * 				or right to left (that is node1 <-- node2)
	 *
	 * @param nodeId1
	 * @param nodeId2
	 * @param relationshipType
	 * @param direction LeftToRight or RightToLeft
	 */
	private void createEdge(Long nodeId1, Long nodeId2, String relationshipType, String direction){

		assert direction == "LeftToRight" || direction == "RightToLeft" : "Direction must either be LeftToRight or RightToLeft";

		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";

		logger.debug("createEdge called with ID1 " + nodeId1 + " and ID2 " + nodeId2 + ", edge type " + relationshipType + " and direction " + direction + " - working with " + dbServerUrl);

		// neo4j queries can be represented as ascii art
		String asciiArt;
		asciiArt = "(Node) - [r:RelationshipType] -> (Node)";

		asciiArt = "(" + nodeId1 + ")";
		if (direction == "LeftToRight") {
			asciiArt += " - [r:" + relationshipType + "] -> ";
		} else {
			asciiArt += " <- [r:" + relationshipType + "] - ";
		}
		asciiArt += "(" + nodeId2 + ")";
		logger.debug("this is the asciiArt command " + asciiArt);

		// but of course we use cypher for that
		String cypher;
		cypher = "";
		logger.debug("this is the cypher query " + cypher);

		//prepareConnection();
		String output = null;
		String locationHead = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(nodePointUrl);

		// set header
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);

		logger.debug("this is the header " + mtHeader.toString());

		// establish connections between nodes
		try {
			logger.info("creating connection " + asciiArt);
			StringRequestEntity connectEntity = new StringRequestEntity(asciiArt,
	                                                                        "application/json",
	                                                                        "UTF-8");
			mPost.setRequestEntity(connectEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(status);

			logger.debug("Status is " + statusCode + "/ Ok is " + statusCode.isOk());

			output = mPost.getResponseBodyAsString( );
			Header locationHeader =  mPost.getResponseHeader("location");
			locationHead = locationHeader.getValue();
			mPost.releaseConnection( );

			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (statusCode.isOk()){
				logger.error(Neo4JErrorHandling.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				logger.info("connection between node 1 " + nodeId1 + " and node 2 " + nodeId2 + " created");
			}

			logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);

		} catch(Exception e) {
			logger.error("Creating connection between node 1 (ID " + nodeId1 + ") and node 2 (" + nodeId2 + ") in neo4j " + e.getMessage());
		}
	}


	private Long findNode(String name){
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location; // + "/node";

		logger.debug(" - working with " + dbServerUrl);

		// this is what we'll return upon successful find
		Long graphNodeId = -1L;

		// neo4j queries can be represented as ascii art
		String asciiArt = "";
		asciiArt = "(Node) - [r:RelationshipType] -> (Node)";
		/*
		asciiArt = "(" + nodeId1 + ")";
		if (direction == "leftToRight") {
			asciiArt += " - [r:" + edgeType + "] -> ";
		} else {
			asciiArt += " <- [r:" + edgeType + "] - ";
		}
		asciiArt += "(" + nodeId2 + ")";
		*/
		logger.debug("this is the asciiArt command " + asciiArt);


		// but of course we use cypher for that
		String cypher;
		cypher = "";
		logger.debug("this is the cypher query " + cypher);

		//prepareConnection();
		String output = null;
		String locationHead = null;
		HttpClient client = new HttpClient();
		PostMethod mPost = new PostMethod(nodePointUrl);

		// set header
		Header mtHeader = new Header();
		mtHeader.setName("content-type");
		mtHeader.setValue("application/json");
		mtHeader.setName("accept");
		mtHeader.setValue("application/json");
		mPost.addRequestHeader(mtHeader);

		logger.debug("this is the header " + mtHeader.toString());

		// execute search
		try {
			StringRequestEntity connectEntity = new StringRequestEntity(cypher,
	                                                                        "application/json",
	                                                                        "UTF-8");
			mPost.setRequestEntity(connectEntity);
			int status = client.executeMethod(mPost);
			HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(status);

			logger.debug("Status is " + statusCode + "/ Ok is " + statusCode.isOk());

			output = mPost.getResponseBodyAsString( );
			Header locationHeader =  mPost.getResponseHeader("location");
			locationHead = locationHeader.getValue();
			mPost.releaseConnection( );

			// in case everything is fine, neo4j should return 200, 201 or 202. any other case needs to be investigated
			if (!statusCode.isOk()){
				logger.error(Neo4JErrorHandling.getHttpErrorText(statusCode.getErrorCode()));
			} else {
				logger.info("found the node");
			}

			logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);

		} catch(Exception e) {
			logger.error("Searching data in the graph " + e.getMessage());
		}

		return graphNodeId;
	}

	private String generateJsonRelationship(String endNodeURL,
            								String relationshipType,
            								String ... jsonAttributes
            								) {
		StringBuilder sb = new StringBuilder();

		/* this is how the json-string for a relationship should look like
		{
			"extensions" : {},
			"start" : "http://localhost:7474/db/data/node/133",
			"property" : "http://localhost:7474/db/data/relationship/56/properties/{key}",
			"self" : "http://localhost:7474/db/data/relationship/56",
			"properties" : "http://localhost:7474/db/data/relationship/56/properties",
			"type" : "know",
			"end" : "http://localhost:7474/db/data/node/132",
			"data" : {}
		}
		*/

		/*
			sb.append("{ to : ");
			sb.append(endNodeURL);
			sb.append("");

			sb.append("" + relationshipType + " : ");
			sb.append(relationshipType);
		 */
			if(jsonAttributes == null || jsonAttributes.length < 1) {
				sb.append("");
			} else {
				sb.append(", " + "data" + " : ");
				for(int i = 0; i < jsonAttributes.length; i++) {
					sb.append(jsonAttributes[i]);
					if(i < jsonAttributes.length -1) { // Miss off the final comma
						sb.append(", ");
					}
				}
			}

			sb.append(" }");
			return sb.toString();
	}


	/**
	 * @obsolete
	 * @description opens a connection to the REST-API of the neo4j graph-db server
	 */
	private HttpStatusCode prepareConnection() {
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";

		logger.debug("preparing connection " + dbServerUrl);

		// initialize the return value and whether this is acceptable or not.

		HttpStatusCode statusCode = HttpStatusCode.UNKNOWN;

        try {
	        HttpClient client = new HttpClient();
	        GetMethod mGet = new GetMethod(dbServerUrl);
	        int status = client.executeMethod(mGet);
			statusCode = HttpStatusCode.getHttpStatusCode(status);

			logger.debug("Status is " + statusCode + "/ Ok is " + statusCode.isOk());

	        mGet.releaseConnection();
	    } catch(Exception e) {
	    	logger.error(e);
	    }


		return statusCode;
	}

	/**
	 * @description returns the server status
	 */
	public HttpStatusCode getServerStatus(){
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";

		logger.debug("Querying server status for " + dbServerUrl);

		// initialize the return value and whether this is acceptable or not.

		HttpStatusCode status = HttpStatusCode.UNKNOWN;

        try{
	        HttpClient client = new HttpClient();
	        GetMethod mGet =   new GetMethod(dbServerUrl);
	        status = HttpStatusCode.getHttpStatusCode(client.executeMethod(mGet));
	        mGet.releaseConnection();
	    }catch(Exception e){
	    	logger.error(e);
	    }

        if (!status.isOk()){
        	logger.warn(Neo4JErrorHandling.getHttpErrorText(status.getErrorCode()));
        } else {
        	logger.debug("Return code " + status + " (" + status.getErrorCode() + ") everything should be fine");
        }

	    return status;
	}

	/**
	 * @description Entschluesselt Werte aus der Konfig fuer die Connection
	 *
	 * @param param
	 *            der Wert der entschluesselt werden soll
	 * @return Klartext
	 *
	 */
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// byte-Array kommt vom Decoder zurueck und kann dann in String uebernommen und zurueckgegeben werden
		byte[] base64Array;

		// Validierung das auch ein verschluesselter Wert da angekommen ist
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschluesselt");
		}
		// konvertiere in String
		return new String(base64Array);
	}

	// standard getter and setter
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getServiceUserEndpoint() {
		return serviceUserEndpoint;
	}

	public void setServiceUserEndpoint(String serviceUserEndpoint) {
		this.serviceUserEndpoint = serviceUserEndpoint;
	}

	public String getServicePostEndpoint() {
		return servicePostEndpoint;
	}

	public void setServicePostEndpoint(String servicePostEndpoint) {
		this.servicePostEndpoint = servicePostEndpoint;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
}
