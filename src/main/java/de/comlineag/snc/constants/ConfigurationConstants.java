package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.1
 * 
 * @description provides constants for use by the Social Network crawler and parser. 
 * 				The constants herein are used to determine which keys from applicaionContext.xml
 * 				to use and what parameter to passto the configuration handler
 *
 * @changelog	0.1 class created
 * 
 */
public class ConfigurationConstants {
	// these values are section names within the configuration db 
	public final String CONSTRAINT_TERM_TEXT				= "term";
	public final String CONSTRAINT_USER_TEXT				= "user";
	public final String CONSTRAINT_LANGUAGE_TEXT			= "language";
	public final String CONSTRAINT_SITE_TEXT				= "site";
	public final String CONSTRAINT_BOARD_TEXT				= "board";
	public final String CONSTRAINT_BLOG_TEXT				= "blog";
	public final String CONSTRAINT_LOCATION_TEXT			= "location";
	
	// these values are for job details from within applicationContext.xml 
	// connection end points
	public final String HTTP_ENDPOINT_PROTOCOL_KEY = "protocol";
	public final String HTTP_ENDPOINT_SERVER_URL_KEY = "server_url";
	public final String HTTP_ENDPOINT_PORT_KEY = "port";
	public final String HTTP_ENDPOINT_REST_API_LOC_KEY = "rest_api_loc";
	public final String HTTP_ENDPOINT_GRAPH_API_LOC_KEY = "graph_api_loc";
	
	public final String JDBC_ENDPOINT_PORT_KEY = "jdbcPort";
	
	public final String ODATA_ENDPOINT_USER_SERVICE_KEY = "serviceUserEndpoint";
	public final String ODATA_ENDPOINT_POST_SERVICE_KEY = "serviceUserEndpoint";
	
	public final String DB_PATH_KEY = "db_path";
	public final String LOCATION_KEY = "location";
	
	// cryptographic keys for authentication (introduced for twitter api)
	public final String AUTHENTICATION_CLIENT_ID_KEY = "consumerKey";
	public final String AUTHENTICATION_CLIENT_SECRET_KEY = "consumerSecret";
	public final String AUTHENTICATION_TOKEN_ID_KEY = "token";
	public final String AUTHENTICATION_TOKEN_SECRET_KEY = "tokenSecret";
	// basic authentication with username and password
	public final String AUTHENTICATION_USER_KEY = "user";
	public final String AUTHENTICATION_PASSWORD_KEY = "passwd";
	
	public ConfigurationConstants(){}
}
