package de.comlineag.snc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;

import de.comlineag.snc.crypto.GenericEncryptionException;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationEncryptionHandler;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Connector Class
 * @version 	0.9c
 * @status		beta
 *
 * @description handles the connectivity to the SAP HANA Systems and saves posts and users in the DB
 * 
 * @changelog	0.1 (Chris)			skeleton created
 * 				0.2	(Magnus)		savePost implemented
 * 				0.3 				saveUser implemented
 * 				0.4 				added skeleton for geo-location information
 * 				0.5 				added support for encrypted user and password
 * 				0.6 				bug fixing and optimization
 * 				0.7 (Chris)			first productive version, saves users and posts as is (no geo-information)
 *				0.8	(Thomas)		added JDBC support
 * 				0.9	(Chris)			added search for dataset prior inserting one
 * 				0.9a 				added query to determine if an exception during persistence operation shall 
 * 									terminate the crawler or not
 * 				0.9b				moved Base64Encryption in its own class
 * 				0.9c				added support for different encryption provider, the actual one is set in applicationContext.xml 
 *
 * TODO	1. establish Update functionality for user and posts
 * TODO	2. enable query on field dimensions in DB so that (e.g. String)-fields can be truncated prior inserting
 * TODO	3. establish proper error handling (see comments in source code)
 * TODO 4. enable location support for user
 * TODO 5. clean up code - split the big methods saveXXX and create private methods
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
	
	// as the name says, if set to true, the crawler will be terminated in case of a persistence exception
	private static final boolean terminateJobOnException = true;
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationEncryptionHandler configurationEncryptionProvider = new ConfigurationEncryptionHandler();
	
	
	public HANAPersistence() {}

	/**
	 * @description save a post from social network to the HANA DB with these elements
	 *
	 * <Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
	 * <Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
	 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 * <Property Name="timestamp" Type="Edm.DateTime"/>
	 * <Property Name="postLang" Type="Edm.String" MaxLength="64"/>
	
	 * <Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
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
	 * 
	 * @throws GenericEncryptionException 
	 * 
	 */
	public void savePosts(PostData postData) {
		// first check if the dataset is already in the database
		if (!checkIfEntryExist(postData.getId(), postData.getSnId(), "post")) {
			logger.info("Inserting post ("+postData.getSnId()+"-"+postData.getId()+") into the DB");
			
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
					user = configurationEncryptionProvider.decryptValue(this.user);
					password = configurationEncryptionProvider.decryptValue(this.pass);
				} catch (GenericEncryptionException e) {
					logger.error("EXCEPTION :: could not decrypt user and/or password" + e.getMessage(), e);
				}
	            
	            logger.trace("trying to insert data with jdbc url="+url+" user="+user);
	            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
				
	            // prepare the SQL statement
				String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
						+ "(\"sn_id\", \"post_id\",\"user_id\",\"timestamp\",\"postLang\" "
						+ ",\"text\",\"raw_text\",\"teaser\",\"subject\",\"viewcount\","
						+ ",\"favoritecount\",\"client\",\"truncated\",\"inReplyTo\" "
						+ ",\"inReplyToUserID\",\"inReplyToScreenName\" "
						
						+ ",\"geoLocation_longitude\","
						+ "\"geoLocation_latitude\" "
						+ ",\"placeID\","
						+ "\"plName\","
						+ "\"plCountry\" "
						+ ",\"plAround_longitude\","
						+ "\"plAround_latitude\" "
						+ ") "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" 
						// these are for the geo location information
						+ "?,?,?,?,?,?,?"
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
				stmt.setString(17, postData.getGeoLongitude());
				stmt.setString(18, postData.getGeoLatitude());
				stmt.setString(19, postData.getGeoPlaceId());
				stmt.setString(20, postData.getGeoPlaceName());
				stmt.setString(21, postData.getGeoPlaceCountry());
				stmt.setString(22, postData.getGeoAroundLongitude());
				stmt.setString(23, postData.getGeoAroundLatitude());
				
				int rowCount = stmt.executeUpdate();
				
				// TODO establish proper error handling
				logger.trace("any warnings?: " + stmt.getWarnings());
				logger.info("Post ("+postData.getSnId()+"-"+postData.getId()+") added to DB");
				logger.debug(rowCount+" rows added");
				
				stmt.close() ; conn.close() ;
			
			// in case the jdbc library to connect to the SAP HANA DB is not available, fall back to OData to save the post
			} catch (java.lang.ClassNotFoundException le) {
				logger.warn("JDBC driver not available - falling back to OData");
				
				// in case the connection is NOT opened already, attempt to create an OData Consumer
				if (postService == null) {
					try {
						String _user = configurationEncryptionProvider.decryptValue(this.user);
						String _pw = configurationEncryptionProvider.decryptValue(this.pass);
						
						BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
						String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
						String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
						logger.debug("Initiating connection to service endpoint "+postURI+" of the hana database");
						
						ODataConsumer.Builder builder = ODataConsumer.newBuilder(postURI);
						builder.setClientBehaviors(bAuth);
						postService = builder.build();
						
						logger.info("HANAPersistence Service for users connected");
					} catch (GenericEncryptionException e) {
						logger.error("EXCEPTION :: could not decrypt user and/or password. " + e.getMessage(), e);
						if (terminateJobOnException)
							System.exit(-1);
					} catch (Exception e) {
						logger.error("EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage() + ". I'm giving up!");
						e.printStackTrace();
						if (terminateJobOnException)
							System.exit(-1);
					}
				}
				logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
				
				
				// now build the OData statement and execute it against the connection endpoint
				OEntity newPost = null;
				try {
					newPost = postService.createEntity("post")
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
							
							.properties(OProperties.string("client", postData.getClient()))
							.properties(OProperties.int32("truncated", new Integer(truncated)))
	
							.properties(OProperties.int64("inReplyTo", postData.getInReplyTo()))
							.properties(OProperties.int64("inReplyToUserID", postData.getInReplyToUser()))
							.properties(OProperties.string("inReplyToScreenName", postData.getInReplyToUserScreenName()))
							
							.properties(OProperties.string("geoLocation_longitude", postData.getGeoLongitude()))
							.properties(OProperties.string("geoLocation_latitude", postData.getGeoLatitude()))
							.properties(OProperties.string("placeID", postData.getGeoPlaceId()))
							.properties(OProperties.string("plName",  postData.getGeoPlaceName()))
							.properties(OProperties.string("plCountry", postData.getGeoPlaceCountry()))
							.properties(OProperties.string("plAround_longitude", postData.getGeoAroundLongitude()))
							.properties(OProperties.string("plAround_latitude", postData.getGeoAroundLatitude()))
							
							.execute();
					
					logger.trace("entity set for the post: " + newPost.getEntitySet().toString());
					
					logger.info("New post ("+postData.getSnId()+"-"+postData.getId()+") created");
				
					/*
					 * in case of an error during post, the following XML structure is returned as part of the exception:
					 * 		<?xml version="1.0" encoding="utf-8" standalone="yes"?>
					 * 			<error xmlns="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"><code/>
					 * 				<message xml:lang="en-US">
					 * 					Service exception: inserted value too large for column.
					 * 				</message>
					 * 			</error>
					 */
				} catch (RuntimeException e) {
					logger.error("EXCEPTION :: could not create post ("+postData.getSnId()+"-"+postData.getId()+"): " + e.getLocalizedMessage());
					e.printStackTrace();
					
					logger.trace("entity set for the post: " + newPost.getEntitySet().toString());
					
					// TODO find out how to get the http error code 
					// check on the error code, some may be ok, for the crawler to continue 
					// (everything above 499) some are client errors, where we should bail out.
					
					if (terminateJobOnException)
						System.exit(-1);
				} catch (Exception e) {
					logger.error("EXCEPTION :: unforseen error condition, post ("+postData.getSnId()+"-"+postData.getId()+") NOT created: " + e.getLocalizedMessage());
					e.printStackTrace();
					if (terminateJobOnException)
						System.exit(-1);
				}
			} catch (SQLException le){
				logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
				le.printStackTrace();
				if (terminateJobOnException)
					System.exit(-1);
			} catch (Exception le) {
				logger.error("EXCEPTION :: unforseen error condition, post ("+postData.getSnId()+"-"+postData.getId()+") NOT created: " + le.getLocalizedMessage());
				le.printStackTrace();
				if (terminateJobOnException)
					System.exit(-1);
			}
		} else {
			logger.info("The post ("+postData.getSnId()+"-"+postData.getId()+") is already in the database - updating dataset");
			// TODO: establish update procedure
		}
	}

	public void saveUsers(UserData userData) {
		// first check if the dataset is already in the database
		if (!checkIfEntryExist(userData.getId(), userData.getSnId(), "user")) {
			logger.info("inserting user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") into the DB");
			
			// first try to save the data via jdbc and fall back to OData (see below) if that is not possible
			try {
				Class.forName("com.sap.db.jdbc.Driver");
				String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
				// decrypting the values because the jdbc driver needs these values in clear text
				String user = null;
				String password = null;
				
				// get the user and password for the JDBC connection
				try {
					user = configurationEncryptionProvider.decryptValue(this.user);
					password = configurationEncryptionProvider.decryptValue(this.pass);
				} catch (GenericEncryptionException e) {
					logger.error("EXCEPTION :: could not decrypt user and/or password" + e.getMessage(), e);
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
				logger.info("User "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") added to DB");
				logger.debug(rowCount+" rows added");
				
				stmt.close() ; conn.close() ;
				
			// in case the jdbc library to connect to the SAP HANA DB is not available, fall back to OData to save the user
			} catch (java.lang.ClassNotFoundException le) {
				logger.warn("JDBC driver not available - falling back to OData");
				
				if (userService == null) {
					try {
						String _user = configurationEncryptionProvider.decryptValue(this.user);
						String _pw = configurationEncryptionProvider.decryptValue(this.pass);
						String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
						String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
							
						BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
						logger.debug("Initiating connection to service endpoint "+userURI+" of the hana database");
						
						ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
						builder.setClientBehaviors(bAuth);
						userService = builder.build();
						
						logger.info("HANAPersistence Service for users connected");
					} catch (GenericEncryptionException e) {
						logger.error("EXCEPTION :: could not decrypt user and/or password. " + e.getMessage(), e);
						if (terminateJobOnException)
							System.exit(-1);
					} catch (Exception e) {
						logger.error("EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage());
						e.printStackTrace();
						if (terminateJobOnException)
							System.exit(-1);
					}
				}
				logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
				
				
				OEntity newUser = null;
				try {
					newUser = userService.createEntity("user")
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
					
					/* prints out the EDM Definitions and values for the created user
					logger.trace("entity set for the user: ");
					List prop = newUser.getProperties();
					for (int i = 0 ; i < newUser.getProperties().size() ; i++)
						logger.trace("property " + prop.get(i).toString());
					*/
					logger.info("User " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") added to DB");
				} catch (RuntimeException e) {
					/*
					 * in case of an error during post, the following XML structure is returned as part of the exception:
					 * 		<?xml version="1.0" encoding="utf-8" standalone="yes"?>
					 * 			<error xmlns="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"><code/>
					 * 				<message xml:lang="en-US">
					 * 					Service exception: inserted value too large for column.
					 * 				</message>
					 * 			</error>
					 */
					logger.error("ERROR :: Could not create user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
					e.printStackTrace();
					
					// TODO find out how to get the http error code 
					// check on the error code, some may be ok, for the crawler to continue 
					// (everything above 499) some are client error, where we should bail out.
					
					if (terminateJobOnException)
						System.exit(-1);
				} catch (Exception e) {
					logger.error("EXCEPTION :: unforseen error condition, user ("+userData.getSnId()+"-"+userData.getId()+") NOT added to the DB: " + e.getLocalizedMessage());
					e.printStackTrace();
					if (terminateJobOnException)
						System.exit(-1);
				}
				
			} catch (SQLException le){
				logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted: " + le.getLocalizedMessage());
				le.printStackTrace();
				if (terminateJobOnException)
					System.exit(-1);
			} catch (Exception le) {
				logger.error("EXCEPTION :: unforseen error condition, user ("+userData.getSnId()+"-"+userData.getId()+") NOT added to the DB: " + le.getLocalizedMessage());
				le.printStackTrace();
				if (terminateJobOnException)
					System.exit(-1);
			}
		} else {
			logger.info("The user " + userData.getUsername() + " (" + userData.getSnId()  + "-" + userData.getId() + ") is already in the database ");
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
		assert type == "user" || type == "post" : "type must be either \'user\' or \'post\'";
		
		BasicAuthenticationBehavior bAuth = null;
		String _user = null;
		String _pw = null;
		ODataConsumer.Builder builder = null;
		String entitySetName = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
		String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		
		try {
			_user = configurationEncryptionProvider.decryptValue(this.user);
			_pw = configurationEncryptionProvider.decryptValue(this.pass);
			bAuth = new BasicAuthenticationBehavior(_user, _pw);
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not decrpt values for user and passowrd " + e.getMessage());
			return false;
		}
		
		if (type == "user") {
			logger.debug("checking if "+type+" " + sn_id + "-" + id + " at " + userURI + " exists");
			
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
			logger.debug("checking if "+type+" " + sn_id + "-" + id + " at " + postURI + " exists");
			
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
	
	
	// getter and setter
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
