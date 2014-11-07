package de.comlineag.snc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.sun.jersey.api.client.ClientHandlerException;

import org.joda.time.DateTimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SNCStatusCodes;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.data.PostingData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.helper.StringServices;
import de.comlineag.snc.constants.HanaDataConstants;
import de.comlineag.snc.persistence.JsonFilePersistence;

/**
 *
 * @author 		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Persistence Manager
 * @version 	0.9m				- 22.10.2014
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
 *				0.9j				added support for objectStatus. can be new, old, ok or fail. the field is used by FsCrawler to determine
 *									if an object shall be uploaded to persistence db or not
 *				0.9k				changed access to runtime configuration to non-static
 *				0.9l				changed access to HANA Data configuration to non-static
 *				0.9m				changed id from Long to String
 * 
 * TODO fix error while inserting/updating dataset with SQL
 * TODO establish proper error handling to get the HTTP error code from OData calls
 * TODO enable geoLocation support for users
 * 
 */
public class HANAPersistence implements IPersistenceManager {
	// this holds a reference to the runtime configuration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private final DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	// this is a reference to the HANA configuration settings
	private final HanaConfiguration hco = HanaConfiguration.getInstance();
	
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
	
	public HANAPersistence() {}

	/**
	 * @description save a post from social network to the HANA DB
	 * 
	 * @param	PostingData
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
	public void savePosts(PostingData postingData) {
		logger.trace("savePosts called");
		//logger.trace("    postData content " + postData.getAllContent());
		
		try {
			// first check if the entry already exists
			OEntity theData = returnOEntityHandler(postingData.getSnId(), postingData.getId(), "post");
			
			// truncate all fields to maximum length allotted by HANA
			setPostFieldLength(postingData);
			
			// first check if the dataset is already in the database
			if (theData == null) {
				try{
					// first try to save the data via jdbc, we do this by tryng to load the jdbc driver
					Class.forName(this.dbDriver);
					insertPostWithSQL(postingData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the post
					insertPostWithOData(postingData);
				} 
			// if record exists, update it...
			} else {
				//logger.trace(theData.getEntityKey().toKeyStringWithoutParentheses());
				try{
					// first try to update the data via jdbc
					Class.forName(this.dbDriver);
					updatePostWithSQL(postingData);
				} catch (java.lang.ClassNotFoundException le) {
					// in case the jdbc library is not available, fall back to OData to save the post
					updatePostWithOData(postingData, theData);
				} 
			}
		} catch (ClientHandlerException e) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: could not connect to HANA system " + e.getLocalizedMessage());
			
			if (!rtc.getBooleanValue("CreatePostJsonOnError", "runtime")) {
				logger.debug("insert failed - storing object in backup directory for later processing");
				postingData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
			}
			
			if (rtc.getBooleanValue("StopOnPersistenceFailure", "runtime"))
				System.exit(SNCStatusCodes.FATAL.getErrorCode());
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing post "+postingData.getSnId()+"-"+postingData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
			
			if (rtc.getBooleanValue("CreatePostJsonOnError", "runtime")) {
				logger.debug("insert failed - storing object in backup directory for later processing");
				postingData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
			}
			
			if (rtc.getBooleanValue("StopOnPersistenceFailure", "runtime"))
				System.exit(SNCStatusCodes.FATAL.getErrorCode());
		} 
	}
	
	
	/**
	 * @description	save a user from social network to the HANA DB
	 * @param		UserData
	 */
	public void saveUsers(UserData userData) {
		logger.trace("saveUsers called");
		logger.trace("    userData content " + userData.getAllContent());
		
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
		} catch (ClientHandlerException e) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: could not connect to HANA system " + e.getLocalizedMessage());
			
