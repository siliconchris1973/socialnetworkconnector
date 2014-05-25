package de.comlineag.sbm.persistence;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import de.comlineag.sbm.data.HttpStatusCodes;
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

	public Neo4JPersistence() {
		
		/*
		int connectionStatus = 0;
		
		//TODO check what's the matter with the persisted values of protocol, host, port and the like from ApplicationSettings.xml, the do not exist at this stage of the program
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";
		
		if (this.protocol == null || this.host == null || this.port == null || this.location == null){
			logger.debug("no connection to neo4j server - initializing");
			connectionStatus = prepareConnection();
		}
		
		logger.debug("connection status code is " + connectionStatus + " / neo4j server url is " + dbServerUrl + " / nodePointUrl is " + nodePointUrl);
		*/
		
	}

	private int prepareConnection() {
		dbServerUrl = this.protocol + "://" + this.host + ":" + this.port;
		nodePointUrl = dbServerUrl + this.location + "/node";
		
		logger.debug("preparing connection " + dbServerUrl);
		
		// initialize the return value and whether this is acceptable or not. 
		int status = HttpStatus.SC_NO_CONTENT;
		String statusText = HttpStatus.getStatusText(status);
		String okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
        logger.debug("current value (before connection) of okOrNotOk is " + okOrNotOk );		
        
        try {
	        HttpClient client = new HttpClient();
	        GetMethod mGet = new GetMethod(dbServerUrl);
	        status = client.executeMethod(mGet);
	        statusText = HttpStatus.getStatusText(status);
	        okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
	        
	    } catch(Exception e) {
	    	logger.error("Exception in connection to neo4j server " + dbServerUrl, e);
	    	System.out.println("Exception in connecting to neo4j server " + dbServerUrl + " : " + e);
	    }
        
        if (okOrNotOk == "NOK"){
        	logger.warn("Problem connecting, return code " + status + " (" + HttpStatus.getStatusText(status) + ")");
        } else {
        	logger.debug("Connected, return code " + status + " (" + HttpStatus.getStatusText(status) + ")");
        }
        
		return status;
	}
	
	/**
	 * @description returns the server status
	 * @return
	 */
	public int getServerStatus(){
		logger.debug("Querying server status for " + dbServerUrl);

		// initialize the return value and whether this is acceptable or not. 
		int status = HttpStatus.SC_NO_CONTENT;
		String statusText = HttpStatus.getStatusText(status);
		String okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
        
        try{
	        HttpClient client = new HttpClient();
	        GetMethod mGet =   new GetMethod(dbServerUrl);
	        status = client.executeMethod(mGet);
	        mGet.releaseConnection( );
	        
	        status = client.executeMethod(mGet);
	        statusText = HttpStatus.getStatusText(status);
	        okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
	    }catch(Exception e){
	    	logger.error("Exception in connection to neo4j server " + dbServerUrl, e);
	    	System.out.println("Exception in connecting to neo4j " + dbServerUrl + " : " + e);
	    }
        
        if (okOrNotOk == "NOK"){
        	logger.warn("Return code " + status + " (" + HttpStatus.getStatusText(status) + ") something's not right");
        } else {
        	logger.debug("Return code " + status + " (" + HttpStatus.getStatusText(status) + ") everything should be fine");
        }
        
	    return status;
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
	public void savePosts(PostData postData) {
		logger.debug("savePosts for post-id " + postData.getId() + " called - working with " + dbServerUrl);
		prepareConnection();
		
		try {
			// we only store posts in german and english at the moment
			if (postData.getLang().equalsIgnoreCase("de") || postData.getLang().equalsIgnoreCase("en")) {
				String output = null;
				String locationHead = null;
				//String nodePointUrl = this.protocol + "://" + this.host + ":" + this.port + this.location + "/node";
				HttpClient client = new HttpClient();
				PostMethod mPost = new PostMethod(nodePointUrl);
				
				// status handling
				int status = HttpStatus.SC_NO_CONTENT;
				String statusText = HttpStatus.getStatusText(status);
				String okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
				
				logger.debug("Creating node at " + nodePointUrl + " for post (ID " + postData.getId() + " / text (first 20 chars) " + postData.getText().substring(0, 20) + ") in neo4j");
				
				/**
				 * set headers
				 */
				Header mtHeader = new Header();
				mtHeader.setName("content-type");
				mtHeader.setValue("application/json");
				mtHeader.setName("accept");
				mtHeader.setValue("application/json");
				mPost.addRequestHeader(mtHeader);
				
				logger.debug("this is the header " + mtHeader);
				
				/**
				 * set json payload
				 */
				JSONObject p = new JSONObject();
				
				p.put("sn_id", postData.getSnId());
				p.put("post_id", postData.getId());
				p.put("user_id", postData.getUserId());
				p.put("timestamp", postData.getTimestamp());
				p.put("postLang", postData.getLang());
				p.put("text", postData.getText());
				p.put("geoLocation_longitude", postData.getGeoLongitude());
				p.put("geoLocation_latitude", postData.getGeoLatitude());
				p.put("client", postData.getClient());
				p.put("truncated", postData.getTruncated());
				p.put("inReplyTo", postData.getInReplyTo());
				p.put("inReplyToUserID", postData.getInReplyToUser());
				p.put("inReplyToScreenName", postData.getInReplyToUserScreenName());
				p.put("placeID", postData.getGeoPlaceId());
				p.put("plName", postData.getGeoPlaceName());
				p.put("plCountry", postData.getGeoPlaceCountry());
				p.put("plAround_longitude", "00 00 00 00 00");
				p.put("plAround_latitude", "00 00 00 00 00");
				
				logger.debug("about to insert the following data in the graph: " + p.toString());
				
				StringRequestEntity requestEntity = new StringRequestEntity(p.toString(),
		                                                                        "application/json",
		                                                                        "UTF-8");
				mPost.setRequestEntity(requestEntity);
				status = client.executeMethod(mPost);
				statusText = HttpStatus.getStatusText(status);
		        okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
		        
				output = mPost.getResponseBodyAsString( );
				Header locationHeader =  mPost.getResponseHeader("location");
				locationHead = locationHeader.getValue();
				mPost.releaseConnection( );
				
				
				// in case everything is fine, neo4j should return 200. any other case needs to be investigated
				if (okOrNotOk == "NOK"){
					//TODO: establish correct error handling
					logger.error("something went wrong inserting/updating the data in the graph - http status code is " + status + " (" + HttpStatus.getStatusText(status) + ")");
				} else {
					logger.info("post " + postData.getId() + " created at location " + location + " in the graph");
				}
				
				logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);
			}
		} catch(Exception e) {
			logger.error("Exception in creating node for post (ID " + postData.getId() + " / text (first 20 chars) " + postData.getText().substring(0, 20) + ") in neo4j " + e.getMessage());
		    System.out.println("Exception in creating node for post (ID " + postData.getId() + " / text (first 20 chars) " + postData.getText().substring(0, 20) + ") in neo4j : " + e);
		}
	}

	public void saveUsers(UserData userData) {
		logger.debug("saveUsers called - working with " + dbServerUrl);
		prepareConnection();
		
		try {
			String output = null;
			String locationHead = null;
			//String nodePointUrl = this.protocol + "://" + this.host + ":" + this.port + this.location + "/node";
			HttpClient client = new HttpClient();
			PostMethod mPost = new PostMethod(nodePointUrl);
			
			// status handling
			int status = HttpStatus.SC_NO_CONTENT;
			String statusText = HttpStatus.getStatusText(status);
			String okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
			
			logger.debug("Creating node at " + nodePointUrl + " for user (ID " + userData.getId() + " / screenname " + userData.getScreenName() + ") in neo4j");
			
			/**
			 * set headers
			 */
			Header mtHeader = new Header();
			mtHeader.setName("content-type");
			mtHeader.setValue("application/json");
			mtHeader.setName("accept");
			mtHeader.setValue("application/json");
			mPost.addRequestHeader(mtHeader);
			
			logger.debug("this is the header " + mtHeader);
			
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
			status = client.executeMethod(mPost);
			statusText = HttpStatus.getStatusText(status);
	        okOrNotOk = HttpStatusCodes.valueOf(statusText).toString();
	        
			output = mPost.getResponseBodyAsString( );
			Header locationHeader = mPost.getResponseHeader("location");
			locationHead = locationHeader.getValue();
			mPost.releaseConnection( );
			
			// in case everything is fine, neo4j should return 200. any other case needs to be investigated
			if (okOrNotOk == "NOK"){
				//TODO: establish correct error handling
				logger.error("something went wrong inserting/updating the user in the graph - http status code is " + status + " (" + HttpStatus.getStatusText(status) + ")");
			} else {
				logger.info("user " + userData.getScreenName() + " created at location " + location + " in the graph");
			}
			
			logger.debug("status = " + status + " / location = " + locationHead + " / output = " + output);
			
			
		} catch (Exception e) {
			logger.error("Exception in creating node for user (ID " + userData.getId() + " / name " + userData.getScreenName() + ") in neo4j " + e.getMessage());
			System.out.println("Exception in creating node for user (ID " + userData.getId() + " / name " + userData.getScreenName() + ") in neo4j : " + e);
		}
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
