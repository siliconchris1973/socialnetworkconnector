package de.comlineag.sbm.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTimeZone;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Connector Class
 * @version 	1.4
 *
 * @description handles the connectivity to the SAP HANA Systems and saves posts and users in the DB
 * 
 * @changelog	1.0 savePost implemented
 * 				1.1	saveUsers implemented
 * 				1.2	added support for encrypted user and password
 * 				1.3	added JDBC support
 * 				1.4	added search for dataset prior inserting one 
 *
 */
public class HANAPersistence implements IPersistenceManager {

	// Servicelocation
	private String host;
	private String port;
	private String jdbcPort;
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

	public HANAPersistence() {}

	/**
	 * save a post from social network to the HANA DB with these elements
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
	
	 * <Property Name="client" Type="Edm.String" MaxLength="2048"/>
	 * <Property Name="truncated" Type="Edm.Byte" DefaultValue="0"/>
	 * <Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
	 *
	 * <Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 * <Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * <Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 * @throws NoBase64EncryptedValue 
	 * 
	 */
	public void savePosts(PostData postData) {
		// first check if the dataset is already in the database
		if (!checkIfEntryExist(postData.getId(), postData.getSnId(), "post")) {
			logger.info("savePost called for message ("+postData.getSnId()+"-"+postData.getId()+")");
			
			// static variant to set the truncated flag - which is not used anyway at the moment
			int truncated = (postData.getTruncated()) ? 1 : 0;
		
			// first try to save the data via jdbc and fall back to OData (see below) if that is not possible
			try{
				Class.forName("com.sap.db.jdbc.Driver");
				String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
				String user = null;
				String password  = null;
				
				// get the user and password for the JDBC connection
				try {
					user = decryptValue(this.user);
					password = decryptValue(this.pass);
				} catch (NoBase64EncryptedValue e) {
					logger.error("EXCEPTION :: could not decrypt user and/or password - not encrypted? " + e.getMessage(), e);
				}
	            
	            logger.trace("trying to insert data with jdbc url="+url+" user="+user);
	            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
				
	            // prepare the SQL statement
				String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
						+ "(\"sn_id\", \"post_id\",\"user_id\",\"timestamp\",\"postLang\" "
						+ ",\"text\",\"raw_text\",\"teaser\",\"subject\",\"viewcount\","
						+ ",\"favoritecount\",\"client\",\"truncated\",\"inReplyTo\" "
						+ ",\"inReplyToUserID\",\"inReplyToScreenName\" "
						// TODO activate these, when geo location works
						//+ ",\"geoLocation_longitude\",\"geoLocation_latitude\" "
						//+ ",\"placeID\",\"plName\",\"plCountry\" "
						//+ ",\"plAround_longitude\",\"plAround_latitude\" "
						+ ") "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" 
						//+ "?,?,?,?,?,?,?"
						+ ")";
				
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
				// TODO activate these, when geo location works
				//stmt.setString(17, postData.getGeoLongitude());
				//stmt.setString(18, postData.getGeoLatitude());
				//stmt.setString(19, postData.getLocation());
				//stmt.setString(20, postData.getPLName());
				//stmt.setString(21, postData.getPLCountry());
				//stmt.setString(22, postData.getPLAroundLongitude());
				//stmt.setString(23, postData.getPLAroundLatitude());
				
				int rowCount = stmt.executeUpdate();
				
				// TODO establish proper error handling
				logger.trace("any warnings?: " + stmt.getWarnings());
				logger.info("Post ("+postData.getSnId()+"-"+postData.getId()+") added to DB ("+rowCount+" rows added)");
				
				stmt.close() ; conn.close() ;
			
			// in case the jdbc library to connect to the SAP HANA DB is not available, fall back to OData to save the post
			} catch (java.lang.ClassNotFoundException le) {
				logger.warn("JDBC driver not available - falling back to OData");
				
				if (postService == null) {
					try {
						String _user = decryptValue(this.user);
						String _pw = decryptValue(this.pass);
						
						BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
						String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
						String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
						logger.debug("Initiating connection to service endpoint "+postURI+" of the hana database");
						
						ODataConsumer.Builder builder = ODataConsumer.newBuilder(postURI);
						builder.setClientBehaviors(bAuth);
						postService = builder.build();
						
						logger.info("HANAPersistence Service for users connected");
					} catch (NoBase64EncryptedValue e) {
						logger.error("EXCEPTION :: could not decrypt user and/or password - not encrypted? " + e.getMessage(), e);
					} catch (Exception e) {
						logger.error("EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage());
						e.printStackTrace();
					}
				} else {
					logger.debug("already connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
				}
				
				// now let us try 
				try {
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
						
					logger.info("New post ("+postData.getSnId()+"-"+postData.getId()+") created");
				
				} catch (RuntimeException e) {
					logger.error("ERROR :: could not create post ("+postData.getSnId()+"-"+postData.getId()+"): " + e.getLocalizedMessage());
					e.printStackTrace();
				} catch (Exception e) {
					logger.error("EXCEPTION :: unforseen error condition, post ("+postData.getSnId()+"-"+postData.getId()+") NOT created: " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			} catch (SQLException le){
				logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
				le.printStackTrace();
			} catch (Exception le) {
				logger.error("EXCEPTION :: unforseen error condition, post ("+postData.getSnId()+"-"+postData.getId()+") NOT created: " + le.getLocalizedMessage());
				le.printStackTrace();
			}
		} else {
			logger.info("the post ("+postData.getSnId()+"-"+postData.getId()+") is already in the database");
		}
	}

	public void saveUsers(UserData userData) {
		// first check if the dataset is already in the database
		if (!checkIfEntryExist(userData.getId(), userData.getSnId(), "user")) {
			logger.info("saveUser called for user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+")");
			
			// first try to save the data via jdbc and fall back to OData (see below) if that is not possible
			try {
				Class.forName("com.sap.db.jdbc.Driver");
				String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
				// decrypting the values because the jdbc driver needs these values in clear text
				String user = null;
				String password = null;
				
				// get the user and password for the JDBC connection
				try {
					user = decryptValue(this.user);
					password = decryptValue(this.pass);
				} catch (NoBase64EncryptedValue e) {
					logger.error("EXCEPTION :: could not decrypt user and/or password - not encrypted? " + e.getMessage(), e);
				}
	            
	            logger.debug("trying to insert data with jdbc url="+url+" user="+user);
	            
	            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
	            String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::users\" "
	            		+ "(\"sn_id\",\"user_id\",\"userName\",\"nickName\",\"userLang\",\"follower\","
	            		+ "\"friends\",\"postingsCount\",\"favoritesCount\",\"listsAndGroupsCount\") "
	            		+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
				 
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
				
				// TODO establish proper error handling
				logger.trace("any warnings?: " + stmt.getWarnings());
				logger.info("User "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") added to DB ("+rowCount+" rows added)");
				
				stmt.close() ; conn.close() ;
				
			// in case the jdbc library to connect to the SAP HANA DB is not available, fall back to OData to save the user
			} catch (java.lang.ClassNotFoundException le) {
				logger.warn("JDBC driver not available - falling back to OData");
				
				if (userService == null) {
					try {
						String _user = decryptValue(this.user);
						String _pw = decryptValue(this.pass);
						String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
						String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
							
						BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
						logger.debug("Initiating connection to service endpoint "+userURI+" of the hana database");
						
						ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
						builder.setClientBehaviors(bAuth);
						userService = builder.build();
						
						logger.info("HANAPersistence Service for users connected");
					} catch (NoBase64EncryptedValue e) {
						logger.error("EXCEPTION :: could not decrypt user and/or password - not encrypted? " + e.getMessage(), e);
					} catch (Exception e) {
						logger.error("EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage());
						e.printStackTrace();
					}
				} else {
					logger.debug("already connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
				}
				
				try {
					OEntity newUser = userService.createEntity("user")
							.properties(OProperties.string("sn_id", userData.getSnId()))
							.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
							.properties(OProperties.string("userName", userData.getUsername()))
		
							.properties(OProperties.string("nickName", userData.getScreenName()))
							.properties(OProperties.string("userLang", userData.getLang()))
							// TODO check location values for user
							//.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))
		
							.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
							.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
							.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
							.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
							.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))
		
							.execute();
						
					logger.info("New user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") created");
				} catch (RuntimeException e) {
					// TODO find out how to retrieve return status from odata call
					// <?xml version="1.0" encoding="utf-8" standalone="yes"?><error xmlns="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"><code/><message xml:lang="en-US">Service exception: inserted value too large for column.</message></error>
					
					logger.error("ERROR :: Could not create user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
					e.printStackTrace();
				} catch (Exception e) {
					logger.error("EXCEPTION :: unforseen error condition, user ("+userData.getSnId()+"-"+userData.getId()+") NOT added to the DB: " + e.getLocalizedMessage());
					e.printStackTrace();
				}
				
			} catch (SQLException le){
				logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted: " + le.getLocalizedMessage());
				le.printStackTrace();
			} catch (Exception le) {
				logger.error("EXCEPTION :: unforseen error condition, user ("+userData.getSnId()+"-"+userData.getId()+") NOT added to the DB: " + le.getLocalizedMessage());
				le.printStackTrace();
			}
		} else {
			logger.info("The user " + userData.getUsername() + " (" + userData.getSnId()  + "-" + userData.getId() + ") is already in the database");
		}
	}
	
	
	/**
	 * 
	 * @description searches for users or posts in the database and returns true if the dataset
	 * 				exists, or false in any other case - including errors, that is!	
	 * 
	 * @param 		id
	 * 					id of the user or the post
	 * @param 		sn_id
	 * 					shortcut of the social network as defined in enum SocialNetworks 
	 * @param 		type
	 * 					must be set to either post or user
	 * 
	 * @return 		true on success (found) or false in case of ANY error
	 */
	private boolean checkIfEntryExist(long id, String sn_id, String type) {
		assert type == "user" || type == "post" : "type must be either user or post";
		
		BasicAuthenticationBehavior bAuth = null;
		String _user = null;
		String _pw = null;
		ODataConsumer.Builder builder = null;
		String entitySetName = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
		String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		
		try {
			_user = decryptValue(this.user);
			_pw = decryptValue(this.pass);
			bAuth = new BasicAuthenticationBehavior(_user, _pw);
		} catch (NoBase64EncryptedValue e) {
			logger.error("EXCEPTION :: could not decrpt values for user and passowrd " + e.getMessage());
			return false;
		}
		
		logger.debug("checking if "+type+" " + sn_id + "-" + id + " at " + postURI + " exists");
		logger.trace("     authenticated as user " + _user);
		
		if (type == "user") {
			builder = ODataConsumer.newBuilder(userURI);
			builder.setClientBehaviors(bAuth);
			userService = builder.build();
			
			entitySetName = "user";
			
			// query for the user by id
			try {
				userService.getEntities(entitySetName).filter("user_id eq '"+id+"' and sn_id eq '" + sn_id + "'").top(1).execute().first();
			} catch (Exception e) {
				return false;
			}
			
			return true;
		} else {
			builder = ODataConsumer.newBuilder(postURI);
			builder.setClientBehaviors(bAuth);
			postService = builder.build();
			
			entitySetName = "post";
			
			// query for the post by id
			try {
				postService.getEntities(entitySetName).filter("post_id eq '"+id+"' and sn_id eq '" + sn_id + "'").top(1).execute().first();
			} catch (Exception e) {
				return false;
			}
			
		    return true; 
		}
	}
	
	
	
	/**
	 * @description Decrypts configuration values 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 *
	 * @throws 		NoBase64EncryptedValue
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

	public void setJdbcPort(String jdbcport) {
		this.jdbcPort = jdbcport;
	}
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
}
