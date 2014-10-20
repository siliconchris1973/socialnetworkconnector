package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.4				- 21.07.2014
 * @status		productive
 * 
 * @description provides constants for use by the Social Network crawler and parser. 
 * 				The constants herein are used to determine which keys from applicaionContext.xml
 * 				to use and what parameter to pass to the configuration handler
 *
 * @changelog	0.1 (Chris)		class created
 * 				0.2 			changed everything to static
 * 				0.3				added structure elements for customer specific crawler configuration xml
 * 				0.4				removed structure elements for configuration - now in RuntimeConfiguration.xml
 * 
 */
public class ConfigurationConstants {
	// these values are for job details from within applicationContext.xml 
	// connection end points
	public static final String HTTP_ENDPOINT_PROTOCOL_KEY 			= "protocol";
	public static final String HTTP_ENDPOINT_SERVER_URL_KEY 		= "server_url";
	public static final String HTTP_ENDPOINT_PORT_KEY 				= "port";
	public static final String HTTP_ENDPOINT_REST_API_LOC_KEY 		= "rest_api_loc";
	public static final String HTTP_ENDPOINT_GRAPH_API_LOC_KEY 		= "graph_api_loc";
	
	// as the name implies :-)
	public static final String JDBC_ENDPOINT_PORT_KEY 				= "jdbcPort";
	
	// currently only used by SAP HANA
	public static final String ODATA_ENDPOINT_USER_SERVICE_KEY 		= "serviceUserEndpoint";
	public static final String ODATA_ENDPOINT_POST_SERVICE_KEY 		= "serviceUserEndpoint";
	
	// these values are only needed by file-based db systems, such as Neo4J 
	public static final String DB_PATH_KEY 							= "db_path";
	public static final String LOCATION_KEY 						= "geoLocation";
	
	// cryptographic keys for authentication (introduced for twitter api)
	public static final String AUTHENTICATION_CLIENT_ID_KEY 		= "consumerKey";
	public static final String AUTHENTICATION_CLIENT_SECRET_KEY 	= "consumerSecret";
	public static final String AUTHENTICATION_TOKEN_ID_KEY 			= "token";
	public static final String AUTHENTICATION_TOKEN_SECRET_KEY 		= "tokenSecret";
	
	// this is used to define a maximum number of milliseconds to block the open connection to
	// the twitter endpoint, before closing the connection if no tweet is received
	public static final String TWITTER_API_CLIENT_CONNECTIONTIMEOUT_KEY = "connectionTimeout";
	
	
	// cryptographic keys for authentication (introduced for facebook api)
	public static final String AUTHENTICATION_APP_ID_KEY 			= "appId";
	public static final String AUTHENTICATION_APP_SECRET_KEY 		= "appSecret";
	public static final String AUTHENTICATION_ACCESS_TOKEN_KEY 		= "accessToken";
	public static final String AUTHENTICATION_PERMISSIONSET_KEY 	= "permissionSet";
		
	// basic authentication with username and password
	public static final String AUTHENTICATION_USER_KEY 				= "user";
	public static final String AUTHENTICATION_PASSWORD_KEY 			= "passwd";
}
