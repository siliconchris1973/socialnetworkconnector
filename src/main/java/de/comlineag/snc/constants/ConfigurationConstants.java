package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.3		- 14.07.2014
 * @status		productive
 * 
 * @description provides constants for use by the Social Network crawler and parser. 
 * 				The constants herein are used to determine which keys from applicaionContext.xml
 * 				to use and what parameter to pass to the configuration handler
 *
 * @changelog	0.1 (Chris)		class created
 * 				0.2 			changed everything to static
 * 				0.3				added structure elements for customer specific crawler configuration xml
 * 
 */
public class ConfigurationConstants {
	// the following are basic terms as constants defining entries n the various xml files
	
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
	
	// basic authentication with username and password
	public static final String AUTHENTICATION_USER_KEY 				= "user";
	public static final String AUTHENTICATION_PASSWORD_KEY 			= "passwd";
	
	
	// these values are section names within the configuration db 
	public static final String CONSTRAINT_TERM_TEXT					= "term";
	public static final String CONSTRAINT_USER_TEXT					= "user";
	public static final String CONSTRAINT_LANGUAGE_TEXT				= "language";
	public static final String CONSTRAINT_SITE_TEXT					= "site";
	public static final String CONSTRAINT_BOARD_TEXT				= "board";
	public static final String CONSTRAINT_BLOG_TEXT					= "blog";
	public static final String CONSTRAINT_LOCATION_TEXT				= "geoLocation";
	
	/* structure of customer specific configuration xml
		<configurations>
			<configuration>
				<customer name="CustomerName">
					<name>
						<type>String</type>
						<value>customer name</value>
					</name>
					<matchcode>
						<type>String</type>
						<value>CN</value>
					</matchcode>
					<entry scope="some used identifier">
						<type>String</type>
						<value>value</value>
					</entry>
					<constraints scope="ALL">
						<constraint>
							<term>
								<type>String</type>
								<value>value</value>
							</term>
						</constraint>
					</constraints>
				</customer>
			</configuration>
		</configurations>
	*/ // XML Schema identifiers
	public static final String rootIdentifier 						= "configurations";
	public static final String singleConfigurationIdentifier 		= "configuration";
	public static final String customerIdentifier 					= "customer";
	public static final String customerNameIdentifier				= "name";
	public static final String customerNameForAllValue 				= "ALL";
	public static final String domainIdentifier 					= "domain";
	public static final String domainNameIdentifier					= "name";
	public static final String domainNameForAllValue 				= "ALL";
	public static final String constraintIdentifier 				= "constraints";
	public static final String scopeIdentifier 						= "scope";
	public static final String scopeOnAllValue 						= "ALL";
	public static final String singleConstraintIdentifier 			= "constraint";
	public static final String valueIdentifier 						= "value";
}
