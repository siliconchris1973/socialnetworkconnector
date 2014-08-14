package de.comlineag.snc.persistence;

import com.sun.jersey.api.client.ClientHandlerException;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.ConfigurationCryptoHandler;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.persistence.JsonFilePersistence;

/**
 *
 * @author 		Christian Guenther
 * @category 	Persistence Manager
 * @version 	0.1					- 06.08.2014
 * @status		in development
 *
 * @description handles the connectivity to a Riak-DB ring and saves and updates posts and users in the ring
 * 
 * @changelog	0.1 (Chris)			copy from HANAPersistence 0.9i and reduced to skeleton
 * 
 */
public class RiakDbPersistence implements IPersistenceManager {
	
	// Servicelocation taken from applicationContext.xml
	private String host;
	private String port;
	private String protocol;
	private String location;
	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Rest client endpoints
	private static String userService;
	private static String postService;
		
	// Credentials
	private String user;
	private String pass;
	
	// data sets
	private Long Id;
	private SocialNetworks SN;
	
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();

	
	public RiakDbPersistence() {
		//@SuppressWarnings("unused")
		//final RiakDbConfiguration couchDbConfig = new RiakDbConfiguration();
	}

	/**
	 * @description save a post from social network to the Riak-DB ring
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
			//OEntity theData = returnOEntityHandler(postData.getSnId(), postData.getId(), "post");
			String theData = null;
			
			// first check if the dataset is already in the database
			if (theData == null) {
				insertPost(postData);
			// if record exists, update it...
			} else {
				updatePost(postData, theData);
			}
		} catch (ClientHandlerException e) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: could not connect to Riak-DB ring " + e.getLocalizedMessage());
			if (RuntimeConfiguration.isSTOP_SNC_ON_PERSISTENCE_FAILURE())
				System.exit(-1);
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing post "+postData.getSnId()+"-"+postData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
			if (RuntimeConfiguration.isSTOP_SNC_ON_PERSISTENCE_FAILURE())
				System.exit(-1);
		}
	}
	
	
	/**
	 * @description	save a user from social network to the Riak-DB ring
	 * @param		UserData
	 */
	public void saveUsers(UserData userData) {
		
		try {
			// first check if the entry already exists
			//OEntity theData = returnOEntityHandler(userData.getSnId(), userData.getId(), "user");
			String theData = null;
			
			if (theData == null) {
				insertUser(userData);
			} else {
				updateUser(userData, theData); 
			}
		} catch (ClientHandlerException e) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: could not connect to Riak-DB ring " + e.getLocalizedMessage());
			if (RuntimeConfiguration.isSTOP_SNC_ON_PERSISTENCE_FAILURE())
				System.exit(-1);
		} catch (Exception le) {
			// catch any remaining exceptions and make sure the client (in case of twitter) is closed - done within TwitterCrawler
			logger.error("EXCEPTION :: unforseen error condition processing user "+userData.getSnId()+"-"+userData.getId()+": " + le.getLocalizedMessage());
			le.printStackTrace();
			if (RuntimeConfiguration.isSTOP_SNC_ON_PERSISTENCE_FAILURE())
				System.exit(-1);
		}
	}
	
	
	/**
	 * 
	 * @description searches for users or posts in the database and returns a REST handler to it
	 * 				or null in any other case - including errors, that is!	
	 * 
	 * @param 		sn_id
	 * 					shortcut of the social network as defined in enum SocialNetworks 
	 * @param 		id
	 * 					id of the user or the post
	 * @param 		type
	 * 					must be set to either post or user
	 * 
	 * @return 		Rest handler to the object on success (found) or null if not found or in case of ANY error
	 */
	private String returnRestHandler(String SN, Long Id, String type) {
		assert (!"user".equals(type) && !"post".equals(type)) : "ERROR :: type must be either \'user\' or \'post\'";
		
		//logger.info("searching for "+type+" with id "+SN+"-"+Id + "");
		String _user = null;
		String _pw = null;
		
		String theDataset = null;
		
		String baseLocation = new String(this.protocol + "://" + this.host + ":" + this.port + this.location);
		
		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		try {
			logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
			_user = configurationCryptoProvider.decryptValue(this.user);
			_pw = configurationCryptoProvider.decryptValue(this.pass);
			
		} catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			return null;
		}
		
		// looking for user
		if ("user".equals(type)) {
			String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		
			logger.debug("searching for "+type+" " + SN + "-" + Id + " at location " + userURI);
			
			// query for the user by id
			try {
				//theDataset = userService.getEntities(type).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				//theDataset = userService.getEntities(type).filter("user_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (Exception e) {
				logger.debug("EXCEPTION :: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		// looking for ppst not user
		} else {
			String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
			
			logger.trace("searching for "+type+" " + SN.toString() + "-" + Id + " at location " + postURI);
			
			// query for the post by id
			try {
				//theDataset = postService.getEntities(type).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
			} catch (RuntimeException e) {
				logger.warn("runtime exception while searching for " + type + " " + SN + "-" + Id + " ... retrying");
				//theDataset = postService.getEntities(type).filter("post_id eq '"+Id+"' and sn_id eq '" + SN.toString() + "'").top(1).execute().firstOrNull();
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
	
	
	
	//
	// the actual methods to insert posts and users
	//
	/**
	 * @description	insert the post
	 * 
	 * @param 		PostData postData
	 */
	private void insertPost(PostData postData){
		logger.info("creating post "+postData.getSnId()+"-"+postData.getId());
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		// in case the connection is NOT opened already, attempt to create an Rest Consumer
		if (postService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				
				//BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
				logger.debug("Initiating connection to service endpoint "+postURI+" of the Riak-DB ring");
				
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the Rest statement and execute it against the connection endpoint
		@SuppressWarnings("unused")
		String newPost = null;
		try {
			/*
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
			*/
			logger.info("post ("+postData.getSnId()+"-"+postData.getId()+") created");
			
			if (RuntimeConfiguration.isCREATE_POST_JSON_ON_SUCCESS()) {
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
			
		/*
		 * in case of an error during post, the following XML structure is returned as part of the exception:
		 * 		
		 */
		} catch (RuntimeException e) {
			logger.error("EXCEPTION :: could not create post ("+postData.getSnId()+"-"+postData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
			
			if (RuntimeConfiguration.isCREATE_POST_JSON_ON_ERROR()) {
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(postData);
			}
		} /*catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}*/
	}
	
	
	/**
	 * @description	insert user 
	 * 
	 * @param 		UserData userData
	 */
	private void insertUser(UserData userData){
		logger.info("creating user "+userData.getSnId()+"-"+userData.getId());
		
		if (userService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
					
				logger.debug("Initiating connection to service endpoint "+userURI+" of the Riak-DB ring");
				
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt value for user/passwd with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.toString(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		@SuppressWarnings("unused")
		String newUser = null;
		try {
			/*
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

			 */
			logger.info("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") created");
		
			if (RuntimeConfiguration.isCREATE_USER_JSON_ON_SUCCESS()){
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
			
			if (RuntimeConfiguration.isCREATE_USER_JSON_ON_ERROR()){
				// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
				@SuppressWarnings("unused")
				JsonFilePersistence failsave = new JsonFilePersistence(userData);
			}
		} /*catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}*/
	}
	
	
	//
	// the actual methods to update posts and users
	//
	/**
	 * @description	update post with Rest
	 * 
	 * @param 		PostData postData
	 * @param		OEntity thePostEntity
	 */
	private void updatePost(PostData postData, String thePostEntity){
		logger.info("updating post "+postData.getSnId()+"-"+postData.getId());

		// this is just example code to show, how to interact with the CryptoProvider enum
		//String desiredStrength = "low";
		//CryptoProvider cryptoProviderToUse = CryptoProvider.getCryptoProvider(desiredStrength);
		//logger.trace("determined " + cryptoProviderToUse.getName() + " to be the best suited provider for desired strength " + desiredStrength);
		
		// static variant to set the truncated flag - which is not used anyway at the moment
		int truncated = (postData.getTruncated()) ? 1 : 0;
		
		// in case the connection is NOT opened already, attempt to create an Rest Consumer
		if (postService == null) {
			try {
				logger.debug("decrypting authorization details from job control with " + configurationCryptoProvider.getCryptoProviderName());
				String _user = configurationCryptoProvider.decryptValue(this.user);
				String _pw = configurationCryptoProvider.decryptValue(this.pass);
				
				String baseLocation = new String(this.protocol+"://" + this.host + ":" + this.port + this.location);
				String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);
				logger.debug("Initiating connection to service endpoint "+postURI+" of the Riak-DB ring");
				
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.servicePostEndpoint);
		
		// now build the Rest statement and execute it against the connection endpoint
		try {
			/* 
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
					
			*/
			
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
		} /*catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		}*/
	}
	
	
	/**
	 * @description	update user with Rest
	 * 
	 * @param 		UserData userData
	 * @param 		OEntity theUserEntity 
	 */
	private void updateUser(UserData userData, String theUserEntity){
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
					
				logger.debug("Initiating connection to service endpoint "+userURI+" of the Riak-DB ring");
				
			} catch (GenericCryptoException e) {
				logger.error("EXCEPTION :: could not decrypt user and/or password with " + configurationCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
			}
		}
		logger.debug("connected to service endpoint " + this.protocol+"://" + this.host + ":" + this.port + this.location + "/" + this.serviceUserEndpoint);
		
		try {
			/*
					.properties(OProperties.string("userName", dataCryptoProvider.encryptValue(userData.getUsername())))

					.properties(OProperties.string("nickName", dataCryptoProvider.encryptValue(userData.getScreenName())))
					.properties(OProperties.string("userLang", userData.getLang()))
			//		.properties(OProperties.string("geoLocation", userData.getGeoLocation().toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))
					
			*/
			logger.info("user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+") updated");
		
		/*
		 * in case of an error during post, the following XML structure is returned as part of the exception:
		 
		 */
		} catch (RuntimeException e) {
			logger.error("ERROR :: Could not update user " + userData.getUsername() + " ("+userData.getSnId()+"-"+userData.getId()+"): " + e.getLocalizedMessage());
			e.printStackTrace();
		} /*catch (GenericCryptoException e) {
			logger.error("EXCEPTION :: could not on-the-fly encrypt data with " + dataCryptoProvider.getCryptoProviderName() + ": " + e.getMessage(), e);
		} */
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
	
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
}