			if (rtc.getBooleanValue("CreateUserJsonOnError", "runtime")) {
				logger.debug("insert failed - storing object in backup directory for later processing");
				userData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
			
			if (rtc.getBooleanValue("StopOnPersistenceFailure", "runtime"))
				System.exit(SNCStatusCodes.FATAL.getErrorCode());
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing user "+userData.getSnId()+"-"+userData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
			
			if (rtc.getBooleanValue("CreateUserJsonOnError", "runtime")) {
				logger.debug("insert failed - storing object in backup directory for later processing");
				userData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
			
			if (rtc.getBooleanValue("StopOnPersistenceFailure", "runtime"))
				System.exit(SNCStatusCodes.FATAL.getErrorCode());
		}
	}
	
	
	/**
	 * 
	 * @description searches for users or posts in the database and returns an OData handler to it
	 * 				or null in any other case - including errors, that is!	
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
	private OEntity returnOEntityHandler(String SN, String Id, String type) {
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
		
		OEntity theDataset = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		
		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		try {
			logger.trace("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
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
			
			// query for the user by id
			try {
				theDataset = userService.getEntities(type).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				theDataset = userService.getEntities(type).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (Exception e) {
				logger.error("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		// looking for ppst not user
		} else {
			String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
			
			logger.trace("searching for "+type+" " + SN.toString() + "-" + Id + " at location " + postURI);
			
			ODataConsumer.Builder postBuilder = ODataConsumer.newBuilder(postURI);
			postBuilder.setClientBehaviors(bAuth);
			postService = postBuilder.build();
			
			// query for the post by id
			try {
				theDataset = postService.getEntities(type).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				theDataset = postService.getEntities(type).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (Exception e) {
				logger.error("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		// check value of dataSet (for debugging) and return it
		if (theDataset == null)
			logger.debug("the " + type + " " + SN.toString() + "-" + Id + " does not exist");
		else 
			logger.debug("found the " + type + " " + SN + "-" + Id);
		
		return theDataset;
	}
	
	
	
	//
	// the actual methods to insert posts and users
	//
	/**
	 * @description	insert the post with sql
	 * 
	 * @param 		PostingData postData
	 */
	private void insertPostWithSQL(PostingData postingData) {
		logger.info("creating post "+postingData.getSnId()+"-"+postingData.getId());
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postingData.getTruncated()) ? 1 : 0;
		
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
			String sql="INSERT INTO \""+hco.getSCHEMA_NAME()+"\".\""+hco.getPATH_TO_TABLES()+"::"+hco.getPOSTS_TABLE()+"\" "
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
			
