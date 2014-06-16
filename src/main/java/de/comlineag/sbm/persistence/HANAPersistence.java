package de.comlineag.sbm.persistence;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
//import org.odata4j.edm.EdmDataServices;


import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

/**
 *
 * @author Magnus Leinemann, Christian Guenther
 * @category Connector Class
 *
 * @description handles the connectivity to SAP HANA Systems and saves posts and users in the DB
 * @version 1.1
 *
 */
public class HANAPersistence implements IPersistenceManager {

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

	private static ODataConsumer userService;
	private static ODataConsumer postService;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public HANAPersistence() {
		logger.debug("HANAPersistence called");
	}

	/**
	 * Speichern der Post auf HANA mit folgenden Servicedaten:
	 *
	 * <Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
	 * <Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
	 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="timestamp" Type="Edm.DateTime"/>
	 * <Property Name="postLang" Type="Edm.String" MaxLength="64"/>
	 * <Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="1024"/>
	 * <Property Name="raw_text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
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
		logger.debug("savePosts called for post with id " + postData.getId());
		int truncated = (postData.getTruncated()) ? 0 : 1;

		try {
			if (postService == null)
				prepareConnections();
			
			//TODO Check if we really need to check on languages at all. this looks like a bad workaround to me
			if ( (postData.getLang().equalsIgnoreCase("de") || postData.getLang().equalsIgnoreCase("en")) ) {

				logger.trace("Setting timestamp " + postData.getTimestamp().toString());
				
				try{
					OEntity newPost = postService.createEntity("post")
							.properties(OProperties.string("sn_id", postData.getSnId()))
							.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
							.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
							.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
							.properties(OProperties.string("postLang", postData.getLang()))
							.properties(OProperties.string("text", postData.getText()))
							.properties(OProperties.string("raw_text", postData.getRawText()))
							.properties(OProperties.string("geoLocation_longitude", postData.getGeoLongitude()))
							.properties(OProperties.string("geoLocation_latitude", postData.getGeoLatitude()))
							.properties(OProperties.string("client", postData.getClient()))
							.properties(OProperties.int32("truncated", new Integer(truncated)))
	
							.properties(OProperties.int64("inReplyTo", postData.getInReplyTo()))
							.properties(OProperties.int64("inReplyToUserID", postData.getInReplyToUser()))
							.properties(OProperties.string("inReplyToScreenName", postData.getInReplyToUserScreenName()))
							// .properties(OProperties.string("placeID", postData.getLocation()))
							// .properties(OProperties.string("plName", "Client"))
							// .properties(OProperties.string("plCountry", "Client"))
							// .properties(OProperties.string("plAround_longitude", "Client"))
							// .properties(OProperties.string("plAround_latitude", "Client"))
	
							.execute();
					
					logger.info("New post " + newPost.getEntityKey().toKeyString());
				} catch (Exception le){
					logger.error("EXCEPTION :: Odata call failed " + le.getLocalizedMessage());
				}
			}
		} catch (NoBase64EncryptedValue e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error("EXCEPTION :: Failure in savePost " + e.getLocalizedMessage(), e);
		}

	}

	public void saveUsers(UserData userData) {
		logger.debug("saveUsers called for user " + userData.getScreenName());
		//EdmDataServices serviceMeta;

		try {
			if (userService == null)
				prepareConnections();

			OEntity newUser = userService.createEntity("user")
					.properties(OProperties.string("sn_id", userData.getSnId()))
					.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("userName", userData.getUsername()))

					.properties(OProperties.string("nickName", userData.getScreenName()))
					.properties(OProperties.string("userLang", userData.getLang()))
					.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))

					.execute();

			/*
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

			logger.info("New user " + newUser.getEntityKey().toKeyString());

		} catch (NoBase64EncryptedValue e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error("EXCEPTION :: Failure in saveUser: " + e.getLocalizedMessage(), e);
		}
	}

	private void prepareConnections() throws NoBase64EncryptedValue {

		logger.debug("Start prepareConnection");

		String _user = decryptValue(this.user);
		String _pw = decryptValue(this.pass);
		// logger.debug("b64 USER: " + _user);
		// logger.debug("b64 PASS: " + _pw);

		BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
		String baseLocation = new String("http://" + this.host + ":" + this.port + this.location);
		String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);

		ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
		builder.setClientBehaviors(bAuth);
		userService = builder.build();

		builder = ODataConsumer.newBuilder(postURI);
		builder.setClientBehaviors(bAuth);
		postService = builder.build();

		logger.debug("HANAPersistence Services connected");

	}

	public void setPostingTextWithJdbc(String textElement){
		try {
			Class.forName("com.sap.db.jdbc.Driver");
			java.sql.Connection conn = java.sql.DriverManager.getConnection(""
					+ "jdbc:sap://"+this.host+":"+this.port,this.user,this.pass);
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "UPDATE text" );
			
			SimpleDateFormat sd = new SimpleDateFormat("dd.MM.yyyy");
			while(rs.next()) {
				System.out.print( rs.getString(1) + " | "); System.out.print( rs.getString(2) + " ");
				System.out.print( rs.getString(3) + " | "); System.out.print( sd.format(rs.getTimestamp(4)) + " | "); System.out.println( rs.getString(5) );
			}
			rs.close() ; stmt.close() ; conn.close() ;
		} catch(Exception e) {
			logger.error("EXCEPTION :: could not create element " + e.getStackTrace().toString());
		}
	}
	
	/**
	 * Entschluesselt Werte aus der Konfig fuer die Connection
	 *
	 * @param param
	 *            der Wert der entschluesselt werden soll
	 * @return Klartext
	 *
	 */
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// the decode returns a byte-Array - this is converted in a string and returned
		byte[] base64Array;

		// Check that the returned string is correctly coded as bas64
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("EXCEPTION :: Parameter " + param + " not Base64-encrypted: " + e.getLocalizedMessage());
		}
		return new String(base64Array);
	}

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
