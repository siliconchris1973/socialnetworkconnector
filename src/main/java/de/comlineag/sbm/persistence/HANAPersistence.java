package de.comlineag.sbm.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;

import de.comlineag.sbm.data.SocialNetworks;
import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Connector Class
 *
 * @description handles the connectivity to the SAP HANA Systems and saves posts and users in the DB
 * @version 	1.3
 *
 */
public class HANAPersistence implements IPersistenceManager {

	// Servicelocation
	private String host;
	private String port;
	private String jdbcPort="30015";
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
	 * <Property Name="subject" Type="Edm.String" DefaultValue="" MaxLength="20"/>
	 * <Property Name="teaser" Type="Edm.String" DefaultValue="" MaxLength="256"/>
	
	 * <Property Name="viewcount" Type="Edm.int" DefaultValue=0/>
	 * <Property Name="favoritecount" Type="Edm.int" DefaultValue=0/>
	
	 * <Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	
	 * <Property Name="client" Type="Edm.String" MaxLength="2048"/>
	 * <Property Name="truncated" Type="Edm.Byte" DefaultValue="0"/>
	 * <Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
	 *
	 */
	public void savePosts(PostData postData) {
		logger.debug("savePosts called for post with id " + postData.getId());
		
		// first check if the post was already added to the database
		try {
			if (postService == null)
				prepareConnections();
			
			logger.trace("trying to find the message " + postData.getId() + " from network " + SocialNetworks.getNetworkNameByValue(postData.getSnId()) + " in the db...");
			OEntity queryPost = (OEntity) postService.getEntity("id", postData.getId());
			logger.trace(queryPost.getEntityKey());		
					//.properties(OProperties.string("sn_id", postData.getSnId()))
					//.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString()));
			
		} catch (NoBase64EncryptedValue e1) {
			logger.error("could not decrypt values from configuration " + e1.getMessage().toString());
		} catch (Exception e2) {
			logger.error("could not query for the post ", e2);
		}
		
		// static variant to set the truncated flag - not used anyway
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		try {
			if (postService == null)
				prepareConnections();
			
			//TODO Check if we really need to check on languages at all. this looks like a bad workaround to me
			if ( (postData.getLang().equalsIgnoreCase("de") || postData.getLang().equalsIgnoreCase("en")) ) {

				logger.trace("Setting timestamp " + postData.getTimestamp().toString());
				
				try{
					
					
					Class.forName("com.sap.db.jdbc.Driver");
					String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
					
		            String user = decryptValue(this.user);
		            String password = decryptValue(this.pass);
		            
		            logger.trace("trying to insert data with jdbc url="+url+" user="+user+" Password="+password);
		            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
					
					String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
							+ "(\"sn_id\", \"post_id\",\"user_id\",\"timestamp\",\"postLang\",\"text\",\"raw_text\",\"teaser\",\"subject\",\"viewcount\",\"favoritecount\",\"client\",\"truncated\",\"inReplyTo\",\"inReplyToUserID\",\"inReplyToScreenName\" ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					
					PreparedStatement stmt = conn.prepareStatement(sql);
					logger.trace("SQL: "+sql);
					
					stmt.setString(1, postData.getSnId());
					stmt.setLong(2, new Long(postData.getId()));
					stmt.setLong(3,new Long(postData.getUserId()));
					stmt.setTimestamp(4,new Timestamp((postData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
					stmt.setString(5, postData.getLang());
					stmt.setString(6, postData.getText());
					stmt.setString(7, postData.getRawText());
					stmt.setString(8, postData.getTeaser());
					stmt.setString(9, postData.getSubject());
					stmt.setLong(10, postData.getViewCount());
					stmt.setLong(11, postData.getFavoriteCount());
					stmt.setString(12, postData.getClient());
					stmt.setInt(13, truncated);
					stmt.setLong(14, postData.getInReplyTo());
					stmt.setLong(15, postData.getInReplyToUser());
					stmt.setString(16, postData.getInReplyToUserScreenName());
					
					int rowCount = stmt.executeUpdate();
					stmt.close() ; conn.close() ;
					logger.trace("Insert "+rowCount+" Rows");
				} catch (java.lang.ClassNotFoundException le) {
					logger.warn("JDBC driver not available - falling back to OData");
					
					logger.trace("this is the data we are sending over the wire: \n"
														+ "			sn_id				"	+ postData.getSnId() 						+ "\n"
														+ "			post_id				" 	+ new Long(postData.getId()).toString() 	+ "\n"
														+ "			user_id				" 	+ new Long(postData.getUserId()).toString()	+ "\n"
														+ "			timestamp			" 	+ postData.getTimestamp()					+ "\n"
														+ "			postLang			" 	+ postData.getLang()						+ "\n"
														+ "			text				" 	+ postData.getText().substring(0, 50)		+ "...\n"
														+ "			raw_text			" 	+ postData.getRawText().substring(0, 50)	+ "...\n"
														+ "			teaser				" 	+ postData.getTeaser()						+ "\n"
														+ "			subject				" 	+ postData.getSubject()						+ "\n"
														+ "			viewcount			" 	+ new Long(postData.getViewCount())			+ "\n"
														+ "			favoritecount		" 	+ new Long(postData.getFavoriteCount())		+ "\n"
														// TODO when geo location is working, reactivate these fields
														//+ "			geoLocation_longitude	"	+ postData.getGeoLongitude()				+ "\n"
														//+ "			geoLocation_latitude	" 	+ postData.getGeoLatitude()					+ "\n"
														//+ "			placeID 				" 	+ postData.getLocation()					+ "\n"
														//+ "			plName 					" 	+ postData.getPLName()						+ "\n"
														//+ "			plCountry 				" 	+ postData.getPLCountry()					+ "\n"
														//+ "			plAround_longitude 		" 	+ postData.getPLAroundLongitude()			+ "\n"
														//+ "			plAround_latitude 		" 	+ postData.getPLAroundLatitude()			+ "\n"
														+ "			client 				" 	+ postData.getClient()						+ "\n"
														+ "			truncated 				" 	+ new Integer(truncated)					+ "\n"
														+ "			inReplyTo 				" 	+ postData.getInReplyTo()					+ "\n"
														+ "			inReplyToUserID 		" 	+ postData.getInReplyToUser()				+ "\n"
														+ "			inReplyToScreenName 	" 	+ postData.getInReplyToUserScreenName()		+ "\n"	
							);
					
					OEntity newPost = postService.createEntity("post")
							.properties(OProperties.string("sn_id", postData.getSnId()))
							.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
							.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
							.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
							.properties(OProperties.string("postLang", postData.getLang()))
							
							.properties(OProperties.string("text", postData.getText()))
							.properties(OProperties.string("raw_text", postData.getRawText()))
							.properties(OProperties.string("teaser", postData.getTeaser()))
							.properties(OProperties.string("subject", postData.getSubject()))
							
							.properties(OProperties.int64("viewcount", new Long(postData.getViewCount())))
							.properties(OProperties.int64("favoritecount", new Long(postData.getFavoriteCount())))
							
							// TODO when geo location is working, reactivate these fields
							//.properties(OProperties.string("geoLocation_longitude", postData.getGeoLongitude()))
							//.properties(OProperties.string("geoLocation_latitude", postData.getGeoLatitude()))
							//.properties(OProperties.string("placeID", postData.getLocation()))
							//.properties(OProperties.string("plName",  postData.getPLName()))
							//.properties(OProperties.string("plCountry", postData.getPLCountry()))
							//.properties(OProperties.string("plAround_longitude", postData.getPLAroundLongitude()))
							//.properties(OProperties.string("plAround_latitude", postData.getPLAroundLatitude()))
							
							.properties(OProperties.string("client", postData.getClient()))
							.properties(OProperties.int32("truncated", new Integer(truncated)))
	
							.properties(OProperties.int64("inReplyTo", postData.getInReplyTo()))
							.properties(OProperties.int64("inReplyToUserID", postData.getInReplyToUser()))
							.properties(OProperties.string("inReplyToScreenName", postData.getInReplyToUserScreenName()))
							
							.execute();
					
					logger.info("New post " + newPost.getEntityKey().toKeyString());
					
				} catch (Exception le){
					logger.error("EXCEPTION :: JDBC call failed " + le.getLocalizedMessage());
					le.printStackTrace();
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
		
		try {
			if (userService == null)
				prepareConnections();
			
			logger.trace("trying to insert data with jdbc");
			
			Class.forName("com.sap.db.jdbc.Driver");
			String url = "jdbc:sap://"+this.host+":"+this.port+"/CL_SBM";
            // decrypting the values because the jdbc driver needs these values in clear text
            String user = decryptValue(this.user);
            String password = decryptValue(this.pass);

            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
            String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::users\" (\"sn_id\",\"user_id\",\"userName\",\"nickName\",\"userLang\",\"follower\",\"friends\",\"postingsCount\",\"favoritesCount\",\"listsAndGroupsCount\") VALUES (?,?,?,?,?,?,?,?,?,?)";
			//ToDo follower,friends,postimgCount,favoritesCount,listsAndGroupsCount sollten in eine extra Tabelle ausgelagert werden mit user_id und Timestamp um den zeitlichen Verlauf dieser Werte auswerten zu k��nnen
            
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userData.getSnId());
			stmt.setLong(2, userData.getId());
			stmt.setString(2, userData.getUsername());
			stmt.setString(3, userData.getScreenName());
			stmt.setString(4, userData.getLang());
			stmt.setLong(5, userData.getFollowersCount());
			stmt.setLong(6, userData.getFriendsCount());
			stmt.setLong(7, userData.getPostingsCount());
			stmt.setLong(8, userData.getFavoritesCount());
			stmt.setLong(9, userData.getListsAndGrooupsCount());
			stmt.setString(10, userData.getSnId());
			
			int rowCount = stmt.executeUpdate();
			 logger.trace("Insert "+rowCount+" Rows");
			 stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("JDBC driver not available - falling back to OData");
			
			logger.trace("this is the data we are sending over the wire: \n"
												+ "			sn_id				"	+ userData.getSnId() 													+ "\n"
												+ "			user_id				" 	+ new String(new Long(userData.getId()).toString())						+ "\n"
												+ "			username			" 	+ userData.getUsername()												+ "\n"
												+ "			nickname			" 	+ userData.getScreenName()												+ "\n"
												+ "			userLang			" 	+ userData.getLang() 													+ "\n"
												//+ "			location			" 	+ userData.getLocation().get(0).toString()  						+ "\n"
												+ "			follower			" 	+ new Integer((int) userData.getFollowersCount()) 						+ "\n"
												+ "			friends				" 	+ new Integer((int) userData.getFriendsCount()) 						+ "\n"
												+ "			postingsCount		" 	+ new Integer((int) userData.getPostingsCount()) 						+ "\n"
												+ "			favoritesCount		" 	+ new Integer((int) userData.getFavoritesCount()) 						+ "\n"
												+ "			listsAndGroupsCount	" 	+ new Integer((int) userData.getListsAndGrooupsCount()) 				+ "\n"
					);
			
			OEntity newUser = userService.createEntity("user")
					.properties(OProperties.string("sn_id", userData.getSnId()))
					.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("userName", userData.getUsername()))

					.properties(OProperties.string("nickName", userData.getScreenName()))
					.properties(OProperties.string("userLang", userData.getLang()))
					//.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))

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

	/**
	 * 
	 * @description		update a the text field element of a post with a given string 
	 * @param 			txtString
	 * 						the new string
	 * @param 			idToUpdate
	 * 						the element to update identified by post_id
	 */
	public void setPostingTextWithJdbc(String txtField, String txtString, long idToUpdate){
		
		try {
			Class.forName("com.sap.db.jdbc.Driver");

            // decrypting the values because the jdbc driver needs these values in clear text
			String user = decryptValue(this.user);
            String password = decryptValue(this.pass);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
            logger.trace("trying to insert data with jdbc url="+url+" user="+user+" Password="+password);
            
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			String sql="UPDATE \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" set \"" + txtField + "\" = ? where \"post_id\" =  ?";
												
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, txtString);
            stmt.setLong(2, idToUpdate);
			
			// this is the update statement as executed from sql console
			// UPDATE "CL_SBM"."comline.sbm.data.tables::posts_1" set "CL_SBM"."comline.sbm.data.tables::posts_1"."text" = 'XYZ' where "CL_SBM"."comline.sbm.data.tables::posts_1"."post_id" = '1'

			int rowCount = stmt.executeUpdate( );
			 logger.trace("Insert "+rowCount+" Rows");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.error("EXCEPTION :: the jdbc driver could not be found. Could not update text fields in database");
			
		} catch(Exception e) {
			logger.error("EXCEPTION :: could not update element " + idToUpdate + ": " + e.getStackTrace().toString());
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

	public String getJdbcPort() {
		return jdbcPort;
	}

	public void setJdbcPort(String port) {
		this.jdbcPort = port;
	}
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}
