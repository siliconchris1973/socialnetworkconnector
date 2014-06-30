package de.comlineag.snc.data;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	1.1
 * 
 * @description provides constants for use with the Lithium Network crawler and parser. 
 * 				This class is instantiated by LithiumCrawler and LithiumParser and the
 * 				values are referenced therein
 *
 * @changelog	1.0 class created
 * 				1.1 added DIVIDE_NUMBER_OF_CORES_BY
 * 
 */
public final class LithiumConstants {
	// constants for the json response structure
	public final String JSON_RESPONSE_OBJECT_TEXT				= "response";
	public final String JSON_STATUS_CODE_TEXT					= "status";
	
	public final String JSON_ERROR_OBJECT_TEXT					= "error";
	public final String JSON_ERROR_CODE_TEXT					= "code";
	public final String JSON_ERROR_MESSAGE_TEXT					= "message";
	
	public final String JSON_MESSAGES_OBJECT_IDENTIFIER			= "messages";
	public final String JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER	= "message";
	public final String JSON_MESSAGE_REFERENCE					= "href";
	
	// these are used within a message to identify the user (aka author of the post)
	public final String JSON_AUTHOR_OBJECT_IDENTIFIER			= "author";
	public final String JSON_AUTHOR_REFERENCE					= "href";
	// this is used within the user object itself
	public final String JSON_USER_OBJECT_IDENTIFIER				= "user";
	
	// path to REST end points
	public final String REST_MESSAGES_SEARCH_URI				= "/search/messages";
	public final String REST_USERS_SEARCH_URI					= "/search/users";
		
	// constants for the search process
	public final String SEARCH_TERM								= "phrase";
	
	// constants for the http connection
	public final String HTTP_RESPONSE_FORMAT					= "json";
	public final String HTTP_RESPONSE_FORMAT_COMMAND			= "restapi.response_format";
	
	// this is a divider. it is used to reduce the maximum number of parallel processes when searching the lithium etwork
	public final int DIVIDE_NUMBER_OF_CORES_BY					= 2;
	
	public LithiumConstants(){}
}
