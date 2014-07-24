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

import de.comlineag.snc.appstate.GeneralConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.helper.DataHelper;
import de.comlineag.snc.constants.HanaDataConstants;
import de.comlineag.snc.persistence.JsonFilePersistence;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Persistence Manager
 * @version 	0.9h	- 20.07.2014
 * @status		productive
 *
 * @description handles the connectivity to the SAP HANA Systems and saves and updates posts and users in the DB
 * 
 * @changelog	0.1 (Chris)			skeleton created
 * 				0.2	(Magnus)		savePost implemented
 * 				0.3 				saveUser implemented
 * 				0.4 				added skeleton for geo-geoLocation information
 * 				0.5 				added support for encrypted user and password
 * 				0.6 				bug fixing and optimization
 * 				0.7 (Chris)			first productive version, saves users and posts as is (no geo-information)
 *				0.8	(Thomas)		added JDBC support
 * 				0.9	(Chris)			added search for dataset prior inserting one
 * 				0.9a 				added query to determine if an exception during persistence operation shall 
 * 									terminate the crawler or not
 * 				0.9b				moved Base64Encryption in its own class
 * 				0.9c				added support for different encryption provider, the actual one is set in applicationContext.xml 
 * 				0.9d				added support to en/decrypt parts of the data (posting text and user name currently) on the fly
 * 				0.9e				moved insert statements for post and user to private methods
 * 				0.9f				added update methods to update an existing record
 *				0.9g				added support to honor actual field length in the database - all fields too long, will be truncated
 *				0.9h				added failsave method to store posts and users on disk in case db fails to save them
 *				0.9i				added domain - at the moment simple string, should be handled as a list in the version 1.1
 * 
 * TODO 1. fix crawler bug, that causes the persistence to try to insert a post or user multiple times
 * 			This bug has something to do with the number of threads provided by the Quartz job control
 * 			In case 1 thread is provided, everything is fine, but if 2 threads are provided (as needed if two crawler
 * 			run - Twitter and Lithium) the Twitter crawler tries to insert some posts more then once - an attempt 
 * 			obviously doomed. We log the following error: 
 * 				could not create post (TW-488780024691322880): Expected status 201, found 400
 * 			and the hana db returns this message:
 * 				Service exception: unique constraint violated.
 * 
 * TODO 2. fix another erro that occasionally occurs when trying to insert a dataset in the db. This second erro 
 * 			states taht there is a syntax error at line 0. - happens more often then error 1
 *  
 * TODO	3. establish proper error handling and find out how to get the HTTP error code during OData calls
 * TODO 4. enable geoLocation support for users
 * 
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
	
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();

	
	public HANAPersistence() {}

	/**
	 * @description save a post from social network to the HANA DB
	 * 
	 * @param	PostData
	 * 				<Property Name="domain" Type="Edm.String" Nullable="false" MaxLength="1024"/>
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
	 * @throws GenericCryptoException 
	 * 
	 */
	public void savePosts(PostData postData) {
		try {
			// first check if the entry already exists
			OEntity theData = returnOEntityHandler(postData.getSnId(), postData.getId(), "post");
			
			// truncate all fields to maximum length allotted by HANA
			setPostFieldLength(postData);
			
			// first check if the dataset is already in the database
			if (theData == null) {
				try{
					// first try to save the data via jdbc, we do this by tryng to load the jdbc driver
					Class.forName(this.dbDriver);
					insertPostWithSQL(postData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the post
					insertPostWithOData(postData);
				} 
			// if record exists, update it...
			} else {
				//logger.trace(theData.getEntityKey().toKeyStringWithoutParentheses());
				try{
					// first try to update the data via jdbc
					Class.forName(this.dbDriver);
					updatePostWithSQL(postData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the post
					updatePostWithOData(postData, theData);
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
			// first check if the entry already exists
			OEntity theData = returnOEntityHandler(userData.getSnId(), userData.getId(), "user");
			
			// then truncate all fields to maximum length allotted by HANA
			setUserFieldLength(userData);
			
			if (theData == null) {
				try {
					// first try to save the user via jdbc, we do this by tryng to load the jdbc driver
					Class.forName(this.dbDriver);
					insertUserWithSQL(userData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the user
					insertUserWithOData(userData);
				} 
			} else {
				try {
					// first try to save the data via jdbc
					Class.forName(this.dbDriver);
					updateUserWithSQL(userData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the user
					updateUserWithOData(userData, theData);
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
	 * @param 		PostData postData
	 */
	private void insertPostWithSQL(PostData postData) {
		logger.info("creating post "+postData.getSnId()+"-"+postData.getId());
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		try{
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			String user = null;
			String password  = null;
			
			// get the user and password for the JDBC connection
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				user = configurationCryptoProvider.decryptValue(this.user);
				password = configurationCryptoProvider.decryptValue(this.pass);
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			}
            
            logger.debug("trying to insert data with jdbc url="+url+" user="+user);
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			
            // prepare the SQL statement
			String sql="INSERT INTO \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
					+ "("
					+ "     \"sn_id\" "
					+ ",	\"sn_id\" "
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
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"
					+ ")";
			
			PreparedStatement stmt = conn.prepareStatement(sql);
			logger.trace("    SQL: "+sql);
			
			stmt.setString(1, postData.getDomain());
			stmt.setString(2, postData.getSnId());
			stmt.setLong(3, new Long(postData.getId()));
			stmt.setLong(4,new Long(postData.getUserId()));
			stmt.setTimestamp(5,new Timestamp((postData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(6, postData.getLang());
			stmt.setString(7, dataCryptoProvider.encryptValue(postData.getText()));
			stmt.setString(8, dataCryptoProvider.encryptValue(postData.getRawText()));
			stmt.setString(9, dataCryptoProvider.encryptValue(postData.getTeaser()));
			stmt.setString(10, dataCryptoProvider.encryptValue(postData.getSubject()));
			stmt.setLong(11, postData.getViewCount());
			stmt.setLong(12, postData.getFavoriteCount());
			stmt.setString(13, postData.getClient());
			stmt.setInt(14, truncated);
			stmt.setLong(15, postData.getInReplyTo());
			stmt.setLong(16, postData.getInReplyToUser());
			stmt.setString(17, postData.getInReplyToUserScreenName());
			stmt.setString(18, postData.getGeoLongitude());
			stmt.setString(19, postData.getGeoLatitude());
			stmt.setString(20, postData.getGeoPlaceId());
			stmt.setString(21, postData.getGeoPlaceName());
			stmt.setString(22, postData.getGeoPlaceCountry());
			stmt.setString(23, postData.getGeoAroundLongitude());
			stmt.setString(24, postData.getGeoAroundLatitude());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("post ("+postData.getSnId()+"-"+postData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with "+ dataCryptoProvider.getCryptoProviderName() + ": "+ e.getMessage(), e);
		}
	}
	
	/**
	 * @description	insert the post with OData
	 * 
	 * @param 		PostData postData
	 */
	private void insertPostWithOData(PostData postData){
		logger.info("creating post "+postData.getSnId()+"-"+postData.getId());
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		// in case the connection is NOT opened already, attempt to create an OData Consumer
		if (postService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				
				BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
				logger.debug("Initiating connection to service endpoint "+postURI+" of the hana database");
				
				ODataConsumer.Builder builder = ODataConsumer.newBuilder(postURI);
				builder.setClientBehaviors(bAuth);
				postService = builder.build();
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the OData statement and execute it against the connection endpoint
		@SuppressWarnings("unused")
		OEntity newPost = null;
		try {
			postService.createEntity("post")
					.properties(OProperties.string("domain", postData.getDomain()))
					.properties(OProperties.string("sn_id", postData.getSnId()))
					.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
					.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
					.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
					.properties(OProperties.string("postLang", postData.getLang()))
					
					.properties(OProperties.string("text", dataCryptoProvider.encryptValue(postData.getText())))
					.properties(OProperties.string("raw_text", dataCryptoProvider.encryptValue(postData.getRawText())))
					.properties(OProperties.string("teaser", dataCryptoProvider.encryptValue(postData.getTeaser())))
					.properties(OProperties.string("subject", dataCryptoProvider.encryptValue(postData.getSubject())))
					
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
			
			logger.info("post ("+postData.getSnId()+"-"+postData.getId()+") created");
			
			if (GeneralConfiguration.isCREATE_POST_JSON_ON_SUCCESS()) {
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
			
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
			
			if (GeneralConfiguration.isCREATE_POST_JSON_ON_ERROR()) {
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	insert user with SQL
	 * 
	 * @param 		UserData userData
	 */
	private void insertUserWithSQL(UserData userData) {
		logger.info("creating user "+userData.getSnId()+"-"+userData.getId());
		
		// first try to save the data via jdbc
		try {
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			// decrypting the values because the jdbc driver needs these values in clear text
			String user = null;
			String password = null;
			
			// get the user and password for the JDBC connection
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				user = configurationCryptoProvider.decryptValue(this.user);
				password = configurationCryptoProvider.decryptValue(this.pass);
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
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
            		//+ ",	\"geoLocation\" "
            		+ ") "
            		+ "VALUES (?,?,?,?,?,?,?,?,?,?)"; // add a ? to the end of the line, after activating geoLocation 
			 
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userData.getSnId());
			stmt.setLong(2, userData.getId());
			stmt.setString(3, dataCryptoProvider.encryptValue(userData.getUsername()));
			stmt.setString(4, dataCryptoProvider.encryptValue(userData.getScreenName()));
			stmt.setString(5, userData.getLang());
			stmt.setLong(6, userData.getFollowersCount());
			stmt.setLong(7, userData.getFriendsCount());
			stmt.setLong(8, userData.getPostingsCount());
			stmt.setLong(9, userData.getFavoritesCount());
			stmt.setLong(10, userData.getListsAndGrooupsCount());
			//stmt.setString(11, userData.getGeoLocation());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	insert user with OData
	 * 
	 * @param 		UserData userData
	 */
	private void insertUserWithOData(UserData userData){
		logger.info("creating user "+userData.getSnId()+"-"+userData.getId());
		
		if (userService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
					
				BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
				logger.debug("Initiating connection to service endpoint "+userURI+" of the hana database");
				
				ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
				builder.setClientBehaviors(bAuth);
				userService = builder.build();
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		@SuppressWarnings("unused")
		OEntity newUser = null;
		try {
			userService.createEntity("user")
					.properties(OProperties.string("sn_id", userData.getSnId()))
					.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("userName", dataCryptoProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataCryptoProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
					//.properties(OProperties.string("geoLocation", userData.getGeoLocation().toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))

					.execute();
			
			logger.info("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") created");
		
			if (GeneralConfiguration.isCREATE_USER_JSON_ON_SUCCESS()){
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
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
			
			if (GeneralConfiguration.isCREATE_USER_JSON_ON_ERROR()){
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	
	//
	// the actual methods to update posts and users
	//
	/**
	 * @description	update post with sql
	 * 
	 * @param 		postData
	 */
	private void updatePostWithSQL(PostData postData) {
		logger.info("updating post "+postData.getSnId()+"-"+postData.getId());
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		try{
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			String user = null;
			String password  = null;
			
			// get the user and password for the JDBC connection
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				user = configurationCryptoProvider.decryptValue(this.user);
				password = configurationCryptoProvider.decryptValue(this.pass);
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
			}
            
            logger.debug("trying to update data with jdbc url="+url+" user="+user);
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			
            // prepare the SQL statement
            String sql="UPDATE \"CL_SBM\".\"comline.sbm.data.tables::posts_1\" "
					+ " SET ("
					+ "\"domain\" = ? "
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
			
			stmt.setString(1, postData.getDomain());
			stmt.setLong(2,new Long(postData.getUserId()));
			stmt.setTimestamp(3,new Timestamp((postData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(4, postData.getLang());
			stmt.setString(5, dataCryptoProvider.encryptValue(postData.getText()));
			stmt.setString(6, dataCryptoProvider.encryptValue(postData.getRawText()));
			stmt.setString(7, dataCryptoProvider.encryptValue(postData.getTeaser()));
			stmt.setString(8, dataCryptoProvider.encryptValue(postData.getSubject()));
			stmt.setLong(9, postData.getViewCount());
			stmt.setLong(10, postData.getFavoriteCount());
			stmt.setString(11, postData.getClient());
			stmt.setInt(12, truncated);
			stmt.setLong(13, postData.getInReplyTo());
			stmt.setLong(14, postData.getInReplyToUser());
			stmt.setString(15, postData.getInReplyToUserScreenName());
			stmt.setString(16, postData.getGeoLongitude());
			stmt.setString(17, postData.getGeoLatitude());
			stmt.setString(18, postData.getGeoPlaceId());
			stmt.setString(19, postData.getGeoPlaceName());
			stmt.setString(20, postData.getGeoPlaceCountry());
			stmt.setString(21, postData.getGeoAroundLongitude());
			stmt.setString(22, postData.getGeoAroundLatitude());
			stmt.setString(23, postData.getSnId());
			stmt.setLong(24, new Long(postData.getId()));
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("post ("+postData.getSnId()+"-"+postData.getId()+") updated");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postData.getSnId()+"-"+postData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	update post with OData
	 * 
	 * @param 		PostData postData
	 * @param		OEntity thePostEntity
	 */
	private void updatePostWithOData(PostData postData, OEntity thePostEntity){
		logger.info("updating post "+postData.getSnId()+"-"+postData.getId());

		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		// in case the connection is NOT opened already, attempt to create an OData Consumer
		if (postService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				
				BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
				logger.debug("Initiating connection to service endpoint "+postURI+" of the hana database");
				
				ODataConsumer.Builder builder = ODataConsumer.newBuilder(postURI);
				builder.setClientBehaviors(bAuth);
				postService = builder.build();
				
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the OData statement and execute it against the connection endpoint
		try {
			postService.updateEntity(thePostEntity) 
					//.properties(OProperties.string("sn_id", postData.getSnId()))
					//.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
					.properties(OProperties.string("domain", postData.getDomain()))
					.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
					.properties(OProperties.datetime("timestamp", postData.getTimestamp()))
					.properties(OProperties.string("postLang", postData.getLang()))
					
					.properties(OProperties.string("text", dataCryptoProvider.encryptValue(postData.getText())))
					.properties(OProperties.string("raw_text", dataCryptoProvider.encryptValue(postData.getRawText())))
					.properties(OProperties.string("teaser", dataCryptoProvider.encryptValue(postData.getTeaser())))
					.properties(OProperties.string("subject", dataCryptoProvider.encryptValue(postData.getSubject())))
					
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
			
			logger.info("post ("+postData.getSnId()+"-"+postData.getId()+") updated");
		
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
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @description	update user with SQL
	 * 
	 * @param 		userData
	 */
	private void updateUserWithSQL(UserData userData) {
		logger.info("updating user "+userData.getSnId()+"-"+userData.getId());
		
		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
				
		// first try to save the data via jdbc
		try {
			Class.forName(this.dbDriver);
			String url = "jdbc:sap://"+this.host+":"+this.jdbcPort;
			// decrypting the values because the jdbc driver needs these values in clear text
			String user = null;
			String password = null;
			
			// get the user and password for the JDBC connection
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				user = configurationCryptoProvider.decryptValue(this.user);
				password = configurationCryptoProvider.decryptValue(this.pass);
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
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
            		//+ ",\"geoLocation\" = ? "
            		+ ") "
            		+ "WHERE (\"sn_id\" = ? AND \"user_id\" = ?)";
			 
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, dataCryptoProvider.encryptValue(userData.getUsername()));
			stmt.setString(2, dataCryptoProvider.encryptValue(userData.getScreenName()));
			stmt.setString(3, userData.getLang());
			stmt.setLong(4, userData.getFollowersCount());
			stmt.setLong(5, userData.getFriendsCount());
			stmt.setLong(6, userData.getPostingsCount());
			stmt.setLong(7, userData.getFavoritesCount());
			stmt.setLong(8, userData.getListsAndGrooupsCount());
			stmt.setString(9, userData.getSnId());
			//stmt.setString(10, userData.getGeoLocation()); // activate above and this geoLocation and increase numbers below by one
			stmt.setString(10, userData.getSnId());
			stmt.setLong(11, userData.getId());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	update user with OData
	 * 
	 * @param 		UserData userData
	 * @param 		OEntity theUserEntity 
	 */
	private void updateUserWithOData(UserData userData, OEntity theUserEntity){
		logger.info("updating user "+userData.getSnId()+"-"+userData.getId());
		
		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		if (userService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
					
				BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
				logger.debug("Initiating connection to service endpoint "+userURI+" of the hana database");
				
				ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
				builder.setClientBehaviors(bAuth);
				userService = builder.build();
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		try {
			userService.updateEntity(theUserEntity)
					.properties(OProperties.string("userName", dataCryptoProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataCryptoProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
			//		.properties(OProperties.string("geoLocation", userData.getGeoLocation().toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))
					
					.execute();
			
			logger.info("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") updated");
		
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
			logger.error("ERROR :: Could not update user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * 
	 * @description	truncates fields of post data to the maximum length allowed by HANA DB
	 * 
	 * @param 		postData
	 * 
	 */
	private void setPostFieldLength(PostData postData){
		if (postData.getText() != null && postData.getText().length()>HanaDataConstants.POSTING_TEXT_SIZE) {
			logger.warn("truncating text of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.POSTING_TEXT_SIZE);
			postData.setText(postData.getText().substring(0, HanaDataConstants.POSTING_TEXT_SIZE));
			postData.setTruncated(true);
		}
		if (postData.getRawText() != null && postData.getRawText().length()>HanaDataConstants.POSTING_TEXT_SIZE) {
			logger.warn("truncating raw text of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.POSTING_TEXT_SIZE);
			
			postData.setRawText((String) DataHelper.htmlTruncator(postData.getRawText().substring(0, (HanaDataConstants.POSTING_TEXT_SIZE-5)), (HanaDataConstants.POSTING_TEXT_SIZE-1)));
			logger.debug("     truncated raw text now has " + postData.getRawText().length() + " characters");
		}
		if (postData.getTeaser() != null && postData.getTeaser().length()>HanaDataConstants.TEASER_TEXT_SIZE) {
			logger.info("truncating teaser of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.TEASER_TEXT_SIZE);
			postData.setTeaser(postData.getTeaser().substring(0, HanaDataConstants.TEASER_TEXT_SIZE));
		}
		if (postData.getSubject() != null && postData.getSubject().length()>HanaDataConstants.SUBJECT_TEXT_SIZE) {
			logger.info("truncating subject of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.SUBJECT_TEXT_SIZE);
			postData.setSubject(postData.getSubject().substring(0, HanaDataConstants.SUBJECT_TEXT_SIZE));
		}
		if (postData.getLang() != null && postData.getLang().length()>HanaDataConstants.POSTLANG_TEXT_SIZE) {
			logger.error("truncating language of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.POSTLANG_TEXT_SIZE + "! This is probably bad.");
			postData.setLang(postData.getLang().substring(0, HanaDataConstants.POSTLANG_TEXT_SIZE));
		}
		if (postData.getGeoLongitude() != null && postData.getGeoLongitude().length()>HanaDataConstants.LONGITUDE_TEXT_SIZE) {
			logger.error("truncating geo longitude of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.LONGITUDE_TEXT_SIZE + "! This is probably bad.");
			postData.setGeoLongitude(postData.getGeoLongitude().substring(0, HanaDataConstants.LONGITUDE_TEXT_SIZE));
		}
		if (postData.getGeoLatitude() != null && postData.getGeoLatitude().length()>HanaDataConstants.LATITUDE_TEXT_SIZE) {
			logger.error("truncating geo latitude of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.LATITUDE_TEXT_SIZE + "! This is probably bad.");
			postData.setGeoLatitude(postData.getGeoLatitude().substring(0, HanaDataConstants.LATITUDE_TEXT_SIZE));
		}
		if (postData.getClient() != null && postData.getClient().length()>HanaDataConstants.CLIENT_TEXT_SIZE) {
			logger.warn("truncating client of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.CLIENT_TEXT_SIZE);
			postData.setClient(postData.getClient().substring(0, HanaDataConstants.CLIENT_TEXT_SIZE));
		}
		if (postData.getInReplyToUserScreenName() != null && postData.getInReplyToUserScreenName().length()>HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE) {
			logger.warn("truncating in_reply_to_user_screenname of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE);
			postData.setInReplyToUserScreenName(postData.getInReplyToUserScreenName().substring(0, HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE));
		}
		if (postData.getGeoPlaceName() != null && postData.getGeoPlaceName().length()>HanaDataConstants.PLNAME_TEXT_SIZE) {
			logger.warn("truncating geo place name of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.PLNAME_TEXT_SIZE);
			postData.setGeoPlaceName(postData.getGeoPlaceName().substring(0, HanaDataConstants.PLNAME_TEXT_SIZE));
		}
		if (postData.getGeoPlaceCountry() != null && postData.getGeoPlaceCountry().length()>HanaDataConstants.PLCOUNTRY_TEXT_SIZE) {
			logger.warn("truncating geo country of " + postData.getSnId()+"-"+postData.getId() + " to " + HanaDataConstants.PLCOUNTRY_TEXT_SIZE);
			postData.setGeoPlaceCountry(postData.getGeoPlaceCountry().substring(0, HanaDataConstants.PLCOUNTRY_TEXT_SIZE));
		}
	}
	/**
	 * 
	 * @description	truncates fields of user data to the maximum length allowed by HANA DB
	 * 
	 * @param 		userData
	 * 
	 */
	private void setUserFieldLength(UserData userData){
		if (userData.getUsername() != null && userData.getUsername().length()>HanaDataConstants.USERNAME_TEXT_SIZE) {
			logger.warn("truncating user name of " + userData.getSnId()+"-"+userData.getId() + " to " + HanaDataConstants.USERNAME_TEXT_SIZE);
			userData.setUsername(userData.getUsername().substring(0, HanaDataConstants.USERNAME_TEXT_SIZE));
		}
		if (userData.getScreenName() != null && userData.getScreenName().length()>HanaDataConstants.USERNICKNAME_TEXT_SIZE) {
			logger.warn("truncating user nick name of " + userData.getSnId()+"-"+userData.getId() + " to " + HanaDataConstants.USERNICKNAME_TEXT_SIZE);
			userData.setScreenName(userData.getScreenName().substring(0, HanaDataConstants.USERNICKNAME_TEXT_SIZE));
		}
		if (userData.getLang() != null && userData.getLang().length()>HanaDataConstants.USERLANG_TEXT_SIZE) {
			logger.error("truncating user lang of " + userData.getSnId()+"-"+userData.getId() + " to " + HanaDataConstants.USERLANG_TEXT_SIZE + "! This is probably bad.");
			userData.setLang(userData.getLang().substring(0, HanaDataConstants.USERLANG_TEXT_SIZE));
		}
		if (userData.getGeoLocation() != null && userData.getGeoLocation().length()>HanaDataConstants.USERLOCATION_TEXT_SIZE) {
			logger.error("truncating user geoLocation of " + userData.getSnId()+"-"+userData.getId() + " to " + HanaDataConstants.USERLOCATION_TEXT_SIZE + "! This is probably bad.");
			userData.setGeoLocation(userData.getGeoLocation().substring(0, HanaDataConstants.USERLOCATION_TEXT_SIZE));
		}
	}
	
	/**
	 * 
	 * @description searches for users or posts in the database and returns true if the dataset
	 * 				exists, or false in any other case - including errors, that is!	
	 * 
	 * @param 		sn_id
	 * 					shortcut of the social network as defined in enum SocialNetworks 
	 * @param 		id
	 * 					id of the user or the post
	 * @param 		type
	 * 					must be set to either post or user
	 * 
	 * @return 		OData entity handler to the object on success (found) or null if not found or in case of ANY error
	 */
	private OEntity returnOEntityHandler(String SN, Long Id, String type) {
		assert (!"user".equals(type) && !"post".equals(type)) : "ERROR :: type must be either \'user\' or \'post\'";
		
		/*
		 *  OEntity-Structure might be useful to find out field dimensions prior inserting dataset 
		 *  	[
		 *  	OProperty [sn_id,				EdmSimpleType [Edm.String], 	TW],
		 *  	OProperty [user_id,				EdmSimpleType [Edm.String], 	2443766328],
		 *  	OProperty [userName,			EdmSimpleType [Edm.String], 	Iesha],
		 *  	OProperty [nickName,			EdmSimpleType [Edm.String], 	iesha785],
		 *  	OProperty [userLang,			EdmSimpleType [Edm.String], 	en],
		 *  	OProperty [geoLocation,			EdmSimpleType [Edm.String], 	null],
		 *  	OProperty [follower,			EdmSimpleType [Edm.Int32], 		261],
		 *  	OProperty [friends,				EdmSimpleType [Edm.Int32], 		1],
		 *  	OProperty [postingsCount,		EdmSimpleType [Edm.Int32], 		209967],
		 *  	OProperty [favoritesCount,		EdmSimpleType [Edm.Int32], 		0],
		 *  	OProperty [listsAndGroupsCount,	EdmSimpleType [Edm.Int32], 		0]
		 *  	]
		 */
		
		//logger.info("searching for "+type+" with id "+SN+"-"+Id + "");
		
		BasicAuthenticationBehavior bAuth = null;
		String _user = null;
		String _pw = null;
		
		String entitySetName = null;
		OEntity theDataset = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		
		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		try {
			logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
			_user = configurationCryptoProvider.decryptValue(this.user);
			_pw = configurationCryptoProvider.decryptValue(this.pass);
			bAuth = new BasicAuthenticationBehavior(_user, _pw);
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			return null;
		}
		
		// looking for user
		if ("user".equals(type)) {
			String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		
			logger.debug("searching for "+type+" " + SN + "-" + Id + " at location " + userURI);
			
			ODataConsumer.Builder userBuilder = ODataConsumer.newBuilder(userURI);
			userBuilder.setClientBehaviors(bAuth);
			userService = userBuilder.build();
			
			entitySetName = "user";
			
			// query for the user by id
			try {
				theDataset = userService.getEntities(entitySetName).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				theDataset = userService.getEntities(entitySetName).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (Exception e) {
				logger.debug("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		// looking for ppst not user
		} else {
			String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
			
			logger.trace("searching for "+type+" " + SN.toString() + "-" + Id + " at location " + postURI);
			
			ODataConsumer.Builder postBuilder = ODataConsumer.newBuilder(postURI);
			postBuilder.setClientBehaviors(bAuth);
			postService = postBuilder.build();
			
			entitySetName = "post";
			
			// query for the post by id
			try {
				theDataset = postService.getEntities(entitySetName).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				theDataset = postService.getEntities(entitySetName).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (Exception e) {
				logger.error("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		// check value of dataSet (for debugging) and return it
		if (theDataset == null)
			logger.info("the " + type + " " + SN.toString() + "-" + Id + " does not exist");
		else 
			logger.debug("found the " + type + " " + SN + "-" + Id);
		
		return theDataset;
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