			stmt.setString(1, postingData.getDomain());
			stmt.setString(2, postingData.getSnId());
			stmt.setLong(3, new Long(postingData.getId()));
			stmt.setLong(4,new Long(postingData.getUserId()));
			stmt.setTimestamp(5,new Timestamp((postingData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(6, postingData.getLang());
			stmt.setString(7, dataCryptoProvider.encryptValue(postingData.getText()));
			stmt.setString(8, dataCryptoProvider.encryptValue(postingData.getRawText()));
			stmt.setString(9, dataCryptoProvider.encryptValue(postingData.getTeaser()));
			stmt.setString(10, dataCryptoProvider.encryptValue(postingData.getSubject()));
			stmt.setLong(11, postingData.getViewCount());
			stmt.setLong(12, postingData.getFavoriteCount());
			stmt.setString(13, postingData.getClient());
			stmt.setInt(14, truncated);
			stmt.setLong(15, postingData.getInReplyTo());
			stmt.setLong(16, postingData.getInReplyToUser());
			stmt.setString(17, postingData.getInReplyToUserScreenName());
			stmt.setString(18, postingData.getGeoLongitude());
			stmt.setString(19, postingData.getGeoLatitude());
			stmt.setString(20, postingData.getGeoPlaceId());
			stmt.setString(21, postingData.getGeoPlaceName());
			stmt.setString(22, postingData.getGeoPlaceCountry());
			stmt.setString(23, postingData.getGeoAroundLongitude());
			stmt.setString(24, postingData.getGeoAroundLatitude());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("post ("+postingData.getSnId()+"-"+postingData.getId()+") created");
			
			stmt.close() ; conn.close() ;
			
			if (rtc.getBooleanValue("CreatePostJsonOnSuccess", "runtime")) {
				postingData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
			}
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postingData.getSnId()+"-"+postingData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
			/*
			if (rtc.getBooleanValue("CREATE_POST_JSON_ON_ERROR", "runtime")) {
				postData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
			*/
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with "+ dataCryptoProvider.getCryptoProviderName() + ": "+ e.getMessage(), e);
		}
	}
	
	/**
	 * @description	insert the post with OData
	 * 
	 * @param 		PostingData postData
	 */
	private void insertPostWithOData(PostingData postingData){
		logger.info("creating post "+postingData.getSnId()+"-"+postingData.getId());
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postingData.getTruncated()) ? 1 : 0;
		
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
					.properties(OProperties.string("domain", postingData.getDomain()))
					.properties(OProperties.string("sn_id", postingData.getSnId()))
					.properties(OProperties.string("post_id", postingData.getId()))
					.properties(OProperties.string("user_id", postingData.getUserId()))
					.properties(OProperties.datetime("timestamp", postingData.getTimestamp()))
					.properties(OProperties.string("postLang", postingData.getLang()))
					
					.properties(OProperties.string("text", dataCryptoProvider.encryptValue(postingData.getText())))
					.properties(OProperties.string("raw_text", dataCryptoProvider.encryptValue(postingData.getRawText())))
					.properties(OProperties.string("teaser", dataCryptoProvider.encryptValue(postingData.getTeaser())))
					.properties(OProperties.string("subject", dataCryptoProvider.encryptValue(postingData.getSubject())))
					
					.properties(OProperties.int64("viewcount", new Long(postingData.getViewCount())))
					.properties(OProperties.int64("favoritecount", new Long(postingData.getFavoriteCount())))
					
					.properties(OProperties.string("client", postingData.getClient()))
					.properties(OProperties.int32("truncated", new Integer(truncated)))

					.properties(OProperties.int64("inReplyTo", postingData.getInReplyTo()))
					.properties(OProperties.int64("inReplyToUserID", postingData.getInReplyToUser()))
					.properties(OProperties.string("inReplyToScreenName", postingData.getInReplyToUserScreenName()))
					
					.properties(OProperties.string("geoLocation_longitude", postingData.getGeoLongitude()))
					.properties(OProperties.string("geoLocation_latitude", postingData.getGeoLatitude()))
					.properties(OProperties.string("placeID", postingData.getGeoPlaceId()))
					.properties(OProperties.string("plName",  postingData.getGeoPlaceName()))
					.properties(OProperties.string("plCountry", postingData.getGeoPlaceCountry()))
					.properties(OProperties.string("plAround_longitude", postingData.getGeoAroundLongitude()))
					.properties(OProperties.string("plAround_latitude", postingData.getGeoAroundLatitude()))
					
					.execute();
			
			logger.info("post ("+postingData.getSnId()+"-"+postingData.getId()+") created");
			
			if (rtc.getBooleanValue("CreatePostJsonOnSuccess", "runtime")) {
				postingData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
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
			logger.error("EXCEPTION :: could not create post ("+postingData.getSnId()+"-"+postingData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
			
			if (rtc.getBooleanValue("CreatePostJsonOnError", "runtime")) {
				logger.debug("insert failed - storing object in backup directory for later processing");
				postingData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
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
            String sql="INSERT INTO \""+hco.getSCHEMA_NAME()+"\".\""+hco.getPATH_TO_TABLES()+"::"+hco.getUSERS_TABLE()+"\" "
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
			stmt.setString(2, userData.getId());
			stmt.setString(3, dataCryptoProvider.encryptValue(userData.getUsername()));
			stmt.setString(4, dataCryptoProvider.encryptValue(userData.getScreenName()));
			stmt.setString(5, userData.getLang());
			stmt.setLong(6, userData.getFollowersCount());
			stmt.setLong(7, userData.getFriendsCount());
			stmt.setLong(8, userData.getPostingsCount());
			stmt.setLong(9, userData.getFavoritesCount());
			stmt.setLong(10, userData.getListsAndGroupsCount());
			//stmt.setString(11, userData.getGeoLocation());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
			
			if (rtc.getBooleanValue("CreateUserJsonOnSuccess", "runtime")){
				userData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
			/*
			if (rtc.getBooleanValue("CREATE_USER_JSON_ON_ERROR()){
				userData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
			*/
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
					.properties(OProperties.string("user_id", userData.getId()))
					.properties(OProperties.string("userName", dataCryptoProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataCryptoProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
					//.properties(OProperties.string("geoLocation", userData.getGeoLocation().toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGroupsCount())))

					.execute();
			
			logger.info("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") created");
		
			if (rtc.getBooleanValue("CreateUserJsonOnSuccess", "runtime")){
				userData.setObjectStatus("ok");
				
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
			
			if (rtc.getBooleanValue("CreateUserJsonOnError", "runtime")){
				logger.debug("insert failed - storing object in backup directory for later processing");
				userData.setObjectStatus("fail");
				
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
	 * @param 		postingData
	 */
	private void updatePostWithSQL(PostingData postingData) {
		logger.info("updating post "+postingData.getSnId()+"-"+postingData.getId());
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postingData.getTruncated()) ? 1 : 0;
		
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
            String sql="UPDATE \""+hco.getSCHEMA_NAME()+"\".\""+hco.getPATH_TO_TABLES()+"::"+hco.getPOSTS_TABLE()+"\" "
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
					+ "WHERE (\"sn_id\" = ? AND \"post_id\" = ?)";					
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, postingData.getDomain());
			stmt.setLong(2,new Long(postingData.getUserId()));
			stmt.setTimestamp(3,new Timestamp((postingData.getTimestamp().toDateTime(DateTimeZone.UTC)).getMillis() ));
			stmt.setString(4, postingData.getLang());
			stmt.setString(5, dataCryptoProvider.encryptValue(postingData.getText()));
			stmt.setString(6, dataCryptoProvider.encryptValue(postingData.getRawText()));
			stmt.setString(7, dataCryptoProvider.encryptValue(postingData.getTeaser()));
			stmt.setString(8, dataCryptoProvider.encryptValue(postingData.getSubject()));
			stmt.setLong(9, postingData.getViewCount());
			stmt.setLong(10, postingData.getFavoriteCount());
			stmt.setString(11, postingData.getClient());
			stmt.setInt(12, truncated);
			stmt.setLong(13, postingData.getInReplyTo());
			stmt.setLong(14, postingData.getInReplyToUser());
			stmt.setString(15, postingData.getInReplyToUserScreenName());
			stmt.setString(16, postingData.getGeoLongitude());
			stmt.setString(17, postingData.getGeoLatitude());
			stmt.setString(18, postingData.getGeoPlaceId());
			stmt.setString(19, postingData.getGeoPlaceName());
			stmt.setString(20, postingData.getGeoPlaceCountry());
			stmt.setString(21, postingData.getGeoAroundLongitude());
			stmt.setString(22, postingData.getGeoAroundLatitude());
			stmt.setString(23, postingData.getSnId());
			stmt.setLong(24, new Long(postingData.getId()));
			
			logger.trace("SQL: "+sql);
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.debug("post ("+postingData.getSnId()+"-"+postingData.getId()+") updated");
			
			stmt.close() ; conn.close() ;
			
			if (rtc.getBooleanValue("CreatePostJsonOnSuccess", "runtime")){
				postingData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
			}
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available - this should normally not happen, as jdbc is checked before calling this method");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, post ("+postingData.getSnId()+"-"+postingData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
			/*
			if (rtc.getBooleanValue("CREATE_POST_JSON_ON_ERROR()){
				postData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
			*/
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * @description	update post with OData
	 * 
	 * @param 		PostingData postData
	 * @param		OEntity thePostEntity
	 */
	private void updatePostWithOData(PostingData postingData, OEntity thePostEntity){
		logger.info("updating post "+postingData.getSnId()+"-"+postingData.getId());

		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postingData.getTruncated()) ? 1 : 0;
		
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
					.properties(OProperties.string("domain", postingData.getDomain()))
					.properties(OProperties.string("user_id", postingData.getUserId()))
					.properties(OProperties.datetime("timestamp", postingData.getTimestamp()))
					.properties(OProperties.string("postLang", postingData.getLang()))
					
					.properties(OProperties.string("text", dataCryptoProvider.encryptValue(postingData.getText())))
					.properties(OProperties.string("raw_text", dataCryptoProvider.encryptValue(postingData.getRawText())))
					.properties(OProperties.string("teaser", dataCryptoProvider.encryptValue(postingData.getTeaser())))
					.properties(OProperties.string("subject", dataCryptoProvider.encryptValue(postingData.getSubject())))
					
					.properties(OProperties.int64("viewcount", new Long(postingData.getViewCount())))
					.properties(OProperties.int64("favoritecount", new Long(postingData.getFavoriteCount())))
					
					.properties(OProperties.string("client", postingData.getClient()))
					.properties(OProperties.int32("truncated", new Integer(truncated)))

					.properties(OProperties.int64("inReplyTo", postingData.getInReplyTo()))
					.properties(OProperties.int64("inReplyToUserID", postingData.getInReplyToUser()))
					.properties(OProperties.string("inReplyToScreenName", postingData.getInReplyToUserScreenName()))
					
					.properties(OProperties.string("geoLocation_longitude", postingData.getGeoLongitude()))
					.properties(OProperties.string("geoLocation_latitude", postingData.getGeoLatitude()))
					.properties(OProperties.string("placeID", postingData.getGeoPlaceId()))
					.properties(OProperties.string("plName",  postingData.getGeoPlaceName()))
					.properties(OProperties.string("plCountry", postingData.getGeoPlaceCountry()))
					.properties(OProperties.string("plAround_longitude", postingData.getGeoAroundLongitude()))
					.properties(OProperties.string("plAround_latitude", postingData.getGeoAroundLatitude()))
					
					.execute();
			
			logger.debug("post ("+postingData.getSnId()+"-"+postingData.getId()+") updated");
			
			if (rtc.getBooleanValue("CreatePostJsonOnSuccess", "runtime")){
				postingData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
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
			logger.error("EXCEPTION :: could not update post ("+postingData.getSnId()+"-"+postingData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
			
			if (rtc.getBooleanValue("CreatePostJsonOnError", "runtime")){
				logger.debug("update failed - storing object in backup directory for later processing");
				postingData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postingData);
			}
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
            String sql="UPDATE \""+hco.getSCHEMA_NAME()+"\".\""+hco.getPATH_TO_TABLES()+"::"+hco.getUSERS_TABLE()+"\" "
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
			stmt.setLong(8, userData.getListsAndGroupsCount());
			stmt.setString(9, userData.getSnId());
			//stmt.setString(10, userData.getGeoLocation()); // activate above and this geoLocation and increase numbers below by one
			stmt.setString(10, userData.getSnId());
			stmt.setString(11, userData.getId());
			
			@SuppressWarnings("unused")
			int rowCount = stmt.executeUpdate();
			
			logger.info("user "+userData.getScreenName()+" ("+userData.getSnId()+"-"+userData.getId()+") created");
			
			stmt.close() ; conn.close() ;
			
			if (rtc.getBooleanValue("CreateUserJsonOnSuccess", "runtime")){
				userData.setObjectStatus("ok");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
		} catch (java.lang.ClassNotFoundException le) {
			logger.warn("jdbc not available");
		} catch (SQLException le){
			logger.error("EXCEPTION :: JDBC call failed, user ("+userData.getSnId()+"-"+userData.getId()+") not inserted " + le.getLocalizedMessage());
			le.printStackTrace();
			/*
			if (rtc.getBooleanValue("CREATE_USER_JSON_ON_ERROR()){
				userData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
			*/
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
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGroupsCount())))
					
					.execute();
			
			logger.debug("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") updated");
			
			if (rtc.getBooleanValue("CreateUserJsonOnSuccess", "runtime")){
				userData.setObjectStatus("ok");
				
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
			logger.error("ERROR :: Could not update user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
			if (rtc.getBooleanValue("CreateUserJsonOnError", "runtime")){
				logger.debug("update failed - storing object in backup directory for later processing");
				userData.setObjectStatus("fail");
				
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * 
	 * @description	truncates fields of post data to the maximum length allowed by HANA DB
	 * 
	 * @param 		postingData
	 * 
	 */
	private void setPostFieldLength(PostingData postingData){
		if (postingData.getText() != null && postingData.getText().length()>HanaDataConstants.POSTING_TEXT_SIZE) {
			logger.warn("truncating text of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.POSTING_TEXT_SIZE);
			postingData.setText(postingData.getText().substring(0, HanaDataConstants.POSTING_TEXT_SIZE));
			logger.debug("     truncated text now has " + postingData.getText().length() + " characters");
			postingData.setTruncated(true);
		}
		if (postingData.getRawText() != null && postingData.getRawText().length()>HanaDataConstants.POSTING_TEXT_SIZE) {
			logger.warn("truncating raw text of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.POSTING_TEXT_SIZE);
			
			postingData.setRawText((String) StringServices.htmlTruncator(postingData.getRawText().substring(0, (HanaDataConstants.POSTING_TEXT_SIZE-20)), (HanaDataConstants.POSTING_TEXT_SIZE-1)));
			logger.debug("     truncated raw text now has " + postingData.getRawText().length() + " characters");
		}
		if (postingData.getTeaser() != null && postingData.getTeaser().length()>HanaDataConstants.TEASER_TEXT_SIZE) {
			logger.debug("truncating teaser of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.TEASER_TEXT_SIZE);
			postingData.setTeaser(postingData.getTeaser().substring(0, HanaDataConstants.TEASER_TEXT_SIZE));
		}
		if (postingData.getSubject() != null && postingData.getSubject().length()>HanaDataConstants.SUBJECT_TEXT_SIZE) {
			logger.debug("truncating subject of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.SUBJECT_TEXT_SIZE);
			postingData.setSubject(postingData.getSubject().substring(0, HanaDataConstants.SUBJECT_TEXT_SIZE));
		}
		if (postingData.getLang() != null && postingData.getLang().length()>HanaDataConstants.POSTLANG_TEXT_SIZE) {
			logger.warn("truncating language of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.POSTLANG_TEXT_SIZE + "! This is probably bad.");
			postingData.setLang(postingData.getLang().substring(0, HanaDataConstants.POSTLANG_TEXT_SIZE));
		}
		if (postingData.getGeoLongitude() != null && postingData.getGeoLongitude().length()>HanaDataConstants.LONGITUDE_TEXT_SIZE) {
			logger.warn("truncating geo longitude of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.LONGITUDE_TEXT_SIZE + "! This is probably bad.");
			postingData.setGeoLongitude(postingData.getGeoLongitude().substring(0, HanaDataConstants.LONGITUDE_TEXT_SIZE));
		}
		if (postingData.getGeoLatitude() != null && postingData.getGeoLatitude().length()>HanaDataConstants.LATITUDE_TEXT_SIZE) {
			logger.warn("truncating geo latitude of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.LATITUDE_TEXT_SIZE + "! This is probably bad.");
			postingData.setGeoLatitude(postingData.getGeoLatitude().substring(0, HanaDataConstants.LATITUDE_TEXT_SIZE));
		}
		if (postingData.getClient() != null && postingData.getClient().length()>HanaDataConstants.CLIENT_TEXT_SIZE) {
			logger.warn("truncating client of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.CLIENT_TEXT_SIZE);
			postingData.setClient(postingData.getClient().substring(0, HanaDataConstants.CLIENT_TEXT_SIZE));
		}
		if (postingData.getInReplyToUserScreenName() != null && postingData.getInReplyToUserScreenName().length()>HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE) {
			logger.warn("truncating in_reply_to_user_screenname of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE);
			postingData.setInReplyToUserScreenName(postingData.getInReplyToUserScreenName().substring(0, HanaDataConstants.INREPLYTOSCREENNAME_TEXT_SIZE));
		}
		if (postingData.getGeoPlaceName() != null && postingData.getGeoPlaceName().length()>HanaDataConstants.PLNAME_TEXT_SIZE) {
			logger.warn("truncating geo place name of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.PLNAME_TEXT_SIZE);
			postingData.setGeoPlaceName(postingData.getGeoPlaceName().substring(0, HanaDataConstants.PLNAME_TEXT_SIZE));
		}
		if (postingData.getGeoPlaceCountry() != null && postingData.getGeoPlaceCountry().length()>HanaDataConstants.PLCOUNTRY_TEXT_SIZE) {
			logger.warn("truncating geo country of " + postingData.getSnId()+"-"+postingData.getId() + " to " + HanaDataConstants.PLCOUNTRY_TEXT_SIZE);
			postingData.setGeoPlaceCountry(postingData.getGeoPlaceCountry().substring(0, HanaDataConstants.PLCOUNTRY_TEXT_SIZE));
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
