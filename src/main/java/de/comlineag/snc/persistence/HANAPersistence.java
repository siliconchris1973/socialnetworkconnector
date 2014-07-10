package de.comlineag.snc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTimeZone;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericEncryptionException;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationEncryptionHandler;
import de.comlineag.snc.handler.DataEncryptionHandler;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Connector Class
 * @version 	0.9f
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
 * 				0.9d				added support to en/decrypt parts of the data (posting text and username)
 * 				0.9e				moved insert statements for post and user to private methods
 * 				0.9f				added update methods to update an existing record
 *
 * TODO	2. enable query on field dimensions in DB so that (e.g. String)-fields can be truncated prior inserting
 * TODO	3. establish proper error handling (see comments in source code)
 * TODO 4. enable location support for user
 * TODO 6. find out how to get the http error code during odata calls
 */
public class HANAPersistence implements IPersistenceManager {
	
	// Servicelocation taken from applicationContext.xml
	private String host;
	private String port;
	private String jdbcPort;
	private String dbDriver;
	private String protocol;
	private String location;
	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Credentials
	private String user;
	private String pass;
	// OData client endpoints
	private static ODataConsumer userService;
	private static ODataConsumer postService;
	
	// data sets
	private Long Id;
	private SocialNetworks SN;
	private String networkName;
	
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationEncryptionHandler configurationEncryptionProvider = new ConfigurationEncryptionHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataEncryptionHandler dataEncryptionProvider = new DataEncryptionHandler();

	
	public HANAPersistence() {}

