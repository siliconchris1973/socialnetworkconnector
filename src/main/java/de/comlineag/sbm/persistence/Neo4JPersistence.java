package de.comlineag.sbm.persistence;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;

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

	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Credentials
	private String user;
	private String pass;
	
	private final Logger logger = Logger.getLogger(getClass().getName());

	public Neo4JPersistence() {
		logger.debug("Neo4JPersistence called");
		prepareConnection();
	}

	private void prepareConnection() {

		logger.debug("Neo4jPersistence :: Starte prepareConnection");

		//SERVER_ROOT_URI should resolve to http://localhost:7474 on a standard local system
		//String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		String url = this.protocol + "://" + this.host + ":" + this.port;
        int status = 500;

        try {
	        HttpClient client = new HttpClient();
	        GetMethod mGet =   new GetMethod(url);
	        status = client.executeMethod(mGet);
	        //mGet.releaseConnection( );
	    } catch(Exception e) {
	    	logger.error("Exception in connection to neo4j server " + this.protocol + "://" + this.host + ":" + this.port, e);
	    	System.out.println("Exception in connecting to neo4j : " + e);
	    }

		logger.debug("Neo4JPersistence :: Services connected");

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

	/**
	 * @description returns the server status
	 * @return
	 */
	public int getServerStatus(){
	    int status = 500;
	    try{
	    	//SERVER_ROOT_URI should resolve to http://localhost:7474 on a standard local system
	        String url = this.protocol + "://" + this.host + ":" + this.port;
	        HttpClient client = new HttpClient();
	        GetMethod mGet =   new GetMethod(url);
	        status = client.executeMethod(mGet);
	        mGet.releaseConnection( );
	    }catch(Exception e){
	    	logger.error("Exception in connection to neo4j server " + this.protocol + "://" + this.host + ":" + this.port, e);
	    	System.out.println("Exception in connecting to neo4j : " + e);
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
		logger.debug("Neo4JPersistence :: savePosts called");

		try {
			// we only store posts in german and english at the moment
			if (postData.getLang().equalsIgnoreCase("de") || postData.getLang().equalsIgnoreCase("en")) {

				String output = null;
				String location = null;
				String nodePointUrl = this.protocol + "://" + this.host + ":" + this.port + this.location + "/node";
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

				/**
				 * set json payload
				 */
				StringRequestEntity requestEntity = new StringRequestEntity(postData.toString(),
		                                                                        "application/json",
		                                                                        "UTF-8");
				mPost.setRequestEntity(requestEntity);
				int status = client.executeMethod(mPost);
				output = mPost.getResponseBodyAsString( );
				Header locationHeader =  mPost.getResponseHeader("location");
				location = locationHeader.getValue();
				mPost.releaseConnection( );

				logger.info("status = " + status + " / location = " + location + " / output = " + output);
			}
		} catch(Exception e) {
			logger.error("Exception in creating node in neo4j", e);
		    System.out.println("Exception in creating node in neo4j : " + e);
		}
	}

	public void saveUsers(UserData userData) {
		logger.debug("Neo4JPersistence :: saveUsers called");
		try {
			String output = null;
			String location = null;
			String nodePointUrl = this.protocol + "://" + this.host + ":" + this.port + this.location + "/node";
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

			/**
			 * set json payload
			 */
			StringRequestEntity requestEntity = new StringRequestEntity(userData.toString(),
	                                                                        "application/json",
	                                                                        "UTF-8");
			mPost.setRequestEntity(requestEntity);
			int status = client.executeMethod(mPost);
			output = mPost.getResponseBodyAsString( );
			Header locationHeader =  mPost.getResponseHeader("location");
			location = locationHeader.getValue();
			mPost.releaseConnection( );

			logger.info("status = " + status + " / location = " + location + " / output = " + output);

		} catch (Exception e) {
			logger.error("Failure in saveUser" + e.getMessage());
			System.out.println("Exception in creating node in neo4j : " + e);
		}
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
