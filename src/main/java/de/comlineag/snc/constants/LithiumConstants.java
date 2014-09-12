package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.3		- 10.07.2014
 * @status		productive
 * 
 * @description provides constants for use with the Lithium Network crawler and parser. 
 * 				This class is instantiated by LithiumCrawler and LithiumParser and the
 * 				values are referenced therein to get object values according to the
 * 				Lithium JSON usage pattern. For example, a message reference within the 
 * 				Lithium JSON response is identified by the keyword href. 
 *
 * @changelog	0.1 (Chris)		class created
 * 				0.2 			added DIVIDE_NUMBER_OF_CORES_BY (now obsolete)
 * 				0.3 			changed everything to static
 * 
 */
public final class LithiumConstants {
	// constants for the json response structure
	public static final String JSON_RESPONSE_OBJECT_TEXT				= "response";
	public static final String JSON_STATUS_CODE_TEXT					= "status";
	
	public static final String JSON_ERROR_OBJECT_TEXT					= "error";
	public static final String JSON_ERROR_CODE_TEXT						= "code";
	public static final String JSON_ERROR_MESSAGE_TEXT					= "message";
	
	public static final String JSON_MESSAGES_OBJECT_IDENTIFIER			= "messages";
	public static final String JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER	= "message";
	public static final String JSON_MESSAGE_REFERENCE					= "href";
	
	public static final String JSON_THREADS_OBJECT_IDENTIFIER			= "threads";
	public static final String JSON_SINGLE_THREAD_OBJECT_IDENTIFIER		= "thread";
	
	// these are used within a message to identify the user (aka author of the post)
	public static final String JSON_AUTHOR_OBJECT_IDENTIFIER			= "author";
	public static final String JSON_AUTHOR_REFERENCE					= "href";
	// this is used within the user object itself
	public static final String JSON_USER_OBJECT_IDENTIFIER				= "user";
	
	// path to REST end points
	public static final String REST_MESSAGES_SEARCH_URI					= "/search/messages";
	public static final String REST_USERS_SEARCH_URI					= "/search/users";
	public static final String REST_BOARD_SEARCH_URI					= "/boards/id";
	public static final String REST_THREADS_URI							= "/threads";
	public static final String REST_MESSAGES_URI						= "/messages";
	
	// constants for the search process
	public static final String SEARCH_TERM								= "phrase";
	
	// constants for the http connection
	public static final String HTTP_RESPONSE_FORMAT						= "json";
	public static final String HTTP_RESPONSE_FORMAT_COMMAND				= "restapi.response_format";
	
	// this is a divider. it is used to reduce the maximum number of parallel processes when searching the lithium etwork
	public static final int DIVIDE_NUMBER_OF_CORES_BY					= 2;
}