	/**
	 * @description save a post from social network to the HANA DB
	 * 
	 * @param	PostData
	 * 				<Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
	 * 				<Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
	 * 				<Property Name="user_id" Type="Edm.String" MaxLength="20"/>
	 * 				<Property Name="timestamp" Type="Edm.DateTime"/>
	 * 				<Property Name="postLang" Type="Edm.String" MaxLength="64"/>
	 *
	 * 				<Property Name="text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
	 * 				<Property Name="raw_text" Type="Edm.String" DefaultValue="" MaxLength="5000"/>
	 * 				<Property Name="subject" Type="Edm.String" DefaultValue="" MaxLength="20"/>
	 * 				<Property Name="teaser" Type="Edm.String" DefaultValue="" MaxLength="256"/>
	 *
	 * 				<Property Name="viewcount" Type="Edm.int" DefaultValue=0/>
	 * 				<Property Name="favoritecount" Type="Edm.int" DefaultValue=0/>
	 *
	 * 				<Property Name="client" Type="Edm.String" MaxLength="2048"/>
	 * 				<Property Name="truncated" Type="Edm.Byte" DefaultValue="0"/>
	 * 				<Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
	 * 				<Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
	 * 				<Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
	 *
	 * 				<Property Name="geoLocation_longitude" Type="Edm.String" MaxLength="40"/>
	 * 				<Property Name="geoLocation_latitude" Type="Edm.String" MaxLength="40"/>
	 * 				<Property Name="placeID" Type="Edm.String" MaxLength="16"/>
	 * 				<Property Name="plName" Type="Edm.String" MaxLength="256"/>
	 * 				<Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
	 * 				<Property Name="plAround_longitude" Type="Edm.String" MaxLength="40"/>
	 * 				<Property Name="plAround_latitude" Type="Edm.String" MaxLength="40"/>
	 * 
	 * @throws GenericEncryptionException 
	 * 
	 */
	public void savePosts(PostData postData) {
		try {
			SN = SocialNetworks.getNetworkNameByValue(postData.getSnId());
			Id = postData.getId();
			OEntity theData = checkIfEntryExist(SN, Id, "post");
			
			// first check if the dataset is already in the database
			if (theData == null) {
				try{
					// first try to save the data via jdbc, we do this by tryng to load the jdbc driver
					Class.forName(this.dbDriver);
					insertPostWithSQL(SN, Id, postData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the post
					insertPostWithOData(SN, Id, postData);
				} 
			// if record exists, update it...
			} else {
				//logger.trace(theData.getEntityKey().toKeyStringWithoutParentheses());
				try{
					// first try to update the data via jdbc
					Class.forName(this.dbDriver);
					updatePostWithSQL(SN, Id, postData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to update the post
					updatePostWithOData(SN, Id, postData, theData);
				} 
			}
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing post "+postData.getSnId()+"-"+postData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
		}
	}
	
	
	/**
	 * @description	save a user from social network to the HANA DB
	 * @param		UserData
	 */
	public void saveUsers(UserData userData) {
		try {
			SN = SocialNetworks.getNetworkNameByValue(userData.getSnId());
			Id = userData.getId();
			
			OEntity theData = checkIfEntryExist(SN, Id, "user");
			
			// first check if the dataset is already in the database
			if (theData == null) {
				try {
					// first try to save the user via jdbc, we do this by tryng to load the jdbc driver
					Class.forName(this.dbDriver);
					insertUserWithSQL(SN, Id, userData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the user
					insertUserWithOData(SN, Id, userData);
				} 
			} else {
				try {
					// first try to save the data via jdbc
					Class.forName(this.dbDriver);
					updateUserWithSQL(SN, Id, userData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the user
					updateUserWithOData(SN, Id, userData, theData);
				} 
			}
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing user "+userData.getSnId()+"-"+userData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
		}
	}
	
	
	
	//
	// the actual methods to insert posts and users
	//
	/**
	 * @description	insert the post with sql
	 * 
	 * @param		SocialNetworks SN
	 * @param		Long Id
	 * @param 		PostData postData
	 */
	private void insertPostWithSQL(SocialNetworks SN, Long Id, PostData postData) {
		networkName = SocialNetworks.getNetworkNameByValue(postData.getSnId()).getType();
		logger.info("creating "+networkName+"-post "+SN+"-"+Id);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		try{
			Class.forName(this.dbDriver);
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
            
            logger.debug("trying to insert data with jdbc url="+url+" user="+user);
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			
            // prepare the SQL statement
			String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
					+ "("
					+ "		\"sn_id\" "
					+ ",	\"post_id\" "
					+ ",	\"user_id\" "
					+ ",	\"timestamp\" "
					+ ",	\"postLang\" "
					+ ",	\"text\" "
					+ ",	\"raw_text\" "
					+ ",	\"teaser\" "
					+ ",	\"subject\" "
					+ ",	\"viewcount\" "
					+ ",	\"favoritecount\" "
					+ ",	\"client\" "
					+ ",	\"truncated\" "
					+ ",	\"inReplyTo\" "
					+ ",	\"inReplyToUserID\" "
					+ ",	\"inReplyToScreenName\" "
					+ ",	\"geoLocation_longitude\" "
					+ ",	\"geoLocation_latitude\" "
					+ ",	\"placeID\" "
					+ ",	\"plName\" "
					+ ",	\"plCountry\" "
					+ ",	\"plAround_longitude\" "
					+ ",	\"plAround_latitude\" "
					+ ") "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"
					+ ")";
			
			PreparedStatement stmt = conn.prepareStatement(sql);
			logger.trace("    SQL: "+sql);
			
			stmt.setString(1, postData.getSnId());
			stmt.setLong(2, new Long(postData.getId()));
			stmt.setLong(3,new Long(postData.getUserId()));
			stmt.setTimestamp(4,new Timestamp((postData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(5, postData.getLang());
			stmt.setString(6, dataEncryptionProvider.encryptValue(postData.getText()));
			stmt.setString(7, dataEncryptionProvider.encryptValue(postData.getRawText()));
			stmt.setString(8, dataEncryptionProvider.encryptValue(postData.getTeaser()));
			stmt.setString(9, dataEncryptionProvider.encryptValue(postData.getSubject()));
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
			
			logger.info(networkName + "-post ("+postData.getSnId()+"-"+postData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with "+ dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	insert the post with OData
	 * 
	 * @param		SocialNetworks SN
	 * @param		Long Id
	 * @param 		PostData postData
	 */
	private void insertPostWithOData(SocialNetworks SN, Long Id, PostData postData){
		networkName = SocialNetworks.getNetworkNameByValue(postData.getSnId()).getType();
		logger.info("creating "+networkName+"-post "+SN+"-"+Id);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
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
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password. " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the OData statement and execute it against the connection endpoint
		OEntity newPost = null;
		try {
			postService.createEntity("post")
					.properties(OProperties.string("sn_id", postData.getSnId()))
					.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
					.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
					.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
					.properties(OProperties.string("postLang", postData.getLang()))
					
					.properties(OProperties.string("text", dataEncryptionProvider.encryptValue(postData.getText())))
					.properties(OProperties.string("raw_text", dataEncryptionProvider.encryptValue(postData.getRawText())))
					.properties(OProperties.string("teaser", dataEncryptionProvider.encryptValue(postData.getTeaser())))
					.properties(OProperties.string("subject", dataEncryptionProvider.encryptValue(postData.getSubject())))
					
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
			
			logger.info(networkName + "-post ("+postData.getSnId()+"-"+postData.getId()+") created");
		
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
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	insert user with SQL
	 * 
	 * @param		SocialNetworks SN
	 * @param		Long Id
	 * @param 		UserData userData
	 */
	private void insertUserWithSQL(SocialNetworks SN, Long Id, UserData userData) {
		networkName = SocialNetworks.getNetworkNameByValue(userData.getSnId()).getType();
		logger.info("creating "+networkName+"-user "+SN+"-"+Id);
		
		// first try to save the data via jdbc
		try {
			Class.forName(this.dbDriver);
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
            		+ "("
            		+ "		\"sn_id\" "
            		+ ",	\"user_id\" "
            		+ ",	\"userName\" "
            		+ ",	\"nickName\" "
            		+ ",	\"userLang\" "
            		+ ",	\"follower\" "
            		+ ",	\"friends\" "
            		+ ",	\"postingsCount\" "
            		+ ",	\"favoritesCount\" "
            		+ ",	\"listsAndGroupsCount\" "
            		+ ") "
            		+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
			 
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userData.getSnId());
			stmt.setLong(2, userData.getId());
			stmt.setString(2, dataEncryptionProvider.encryptValue(userData.getUsername()));
			stmt.setString(3, dataEncryptionProvider.encryptValue(userData.getScreenName()));
			stmt.setString(4, userData.getLang());
			stmt.setLong(5, userData.getFollowersCount());
			stmt.setLong(6, userData.getFriendsCount());
			stmt.setLong(7, userData.getPostingsCount());
			stmt.setLong(8, userData.getFavoritesCount());
			stmt.setLong(9, userData.getListsAndGrooupsCount());
			stmt.setString(10, userData.getSnId());
			
			int rowCount = stmt.executeUpdate();
			
			logger.info(networkName + "-user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	insert user with OData
	 * 
	 * @param 		SocialNetworks SN
	 * @param 		Long Id
	 * @param 		UserData userData
	 */
	private void insertUserWithOData(SocialNetworks SN, Long Id, UserData userData){
		networkName = SocialNetworks.getNetworkNameByValue(userData.getSnId()).getType();
		logger.info("creating "+networkName+"-user "+SN+"-"+Id);
		
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
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password. " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		OEntity newUser = null;
		try {
			userService.createEntity("user")
					.properties(OProperties.string("sn_id", userData.getSnId()))
					.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("userName", dataEncryptionProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataEncryptionProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
					// TODO check location values for user
					//.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))

					.execute();
			
			logger.info(networkName + "-user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") created");
		
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
			logger.error("ERROR :: Could not create user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	
	
	//
	// the actual methods to update posts and users
	//
	/**
	 * @description	update post with sql
	 * 
	 * @param		SN
	 * @param		Id
	 * @param 		postData
	 */
	private void updatePostWithSQL(SocialNetworks SN, Long id, PostData postData) {
		networkName = SocialNetworks.getNetworkNameByValue(postData.getSnId()).getType();
		logger.info("updating "+networkName+"-post "+SN+"-"+Id);
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		try{
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			String user = null;
			String password  = null;
			
			// get the user and password for the JDBC connection
			try {
				user = configurationEncryptionProvider.decryptValue(this.user);
				password = configurationEncryptionProvider.decryptValue(this.pass);
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
			}
            
            logger.debug("trying to update data with jdbc url="+url+" user="+user);
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			
            // prepare the SQL statement
            String sql="UPDATE \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
					+ " SET ("
					+ "\"user_id\" = ? "
					+ ",\"timestamp\" = ? "
					+ ",\"postLang\" = ? "
					+ ",\"text\" = ? "
					+ ",\"raw_text\" = ? "
					+ ",\"teaser\" = ? "
					+ ",\"subject\" = ? "
					+ ",\"viewcount\" = ? "
					+ ",\"favoritecount\" = ? "
					+ ",\"client\" = ? "
					+ ",\"truncated\" = ? "
					+ ",\"inReplyTo\" = ? "
					+ ",\"inReplyToUserID\" = ? "
					+ ",\"inReplyToScreenName\" = ? "
					+ ",\"geoLocation_longitude\" = ? "
					+ ",\"geoLocation_latitude\" = ? "
					+ ",\"placeID\" = ? "
					+ ",\"plName\" = ? "
					+ ",\"plCountry\" = ? "
					+ ",\"plAround_longitude\" = ? "
					+ ",\"plAround_latitude\" = ? "
					+ ") "
					+ "WHERE (\"sn_id\" = ? AND \"post_id\" ?)";					
			PreparedStatement stmt = conn.prepareStatement(sql);
			logger.trace("SQL: "+sql);
			
			stmt.setLong(1,new Long(postData.getUserId()));
			stmt.setTimestamp(2,new Timestamp((postData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(3, postData.getLang());
			stmt.setString(4, dataEncryptionProvider.encryptValue(postData.getText()));
			stmt.setString(5, dataEncryptionProvider.encryptValue(postData.getRawText()));
			stmt.setString(6, dataEncryptionProvider.encryptValue(postData.getTeaser()));
			stmt.setString(7, dataEncryptionProvider.encryptValue(postData.getSubject()));
			stmt.setLong(8, postData.getViewCount());
			stmt.setLong(9, postData.getFavoriteCount());
			stmt.setString(10, postData.getClient());
			stmt.setInt(11, truncated);
			stmt.setLong(12, postData.getInReplyTo());
			stmt.setLong(13, postData.getInReplyToUser());
			stmt.setString(14, postData.getInReplyToUserScreenName());
			stmt.setString(15, postData.getGeoLongitude());
			stmt.setString(16, postData.getGeoLatitude());
			stmt.setString(17, postData.getGeoPlaceId());
			stmt.setString(18, postData.getGeoPlaceName());
			stmt.setString(19, postData.getGeoPlaceCountry());
			stmt.setString(20, postData.getGeoAroundLongitude());
			stmt.setString(21, postData.getGeoAroundLatitude());
			stmt.setString(22, postData.getSnId());
			stmt.setLong(23, new Long(postData.getId()));
			
			int rowCount = stmt.executeUpdate();
			
			logger.info(networkName + "-post ("+postData.getSnId()+"-"+postData.getId()+") updated");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	update post with OData
	 * 
	 * @param		SocialNetworks SN
	 * @param		Long Id
	 * @param 		PostData postData
	 * @param		OEntity thePostEntity
	 */
	private void updatePostWithOData(SocialNetworks SN, Long id, PostData postData, OEntity thePostEntity){
		networkName = SocialNetworks.getNetworkNameByValue(postData.getSnId()).getType();
		logger.info("updating "+networkName+"-post "+SN+"-"+Id);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
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
				
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the OData statement and execute it against the connection endpoint
		try {
			postService.updateEntity(thePostEntity) 
					//.properties(OProperties.string("sn_id", postData.getSnId()))
					//.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
					.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
					.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
					.properties(OProperties.string("postLang", postData.getLang()))
					
					.properties(OProperties.string("text", dataEncryptionProvider.encryptValue(postData.getText())))
					.properties(OProperties.string("raw_text", dataEncryptionProvider.encryptValue(postData.getRawText())))
					.properties(OProperties.string("teaser", dataEncryptionProvider.encryptValue(postData.getTeaser())))
					.properties(OProperties.string("subject", dataEncryptionProvider.encryptValue(postData.getSubject())))
					
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
			
			logger.info(networkName + "-post ("+postData.getSnId()+"-"+postData.getId()+") updated");
		
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
			logger.error("EXCEPTION :: could not update post ("+postData.getSnId()+"-"+postData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	update user with SQL
	 * 
	 * @param		SN
	 * @param		Id
	 * @param 		userData
	 */
	private void updateUserWithSQL(SocialNetworks SN, Long id, UserData userData) {
		networkName = SocialNetworks.getNetworkNameByValue(userData.getSnId()).getType();
		logger.info("updating "+networkName+"-user "+SN+"-"+Id);
		
		// first try to save the data via jdbc
		try {
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			// decrypting the values because the jdbc driver needs these values in clear text
			String user = null;
			String password = null;
			
			// get the user and password for the JDBC connection
			try {
				user = configurationEncryptionProvider.decryptValue(this.user);
				password = configurationEncryptionProvider.decryptValue(this.pass);
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
			}
            
            logger.debug("trying to update user "+SN+"-"+Id+" with jdbc url="+url+" user="+user);
            
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
            String sql="UPDATE \"CL_SBM\".\"comline.sbm.data.tables::users\" "
            		+ " SET ("
            		+ ",\"userName\" = ? "
            		+ ",\"nickName\" = ? "
            		+ ",\"userLang\" = ? "
            		+ ",\"follower\" = ? "
            		+ ",\"friends\" = ? "
            		+ ",\"postingsCount\" = ? "
            		+ ",\"favoritesCount\" = ? "
            		+ ",\"listsAndGroupsCount\" = ? "
            		+ ") "
            		+ "WHERE (\"sn_id\" = ? AND \"user_id\" = ?)";
			 
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, dataEncryptionProvider.encryptValue(userData.getUsername()));
			stmt.setString(2, dataEncryptionProvider.encryptValue(userData.getScreenName()));
			stmt.setString(3, userData.getLang());
			stmt.setLong(4, userData.getFollowersCount());
			stmt.setLong(5, userData.getFriendsCount());
			stmt.setLong(6, userData.getPostingsCount());
			stmt.setLong(7, userData.getFavoritesCount());
			stmt.setLong(8, userData.getListsAndGrooupsCount());
			stmt.setString(9, userData.getSnId());
			stmt.setString(10, userData.getSnId());
			stmt.setLong(11, userData.getId());
			
			int rowCount = stmt.executeUpdate();
			
			logger.info(networkName + "-user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	update user with OData
	 * 
	 * @param SocialNetworks SN
	 * @param Long id
	 * @param UserData userData
	 * @param OEntity theUserEntity 
	 */
	private void updateUserWithOData(SocialNetworks SN, Long id, UserData userData, OEntity theUserEntity){
		networkName = SocialNetworks.getNetworkNameByValue(userData.getSnId()).getType();
		logger.info("updating "+networkName+"-user "+SN+"-"+Id);
		
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
			} catch (GenericEncryptionException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		try {
			userService.updateEntity(theUserEntity)
					//.properties(OProperties.string("sn_id", userData.getSnId()))
					//.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("userName", dataEncryptionProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataEncryptionProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
					// TODO check location values for user
					//.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))

					.execute();
			
			logger.info(networkName + "-user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") updated");
		
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
			logger.error("ERROR :: Could not update "+networkName+"-user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataEncryptionProvider.getClass().getSimpleName() + e.getMessage(), e);
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
	private OEntity checkIfEntryExist(SocialNetworks SN, Long Id, String type) {
		assert type == "user" || type == "post" : "type must be either \'user\' or \'post\'";
		
		/*
		 *  OEntity-Structure might be useful to find out field dimensions prior inserting dataset 
		 *  	[
		 *  	OProperty [sn_id,				EdmSimpleType [Edm.String], 	TW],
		 *  	OProperty [user_id,				EdmSimpleType [Edm.String], 	2443766328],
		 *  	OProperty [userName,			EdmSimpleType [Edm.String], 	Iesha],
		 *  	OProperty [nickName,			EdmSimpleType [Edm.String], 	iesha785],
		 *  	OProperty [userLang,			EdmSimpleType [Edm.String], 	en],
		 *  	OProperty [location,			EdmSimpleType [Edm.String], 	null],
		 *  	OProperty [follower,			EdmSimpleType [Edm.Int32], 		261],
		 *  	OProperty [friends,				EdmSimpleType [Edm.Int32], 		1],
		 *  	OProperty [postingsCount,		EdmSimpleType [Edm.Int32], 		209967],
		 *  	OProperty [favoritesCount,		EdmSimpleType [Edm.Int32], 		0],
		 *  	OProperty [listsAndGroupsCount,	EdmSimpleType [Edm.Int32], 		0]
		 *  	]
		 */
		
		networkName = SocialNetworks.getNetworkNameByValue(SN.toString()).getType();
		logger.info("checking if "+networkName+"-"+type+" "+SN+"-"+Id + " exists");
		
		BasicAuthenticationBehavior bAuth = null;
		String _user = null;
		String _pw = null;
		ODataConsumer.Builder builder = null;
		String entitySetName = null;
		OEntity theDataset = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
		String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		
		try {
			_user = configurationEncryptionProvider.decryptValue(this.user);
			_pw = configurationEncryptionProvider.decryptValue(this.pass);
			bAuth = new BasicAuthenticationBehavior(_user, _pw);
		} catch (GenericEncryptionException e) {
			logger.error("EXCEPTION :: could not decrpt values for user and passowrd " + e.getMessage());
			return null;
		}
		
		if ("user".equals(type)) {
			logger.trace("searching for "+type+" " + SN.toString() + "-" + Id + " at location " + userURI);
			
			builder = ODataConsumer.newBuilder(userURI);
			builder.setClientBehaviors(bAuth);
			userService = builder.build();
			
			entitySetName = "user";
			
			// query for the user by id
			try {
				theDataset = userService.getEntities(entitySetName).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().first();
				logger.debug("found the "+type+ " " + SN +"-"+Id);
				logger.trace("     => " + theDataset.toString());
				
				return theDataset;
			} catch (RuntimeException e) {
				logger.debug("the "+type+" " + SN.toString() + "-" + Id + " does not exist");
				return null;
			} catch (Exception e) {
				logger.debug("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
				return null;
			}
		} else {
			logger.trace("searching for "+type+" " + SN.toString() + "-" + Id + " at location " + postURI);
			
			builder = ODataConsumer.newBuilder(postURI);
			builder.setClientBehaviors(bAuth);
			postService = builder.build();
			
			entitySetName = "post";
			
			// query for the post by id
			try {
				theDataset = postService.getEntities(entitySetName).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().first();
				logger.debug("found the "+type+ " " + SN +"-"+Id);
				logger.trace("     => " + theDataset.toString());
				
				return theDataset; 
			} catch (RuntimeException e) {
				logger.debug("the "+type+" " + SN.toString() + "-" + Id + " does not exist");
				return null;
			} catch (Exception e) {
				logger.debug("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
				return null;
			}
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
	
	public String getDbDriver() {
		return dbDriver;
	}
	public void setDbDriver(String db_driver) {
		this.dbDriver = db_driver;
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
